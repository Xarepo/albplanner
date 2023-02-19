package se.ltu.alb.salbp.score;

import se.ltu.alb.salbp.model.*;
import static ch.rfin.util.Predicates.compose;
import static ch.rfin.util.Predicates.nonNull;
import static ch.rfin.util.Predicates.greaterThan;
import static ch.rfin.util.IntFunctions.decBy;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;

/**
 * Computes various score features that are useful for SALBP-1/2 planning.
 * There are two ways to interpret the concept of "number of stations used".
 * One is to literally count the number of stations that have at least one
 * task assigned to them.
 * The other is to enforce that tasks are assigned to a contiguous block of
 * stations, starting with the first one.
 * Either one can be used, but the former is more lax and implies that gaps
 * formed by unused stations between non-empty stations don't matter.
 * This is the case when there are no resource/equipment requirements and
 * a solution with gaps can easily be converted to a solution without gaps.
 *
 * @author Christoffer Fink
 */
public class ScoreFeatures {

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
        return countDependencyInversions(plan, true);
    }

    public static int countDependencyInversionsIgnoringNulls(AssemblyPlan plan) {
        return countDependencyInversions(plan, false);
    }

    public static int countDependencyInversions(AssemblyPlan plan, boolean countNull) {
        int count = 0;
        for (final var task : plan.tasks()) {
            if (task.station() == null) {
                continue;
            }
            final int station = task.station().number();
            for (final var dep : task.taskDependencies()) {
                if (dep.station() == null && countNull) {
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
            .map(Step::station)
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
            .map(Step::station)
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
            .filter(compose(nonNull(), Step::station))
            .collect(groupingBy(Step::station, summingInt(Step::time)))
            .values()
            .stream()
            .mapToInt(Integer::intValue)
            .max().getAsInt();
    }

    /**
     * Sum up the squared total load of each station.
     * This is a measure of how unevenly tasks are distributed.
     */
    public static long sumSquaredStationLoads(AssemblyPlan plan) {
        final long sum = plan
            .tasks()
            .stream()
            .filter(compose(nonNull(), Step::station))
            .collect(groupingBy(Step::station, summingInt(Step::time)))
            .values()
            .stream()
            .mapToLong(i -> i*i)
            .sum();
        return (sum < 0)
            ? Long.MAX_VALUE
            : sum;
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
            .filter(compose(nonNull(), Step::station))
            .collect(groupingBy(Step::station, summingInt(Step::time)))
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
            .filter(compose(nonNull(), Step::station))
            .collect(groupingBy(Step::station, summingInt(Step::time)))
            .values()
            .stream()
            .filter(greaterThan(cycleTime))
            .mapToInt(Integer::intValue)
            .map(decBy(cycleTime))
            .sum();
    }

    /**
     * Compute a lower bound on the minimum cycle time.
     * Returns the max of maximum task time and total task time averaged
     * over all stations.
     */
    public static int lowerBoundCycleTime(AssemblyPlan plan) {
        final int stations = plan.stations().size();
        final int maxTaskTime = plan
            .tasks()
            .stream()
            .mapToInt(Step::time)
            .max()
            .getAsInt();  // Allow to throw. There has to be at least one!
        final double totalTaskTime = plan
            .tasks()
            .stream()
            .mapToInt(Step::time)
            .sum();
        final int avgLoad = (int) Math.ceil(totalTaskTime / stations);
        return Math.max(avgLoad, maxTaskTime);
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
            .mapToInt(Step::time)
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
