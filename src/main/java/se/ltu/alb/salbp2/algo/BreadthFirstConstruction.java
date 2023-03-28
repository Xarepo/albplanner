package se.ltu.alb.salbp2.algo;

import java.util.*;
import java.util.function.Function;
import ch.rfin.util.Rng;
import ch.rfin.util.Streams;
import ch.rfin.util.Pair;
import se.ltu.alb.salbp.model.*;
import ch.rfin.util.Graphs;
import se.ltu.alb.util.Util;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.phase.custom.CustomPhaseCommand;
import static ch.rfin.util.Functions.max;
import static ch.rfin.util.Functions.ceil;

/**
 * Custom phase command implementing a station-oriented, breadth-first
 * construction heuristic.
 * There are implementations of multiple variations on the basic algorithm.
 * They are all breadth-first in the sense that they work layer by layer.
 * A target cycle time is estimated as the maximum of the average station time
 * and the largest task time.
 * Then tasks are assigned to one station at a time in an attempt to attain the
 * target cycle time.
 * <p>
 * Variant 1 opens a new station when a task cannot be added to the current
 * station without exceeding the target cycle time, provided we have not yet
 * reached the upper limit on the number of stations.
 * Variant 2 computes the absolute difference between the station time and the
 * target cycle time. If assigning the task to the current station would
 * improve (decrease) this difference, the assignment is made. Otherwise, a new
 * station is opened (provided the limit has not been reached).
 * Note that exceeding the target is allowed. The excess must simply be smaller
 * than the deficit (under utilization) was before the assignment.
 * <p>
 * Variant a shuffles layers; variant b sorts them by task time.
 * <p>
 * The default is breadth-first-1a ("bf1a").
 * @author Christoffer Fink
 */
public class BreadthFirstConstruction implements CustomPhaseCommand<AssemblyPlan> {

    private Rng rng = new Rng(123);

    private String algorithm = "bf1a";
    public final Map<String, Function<AssemblyPlan, List<? extends Collection<Step>>>> algos = new HashMap<>();
    {
        algos.put("bf1a", this::breadthFirst1a);
        algos.put("bf1b", this::breadthFirst1b);
        algos.put("bf2a", this::breadthFirst2a);
        algos.put("bf2b", this::breadthFirst2b);
    }

    /**
     * Set the algorithm variant to use.
     */
    public void setAlgorithm(String algorithm) {
        if (!algos.containsKey(algorithm)) {
            String msg = String
                .format("Unsupported algorithm: %s. Use one of %s.",
                        algorithm, algos.keySet());
            throw new IllegalArgumentException(msg);
        }
        this.algorithm = algorithm;
    }

    @Override
    public void changeWorkingSolution(final ScoreDirector<AssemblyPlan> scoreDirector) {
        final AssemblyPlan plan = scoreDirector.getWorkingSolution();
        Function<AssemblyPlan, List<? extends Collection<Step>>> f = algos.get(algorithm);
        applyAssignments(f.apply(plan), scoreDirector);
    }

    public AssemblyPlan applyAssignments(List<? extends Collection<Step>> assignments, ScoreDirector<AssemblyPlan> scoreDirector) {
        final AssemblyPlan plan = scoreDirector.getWorkingSolution();
        Streams.zip(plan.stations().stream().sorted(), assignments.stream())
            .forEach(p -> {
                p._2.forEach(step -> {
                    scoreDirector.beforeVariableChanged(step, "station");
                    step.station(p._1);
                    scoreDirector.afterVariableChanged(step, "station");
                });
            });
        scoreDirector.triggerVariableListeners();
        return plan;
    }

    // Shuffle layers.
    // Never exceed the target cycle time.
    public List<? extends Collection<Step>> breadthFirst1a(final AssemblyPlan plan) {
        final int m = plan.stations().size();
        final int targetCt = targetCycleTime(plan);
        final Map<Step,Collection<Step>> dag = Util.buildDependencyGraph(plan.tasks(), Step::taskDependencies);
        final List<Collection<Step>> layers = Graphs.topologicalLayers(dag);

        // Shuffle each layer.
        var tasks = layers.stream()
            .map(rng::shuffle)
            .flatMap(Collection::stream)
            .iterator();

        // Represent stations as a list of lists of steps.
        List<Collection<Step>> stations = new ArrayList<>();
        int stationTime = 0;
        List<Step> station = new ArrayList<>();
        stations.add(station);
        while (tasks.hasNext()) {
            final var task = tasks.next();
            if (stationTime + task.time() <= targetCt && stations.size() < m && !station.isEmpty()) {
                // Move on to the next station.
                station = new ArrayList<>();
                stationTime = 0;
                stations.add(station);
            }
            station.add(task);
            stationTime += task.time();
        }

        return stations;
    }

    // Sort layers by task time.
    // Never exceed the target cycle time.
    public List<? extends Collection<Step>> breadthFirst1b(final AssemblyPlan plan) {
        final int m = plan.stations().size();
        final int targetCt = targetCycleTime(plan);
        final Map<Step,Collection<Step>> dag = Util.buildDependencyGraph(plan.tasks(), Step::taskDependencies);
        final List<Collection<Step>> layers = Graphs.topologicalLayers(dag);

        // Sort each layer by task time (large first).
        var tasks = layers.stream()
            .flatMap(layer -> layer.stream().sorted(Comparator.comparing(Step::time).reversed()))
            .iterator();

        // Represent stations as a list of lists of steps.
        List<Collection<Step>> stations = new ArrayList<>();
        int stationTime = 0;
        List<Step> station = new ArrayList<>();
        stations.add(station);
        while (tasks.hasNext()) {
            final var task = tasks.next();
            if (stationTime + task.time() <= targetCt && stations.size() < m && !station.isEmpty()) {
                // Move on to the next station.
                station = new ArrayList<>();
                stationTime = 0;
                stations.add(station);
            }
            station.add(task);
            stationTime += task.time();
        }

        return stations;
    }

    // Shuffle layers.
    // Uses diff before/after.
    public List<? extends Collection<Step>> breadthFirst2a(final AssemblyPlan plan) {
        //final AssemblyPlan plan = scoreDirector.getWorkingSolution();
        final int m = plan.stations().size();
        final int targetCt = targetCycleTime(plan);
        final Map<Step,Collection<Step>> dag = Util.buildDependencyGraph(plan.tasks(), Step::taskDependencies);
        final List<Collection<Step>> layers = Graphs.topologicalLayers(dag);

        // Shuffle each layer.
        var tasks = layers.stream()
            .map(rng::shuffle)
            .flatMap(Collection::stream)
            .iterator();

        // Represent stations as a list of lists of steps.
        List<Collection<Step>> stations = new ArrayList<>();
        int stationTime = 0;
        List<Step> station = new ArrayList<>();
        stations.add(station);
        while (tasks.hasNext()) {
            final var task = tasks.next();
            final int diffBefore = Math.abs(stationTime - targetCt);
            final int diffAfter = Math.abs(stationTime + task.time() - targetCt);
            if (diffAfter > diffBefore && stations.size() < m && !station.isEmpty()) {
                // Move on to the next station.
                station = new ArrayList<>();
                stationTime = 0;
                stations.add(station);
            }
            station.add(task);
            stationTime += task.time();
        }

        return stations;
    }

    // Sort layers by task time.
    // Uses diff before/after.
    public List<? extends Collection<Step>> breadthFirst2b(final AssemblyPlan plan) {
        final int m = plan.stations().size();
        final int targetCt = targetCycleTime(plan);
        final Map<Step,Collection<Step>> dag = Util.buildDependencyGraph(plan.tasks(), Step::taskDependencies);
        final List<Collection<Step>> layers = Graphs.topologicalLayers(dag);

        // Sort each layer by task time (large first).
        var tasks = layers.stream()
            .flatMap(layer -> layer.stream().sorted(Comparator.comparing(Step::time).reversed()))
            .iterator();

        // Represent stations as a list of lists of steps.
        List<Collection<Step>> stations = new ArrayList<>();
        int stationTime = 0;
        List<Step> station = new ArrayList<>();
        stations.add(station);
        while (tasks.hasNext()) {
            final var task = tasks.next();
            int diffBefore = Math.abs(stationTime - targetCt);
            int diffAfter = Math.abs(stationTime + task.time() - targetCt);
            if (diffAfter > diffBefore && stations.size() < m && !station.isEmpty()) {
                // Move on to the next station.
                station = new ArrayList<>();
                stationTime = 0;
                stations.add(station);
            }
            station.add(task);
            stationTime += task.time();
        }

        return stations;
    }

    private static int targetCycleTime(AssemblyPlan plan) {
        final int m = plan.stations().size();
        final int avgStationTime = ceil(1.0 * plan.tasks().stream().mapToInt(Step::time).sum() / m);
        final int maxTime = plan.tasks().stream().mapToInt(Step::time).max().getAsInt();
        return max(avgStationTime, maxTime);
    }

}
