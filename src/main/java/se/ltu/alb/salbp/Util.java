package se.ltu.alb.salbp;

import java.util.*;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toList;
import se.ltu.alb.salbp.model.AssemblyPlan;
import se.ltu.alb.salbp.model.Step;
import static ch.rfin.util.Graphs.isSorted;
import static ch.rfin.util.Graphs.topologicalSortComparator;

/**
 * Graph operations that work directly with SALBP model classes.
 * @author Christoffer Fink
 */
public class Util {

    /**
     * Are the map keys (graph nodes) in topological sort order?
     */
    public static boolean isTopSorted(final Iterable<Step> steps) {
        // Put into list so we can iterate over the steps more than once.
        // (The iterable could be something like `() -> stream.iterator()` and
        // would then only survive a single scan through.)
        final List<Step> list = new ArrayList<>();
        steps.forEach(list::add);
        final Map<Step, Collection<Step>> graph = buildDependencyGraph(list);
        return isSorted(list, graph);
    }

    /**
     * Are the map keys (graph nodes) in topological sort order?
     */
    public static List<Step> topologicalSort(final Collection<Step> steps) {
        final Map<Step, Collection<Step>> graph = buildDependencyGraph(steps);
        var cmp = topologicalSortComparator(graph).thenComparing(Step::id);
        return steps.stream().sorted(cmp).collect(toList());
    }

    /**
     * Build step dependency graph.
     */
    public static Map<Step, Collection<Step>> buildDependencyGraph(final Iterable<Step> steps) {
        Map<Step, Collection<Step>> graph = new HashMap<>();
        steps.forEach(step -> graph.put(step, step.taskDependencies()));
        return graph;
    }

    /**
     * Convert a dependency graph of tasks to a dependency graph of task IDs.
     */
    @Deprecated // TODO: Currently unused. Remove?
    public static Map<Integer, Collection<Integer>> stepsToIds(final Map<Step, Collection<Step>> graph) {
        return graph.keySet()
            .stream()
            .collect(
                toMap(
                    Step::id,
                    step -> graph.get(step)
                        .stream()
                        .map(Step::id)
                        .collect(toList())));
    }

}
