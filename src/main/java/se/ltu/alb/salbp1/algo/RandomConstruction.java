package se.ltu.alb.salbp1.algo;

import java.util.*;
import static java.util.stream.Collectors.toSet;
import static java.util.function.Predicate.not;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.phase.custom.CustomPhaseCommand;
import ch.rfin.util.Rng;
import ch.rfin.util.Streams;
import ch.rfin.util.Graphs;
import static ch.rfin.util.Collections.increment;
import static ch.rfin.util.Exceptions.illegalArg;
import se.ltu.alb.salbp.model.AssemblyPlan;
import se.ltu.alb.salbp.model.Step;
import se.ltu.alb.salbp.Util;

/**
 * Random feasible solution constructor.
 * Three modes:
 * <ol>
 * <li>breadth-first (layer by layer)</li>
 * <li>depth-first</li>
 * <li>random feasible (sort of layer by layer, but not strictly)</li>
 * </ol>
 * <p>
 * These are all station-oriented, which means tasks are assigned to one
 * station at a time.
 * Rather than using some rule to try to make the best assignments, they
 * all use randomness to choose one of several possible feasible assignments.
 * The solution is represented by a list of collections of tasks.
 * The ith element in the list is the collection of tasks to be
 * assigned to station {@code i}.
 * But the solution can also be directly applied to the current working solution
 * held by a score director.
 * <p>
 * Note: these heuristics assume that a task can be assigned to the same station
 * as tasks that it depends on. In other words, the station must be greater than
 * or equal to, rather than strictly greater.
 * <p>
 * Some rough performance metrics:
 * <pre>
 *   n  |     heuristic   | score | time
 * ---------------------------------------
 * 1000 |   breadh first  | 0/-91 | 219 ms
 * 1000 |    depth first  | 0/-90 | 223 ms
 * 1000 |       compact   | 0/-47 | 208 ms
 * 1000 | random feasible | 0/-91 | 229 ms
 *  100 |   breadh first  | 0/-7  | 179 ms
 *  100 |    depth first  | 0/-7  | 175 ms
 *  100 |       compact   | 0/-4  | 171 ms
 *  100 | random feasible | 0/-6  | 183 ms
 * </pre>
 *
 * @see se.ltu.alb.salbp.algo.RandomType1
 * @author Christoffer Fink
 */
public class RandomConstruction implements CustomPhaseCommand<AssemblyPlan> {

    private Rng rng = new Rng(123);
    private String mode = "breadthFirst";
    private static final Set<String> modes = Set.of(
            "breadthFirst", "depthFirst", "compact", "feasible");

    public void setMode(String mode) {
        if (!modes.contains(mode)) {
            illegalArg("%s not supported. Must be on of %s.", mode, modes);
        }
        this.mode = mode;
    }

    public void setSeed(long seed) {
        rng = new Rng(seed);
    }

    @Override
    public void changeWorkingSolution(final ScoreDirector<AssemblyPlan> scoreDirector) {
        switch (mode) {
            case "breadthFirst":
                breadthFirst(scoreDirector);
                break;
            case "depthFirst":
                depthFirst(scoreDirector);
                break;
            case "compact":
                compactBreadthFirst(scoreDirector);
                break;
            case "feasible":
                randomFeasible(scoreDirector);
                break;
        }
    }

    public AssemblyPlan applyAssignments(List<? extends Collection<Step>> assignments, ScoreDirector<AssemblyPlan> scoreDirector) {
        final AssemblyPlan plan = scoreDirector.getWorkingSolution();
        Streams.zip(plan.stations().stream().sorted(), assignments.stream())
            .forEach(p -> {
                p._2.forEach(task -> {
                    scoreDirector.beforeVariableChanged(task, "station");
                    task.station(p._1);
                    scoreDirector.afterVariableChanged(task, "station");
                });
            });
        scoreDirector.triggerVariableListeners();
        return plan;
    }

    public AssemblyPlan applyAssignments(List<? extends Collection<Step>> assignments, AssemblyPlan plan) {
        Streams.zip(plan.stations().stream().sorted(), assignments.stream())
            .forEach(p -> {
                p._2.forEach(task -> {
                    task.station(p._1);
                });
            });
        return plan;
    }

    // What counts as a feasible task could mean two different things:
    // - Its dependencies have been satisfied, so it is a candidate for what to
    //   do next.
    // - Dependencies satisfied, but it also fits in the current station.
    // The method below uses the first definition. So if a feasible task doesn't fit
    // in the current station, it's simply added to the next instead. (A new station
    // is opened, and previous stations are never considered again.)

    /**
     * Generate a random feasible solution to the ALB instance.
     * The algorithm maintains a collection of feasible tasks, which are the
     * tasks that depend only on the tasks that have been assigned so far.  One
     * task at a time is removed from the feasible set at random, and then all
     * its successor tasks that become feasible are added to the set.  If the
     * randomly chosen task does not fit in the current station, a new station
     * is opened.
     * <p>
     * This is the same algorithm as
     * {@link se.ltu.alb.salbp.algo.RandomType1#randomFeasible(AlbInstance)}
     * except it takes an AssemblyPlan and produces a list of collections of tasks.
     * @return a list where element {@code i} is the collection of tasks to be
     * assigned to station {@code i}
     */
    public List<? extends Collection<Step>> randomFeasible(final AssemblyPlan plan) {
        final int cycleTime = plan.cycleTime().getAsInt();
        final Map<Step,Collection<Step>> dag = Util.buildDependencyGraph(plan.tasks());
        final Map<Step,Collection<Step>> dependeeTree = Graphs.flipEdges(dag);
        Set<Step> assigned = new HashSet<>();
        List<Step> feasible = new ArrayList<>(Graphs.roots(dependeeTree));
        // Represent stations as a list of lists of tasks.
        List<List<Step>> stations = new ArrayList<>();
        int stationTime = 0;
        List<Step> station = new ArrayList<>();
        stations.add(station);
        while (!feasible.isEmpty()) {
            final var task = rng.removeRandomElementUnordered(feasible);
            if (stationTime + task.time() > cycleTime) {
                // Move on to the next station.
                station = new ArrayList<>();
                stationTime = 0;
                stations.add(station);
            }
            station.add(task);
            stationTime += task.time();
            assigned.add(task);
            // If all of a task's dependees has its dependencies satisfied,
            // add it as feasible tasks.
            dependeeTree.get(task).stream()
                .filter(s -> assigned.containsAll(s.taskDependencies()))
                .forEach(feasible::add);
        }
        return stations;
    }

    public void randomFeasible(final ScoreDirector<AssemblyPlan> scoreDirector) {
        final AssemblyPlan plan = scoreDirector.getWorkingSolution();
        final var stations = randomFeasible(plan);
        applyAssignments(stations, scoreDirector);
    }


    public void breadthFirst(final ScoreDirector<AssemblyPlan> scoreDirector) {
        final AssemblyPlan plan = scoreDirector.getWorkingSolution();
        final var stations = breadthFirst(plan);
        applyAssignments(stations, scoreDirector);
    }

    /**
     * Generate a feasible solution breadth-first, that is, layer-by-layer.
     * Processes one layer of tasks at a time.
     * Repeatedly remove a random feasible task (from the current layer) and
     * assign to current stations.
     * If the task doesn't fit, open a new station.
     * When we run out of tasks, move on to the next layer.
     */
    public List<? extends Collection<Step>> breadthFirst(final AssemblyPlan plan) {
        final int cycleTime = plan.cycleTime().getAsInt();
        final Map<Step,Collection<Step>> dag = Util.buildDependencyGraph(plan.tasks());
        final List<Collection<Step>> layers = Graphs.topologicalLayers(dag);

        // Shuffle each layer.
        var tasks = layers.stream()
            .map(rng::shuffle)
            .flatMap(Collection::stream)
            .iterator();

        // Represent stations as a list of lists of tasks.
        List<Collection<Step>> stations = new ArrayList<>();
        int stationTime = 0;
        List<Step> station = new ArrayList<>();
        stations.add(station);
        while (tasks.hasNext()) {
            final var task = tasks.next();
            if (stationTime + task.time() > cycleTime) {
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

    // TODO: come up with a better name?
    /**
     * Breadth-First that tries to compact layers.
     * This works like {@link #breadthFirst(ScoreDirector) breadthFirst}, except:
     * rather than adding a task to the "current" station if possible, or else
     * opening a new station and adding it there, each open station is tried
     * until one that can accommodate the task is found, or else a new station
     * is opened.
     * <p>
     * Of course, this also entails a difference in implementation.
     * breadthFirst() iterates through tasks ordered by layer. Hence a task is
     * guaranteed to not be assigned to an earlier station than any of the
     * tasks it depends on.
     * But if we want to retry stations where a previous task did not fit, it
     * had better not be a task that depends on tasks that have already been
     * assigned to later stations (so that it now comes before them).
     * So this method works strictly one layer at a time. Within the same layer,
     * tasks can be placed in any order. So retrying earlier stations does no
     * harm. Then the next layer is limited to trying stations that are no
     * earlier than the max station from the previous layer, etc.
     * <p>
     * Since some effort is made to make the best choice out of a set of
     * possible choices, this method is likely "less random" (in the sense that
     * the solutions should differ less between multiple runs).
     * (<strong>TODO:</strong>Verify this.)
     */
    public List<? extends Collection<Step>> compactBreadthFirst(final ScoreDirector<AssemblyPlan> scoreDirector) {
        final AssemblyPlan plan = scoreDirector.getWorkingSolution();
        final var stations = compactBreadthFirst(plan);
        applyAssignments(stations, scoreDirector);
        return stations;
    }

    public List<? extends Collection<Step>> compactBreadthFirst(final AssemblyPlan plan) {
        final int cycleTime = plan.cycleTime().getAsInt();
        final Map<Step,Collection<Step>> dag = Util.buildDependencyGraph(plan.tasks());
        final List<Collection<Step>> layers = Graphs.topologicalLayers(dag);

        // Represent stations as a list of lists of tasks.
        List<Collection<Step>> stations = new ArrayList<>();
        List<Step> station = new ArrayList<>();
        List<Integer> stationTime = new ArrayList<>();
        stations.add(station);
        stationTime.add(0);

        int minStation = 0;
        for (final var layer : layers) {
            for (final var task : rng.shuffle(layer)) {
                boolean success = false;
                for (int i = minStation; i < stations.size(); i++) {
                    final int time = stationTime.get(i);
                    if (time + task.time() <= cycleTime) {
                        // There is room in this station.
                        stations.get(i).add(task);
                        increment(stationTime, i, task.time());
                        success = true;
                        break;
                    } // else try the next station
                }
                // Failed to add the task to an existing station?
                if (!success) {
                    // Open a new station
                    station = new ArrayList<>();
                    stations.add(station);
                    // and add the task to it.
                    station.add(task);
                    stationTime.add(task.time());
                }
                // The task has been added.
            }
            // The layer has been added.
            // If tasks in this layer were added to up to m stations,
            // then the next layer can be added only to stations >= m.
            minStation = stations.size() - 1;
        }

        return stations;
    }

    /**
     * Assign tasks depth-first.
     * Rather than working layer-by-layer from the leaves, this heuristic
     * starts from the roots.
     * The roots are added in random order. Each root is added by recursively
     * adding the tasks it depends on (also in random order).
     * <p>
     * Obviously, the leaves are assigned first, but a higher-layer task may
     * be assigned before a lower-layer task as long as they are on different
     * paths from a root to a leaf.
     * So it's roughly like adding one path at a time instead of one layer
     * at a time.
     */
    public void depthFirst(final ScoreDirector<AssemblyPlan> scoreDirector) {
        final AssemblyPlan plan = scoreDirector.getWorkingSolution();
        final var stations = depthFirst(plan);
        applyAssignments(stations, scoreDirector);
    }

    public List<? extends Collection<Step>> depthFirst(final AssemblyPlan plan) {
        final int cycleTime = plan.cycleTime().getAsInt();
        final Map<Step,Collection<Step>> dag = Util.buildDependencyGraph(plan.tasks());
        final Collection<Step> roots = Graphs.roots(dag);

        // Represent stations as a list of lists of tasks.
        List<Collection<Step>> stations = new ArrayList<>();
        List<Step> station = new ArrayList<>();
        List<Integer> stationTime = new ArrayList<>();
        stations.add(station);
        stationTime.add(0);
        Collection<Step> added = new HashSet<>();

        rng.shuffle(roots)
            .forEach(task ->
                assignRecursive(task, dag, stations, stationTime, cycleTime, added)
            );

        return stations;
    }

    private void assignRecursive(Step task, Map<Step,Collection<Step>> dag, List<Collection<Step>> stations, List<Integer> stationTime, int cycleTime, Collection<Step> added) {
        assert stations.size() == stationTime.size();
        // recursively add all tasks the task depends on
        rng.randomOrder(dag.get(task).stream().filter(not(added::contains)))
            .forEach(child ->
                assignRecursive(child, dag, stations, stationTime, cycleTime, added)
            );
        // then add the task
        int lastStationIndex = stationTime.size()-1;
        int lastStationTime = stationTime.get(lastStationIndex);
        var lastStation = stations.get(lastStationIndex);
        if (lastStationTime + task.time() <= cycleTime) {
            lastStation.add(task);
            increment(stationTime, lastStationIndex, task.time());
        } else {
            // Open a new station
            var station = new ArrayList<Step>();
            station.add(task);
            stations.add(station);
            stationTime.add(task.time());
        }
        added.add(task);
    }

}
