package se.ltu.alb.equip.score;

import se.ltu.alb.equip.model.*;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import ch.rfin.util.Pair;
import ch.rfin.util.Pairs;
import ch.rfin.util.Bits;
import static ch.rfin.util.Predicates.compose;
import static ch.rfin.util.Predicates.nonNull;
import static ch.rfin.util.Predicates.greaterThan;
import static ch.rfin.util.IntFunctions.ceil;
import static ch.rfin.util.IntFunctions.decBy;
import static ch.rfin.util.Functions.max;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.counting;

/**
 * Computes various score features that are useful for EqALB planning.
 * There are two ways to interpret the concept of "number of stations used".
 * One is to literally count the number of stations that have at least one
 * task assigned to them.
 * The other is to enforce that tasks are assigned to a contiguous block of
 * stations, starting with the first one.
 * Either one can be used, but the former is more lax and implies that gaps
 * formed by unused stations between non-empty stations don't matter.
 * This is the case when there are no resource/equipment requirements and
 * a solution with gaps can easily be converted to a solution without gaps.
 * <p>
 * TODO: Need to update this with regard to equipment.
 * What implications does equipment have for the concept of "used" stations,
 * and for compaction?
 *
 * @author Christoffer Fink
 */
public class ScoreFeatures {

    /**
     * Counts the number of times a task is assigned to a station that is not
     * equipped with the necessary equipment required by the task.
     * Note that the same task may contribute more than once (if it requires
     * more multiple types of equipment that are not available).
     */
    public static int countMissingEquipment(AssemblyPlan plan) {
        Map<Station, Set<Integer>> stationEquipment = plan
            .equipment()
            .stream()
            .filter(equip -> equip.equippedAt() != null)
            .collect(groupingBy(Equipment::equippedAt,
                        mapping(Equipment::type,
                            toSet())));
        return (int) plan.tasks()
            .stream()
            .filter(task -> task.station() != null)
            .mapToLong(task ->
                    task.equipmentDependencies()
                    .stream()
                    .filter(type ->
                        stationEquipment.get(task.station()) == null
                        || !stationEquipment.get(task.station()).contains(type))
                    .count())
            .sum();
    }

    /**
     * Sum up the price for all used equippment.
     * That is, the price for all equipment that has been equipped at a
     * station.
     * Note that "equipment cost" is mean to refer to the cost of acquiring
     * and installing equipment. The way the problem is modelled, unequipped
     * equipment is merely hypothetical or potential equipment. It hasn't been
     * purchased and is now underutilized.
     */
    public static int totalEquipmentCost(AssemblyPlan plan) {
        return plan
            .equipment()
            .stream()
            .filter(equip -> equip.equippedAt() != null)
            .mapToInt(Equipment::price)
            .sum();
    }

    /**
     * Sum up the move cost for all moved equippment.
     * Note that it is not necessary for the initial station to be non-null.
     * It is a matter of modeling the particular variation of the problem to
     * decide whether an initially assigned equipment should have a move cost
     * or not. It would probably be typical for an initially unequipped
     * equipment to have 0 move cost, because the whole point is to (at least
     * potentially) equip it, and that doesn't count as "moving it", since it
     * would be more like "installing" it.
     * However, in atypical cases, unequipped may mean that the equipment
     * *actually* exists, and having to go through the trouble of installing
     * it if not absolutely necessary is indeed considered a cost to be avoided.
     * In other words, it's up to the user modeling the problem to make the
     * initial station null or not, and to make the move cost nonzero or not.
     */
    public static int totalEquipmentMoveCost(AssemblyPlan plan) {
        return plan
            .equipment()
            .stream()
            // Note: Not filtering on whether initiallyAt is null
            .filter(equip -> equip.initiallyAt() != equip.equippedAt())
            .mapToInt(Equipment::moveCost)
            .sum();
    }

    /**
     * Compute a lower bound on the minimum cycle time.
     * Just as with SALBP, two possible lower bounds are the maximum task time
     * and the total task time averaged over all stations.
     * However, with EqALB, there are some additional factors. Now we can also
     * look at the subsets of tasks and stations that have certain equipment.
     * For example, let's say there two instances of one type of equipment.
     * All tasks that require this type of equipment must necessarily be
     * assigned to the stations that have that equipment. So now we can do the
     * same kind of calculation for each equipment type.
     */
    public static int lowerBoundCycleTime(AssemblyPlan plan) {
        final int stations = plan.stations().size();
        final int maxTaskTime = plan
            .tasks()
            .stream()
            .mapToInt(Task::time)
            .max()
            .getAsInt();  // Allow to throw. There has to be at least one!
        final double totalTaskTime = plan
            .tasks()
            .stream()
            .mapToInt(Task::time)
            .sum();
        final Map<Integer,Long> typeCounts = plan.equipment().stream()
            .collect(groupingBy(Equipment::type, counting()));
        // This is an optimistic estimate (not a tight lower bound) because
        // it doesn't take into account that tasks may depend on more than
        // one type of equipment, which would narrow down the number of
        // compatible stations.
        int maxAvgEqLoad = 0;
        for (final int type : typeCounts.keySet()) {
            // The total task time of tasks requiring the equipment type.
            final double totalTimeRequiring = plan.tasks().stream()
                .filter(task -> task.equipmentDependencies().contains(type))
                .mapToDouble(Task::time)
                .sum();
            final int avg = ceil(totalTimeRequiring / typeCounts.get(type));
            maxAvgEqLoad = max(maxAvgEqLoad, avg);
        }
        final int avgLoad = ceil(totalTaskTime / stations);
        return max(avgLoad, maxTaskTime, maxAvgEqLoad);
    }

    private static Collection<Station> compatibleStations(Map<Station,Bits> stations, Bits req) {
        return Pairs.stream(stations)
            .filter(Pairs.test_2(req::subsetOf))
            .map(Pair::get_1)
            .collect(toSet());
    }

    /**
     * Count how many of the equipped sets satisfy the equipment requirement.
     * Each bit set in the collection represents the set of equipment types
     * available at one station. The other bit set represents the requirements
     * of one task.
     */
    private static int countCompatible(Collection<Bits> stations, Bits req) {
        return (int) stations.stream()
            .filter(req::subsetOf)
            .count();
    }

    // ----- Same as SALBP -----

    /**
     * Counts the number of times a task is assigned to a station at or
     * before an immediate dependency.
     * In other words, this feature is used to enforce that a task is
     * performed strictly before any of the tasks it depends on.
     */
    public static int countDependencyInversionsStrict(AssemblyPlan plan) {
        int count = 0;
        for (final var task : plan.tasks()) {
            if (task.station() == null) {
                continue;
            }
            final int station = task.station().number();
            for (final var dep : task.taskDependencies()) {
                if (dep.station() == null) {
                    count++;
                } else if (dep.station().number() >= station) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Counts the number of times a task is assigned to a station
     * before an immediate dependency.
     * In other words, this feature is used to enforce that a task is
     * performed before or at the same station as the tasks it depends on.
     * Dependencies that have not been assigned to a station (station is null)
     * are treated as unsatisfied and count as an inversion.
     */
    public static int countDependencyInversions(AssemblyPlan plan) {
        int count = 0;
        for (final var task : plan.tasks()) {
            if (task.station() == null) {
                continue;
            }
            final int station = task.station().number();
            for (final var dep : task.taskDependencies()) {
                if (dep.station() == null) {
                    count++;
                } else if (dep.station().number() > station) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Counts the number of times a task is assigned to a station
     * before any dependency, whether deep (transitive) or shallow (direct).
     * In other words, this feature is used to enforce that a task is
     * performed before or at the same station as all the tasks it depends
     * on, even if indirectly (transitively).
     */
    public static int countDeepDependencyInversions(AssemblyPlan plan) {
        int count = 0;
        for (final var task : plan.tasks()) {
            if (task.station() == null) {
                continue;
            }
            final int station = task.station().number();
            for (final var dep : task.deepDependencies()) {
                if (dep.station() == null) {
                    count++;
                } else if (dep.station().number() > station) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Sum up the distances (in terms of station numbers) from each task
     * to its immediate dependencies.
     * If a task is assigned to a station immediately before one of its
     * (immediate) dependencies, then the distance would be 1.  If a task is
     * assigned to station number 3, and a task it depends on (immediately) is
     * assigned to station number 7, then the distance would be 4.
     */
    public static int totalDependencyDistance(AssemblyPlan plan) {
        int distance = 0;
        for (final var task : plan.tasks()) {
            if (task.station() == null) {
                continue;
            }
            final int station = task.station().number();
            for (final var dep : task.taskDependencies()) {
                if (dep.station() == null) {
                    continue;
                } else {
                    final int nr = dep.station().number();
                    if (nr > station) {
                        distance += nr - station;
                    }
                }
            }
        }
        return distance;
    }

    /**
     * Like {@link #totalDependencyDistance(AssemblyPlan)}, but
     * also considers transitive dependencies.
     */
    public static int totalDeepDependencyDistance(AssemblyPlan plan) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /**
     * Count the number of stations that have tasks assigned to them, ignoring
     * any gaps and leading/trailing strings of empty stations.
     * For [__|1,2|__|__|3|4,5|__] that would be 3.
     * This could be what matters for type 1 planning, if gaps can be ignored
     * because the solution could simply be compacted to remove the gaps.
     */
    public static int countUsedStations(AssemblyPlan plan) {
        return (int) plan.tasks()
            .stream()
            .map(Task::station)
            .filter(nonNull())
            .distinct()
            .count();
    }

    /**
     * Count the number of unused stations, including gaps and leading/trailing
     * strings of empty stations.
     * For [__|1,2|__|__|3|4,5|__] that would be 4.
     */
    public static int countUnusedStations(AssemblyPlan plan) {
        return plan.stations().size() - countUsedStations(plan);
    }

    /**
     * Compute the number of stations used, including any gaps.
     * Almost computes the highest used station number, except the actual
     * numbers don't matter.
     * This is probably what matters for type 1 planning.
     * For [__|1,2|__|__|3|4,5|__] that would be 6.
     */
    public static int totalStationsUsed(AssemblyPlan plan) {
        final int minStationNr = plan
            .stations()
            .stream()
            .mapToInt(Station::number)
            .min()
            .getAsInt();  // Allow to throw. There has to be at least one!
        final int maxUsedNr = plan
            .tasks()
            .stream()
            .map(Task::station)
            .filter(nonNull())
            .mapToInt(Station::number)
            .max()
            .orElse(minStationNr - 1);
        return maxUsedNr - minStationNr + 1;
    }

    /**
     * Count the number of empty stations that have nonempty stations after.
     * For [__|1,2|__|__|3|4,5|__] that would be 3.
     */
    public static int countStationGaps(AssemblyPlan plan) {
        return totalStationsUsed(plan) - countUsedStations(plan);
    }

    /**
     * Compute the current cycle time -- the maximum station load.
     */
    public static int cycleTime(AssemblyPlan plan) {
        return plan
            .tasks()
            .stream()
            .filter(compose(nonNull(), Task::station))
            .collect(groupingBy(Task::station, summingInt(Task::time)))
            .values()
            .stream()
            .mapToInt(Integer::intValue)
            .max().orElse(0);
    }

    /**
     * Sum up the squared total load of each station.
     * This is a measure of how evenly tasks are distributed.
     */
    public static long sumSquaredStationLoads(AssemblyPlan plan) {
        return plan
            .tasks()
            .stream()
            .filter(compose(nonNull(), Task::station))
            .collect(groupingBy(Task::station, summingInt(Task::time)))
            .values()
            .stream()
            .mapToInt(i -> i*i)
            .sum();
    }

    /**
     * Count the number of stations with a load that exceeds the cycle time.
     * Note that a missing cycle time is ignored.  That means if the plan has
     * no target cycle time set, there will be 0 violations.
     */
    public static int countCycleTimeViolations(AssemblyPlan plan) {
        if (plan.cycleTime().isEmpty()) {
            return 0;
        }
        final int cycleTime = plan.cycleTime().getAsInt();
        return (int) plan
            .tasks()
            .stream()
            .filter(compose(nonNull(), Task::station))
            .collect(groupingBy(Task::station, summingInt(Task::time)))
            .values()
            .stream()
            .filter(greaterThan(cycleTime))
            .count();
    }

    /**
     * Sum up the amount by which stations exceed the cycle time.
     * Note that a missing cycle time is ignored.  That means if the plan has
     * no target cycle time set, there will be 0 violations.
     */
    public static int totalExcessLoad(AssemblyPlan plan) {
        if (plan.cycleTime().isEmpty()) {
            return 0;
        }
        final int cycleTime = plan.cycleTime().getAsInt();
        return plan
            .tasks()
            .stream()
            .filter(compose(nonNull(), Task::station))
            .collect(groupingBy(Task::station, summingInt(Task::time)))
            .values()
            .stream()
            .filter(greaterThan(cycleTime))
            .mapToInt(Integer::intValue)
            .map(decBy(cycleTime))
            .sum();
    }

    /**
     * Compute an upper on the minimum cycle time.
     */
    public static int upperBoundCycleTime(AssemblyPlan plan) {
        // Clone the plan. Then use a custom construction heuristic to generate
        // a feasible solution. Return the number of stations it uses.
        throw new UnsupportedOperationException("Not implemented.");
    }

    /**
     * Compute a lower bound on the minimum number of stations.
     */
    public static int lowerBoundStations(AssemblyPlan plan) {
        double totalTaskTime = plan
            .tasks()
            .stream()
            .mapToInt(Task::time)
            .sum();
        int cycleTime = plan.cycleTime().getAsInt();
        double stations = totalTaskTime / cycleTime;
        return (int) Math.ceil(stations);
    }

    /**
     * Compute an upper on the minimum number of stations.
     */
    public static int upperBoundStations(AssemblyPlan plan) {
        // Clone the plan. Then use a custom construction heuristic to generate
        // a feasible solution. Return the number of stations it uses.
        throw new UnsupportedOperationException("Not implemented.");
    }

}
