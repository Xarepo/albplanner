package se.ltu.alb.util;

import java.util.*;
import java.util.function.Function;
import static java.util.stream.Collectors.toList;
import static ch.rfin.util.Graphs.isSorted;
import static ch.rfin.util.Graphs.topologicalSortComparator;

public class Util {

    /**
     * Are the map keys (graph nodes) in topological sort order?
     * @param tasks the tasks to check the order of
     * @param depFun a function that maps a task to its dependencies
     */
    public static <T> boolean isTopSorted(final Iterable<T> tasks, Function<T, Collection<T>> depFun) {
        // Put into list so we can iterate over the steps more than once.
        // (The iterable could be something like `() -> stream.iterator()` and
        // would then only survive a single scan through.)
        final List<T> list = new ArrayList<>();
        tasks.forEach(list::add);
        final Map<T, Collection<T>> graph = buildDependencyGraph(list, depFun);
        return isSorted(list, graph);
    }

    /**
     * Sorts a collection of tasks in topological sort order
     * (tasks with no dependencies first).
     * @param tasks the tasks to sort
     * @param depFun a function that maps a task to its dependencies
     * @return the tasks in sorted order
     */
    public static <T extends Comparable<? super T>> List<T> topologicalSort(final Collection<T> tasks, Function<T, Collection<T>> depFun) {
        final Map<T, Collection<T>> graph = buildDependencyGraph(tasks, depFun);
        var cmp = topologicalSortComparator(graph).thenComparing(Comparator.naturalOrder());
        return tasks.stream().sorted(cmp).collect(toList());
    }

    /**
     * Build task dependency graph.
     * @param tasks the nodes of the graph
     * @param depFun a function that maps a task to its dependencies
     * @return a map representing the DAG, where each task maps to the tasks it depends on
     */
    public static <T> Map<T, Collection<T>> buildDependencyGraph(final Iterable<T> tasks, Function<T, Collection<T>> depFun) {
        Map<T, Collection<T>> graph = new HashMap<>();
        tasks.forEach(step -> graph.put(step, depFun.apply(step)));
        return graph;
    }

}
