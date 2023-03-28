package se.ltu.alb.salbp2.score;

import java.util.*;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;

import se.ltu.alb.salbp.model.*;

/**
 * Like {@link se.ltu.alb.salbp2.score.HardSoftInline}, but uses squared
 * station times for the soft score.
 * Hard: precedence violations. Medium: cycle time. Soft: sum of squared
 * station times.
 * 
 * @author Christoffer Fink
 */
public class HardMediumSoftInline implements EasyScoreCalculator<AssemblyPlan, HardMediumSoftLongScore> {

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
        int cycleTime = 0;
        long sumSquaredTimes = 0;
        for (final var stationTime : stationTimes.values()) {
            if (stationTime > cycleTime) {
                cycleTime = stationTime;
            }
            sumSquaredTimes += stationTime * stationTime;
        }

        final int hard = -dependencyInversions;
        final int medium = -cycleTime;
        final long soft = -sumSquaredTimes;
        return HardMediumSoftLongScore.of(hard, medium, soft);
    }

}
