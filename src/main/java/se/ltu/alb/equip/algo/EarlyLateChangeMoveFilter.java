package se.ltu.alb.equip.algo;

import java.util.*;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import org.optaplanner.core.impl.heuristic.selector.move.generic.ChangeMove;
import org.optaplanner.core.api.score.director.ScoreDirector;
import se.ltu.alb.equip.model.AssemblyPlan;
import se.ltu.alb.equip.model.Station;
import se.ltu.alb.equip.model.Task;
import se.ltu.alb.equip.algo.CompatibleStationFilter;
import ch.rfin.util.Pair;
import ch.rfin.util.Bits;
import static ch.rfin.util.Functions.max;
import static ch.rfin.util.Functions.min;
import static ch.rfin.util.IntFunctions.ceil;
import static ch.rfin.util.Misc.add;
import static ch.rfin.util.Misc.increment;

/**
 * Value selection filter that only allows tasks to be assigned to a range of
 * (earliest/latest) <strong>compatible</strong> stations.
 * Note that this filter is composed with a
 * {@link se.ltu.alb.equip.algo.CompatibleStationFilter}
 * so that only stations with the necessary installed equipment are accepted.
 * <p>
 * The earliest/latest station range is based on required and available equipment.
 * Task times and maximum cycle time are currently NOT considered.
 * @author Christoffer Fink
 */
public class EarlyLateChangeMoveFilter implements SelectionFilter<AssemblyPlan, ChangeMove<AssemblyPlan>> {

    private Map<Integer, Pair<Integer, Integer>> stationIntervals = null;
    private State state = null;
    private boolean initialized = false;
    private CompatibleStationFilter compStatFilter = new CompatibleStationFilter();

    public double marginFactor = 1.1;

    protected void init(AssemblyPlan plan) {
        if (initialized) {
            return;
        }
        state = State.init(plan);
        stationIntervals = stationBounds(plan, marginFactor, state);
        initialized = true;
    }

    @Override
    public boolean accept(ScoreDirector<AssemblyPlan> director, ChangeMove<AssemblyPlan> move) {
        return accept(director.getWorkingSolution(), move);
    }

    public boolean accept(AssemblyPlan plan, ChangeMove<AssemblyPlan> move) {
        final Task task = (Task) move.getEntity();
        final Station station = (Station) move.getToPlanningValue();
        return compStatFilter.accept(plan, move) && accept(plan, task, station);
    }

    public boolean accept(AssemblyPlan plan, Task task, Station station) {
        return accept(plan, task.id(), station.number());
    }

    public boolean accept(AssemblyPlan plan, int taskId, int stationNr) {
        init(plan);
        final var interval = stationIntervals.get(taskId);
        final int min = interval._1;
        final int max = interval._2;
        return stationNr >= min && stationNr <= max;
    }


    // NOTE: This version ignores the cycle time! Uses only equipment dependencies.
    public static int earliestStation(int taskId, int cycleTime, State state, Map<Integer, Integer> memory) {
        if (memory.containsKey(taskId)) {
            return memory.get(taskId);
        }
        int earliestPredecessorStation = state.taskPredecessors.get(taskId)
            .stream()
            .map(pred -> earliestStation(pred, cycleTime, state, memory))
            .max()
            .orElse(0); // No predecessors ⇒ can use the first station
        Bits req = state.taskToEquipments.get(taskId);
        int minCompatible = req.isEmpty()
            ? 0
            : state.compatibleStations(req).min().getAsInt();
        int earliestStation = max(earliestPredecessorStation, minCompatible);
        memory.put(taskId, earliestStation);
        return earliestStation;
    }

    // NOTE: This version ignores the cycle time! Uses only equipment dependencies.
    public static int latestStation(int taskId, int cycleTime, double factor, State state, Map<Integer, Integer> memory) {
        if (memory.containsKey(taskId)) {
            return memory.get(taskId);
        }
        int latestSuccessorStation = state.taskSuccessors.get(taskId)
            .stream()
            .map(suc -> latestStation(suc, cycleTime, factor, state, memory))
            .min()
            .orElse(state.stations); // No successors ⇒ can use the last station
        Bits req = state.taskToEquipments.get(taskId);
        int maxCompatible = req.isEmpty()
            ? state.stations
            : state.compatibleStations(req).max().getAsInt();
        int latestStation = min(latestSuccessorStation, maxCompatible);
        memory.put(taskId, latestStation);
        return latestStation;
    }

    public static Map<Integer, Pair<Integer,Integer>> stationBounds(AssemblyPlan plan, State state) {
        return stationBounds(plan, 1.0, state); // XXX: factor does nothing
    }

    public static Map<Integer, Pair<Integer,Integer>> stationBounds(AssemblyPlan plan, double factor, State state) {
        final Collection<Task> tasks = plan.tasks();
        final int cycleTime = computeCycleTime(plan);
        Map<Integer, Pair<Integer,Integer>> intervals = new HashMap<>();
        Map<Integer, Integer> memoryEarly = new HashMap<>();
        Map<Integer, Integer> memoryLate = new HashMap<>();
        for (final var task : tasks) {
            final int min = earliestStation(task.id(), cycleTime, state, memoryEarly);
            final int max = latestStation(task.id(), cycleTime, factor, state, memoryLate);
            intervals.put(task.id(), Pair.of(min, max));
        }
        return intervals;
    }

    /**
     * Map equipment types to the number of stations that are equipped with that type.
     * To take into account that some stations may be equipped with more than one type,
     * the counts can be fractional.
     * For example, a station with 4 different equipment types would count as 0.25 stations.
     */
    public static Map<Integer, Double> stationsWithEquipmentCount(AssemblyPlan plan) {
        Map<Integer, Double> counts = new HashMap<>();
        Map<Integer, Collection<Integer>> equippedAtStations = new HashMap<>();
        Map<Integer, Collection<Integer>> stationEquipment = new HashMap<>();
        // Map each equipment type to the set of stations where that type is equipped,
        // and map each station to the equipment types it is equipped with.
        for (final var eq : plan.equipment()) {
            var station = eq.station();
            if (station == null) {
                continue;
            }
            int stationNr = station.number();
            int type = eq.type();
            add(equippedAtStations, type, stationNr);
            add(stationEquipment, stationNr, type);
        }
        // For each equipment type, count how many stations are equipped with that
        // type, counting fractions of stations when there are also other types
        // at a station.
        for (final var entry : equippedAtStations.entrySet()) {
            final int type = entry.getKey();
            final var stations = entry.getValue();
            double count = 0.0;
            for (final int stationNr : stations) {
                double countAtStation = stationEquipment.get(stationNr).size();
                count += 1.0 / countAtStation;
            }
            counts.put(type, count);
        }
        return counts;
    }

    /**
     * Maps equipment types to the total task time of tasks that depend on that
     * type.
     * When a task depends on more than one type, it contributes a fraction of
     * its task time to each type.
     */
    public static Map<Integer, Double> totalEquippedTaskTimes(AssemblyPlan plan) {
        Map<Integer, Double> counts = new HashMap<>();
        for (final var task : plan.tasks()) {
            final double dependencies = task.equipmentDependencies().size();
            final double time = task.time() / dependencies;
            for (final int type : task.equipmentDependencies()) {
                increment(counts, type, time);
            }
        }
        return counts;
    }

    /**
     * Returns the maximum allowed cycle time if specified, or computes an
     * estimate of the achievable cycle time.
     * However, this computation is not straightforward, and
     * <strong>this implementation is dubious!</strong>
     * The problem is that we need to take into account that equipment
     * dependencies may force many and/or large tasks to be assigned to
     * some station, thereby making the lowest possible cycle time much
     * larger than the average station time.
     * <p>
     * This version attempts to take equipment dependencies into account
     * by computing the total amount of task time for which the equipment
     * type is required, and averaging that over the number of stations
     * where equipment is available.
     */
    public static int computeCycleTime(AssemblyPlan plan) {
        if (plan.cycleTime().isPresent()) {
            return plan.cycleTime().getAsInt();
        }
        final double totalTime = plan.tasks().stream()
            .mapToDouble(Task::time)
            .sum();
        final double avgTime = totalTime / plan.stations().size();
        final double maxTime = plan.tasks().stream()
            .mapToDouble(Task::time)
            .max()
            .getAsDouble();
        Map<Integer, Double> equippedCount = stationsWithEquipmentCount(plan);
        Map<Integer, Double> requiredTime = totalEquippedTaskTimes(plan);
        final double avgEquippedTimes = requiredTime.keySet().stream()
            .mapToDouble(type -> requiredTime.get(type) / equippedCount.get(type))
            .max()
            .getAsDouble();
        return ceil(max(avgTime, maxTime, avgEquippedTimes));
    }

}
