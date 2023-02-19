package se.ltu.alb.salbp.algo;

import java.util.Map;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import org.optaplanner.core.impl.heuristic.selector.move.generic.ChangeMove;
import org.optaplanner.core.api.score.director.ScoreDirector;
import se.ltu.alb.salbp.model.AssemblyPlan;
import se.ltu.alb.salbp.model.Station;
import se.ltu.alb.salbp.model.Step;
import ch.rfin.util.Pair;

/**
 * Value selection filter that only allows tasks to be assigned to a range of
 * (earliest/latest) stations.
 * The earliest/latest station range is based on the cycle time.
 * @see se.ltu.alb.salbp.algo.TaskBounds
 * @author Christoffer Fink
 */
public class EarlyLateChangeMoveFilter implements SelectionFilter<AssemblyPlan, ChangeMove<AssemblyPlan>> {

    private Map<Step, Pair<Integer, Integer>> stationIntervals = null;

    private double marginFactor = 1.5;

    // Hmm, can't pass in custom properties via XML!?
    public void setMarginFactor(double factor) {
        assert stationIntervals == null : "Can't change factor after init";
        marginFactor = factor;
    }

    private void initIntervals(AssemblyPlan plan) {
        if (stationIntervals != null) {
            return;
        }
        stationIntervals = TaskBounds.stationBounds(plan, marginFactor);
    }

    @Override
    public boolean accept(ScoreDirector<AssemblyPlan> director, ChangeMove<AssemblyPlan> move) {
        return accept(director.getWorkingSolution(), move);
    }

    public boolean accept(AssemblyPlan plan, ChangeMove<AssemblyPlan> move) {
        initIntervals(plan);
        final Step task = (Step) move.getEntity();
        final Station station = (Station) move.getToPlanningValue();
        final var interval = stationIntervals.get(task);
        final int min = interval._1;
        final int max = interval._2;
        return station.number >= min && station.number <= max;
    }

}
