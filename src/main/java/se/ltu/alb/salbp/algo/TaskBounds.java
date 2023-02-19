package se.ltu.alb.salbp.algo;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import se.ltu.alb.salbp.model.AssemblyPlan;
import se.ltu.alb.salbp.model.Step;
import ch.rfin.util.Pair;
import static ch.rfin.util.Functions.ceil;
import static ch.rfin.util.Functions.floor;
import static ch.rfin.util.Functions.max;

/**
 * Compute bounds for specific tasks, such as what the earliest and latest
 * possible stations are.
 * @see se.ltu.alb.salbp.algo.EarlyLateChangeMoveFilter
 * @author Christoffer Fink
 */
public class TaskBounds {

    public static int earliestStation(Step step, int cycleTime) {
        int time = step.time() + step.deepDependencies()
            .stream()
            .mapToInt(Step::time)
            .sum();
        return floor(1.0 * time / cycleTime);
    }

    /**
     * Get the latest stations for this task.
     * The margin factor is used to multiply the cycle time limit to add
     * a "fudge factor" when computing the latest station.
     * A larger margin increases the range, which reduces the risk that
     * feasible stations are excluded.
     * A smaller margin reduces the range, which excludes more stations and
     * shrinks the search space.
     * The margin factor should be greater than or equal to 1.0.
     * @param factor fudge factor for the upper limit
     */
    public static int latestStation(Step step, int cycleTime, Collection<Step> steps, double factor) {
        int time = step.time() + steps
            .stream()
            .filter(task -> !task.deepDependencies().contains(step))
            .mapToInt(Step::time)
            .sum();
        /* When comparing 1.0 vs 1.5 vs 2.0, smaller values work better for
         * the 100-task instances, and larger values work better for the
         * 1000-task instances.
         * 1.5 seems to strike the best balance.
         */
        return ceil(factor * time / cycleTime);
    }

    /**
     * Get the earliest/latest stations for each task.
     * @see #earliestStation(Step, int)
     * @see #latestStation(Step, int, Collection, double)
     */
    public static Map<Step, Pair<Integer,Integer>> stationBounds(AssemblyPlan plan, double factor) {
        final Collection<Step> tasks = plan.tasks();
        final int cycleTime = getCycleTime(plan);
        Map<Step, Pair<Integer,Integer>> intervals = new HashMap<>();
        for (final var task : tasks) {
            int min = earliestStation(task, cycleTime);
            int max = latestStation(task, cycleTime, tasks, factor);
            intervals.put(task, Pair.of(min, max));
        }
        return intervals;
    }


    // max(avgTime, maxTime) is better than avgTime
    /**
     * Return the cycle time limit if set (type 1) or compute a lower bound
     * other wise (type 2).
     * The lower bound is the maximum of average station time and max task time.
     */
    public static int getCycleTime(AssemblyPlan plan) {
        if (plan.cycleTime().isPresent()) {
            return plan.cycleTime().getAsInt();
        }
        final int totalTime = plan.tasks().stream().mapToInt(Step::time).sum();
        final int avgTime = totalTime / plan.stations().size();
        final int maxTime = plan.tasks().stream().mapToInt(Step::time).max().getAsInt();
        return max(avgTime, maxTime);
    }

}
