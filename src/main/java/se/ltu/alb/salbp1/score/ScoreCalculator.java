package se.ltu.alb.salbp1.score;

import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import se.ltu.alb.salbp.model.AssemblyPlan;
import static se.ltu.alb.salbp.score.ScoreFeatures.*;

// TODO: should be renamed to HardMediumSoftCalculator
/**
 * Reference easy score calculator.
 * Slow but "obviously correct". Uses
 * {@link se.ltu.alb.salbp.score.ScoreFeatures} to compute the score features.
 * <p>
 * <ul>
 * <li>Hard constraints:
 *   <ul>
 *   <li>Dependency violations
 *   <li>Exceeding the cycle time
 *   </ul>
 * <li>Medium:
 *   <ul>
 *   <li>Number of stations (this is what we're actually trying to minimize)
 *   </ul>
 * <li>Soft:
 *   <ul>
 *   <li>Sum of squared station times (to guide the solver)
 *   </ul>
 * </ul>
 */
public class ScoreCalculator implements EasyScoreCalculator<AssemblyPlan, HardMediumSoftLongScore> {

    @Override
    public HardMediumSoftLongScore calculateScore(AssemblyPlan plan) {
        long inversions = countDependencyInversionsIgnoringNulls(plan);
        long cycleTimeViolations = countCycleTimeViolations(plan);
        long numberOfStations = countUsedStations(plan);
        long hard = -(inversions + cycleTimeViolations);
        long medium = -numberOfStations;
        // The sum of the squared station loads is a measure of how unevenly
        // tasks are distributed. So minimizing this maximizes smoothness.
        // By giving the unevenness as a positive score rather than a penalty,
        // we can force tasks to be packed together.
        long soft = sumSquaredStationLoads(plan);
        return HardMediumSoftLongScore.of(hard, medium, soft);
    }

}
