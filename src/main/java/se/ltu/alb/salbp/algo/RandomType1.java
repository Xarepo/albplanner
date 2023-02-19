package se.ltu.alb.salbp.algo;

import java.util.*;
import ch.rfin.util.Rng;
import ch.rfin.util.Streams;
import ch.rfin.util.Pairs;
import ch.rfin.alb.AlbInstance;
import ch.rfin.util.Graphs;

/**
 * Random feasible solution constructor.
 * Note: assumes that a task can be assigned to the same station
 * as steps that it depends on. In other words, the station must be greater than
 * or equal to, rather than strictly greater.
 *
 * @see se.ltu.alb.salbp1.algo.RandomConstruction
 * @author Christoffer Fink
 */
public class RandomType1 {

    private Rng rng;

    public RandomType1 seed(long seed) {
        return rng(new Rng(seed));
    }

    public RandomType1 rng(Rng rng) {
        this.rng = rng;
        return this;
    }

    /**
     * Generate a random feasible solution to the ALB instance.
     * This is the same algorithm as
     * {@link se.ltu.alb.salbp1.algo.RandomConstruction#randomFeasible(AssemblyPlan)}
     * except it takes an AlbInstance and produces an assignment map.
     * @return an assignment where task IDs map to station numbers.
     */
    public Map<Integer,Integer> randomFeasible(final AlbInstance alb) {
        assert alb.isZeroBased()    // TODO
            : "FIXME: can only handle 0-based at the moment";
        final int cycleTime = alb.cycleTime().getAsInt();   // OK for type 1.
        final Map<Integer,Collection<Integer>> dag = alb.taskDependencies();
        final Map<Integer,Collection<Integer>> dependeeTree = Graphs.flipEdges(dag);
        Set<Integer> assigned = new HashSet<>();
        // Starting with all roots means that we are guaranteed to eventually
        // get to every task. (Handles forests.)
        List<Integer> feasible = new ArrayList<>(Graphs.roots(dependeeTree));
        assert feasible.size() > 0;
        // Represent stations as a list of lists of steps.
        List<Collection<Integer>> stations = new ArrayList<>();
        int stationTime = 0;
        List<Integer> station = new ArrayList<>();
        stations.add(station);
        while (!feasible.isEmpty()) {
            final var task = rng.removeRandomElementUnordered(feasible);
            final int taskTime = alb.taskTimes.get(task);
            if (stationTime + taskTime > cycleTime) {
                // Move on to the next station.
                station = new ArrayList<>();
                stationTime = 0;
                stations.add(station);
            }
            station.add(task);
            stationTime += taskTime;
            assigned.add(task);
            // If all of a task's dependees has its dependencies satisfied,
            // add it as feasible tasks.
            dependeeTree.get(task).stream()
                .filter(s -> assigned.containsAll(dag.get(s)))
                .forEach(feasible::add);
        }
        assert stations.size() >= 1;
        assert stations.stream()
            .flatMap(Collection::stream)
            .count() == alb.tasks();
        // Convert solution to task ↦ station
        return Streams.enumerate(stations)
            .flatMap(iv -> iv._2.stream().map(Pairs.pairWithSecond(iv._1)))
            .collect(Pairs.pairsToMap());
    }

}
