package ch.rfin.util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import ch.rfin.util.Pair;
import static ch.rfin.util.Streams.items;
import static ch.rfin.util.Streams.pairsWithFirst;

/**
 * Graph utilities.
 * @author Christoffer Fink
 */
public class Graphs {

    public static <T> void insertMissingDependencies(final Map<T, Collection<T>> graph) {
        missingDependencies(graph)
            .forEach(node -> graph.put(node, new ArrayList<>()));
    }

    public static <T> Map<Integer, Collection<T>> toGraph(List<Collection<T>> graph) {
        return Collections.toMap(graph);
    }

    /**
     * Are the nodes in topological sort order?
     */
    public static <T> boolean isSorted(final Iterable<T> sorted, final Map<T, Collection<T>> graph) {
        final Set<T> prev = new HashSet<>();
        for (final var node : sorted) {
            final var children = graph.get(node);
            if (!prev.containsAll(children)) {
                //System.out.println(prev + " not a superset of " + children);
                return false;
            }
            prev.add(node);
        }
        return true;
    }

    /**
     * Are the map keys (graph nodes) in topological sort order?
     */
    public static <T> boolean isSorted(final Map<T, Collection<T>> graph) {
        return isSorted(graph.keySet(), graph);
    }

    /**
     * Return the collection of nodes in topologically sorted order.
     */
    public static <T> List<T> topologicalSort(final Collection<T> nodes, final Map<T, Collection<T>> graph) {
        return nodes.stream().sorted(topologicalSortComparator(graph)).collect(toList());
    }

    /**
     * Return the map keys (graph nodes) in a topologically sorted list.
     */
    public static <T> List<T> topologicalSort(final Map<T, Collection<T>> graph) {
        return topologicalLayers(graph).stream().flatMap(Collection::stream).collect(toList());
    }

    /**
     * Map each node to its layer.
     * @see #topologicalLayers(Map)
     */
    public static <T> Map<T, Integer> topologicalLayerMap(final Map<T, Collection<T>> graph) {
        List<Collection<T>> layers = topologicalLayers(graph);
        Map<T, Integer> layerMap = new HashMap<>();
        for (int i = 0; i < layers.size(); i++) {
            for (final var x : layers.get(i)) {
                layerMap.put(x, i);
            }
        }
        return layerMap;
    }

    /**
     * Create a comparator that compares nodes based on their topological
     * sort order. Performs a topological sort of the graph nodes.
     */
    public static <T> Comparator<T> topologicalSortComparator(final Map<T, Collection<T>> graph) {
        Map<T, Integer> layerMap = topologicalLayerMap(graph);
        Comparator<T> cmp = (x,y) -> Integer.compare(layerMap.get(x), layerMap.get(y));
        return cmp;
    }

    /**
     * Return a sorted map (graph) where keys (nodes) appear in topological sort order.
     */
    public static <T> SortedMap<T, Collection<T>> topologicalSortKeys(final Map<T, Collection<T>> graph) {
        SortedMap<T, Collection<T>> map = new TreeMap<>(topologicalSortComparator(graph));
        map.putAll(graph);
        return map;
    }

    /**
     * Return a sorted map (graph) where keys (nodes) and the mapped values
     * appear in topological sort order.
     */
    public static <T> SortedMap<T, Collection<T>> topologicalSortMap(final Map<T, Collection<T>> graph) {
        final var cmp = topologicalSortComparator(graph);
        SortedMap<T, Collection<T>> map = new TreeMap<>(cmp);
        graph.entrySet()
            .forEach(entry ->
                map.put(entry.getKey(), entry.getValue().stream().sorted(cmp).collect(toList())));
        return map;
    }

    // TODO: Θ(n²) in the worst case? Or Θ(n³)?
    /**
     * Sort the nodes of the given DAG into layers, like a contour map.
     * The first collection in the list will have the leaf nodes. The next
     * collection has the nodes above the nodes in the first layer (the nodes
     * that only have terminal nodes as children), etc. The final collection has
     * the root nodes.
     * @param dag a directed, acyclic graph
     * @throws IllegalArgumentException if there is a cycle or if an edge points to a missing node.
     * @see #missingDependency(Map graph)
     * @see #findCycle(Map graph)
     */
    public static <T> List<Collection<T>> topologicalLayers(final Map<T, Collection<T>> dag) {
        int iterations = 0;
        Set<T> nodes = new HashSet<>(dag.keySet());
        Set<T> below = new HashSet<>();
        List<Collection<T>> layers = new ArrayList<>();
        while (!nodes.isEmpty()) {
            // Each layer consists of those nodes that only depend on nodes in previous layers.
            Set<T> layer = nodes.stream()
                .filter(node -> below.containsAll(dag.get(node)))
                .collect(toSet());
            // If the layer is empty, it means there was no node that has all
            // its children in a lower layer. That either means that a child
            // depends on a node node in a higher layer, or that it depends on
            // a node in no layer (missing dependency).
            if (layer.isEmpty()) {
                final String msg = "Malformed graph. May have cycles or missing nodes.";
                throw new IllegalArgumentException(msg);
            }
            layers.add(layer);
            below.addAll(layer);
            nodes.removeAll(layer);
        }

        return layers;
    }

    public static <T> Stream<Pair<T,T>> loopEdges(final Map<T, Collection<T>> graph) {
        return edges(graph)
            .filter(p -> p._1.equals(p._2));
    }

    public static <T> Stream<List<T>> trivialCycles(final Map<T, Collection<T>> graph) {
        return loopEdges(graph)
            .map(p -> List.of(p._1, p._2));
    }

    private static <T> Optional<List<T>> findCycle(
            final T node,
            final Map<T, Collection<T>> graph,
            final Collection<T> ancestors
    ) {
        if (ancestors.contains(node)) {
            List<T> path = new ArrayList<>();
            path.add(node);
            return Optional.of(path);
        }
        var children = graph.get(node);
        if (!children.isEmpty()) {
            ancestors.add(node);
            for (final var child : children) {
                final var result = findCycle(child, graph, ancestors);
                if (result.isPresent()) {
                    final var list = result.get();
                    list.add(node);
                    return result;
                }
            }
        }
        ancestors.remove(node);
        return Optional.empty();
    }

    /**
     * Returns a cycle, if it exists.
     * The cycle is a list of nodes [n₀, n₁, n₂, ..., n₀] such that there is an
     * edge from n₀ to n₁, from n₁ to n₂, ..., and finally an edge back to n₀.
     */
    public static <T> Optional<List<T>> findCycle(final Map<T, Collection<T>> graph) {
        // TODO: does it pay to try to start with the roots?
        /*
        Collection<T> roots = roots(graph);
        if (roots.isEmpty()) {
            System.err.println("No roots! Must have cycles!");
            roots.addAll(graph.keySet());
        }
        //*/
        final var nodes = graph.keySet();
        Set<T> ancestors = new HashSet<>();
        for (final var node : nodes) {
            final var result = findCycle(node, graph, ancestors);
            if (result.isPresent()) {
                final var list = result.get();
                // Find the repeated node and return only the sub path
                // from that node back to itself.
                final var duplicate = list.get(0);
                final int index = list.lastIndexOf(duplicate);
                final var path = list.subList(0, index+1);
                java.util.Collections.reverse(path);
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }

    /**
     * Cycles are allowed.
     */
    public static <T> Collection<T> sourceNodesCollection(final Map<T, Collection<T>> graph) {
        return sourceNodes(graph)
            .collect(toSet());
    }

    /**
     * Source nodes are all the nodes with outgoing edges (out-degree at least 1).
     * Cycles are allowed.
     */
    public static <T> Stream<T> sourceNodes(final Map<T, Collection<T>> graph) {
        return items(outDegreesMap(graph))
            .filter(Pairs.test_2(Predicates::positive))
            .map(Pair::get_1);
    }

    public static <T> Collection<T> targetNodesCollection(final Map<T, Collection<T>> graph) {
        return items(inDegreesMap(graph))
            .filter(Pairs.test_2(Predicates::positive))
            .map(Pair::get_1)
            .collect(toSet());
    }

    // TODO: are cycles allowed?
    /**
     * Target nodes are all the nodes with incoming edges (in-degree at least 1).
     */
    public static <T> Stream<T> targetNodes(final Map<T, Collection<T>> graph) {
        return items(inDegreesMap(graph))
            .filter(Pairs.test_2(Predicates::positive))
            .map(Pair::get_1);
    }

    public static <T> long countEdges(final Map<T, Collection<T>> graph) {
        return edges(graph).count();
    }

    public static <T> Stream<Pair<T,T>> edges(final Map<T, Collection<T>> graph) {
        return items(graph)
            .flatMap(p -> pairsWithFirst(p._1, p._2));
    }

    /**
     * The roots are all the nodes with no incoming edges (in-degree 0).
     */
    public static <T> Collection<T> roots(final Map<T, Collection<T>> graph) {
        return items(inDegreesMap(graph))
            .filter(Pairs.test_2(Predicates::zero))
            .map(Pair::get_1)
            .collect(toSet());
    }

    /**
     * The roots are all the nodes with no outgoing edges (out-degree 0).
     */
    public static <T> Collection<T> leaves(final Map<T, Collection<T>> graph) {
        return items(outDegreesMap(graph))
            .filter(Pairs.test_2(Predicates::zero))
            .map(Pair::get_1)
            .collect(toSet());
    }

    public static <T> Map<T, Integer> outDegreesMap(final Map<T, Collection<T>> graph) {
        return outDegrees(graph)
            .collect(Pairs.pairsToMap());
    }

    public static <T> Stream<Pair<T, Integer>> outDegrees(final Map<T, Collection<T>> graph) {
        return items(graph)
            .map(Pairs.map_2(Collection::size));
    }

    public static <T> Map<T, Integer> inDegreesMap(final Map<T, Collection<T>> graph) {
        final Map<T, Integer> map = Streams.pairsWithSecond(0, graph.keySet())
            .collect(Pairs.pairsToMap());
        if (map.keySet().size() != graph.keySet().size()) {
            throw new AssertionError("Impossible: ");
        }
        graph.values().stream()
            .flatMap(Collection::stream)
            .forEach(t -> map.put(t, map.getOrDefault(t, 0) + 1));
        return map;
    }

    public static <T> Stream<Pair<T, Integer>> inDegrees(final Map<T, Collection<T>> graph) {
        return items(inDegreesMap(graph));
    }

    // OPTIMIZE: This might be exponential. Do dynamic programming?
    // HACK: "Detects" circularity by infinite recursion, resulting in stack overflow.
    //       Detect explicitly (and earlier) instead and throw a more appropriate exception?
    /**
     * Get <strong>all</strong> the nodes that the given node depends on
     * (including their dependencies, etc).
     * @throws StackOverflowError if there are circular dependencies.
     */
    @Deprecated(forRemoval = false)
    public static <T> Set<T> deepDependencies(final T node, final Map<T, Collection<T>> graph) {
        Set<T> children = new TreeSet<>(graph.get(node));
        Set<T> deep = new TreeSet<>(children);
        children.stream()
            .map(child -> deepDependencies(child, graph))
            .forEach(deep::addAll);
        return deep;
    }

    /**
     * Map each node to the collection of <strong>all</strong> nodes it depends
     * on (including <strong>their</strong> dependencies, etc).
     */
    public static <T> Map<T, Collection<T>> deepDependencies(final Map<T, Collection<T>> graph) {
        final Map<T, Collection<T>> dependencies = new HashMap<>();
        final var nodes = topologicalSort(graph);
        for (final var node : nodes) {
            final var children = graph.get(node);
            Set<T> deep = new TreeSet<>(children);
            for (final var child : children) {
                deep.addAll(dependencies.get(child));
            }
            dependencies.put(node, deep);
        }
        return dependencies;
    }

    /**
     * Useful for turning a dependency graph (nodes pointing to the nodes they depend on)
     * into a dependee graph (nodes pointing to nodes that depend on them).
     */
    public static <T> Map<T, Collection<T>> flipEdges(final Map<T, Collection<T>> graph) {
        final Collection<T> sorted = topologicalSort(graph);
        final Map<T, Collection<T>> result = new HashMap<>();
        for (final var node : sorted) {
            result.put(node, new HashSet<>());
            for (final var child : graph.get(node)) {
                result.get(child).add(node);
            }
        }
        return result;
    }

    /**
     * Turn the graph into a pruned tree.
     * Paths from one node to another in the tree correspond to the longest
     * possible path from that node to the other node in the graph.
     * In other words, immediate depedendencies are pruned if a transitive
     * dependency on the same node exists.
     * <p>
     * For example, {@code C<-A->B->C} is pruned to {@code A->B->C}.
     */
    public static <T> Map<T, Collection<T>> prunedDependencyTree(final Map<T, Collection<T>> graph) {
        final Map<T, Integer> layerMap = topologicalLayerMap(graph);
        final Collection<T> sorted = topologicalSort(graph);
        final Map<T, Collection<T>> result = new HashMap<>();
        for (final var node : sorted) {
            Collection<T> children = graph.get(node).stream()
                .filter(child -> layerMap.get(child) == layerMap.get(node) - 1)
                .collect(toSet());
            result.put(node, children);
        }
        return result;
    }

    /**
     * Try to find a missing dependency.
     */
    public static <T> Optional<T> missingDependency(final Map<T, Collection<T>> graph) {
        return graph.values()
            .stream()
            .flatMap(Collection::stream)
            .filter(child -> !graph.containsKey(child))
            .findAny();
    }

    /**
     * Find all the missing dependencies - returns an empty collection if none are missing.
     */
    public static <T> Collection<T> missingDependencies(final Map<T, Collection<T>> graph) {
        return graph.values()
            .stream()
            .flatMap(Collection::stream)
            .filter(child -> !graph.containsKey(child))
            .collect(toList());
    }

    public static <T> boolean hasDanglingDependencies(final Map<T, Collection<T>> graph) {
        return missingDependency(graph).isPresent();
    }

    /**
     * Check if the two collections have the same elements by treating them as sets.
     */
    @Deprecated(forRemoval = true) // move to Collections
    public static <T> boolean sameElements(final Collection<T> col1, final Collection<T> col2) {
        // Checking set.containsAll(list) is cheap, but list.containsAll(set) is not.
        // Simple solution. Can be optimized (avoid creating new sets).
        final var set1 = new HashSet<>(col1);
        final var set2 = new HashSet<>(col2);
        return set1.equals(set2);
    }

    /**
     * Two graphs are considered equal if they have the same nodes and edges
     * (same key set, with each key mapping to collections with the same elements).
     * @see #sameElements(Collection, Collection)
     */
    public static <T> boolean equalGraphs(final Map<T, Collection<T>> graph1, final Map<T, Collection<T>> graph2) {
        final var keys1 = graph1.keySet();
        final var keys2 = graph2.keySet();
        if (keys1.size() != keys2.size()) {
            return false;
        }
        if (!graph1.keySet().equals(graph2.keySet())) {
            return false;
        }
        final var keys = keys1; // Both have the same keys.
        for (final var key : keys) {
            final var values1 = graph1.get(key);
            final var values2 = graph2.get(key);
            if (!sameElements(values1, values2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Identifies chains of nodes.
     * The definition of a chain is taken from Systematic data greneration (etc) by
     * Otto et al.
     */
    public static <T> boolean isChain(final T node, final Map<T, Collection<T>> graph) {
        var flipped = flipEdges(graph);
        var children = graph.get(node);  // successors
        var parents = flipped.get(node); // predecessors
        // The node must have a single predecessor and a single successor.
        if (children.size() > 1) {
            return false;
        }
        if (parents.size() > 1) {
            return false;
        }
        // At least one of them must also have a single predecessor and single
        // successor.
        int grandParents = parents.stream()
            .mapToInt(parent -> flipped.get(parent).size())
            .sum();
        if (grandParents == 1) {
            return true;
        }
        int grandChildren = children.stream()
            .mapToInt(child -> graph.get(child).size())
            .sum();
        return grandChildren == 1;
    }

    /**
     * Identifies chains of nodes.
     * The definition of a chain is taken from Systematic data greneration (etc) by
     * Otto et al.
     */
    public static <T> Collection<List<T>> chains(final Map<T, Collection<T>> graph) {
        throw new UnsupportedOperationException("FIXME: Not implemented.");
    }

    /**
     * Identifies bottleneck nodes.
     * The definition of a bottleneck is taken from Systematic data greneration
     * (etc) by Otto et al.
     */
    public static <T> Collection<T> bottlenecks(final Map<T, Collection<T>> graph) {
        return graph.keySet().stream()
            .filter(node -> isBottleneck(node, graph))
            .collect(toSet());
    }

    /**
     * Identifies bottleneck nodes.
     * The definition of a bottleneck is taken from Systematic data greneration
     * (etc) by Otto et al.
     */
    public static <T> boolean isBottleneck(final T node, final Map<T, Collection<T>> graph) {
        var flipped = flipEdges(graph);
        var children = graph.get(node);  // successors
        var parents = flipped.get(node); // predecessors
        // must be the only successor to at least two of its predecessors
        long soleSuccessor = parents.stream()
            .filter(parent -> graph.get(parent).size() == 1)
            .count();
        if (soleSuccessor < 2) {
            return false;
        }
        // must be the only predecessor to at least two of its successors
        long solePredecessor = children.stream()
            .filter(child -> flipped.get(child).size() == 1)
            .count();
        return solePredecessor >= 2;
    }

}

