package se.ltu.alb.equip.algo;

import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import org.optaplanner.core.impl.heuristic.selector.move.generic.SwapMove;
import org.optaplanner.core.api.score.director.ScoreDirector;
import se.ltu.alb.equip.model.AssemblyPlan;
import se.ltu.alb.equip.model.Task;

/**
 * Swap-move filter that prevents two tasks from being swapped if one depends
 * (either directly or transitively) on the other.
 * Note that this assumes that tasks do not start out being out of order.
 * Under that assumption, swapping them must always yield an unfeasible solution.
 * (If the current working solution has precedence constraint violations, then
 * this filter would prevent them from being swapped into the correct order.)
 * <p>
 * This filter is light-weight in the sense that there is no need to examine
 * the current working solution or any other problem facts.
 * @author Christoffer Fink
 */
public class NeverSwapDependentMoveFilter implements SelectionFilter<AssemblyPlan, SwapMove<AssemblyPlan>> {

    @Override
    public boolean accept(ScoreDirector<AssemblyPlan> director, SwapMove<AssemblyPlan> move) {
        return accept(director.getWorkingSolution(), move);
    }

    public boolean accept(AssemblyPlan plan, SwapMove<AssemblyPlan> move) {
        final Task task1 = (Task) move.getLeftEntity();
        final Task task2 = (Task) move.getRightEntity();
        if (task1.deepDependencies().contains(task2)) {
            return false;
        }
        if (task2.deepDependencies().contains(task1)) {
            return false;
        }
        return true;
    }

}
