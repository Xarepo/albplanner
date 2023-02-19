package se.ltu.alb.salbp1.score;

import java.util.*;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;

import se.ltu.alb.salbp.model.*;
import se.ltu.alb.salbp.score.ScoreFeatures;

/**
 * Relatively efficient easy score calculator.
 * Can use {@link se.ltu.alb.salbp1.score.HardSoftCalculator} as a reference.
 * @author Christoffer Fink
 */
public class HardSoftInline implements EasyScoreCalculator<AssemblyPlan, HardMediumSoftLongScore> {

    private boolean testing = false;
    private HardSoftCalculator reference = null;

    /**
     * Control whether the computed score should be compared to a reference
     * score calculator.
     */
    public void setTesting(boolean flag) {
        testing = flag;
        if (flag && reference == null) {
            reference = new HardSoftCalculator();
        }
    }

    @Override
    public HardMediumSoftLongScore calculateScore(AssemblyPlan plan) {
        final Map<Integer,Integer> stationTimes = new HashMap<>();
        int dependencyInversions = 0;
        for (final var task : plan.tasks()) {
            final Station station = task.station();
            if (station == null) {
                continue;
            }
            final int stationNr = station.number();
            for (final var dependency : task.taskDependencies()) {
                final Station depStation = dependency.station();
                if (depStation == null) {
                    continue;
                }
                if (depStation.number() > stationNr) {
                    dependencyInversions++;
                }
            }
            final int stationTime = stationTimes.getOrDefault(stationNr, 0);
            stationTimes.put(stationNr, stationTime + task.time());
        }
        final int cycleTime = plan.cycleTime().getAsInt();
        int cycleTimeViolations = 0;
        for (final var stationTime : stationTimes.values()) {
            if (stationTime > cycleTime) {
                cycleTimeViolations++;
            }
        }

        final int hard = -(dependencyInversions + cycleTimeViolations);
        final int medium = -stationTimes.size();
        final var score = HardMediumSoftLongScore.of(hard, medium, 0);
        if (testing) {
            checkScore(plan, score);
        }
        return score;
    }

    private void checkScore(AssemblyPlan plan, HardMediumSoftLongScore score) {
        final var ref = reference.calculateScore(plan);
        if (!ref.equals(score)) {
            throw new AssertionError(score + " != " + ref);
        }
    }

}
