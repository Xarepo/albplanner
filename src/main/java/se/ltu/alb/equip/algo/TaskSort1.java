package se.ltu.alb.equip.algo;

import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import java.util.*;
import se.ltu.alb.equip.model.AssemblyPlan;
import se.ltu.alb.equip.model.Task;
import static ch.rfin.util.Graphs.flipEdges;
import static ch.rfin.util.Graphs.topologicalLayerMap;
import static se.ltu.alb.util.Util.buildDependencyGraph;

/**
 * A selection sorter weight factory that sorts tasks in topological order.
 *
 * @author Christoffer Fink
 */
public class TaskSort1 implements SelectionSorterWeightFactory<AssemblyPlan, Task> {

    private Map<Task, Integer> weightMap;

    public static int compare(Map<Task, Integer> layerMap, Task t1, Task t2) {
        return compare_a(layerMap, t1, t2);
    }

    /**
     * Compare by layer (lower first), then number of successors (higher first),
     * then task time (higher first), then ID to break any ties.
     */
    public static int compare_a(Map<Task, Integer> layerMap, Task t1, Task t2) {
        // Layer
        final int layer1 = layerMap.get(t1);
        final int layer2 = layerMap.get(t2);
        if (layer1 < layer2) {
            return -1; // task 1 is in a lower layer & should come before task 2
        } else if (layer1 > layer2) {
            return 1;
        }
        // XXX: Maybe it doesn't just matter how many types of equipment the
        // tasks depend on, but also how rare they are?
        final int equip1 = t1.equipmentDependencies().size();
        final int equip2 = t2.equipmentDependencies().size();
        if (equip1 > equip2) {
            return -1;  // task 1 needs more equipment ⇒ more difficult
        } else if (equip1 < equip2) {
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

    private void initWeightMap(final AssemblyPlan plan) {
        final Map<Task,Collection<Task>> dag = buildDependencyGraph(plan.tasks(), Task::taskDependencies);
        final Map<Task, Integer> layerMap = topologicalLayerMap(dag);
        //final Map<Task,Collection<Task>> dependees = flipEdges(dag);
        List<Task> taskList = new ArrayList<>(plan.tasks());
        Comparator<Task> comp = (t1, t2) -> {
            //return compare(layerMap, dependees, t1, t2);
            return compare(layerMap, t1, t2);
        };
        Collections.sort(taskList, comp);
        weightMap = new HashMap<>();
        for (int i = 0; i < taskList.size(); i++) {
            weightMap.put(taskList.get(i), i);
        }
    }

    @Override
    public Integer createSorterWeight(final AssemblyPlan plan, final Task task) {
        if (weightMap == null) {
            initWeightMap(plan);
        }
        return -weightMap.get(task);
    }

}
