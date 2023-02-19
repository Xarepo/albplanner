package se.ltu.alb.salbp1.score;

import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import se.ltu.alb.salbp.score.AbstractIncremental;

/**
 * Incremental score calculator for SALBP-1.
 * <strong>TODO:</strong> describe the score features.
 * @author Christoffer Fink
 */
public class IncrementalCalculator extends AbstractIncremental {

    @Override
    public HardMediumSoftLongScore calculateScore() {
        final int hard = -(dependencyInversions + cycleTimeViolations);
        final int medium = -stationsUsed + stationsLB;
        final long soft = excessLoad > 0
            ? -excessLoad
            : squaredLoads;
        final var score = HardMediumSoftLongScore.of(hard, medium, soft);
        return score;
    }
}
