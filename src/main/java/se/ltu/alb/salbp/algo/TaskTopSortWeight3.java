package se.ltu.alb.salbp.algo;

import java.util.*;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import se.ltu.alb.salbp.model.AssemblyPlan;
import se.ltu.alb.salbp.model.Step;
import static se.ltu.alb.util.Util.buildDependencyGraph;
import static ch.rfin.util.Graphs.flipEdges;
import static ch.rfin.util.Graphs.topologicalLayerMap;

/**
 * A selection sorter weight factory that sorts tasks in topological order.
 * Within each layer, the tasks are also sorted by task time (higher first).
 * This is implemented by {@link #compare_d(Map, Map, Step, Step)}.
 * <p>
 * Contains several different comparing methods that use different combinations
 * of additional sorting criteria.
 * These exist for experimentation purposes, and there is no way to specify
 * which one to use.
 *
 * @author Christoffer Fink
 */
public class TaskTopSortWeight3 implements SelectionSorterWeightFactory<AssemblyPlan, Step> {

    private Map<Step, Integer> weightMap;

    public static int compare(Map<Step, Integer> layerMap, Map<Step, Collection<Step>> dependees, Step t1, Step t2) {
        return compare_d(layerMap, dependees, t1, t2);
    }

    private void initWeightMap(final AssemblyPlan plan) {
        final Map<Step,Collection<Step>> dag = buildDependencyGraph(plan.tasks(), Step::taskDependencies);
        final Map<Step, Integer> layerMap = topologicalLayerMap(dag);
        final Map<Step,Collection<Step>> dependees = flipEdges(dag);
        List<Step> taskList = new ArrayList<>(plan.tasks());
        Comparator<Step> comp = (t1, t2) -> {
            return compare(layerMap, dependees, t1, t2);
        };
        Collections.sort(taskList, comp);
        weightMap = new HashMap<>();
        for (int i = 0; i < taskList.size(); i++) {
            weightMap.put(taskList.get(i), i);
        }
    }

    @Override
    public Integer createSorterWeight(final AssemblyPlan plan, final Step task) {
        if (weightMap == null) {
            initWeightMap(plan);
        }
        // Minus is the correct sign if we want tasks without dependencies to
        // be assigned first.
        return -weightMap.get(task);
    }

    /*
    // Note that using deep predecessors (dependencies) is worse
    // than immediate predecessors. I.e. this is worse:
    if (t1.deepDependencies().size() < t2.deepDependencies().size()) {
        return -1;
    } else if (t1.deepDependencies().size() > t2.deepDependencies().size()) {
        return 1;
    }
    */

    // layer, successors, predecessors, time
    // 14830 (with early-late change move filter)
    /**
     * Compare by layer (lower first), then number of dependees (higher first),
     * then task time (higher first), then ID to break any ties.
     */
    public static int compare_a(Map<Step, Integer> layerMap, Map<Step, Collection<Step>> dependees, Step t1, Step t2) {
        // Layer
        final int layer1 = layerMap.get(t1);
        final int layer2 = layerMap.get(t2);
        if (layer1 < layer2) {
            return -1; // task 1 is in a lower layer & should come before task 2
        } else if (layer1 > layer2) {
            return 1;
        }
        // Successors
        final int dependees1 = dependees.get(t1).size();
        final int dependees2 = dependees.get(t2).size();
        if (dependees1 > dependees2) {
            return -1; // more tasks tepend on task 1; task 1 comes first
        } else if (dependees1 < dependees2) {
            return 1;
        }
        // Predecessors
        if (t1.taskDependencies().size() < t2.taskDependencies().size()) {
            return -1;
        } else if (t1.taskDependencies().size() > t2.taskDependencies().size()) {
            return 1;
        }
        // Task time
        if (t1.time() > t2.time()) {
            return -1;  // task 1 takes more time ⇒ more difficult
        } else if (t1.time() < t2.time()) {
            return 1;
        }
        // Arbitrarily break ties by picking smaller ID first.
        return Integer.compare(t1.id(), t2.id());
    }

    // layer, successors, time, predecessors
    // 14824 (with early-late change move filter)
    /**
     * Compare by layer (lower first), then number of dependees (higher first),
     * then task time (higher first), then ID to break any ties.
     */
    public static int compare_b(Map<Step, Integer> layerMap, Map<Step, Collection<Step>> dependees, Step t1, Step t2) {
        // Layer
        final int layer1 = layerMap.get(t1);
        final int layer2 = layerMap.get(t2);
        if (layer1 < layer2) {
            return -1; // task 1 is in a lower layer & should come before task 2
        } else if (layer1 > layer2) {
            return 1;
        }
        // Successors
        final int dependees1 = dependees.get(t1).size();
        final int dependees2 = dependees.get(t2).size();
        if (dependees1 > dependees2) {
            return -1; // more tasks tepend on task 1; task 1 comes first
        } else if (dependees1 < dependees2) {
            return 1;
        }
        // Task time
        if (t1.time() > t2.time()) {
            return -1;  // task 1 takes more time ⇒ more difficult
        } else if (t1.time() < t2.time()) {
            return 1;
        }
        // Predecessors
        if (t1.taskDependencies().size() < t2.taskDependencies().size()) {
            return -1;
        } else if (t1.taskDependencies().size() > t2.taskDependencies().size()) {
            return 1;
        }
        // Arbitrarily break ties by picking smaller ID first.
        return Integer.compare(t1.id(), t2.id());
    }

    // layer, successors, time
    // 14816 (with early-late change move filter)
    /**
     * Compare by layer (lower first), then number of dependees (higher first),
     * then task time (higher first), then ID to break any ties.
     */
    public static int compare_c(Map<Step, Integer> layerMap, Map<Step, Collection<Step>> dependees, Step t1, Step t2) {
        // Layer
        final int layer1 = layerMap.get(t1);
        final int layer2 = layerMap.get(t2);
        if (layer1 < layer2) {
            return -1; // task 1 is in a lower layer & should come before task 2
        } else if (layer1 > layer2) {
            return 1;
        }
        // Successors
        final int dependees1 = dependees.get(t1).size();
        final int dependees2 = dependees.get(t2).size();
        if (dependees1 > dependees2) {
            return -1; // more tasks tepend on task 1; task 1 comes first
        } else if (dependees1 < dependees2) {
            return 1;
        }
        // Task time
        if (t1.time() > t2.time()) {
            return -1;  // task 1 takes more time ⇒ more difficult
        } else if (t1.time() < t2.time()) {
            return 1;
        }
        // Arbitrarily break ties by picking smaller ID first.
        return Integer.compare(t1.id(), t2.id());
    }

    // layer, time
    // 14803 (with early-late change move filter)
    /**
     * Compare by layer (lower first), then task time (higher first), then ID
     * to break any ties.
     */
    public static int compare_d(Map<Step, Integer> layerMap, Map<Step, Collection<Step>> dependees, Step t1, Step t2) {
        // Layer
        final int layer1 = layerMap.get(t1);
        final int layer2 = layerMap.get(t2);
        if (layer1 < layer2) {
            return -1; // task 1 is in a lower layer & should come before task 2
        } else if (layer1 > layer2) {
            return 1;
        }
        // Task time
        if (t1.time() > t2.time()) {
            return -1;  // task 1 takes more time ⇒ more difficult
        } else if (t1.time() < t2.time()) {
            return 1;
        }
        // Arbitrarily break ties by picking smaller ID first.
        return Integer.compare(t1.id(), t2.id());
    }

    /**
     * Compare by layer (lower first), then ID to break any ties.
     */
    public static int compare_e(Map<Step, Integer> layerMap, Map<Step, Collection<Step>> dependees, Step t1, Step t2) {
        // Layer
        final int layer1 = layerMap.get(t1);
        final int layer2 = layerMap.get(t2);
        if (layer1 < layer2) {
            return -1; // task 1 is in a lower layer & should come before task 2
        } else if (layer1 > layer2) {
            return 1;
        }
        // Arbitrarily break ties by picking smaller ID first.
        return Integer.compare(t1.id(), t2.id());
    }

}
