package se.ltu.alb.equip.score;

import java.util.*;
import java.util.function.Function;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;

import se.ltu.alb.equip.model.*;
import se.ltu.alb.equip.score.ScoreFeatures;
import se.ltu.alb.equip.algo.EarlyLateChangeMoveFilter;

import ch.rfin.util.Bits;
import ch.rfin.util.ImmutableBits;
import static ch.rfin.util.Functions.max;
import static ch.rfin.util.Functions.min;
import static ch.rfin.util.Functions.ceil;

/**
 * An easy score calculator that is optimized for performance.
 * Rather than computing different score features from scratch, they are
 * (mostly) computed inline in a single loop.
 * <p>
 * This score calculator is suitable <strong>only for pure type 2</strong>
 * problems.
 * @author Christoffer Fink
 */
public class HardMediumSoftInline implements EasyScoreCalculator<AssemblyPlan, HardMediumSoftLongScore> {

    public boolean constructionFinished = false;
    private boolean dualMode = true;
    private double ctFactor = 2.0;
    private Function<AssemblyPlan, HardMediumSoftLongScore> chpScore = this::calculateScoreConstruction_3;

    public void setCtFactor(double factor) {
        ctFactor = factor;
    }

    public void setDualMode(boolean flag) {
        dualMode = flag;
    }

    public void setConstructionVersion(int version) {
        switch (version) {
            case 0: chpScore = this::calculateScoreConstruction_0; break;
            case 1: chpScore = this::calculateScoreConstruction_1; break;
            case 2: chpScore = this::calculateScoreConstruction_2; break;
            case 3: chpScore = this::calculateScoreConstruction_3; break;
            case 11: chpScore = this::calculateScoreConstruction_11; break;
            default:
                throw new IllegalArgumentException("Illegal argument: " + version);
        }
    }

    // --- State ---

    private boolean initialized = false;

    // Static
    private final Map<Integer, Bits> equipped = new HashMap<>(); // station ↦ equipment
    private final Map<Integer, Bits> equipmentDependencies = new HashMap<>();
    private int cycleTimeBound = -1;
    // Dynamic
    private final Map<Integer,Integer> stationTimes = new HashMap<>();
    private final List<Integer> stationTimeDiffs = new ArrayList<>();
    private final Map<Integer, Bits> needed = new HashMap<>(); // station ↦ equipment
    private final Map<Task, Integer> assignedTasks = new HashMap<>();
    private int dependencyInversions = -1;
    private int missingEquipment = -1;
    private long sumSquaredTimes = -1;
    //private int equipmentNeededAtStations = -1;
    private int cycleTime = -1;
    private int cycleTimeViolations = -1;
    private int stationsUsed = -1;
    private int excessLoad = -1;
    private int unusedEquipment = -1;

    /**
     * Initialize static state -- runs only once.
     * Subsequent invocations are NOPs.
     * This is called by {@link #reset(AssemblyPlan)}. So there is no need
     * to call it directly.
     */
    void init(AssemblyPlan plan) {
        if (initialized) {
            return;
        }
        for (final var station : plan.stations()) {
            equipped.put(station.number(), ImmutableBits.empty());
        }
        for (final var equip : plan.equipment()) {
            final var station = equip.station();
            if (station == null) {
                continue;
            }
            final var equippedAt = equipped.get(station.number());
            equipped.put(station.number(), equippedAt.set(equip.type()));
        }
        for (final var task : plan.tasks()) {
            equipmentDependencies.put(task.id(), Bits.bits(task.equipmentDependencies()));
        }
        cycleTimeBound = computeCycleTime(plan);
        initialized = true;
    }

    /**
     * Update dynamic state -- runs each time the score is computed.
     * Always calls {@link #init(AssemblyPlan)}.
     */
    void reset(AssemblyPlan plan) {
        init(plan);
        assignedTasks.clear();
        stationTimes.clear();
        stationTimeDiffs.clear();
        needed.clear();
        dependencyInversions = 0;
        missingEquipment = 0;
        cycleTime = 0;
        sumSquaredTimes = 0;
        int uninit = 0;
        for (final var task : plan.tasks()) {
            final Station station = task.station();
            if (station == null) {
                uninit++;
                continue;
            }
            final int stationNr = station.number();
            assignedTasks.put(task, stationNr);
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
            // Equipment
            final var equippedAt = equipped.get(stationNr);
            for (final var type : task.equipmentDependencies()) {
                if (!equippedAt.get(type)) {
                    missingEquipment++;
                }
                final var neededAt = needed.getOrDefault(stationNr, ImmutableBits.empty());
                needed.put(station.number(), neededAt.set(type));
            }
            // NOTE: This is slower!
            /*
            final var equippedAt = equipped.get(stationNr);
            final var required = equipmentDependencies.get(task.id());
            final var neededAt = needed.getOrDefault(stationNr, ImmutableBits.empty());
            needed.put(station.number(), neededAt.union(required));
            missingEquipment += required.size() - equippedAt.intersection(required).size();
            */
        }
        stationsUsed = stationTimes.size();
        for (final int stationTime : stationTimes.values()) {
            cycleTime = max(cycleTime, stationTime);
            sumSquaredTimes += 1L * stationTime * stationTime;
            stationTimeDiffs.add(stationTime - cycleTimeBound);
        }
        constructionFinished = uninit == 0;
    }

    @Override
    public HardMediumSoftLongScore calculateScore(AssemblyPlan plan) {
        reset(plan);
        if (dualMode && !constructionFinished) {
            return calculateScoreConstruction(plan);
        } else {
            return calculateScoreLocalSearch(plan);
        }
    }

    HardMediumSoftLongScore calculateScoreConstruction(AssemblyPlan plan) {
        return chpScore.apply(plan);
    }

    HardMediumSoftLongScore calculateScoreConstruction_3(AssemblyPlan plan) {
        final int timeDiffPenalty = computeCycleTimeDiffPenalty();
        final int hard = -(dependencyInversions + missingEquipment + excessLoad);
        final int medium = -timeDiffPenalty;
        final long soft = computeUnusedEquipment();
        return HardMediumSoftLongScore.of(hard, medium, soft);
    }

    HardMediumSoftLongScore calculateScoreConstruction_2(AssemblyPlan plan) {
        final int excessLoad = computeExcessLoad();
        final int hard = -(dependencyInversions + missingEquipment + excessLoad);
        final int medium = 0;
        final long soft = computeUnusedEquipment();
        return HardMediumSoftLongScore.of(hard, medium, soft);
    }

    HardMediumSoftLongScore calculateScoreConstruction_11(AssemblyPlan plan) {
        final int cycleTimeViolations = computeCycleTimeViolations();
        final int hard = -(dependencyInversions + missingEquipment + cycleTimeViolations);
        final int medium = 0;
        final long soft = 0;
        return HardMediumSoftLongScore.of(hard, medium, soft);
    }

    HardMediumSoftLongScore calculateScoreConstruction_1(AssemblyPlan plan) {
        final int excessLoad = computeExcessLoad();
        final int hard = -(dependencyInversions + missingEquipment + excessLoad);
        final int medium = 0;
        final long soft = 0;
        return HardMediumSoftLongScore.of(hard, medium, soft);
    }

    HardMediumSoftLongScore calculateScoreConstruction_0(AssemblyPlan plan) {
        final int hard = -(dependencyInversions + missingEquipment);
        final int medium = 0;
        final long soft = 0;
        return HardMediumSoftLongScore.of(hard, medium, soft);
    }

    HardMediumSoftLongScore calculateScoreLocalSearch(AssemblyPlan plan) {
        final int hard = -(dependencyInversions + missingEquipment);
        final int medium = -cycleTime;
        final long soft = -sumSquaredTimes;
        return HardMediumSoftLongScore.of(hard, medium, soft);
    }


    // Using the EqALB-specific cycle time computation.
    // This gives significantly improved solutions.
    public static int computeCycleTime(AssemblyPlan plan) {
        return EarlyLateChangeMoveFilter.computeCycleTime(plan);
    }


    int computeExcessLoad() {
        excessLoad = 0;
        final int hardLimit = (int) (ctFactor * cycleTimeBound);
        for (final var stationTime : stationTimes.values()) {
            final int aboveHardLimit = max(0, stationTime - hardLimit);
            excessLoad += aboveHardLimit;
        }
        return excessLoad;
    }

    int computeCycleTimeViolations() {
        cycleTimeViolations = 0;
        final int hardLimit = (int) (ctFactor * cycleTimeBound);
        for (final var stationTime : stationTimes.values()) {
            if (stationTime > hardLimit) {
                cycleTimeViolations++;
            }
        }
        return cycleTimeViolations;
    }

    int computeUnusedEquipment() {
        unusedEquipment = 0;
        for (final var entry : assignedTasks.entrySet()) {
            final Task task = entry.getKey();
            final Integer stationNr = entry.getValue();
            final var required = equipmentDependencies.get(task.id());
            final var equippedAt = equipped.get(stationNr);
            unusedEquipment += equippedAt.size() - required.size();
        }
        return unusedEquipment;
    }

    int computeCycleTimeDiffPenalty() {
        excessLoad = 0;
        int penalty = stationsUsed * cycleTimeBound;
        final int softLimit = (int) (1.5 * cycleTimeBound);
        final int hardLimit = (int) (ctFactor * cycleTimeBound);
        for (final int stationTime : stationTimes.values()) {
            final int aboveHardLimit = max(0, stationTime - hardLimit);
            excessLoad += aboveHardLimit;
            final int belowCT = min(stationTime, softLimit);
            penalty -= belowCT;
            final int aboveSoftLimit = max(0, stationTime - softLimit);
            penalty += aboveSoftLimit;
        }
        return penalty;
    }

}
