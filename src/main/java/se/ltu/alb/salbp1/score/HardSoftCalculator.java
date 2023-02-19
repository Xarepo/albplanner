package se.ltu.alb.salbp1.score;

import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;

import se.ltu.alb.salbp.model.AssemblyPlan;
import static se.ltu.alb.salbp.score.ScoreFeatures.*;

/**
 * Reference easy score calculator (without sum of squared station times).
 * Exactly like {@link se.ltu.alb.salbp1.score.ScoreCalculator}, but does
 * not use sum of squared station times as a soft component.
 * <em>The soft score is always 0.</em>
 * @author Christoffer Fink
 */
public class HardSoftCalculator implements EasyScoreCalculator<AssemblyPlan, HardMediumSoftLongScore> {

    @Override
    public HardMediumSoftLongScore calculateScore(AssemblyPlan plan) {
        long inversions = countDependencyInversionsIgnoringNulls(plan);
        long cycleTimeViolations = countCycleTimeViolations(plan);
        long numberOfStations = countUsedStations(plan);
        long hard = -(inversions + cycleTimeViolations);
        long medium = -numberOfStations;
        return HardMediumSoftLongScore.of(hard, medium, 0);
    }

}
