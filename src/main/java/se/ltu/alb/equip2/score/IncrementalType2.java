package se.ltu.alb.equip2.score;

import org.optaplanner.core.api.score.calculator.IncrementalScoreCalculator;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import java.util.*;

import static ch.rfin.util.Functions.min;
import static ch.rfin.util.Functions.max;
import se.ltu.alb.equip.model.*;
import se.ltu.alb.equip.algo.EarlyLateChangeMoveFilter;
import ch.rfin.util.Bits;
import ch.rfin.util.ImmutableBits;

/**
 * Incremental score calculator for pure type 2 EqALBP.
 * In other words, assumes that station equipment is fixed and the goal is to
 * assign tasks such that we achieve a low cycle time.
 * @author Christoffer Fink
 */
public class IncrementalType2 implements IncrementalScoreCalculator<AssemblyPlan, HardMediumSoftLongScore> {

    // TODO: only support 0-based indexing!

    private boolean dualMode = false;
    private double ctFactor = 2.0;

    /**
     * Set true to enable different score calculations during the construction
     * and local search phase.
     */
    public void setDualMode(boolean flag) {
        dualMode = flag;
    }

    /**
     * Set the factor by which the computed cycle time bound is multiplied
     * to produce the cycle time limit.
     * The default is 2.0.
     */
    public void setCtFactor(double factor) {
        ctFactor = factor;
    }

    // --- State ---

    private int dependencyInversions; // hard
    private int missingEquipment;     // hard
    // Type 2
    private int excessLoad;           // hard   (ONLY CONSTRUCTION)
    private int cycleTimeViolations;  // hard   (ONLY CONSTRUCTION)
    private int cycleTime;            // medium
    private long squaredLoads;        // soft

    // Note that we need to allocate one extra slot so that we can use
    // 1-based task numbers, leaving index 0 empty.
    /** Map stations to their total task times. Just like minIndices. */
    private int[] stationTimes;
    /** Map steps to their assigned station. */
    private int[] stationAssignments;
    /** Map stations to the tasks assigned to them. */
    private Map<Integer,Collection<Integer>> taskAssignments;

    // --- Problem facts ---

    /** Map steps to the steps they depend on. */
    private int[][] taskDependencies;
    /** Map each step to the steps that depend on it. */
    private int[][] precedence;
    /** Map task IDs to the equipment types they need. */
    private Bits[] taskEquipment;
    /** Map station numbers to the equipment types they offer. */
    private Bits[] stationEquipment;

    private int minTaskId = Integer.MAX_VALUE;
    private int maxTaskId = Integer.MIN_VALUE;
    private int minStationNr = Integer.MAX_VALUE;
    private int maxStationNr = Integer.MIN_VALUE;
    private int tasks = -1;
    private int stations = -1;
    private int lowerBoundCycleTime;
    private int ctLimit = -1;

    // --- Auxilliary state ---

    private boolean initialized = false;
    private int expecting;
    // Keep track of the number of uninitialized tasks so that we know when
    // the construction heuristic phase has ended.
    // This is used so that the score can be computed differently during
    // construction.
    private int uninit = 0; // Uninitialized tasks (assuming type 2!)
    private boolean constructionFinished = false;

    @Override
    public void resetWorkingSolution(final AssemblyPlan plan) {
        init(plan);
        reset(plan);

        // TODO: move to separate integrity check method
        assert minTaskId == 0 || minTaskId == 1;
        assert maxTaskId - minTaskId == tasks - 1;
    }

    // Task dependencies are only computed once, since these should be
    // fixed facts for the given problem, and don't change during solving.
    // Reset station assignments.
    // Compute task assignments and update station times at the same time.
    /**
     * Do one-time initialization.
     * This is for setting up the state the first time we see a problem.
     * Resetting the state for the same problem is done in
     * {@link #reset(AssemblyPlan)}.
     */
    private void init(final AssemblyPlan plan) {
        if (initialized) {
            return; // Already initialized. Skip.
        }
        tasks = plan.tasks().size();
        stations = plan.stations().size();
        cycleTime = plan.cycleTime().orElse(0); // Ignore for type 2.
        stationTimes = new int[stations+1];
        taskDependencies = new int[tasks+1][];
        stationAssignments = new int[tasks+1];
        taskEquipment = new Bits[tasks+1];
        final var precedenceMap = new HashMap<Integer,Collection<Integer>>();
        for (final var task : plan.tasks()) {
            final int taskId = task.id();
            final var dependencies = task.taskDependencies();
            final int count = dependencies.size();
            final int[] tmpDep = new int[count];
            int index = 0;
            for (final var dep : dependencies) {
                final int depId = dep.id();
                // Forward dependency.
                tmpDep[index] = depId;
                // Reverse dependency (precedence).
                var tmpPre = precedenceMap.get(depId);
                if (tmpPre == null) {
                    tmpPre = new ArrayList<>();
                    precedenceMap.put(depId, tmpPre);
                }
                tmpPre.add(taskId);
                index++;
            }
            taskDependencies[taskId] = tmpDep;
            taskEquipment[taskId] = Bits.bits(task.equipmentDependencies());
            // Keep track of smallest task ID.
            minTaskId = min(taskId, minTaskId);
            maxTaskId = max(taskId, maxTaskId);
        }
        precedence = new int[tasks+1][];
        for (final var entry : precedenceMap.entrySet()) {
            var id = entry.getKey();
            var tasks = entry.getValue();
            int[] tmp = new int[tasks.size()];
            int index = 0;
            for (final var dep : tasks) {
                tmp[index++] = dep;
            }
            precedence[id] = tmp;
        }
        // Discover min/max indices.
        minStationNr = minTaskId; // 0 or 1, assume indexing is consistent!
        maxStationNr = stations + minStationNr - 1;
        // Equip every station with nothing.
        stationEquipment = new Bits[stations+1];
        taskAssignments = new HashMap<>();
        for (int i = 0; i <= maxStationNr; i++) {
            taskAssignments.put(i, new HashSet<>());
        }
        // Because we are dealing specifically with type 2, we can
        // compute the installed equipment here once.
        // Empty stations of equipment.
        for (int i = 0; i <= maxStationNr; i++) {
            stationEquipment[i] = ImmutableBits.empty();
        }
        // Then equip with current.
        for (final var equip : plan.equipment()) {
            final Station station = equip.station();
            if (station == null) {
                continue;
            }
            final int nr = station.number();
            assert nr >= 0 : "station number >= 0";
            assert nr <= maxStationNr : "station number <= maxStationNr";
            assert stationEquipment[nr] != null : "weird!";
            stationEquipment[nr] = stationEquipment[nr].set(equip.type());
        }
        lowerBoundCycleTime = computeCycleTime(plan);
        ctLimit = (int) (ctFactor * lowerBoundCycleTime);
        initialized = true; // Only initialize once.
    }

    private static int computeCycleTime(AssemblyPlan plan) {
        return EarlyLateChangeMoveFilter.computeCycleTime(plan);
    }

    /**
     * Do resetting that needs to happen on every reset.
     */
    private void reset(final AssemblyPlan plan) {
        assert initialized;
        // Still the same problem.
        assert tasks == plan.tasks().size();
        assert stations == plan.stations().size();
        for (int i = 0; i < stationTimes.length; i++) {
            stationTimes[i] = 0;
        }
        for (int i = 0; i < tasks; i++) {
            stationAssignments[i] = -1; // not assigned
        }
        // Because this is type 2, we only need to reset task assignments.
        // Empty stations of tasks.
        for (int i = 0; i <= maxStationNr; i++) {
            taskAssignments.get(i).clear();
        }
        uninit = 0;
        for (final var task : plan.tasks()) {
            final var station = task.station();
            if (station == null) {
                uninit++;
                continue;
            }
            final int taskId = task.id();
            final int stationNr = task.station().number();
            stationAssignments[taskId] = stationNr;
            stationTimes[stationNr] += task.time();
            taskAssignments.get(stationNr).add(taskId);
        }
        constructionFinished = uninit == 0;
        // Compute score.
        computeScoreFromScratch();
    }

    private void computeScoreFromScratch() {
        // Compute each score feature separately.
        computeDependencyInversions();
        computeMissingEquipment();
        computeCycleTime();
        computeSquaredLoads();
        computeExcessLoad();
        // TODO:
        // Inline the computations here for efficiency.
    }

    // Before.
    void unassign(final Task task) {
        final Station station = task.station();
        if (station == null) {
            return; // Skip.
        }
        uninit++;
        final int taskId = task.id();
        final int stationNr = station.number();
        final long oldLoad = stationTimes[stationNr];
        final long newLoad = oldLoad - task.time();
        taskAssignments.get(stationNr).remove(taskId);
        // Update station times.
        stationTimes[station.number] = (int) newLoad;
        squaredLoads -= oldLoad*oldLoad;
        squaredLoads += newLoad*newLoad;
        // Note: always computing excess load and cycle time is faster
        // (rather than checking whether we're in the construction phase and
        // doing one or the other conditionally)
        if (oldLoad > ctLimit) {
            excessLoad -= oldLoad - ctLimit;  // Remove old excess.
            cycleTimeViolations--;
            if (newLoad > ctLimit) {
                excessLoad += newLoad - ctLimit;  // Add new excess, if any.
                cycleTimeViolations++;
            }
        }
        // Update cycle time. (Only for type 2)
        if (oldLoad == cycleTime) {
            // This station had the maximum station time.
            // However, there might be another station with the same station time.
            // So we need to look at the others to figure out if CT has changed.
            computeCycleTime();
        }

        // Don't need to handle stationAssignments here. That's after.

        // Now handle precedence constraints.
        // Update inversions.
        dependencyInversions -= countInversions(task);
        missingEquipment -= countMissingEquipment(task);
    }

    // After.
    void assign(Task task) {
        final Station station = task.station();
        if (station == null) {
            return; // XXX: NOP (is a task ever unassigned!?)
        }
        uninit--;
        final int stationNr = station.number;
        final int taskId = task.id();
        final long oldLoad = stationTimes[stationNr];
        final long newLoad = oldLoad + task.time();
        taskAssignments.get(stationNr).add(taskId);
        // Update station times.
        stationTimes[stationNr] += task.time();
        // Update sum of squared station loads.
        squaredLoads += newLoad*newLoad - oldLoad*oldLoad;
        if (newLoad > ctLimit) {
            if (oldLoad > ctLimit) {
                excessLoad -= oldLoad - ctLimit;  // Remove old excess, if any.
            } else {
                cycleTimeViolations++;
            }
            excessLoad += newLoad - ctLimit;  // Add new excess.
        }
        // Update cycle time. (Only for type 2)
        if (newLoad > cycleTime) {
            cycleTime = (int) newLoad;
        }

        stationAssignments[taskId] = stationNr;

        // Now handle precedence constraints.
        // Update inversions.
        dependencyInversions += countInversions(task);
        missingEquipment += countMissingEquipment(task);
    }

    private int countMissingEquipment(Task task) {
        return countMissingEquipment(task.id());
    }

    private int countMissingEquipment(int taskId) {
        final var required = taskEquipment[taskId];
        if (required == null) {
            return 0;
        }
        final int station = stationAssignments[taskId];
        if (station == -1) {
            return 0;
        }
        // XXX: looping though types may be faster!
        int count = 0;
        final Bits equipped = stationEquipment[station];
        count += required.size() - equipped.intersection(required).size();
        return count;
    }

    // Note that this depends on stationAssignments being up to date.
    private int countInversions(Task task) {
        if (task.station() == null) {
            return 0;
        }
        return countInversions(task.id());
    }

    // Note that this depends on stationAssignments being up to date.
    /** Count ALL inversions involving this task. */
    private int countInversions(final int id) {
        final int here = stationAssignments[id];
        if (here == -1) {
            return 0; // XXX: hmm, are we counting both sides or not?
        }
        int inversions = 0;
        for (final int dependency : taskDependencies[id]) {
            final int there = stationAssignments[dependency];
            if (there > here || there == -1) {
                // if -1, the step we depend on is not "before".
                inversions++;
            }
        }
        final var dependees = precedence[id];
        if (dependees == null) {
            return inversions;
        }
        for (final int dependee : dependees) {
            final int there = stationAssignments[dependee];
            if (there < here && there != -1) {
                // if -1, the step that depends on us happens "before".
                inversions++;
            }
        }
        return inversions;
    }

    // Compute the inversions from the perspective of this task.
    // In other words, count the number of its dependencies that are not met.
    // We don't consider the number of tasks that might depend on this task.
    // They will get a chance to complain that this task is misplaced when
    // it's their turn.

    // Note that this depends on stationAssignments being up to date.
    private int countInversionsForward(final int id) {
        final int here = stationAssignments[id];
        if (here == -1) {
            return 0;
        }
        int inversions = 0;
        for (final int dependency : taskDependencies[id]) {
            final int there = stationAssignments[dependency];
            if (there > here || there == -1) {
                inversions++;
            }
        }
        return inversions;
    }

    /**
     * Count the number of times a task does not have access to the equipment
     * it needs -- used when computing from scratch.
     */
    private void computeMissingEquipment() {
        missingEquipment = 0;
        for (int i = 0; i < tasks; i++) {
            missingEquipment += countMissingEquipment(i);
        }
    }

    /**
     * Compute cycle time from scratch.
     * This is potentially necessary on each change, since we can only know
     * when the cycle time changes, but can't figure out how it changes.
     * (The cycle time is the maximum station load. So can tell when a station
     * stops having the maximum or 
     */
    private void computeCycleTime() {
        cycleTime = 0;
        for (int i = 0; i < stationTimes.length; i++) {
            if (stationTimes[i] > cycleTime) {
                cycleTime = stationTimes[i];
            }
        }
    }

    // This is used when computing from scratch.
    private void computeDependencyInversions() {
        dependencyInversions = 0;
        for (int i = minTaskId; i <= maxTaskId; i++) {
            final int here = stationAssignments[i];
            if (here != -1) {
                dependencyInversions += countInversionsForward(i);
            }
        }
    }

    private void computeExcessLoad() {
        excessLoad = 0;
        cycleTimeViolations = 0;
        for (int i = 0; i < stationTimes.length; i++) {
            if (stationTimes[i] > ctLimit) {
                excessLoad += (stationTimes[i] - ctLimit);
                cycleTimeViolations++;
            }
        }
    }

    /**
     * Compute sum of squared station loads from scratch.
     * This is only necessary when resetting the working solution.
     * Incremental changes are taken care of in the (un)assign methods.
     */
    private void computeSquaredLoads() {
        squaredLoads = 0;
        for (int i = 0; i < stationTimes.length; i++) {
            final long load = stationTimes[i];
            squaredLoads += load*load;
        }
    }

    @Override
    public HardMediumSoftLongScore calculateScore() {
        // Note: It's important that constructionFinished is updated BEFORE
        // checking which score calculation to use.
        // We want to switch to the local search version when the construction
        // phase is ABOUT TO end. Otherwise local search may fail to find a
        // better solution, because the score of a better solution is still
        // worse based on the new criteria.
        constructionFinished = uninit == 0;
        if (dualMode && !constructionFinished) {
            return calculateScoreConstruction();
        }
        final var score = calculateScoreLocalSearch();
        return score;
    }

    public HardMediumSoftLongScore calculateScoreLocalSearch() {
        final int hard = -(dependencyInversions + missingEquipment);
        final int medium = -cycleTime;
        final long soft = -squaredLoads;
        final var score = HardMediumSoftLongScore.of(hard, medium, soft);
        return score;
    }

    public HardMediumSoftLongScore calculateScoreConstruction() {
        final int hard = -(dependencyInversions + missingEquipment);
        final int medium = -excessLoad;
        final long soft = 0;
        final var score = HardMediumSoftLongScore.of(hard, medium, soft);
        return score;
    }

    // Note:
    // No need to check the type of the entity, since we are only
    // (un)assigning tasks.

    // The variable name doesn't matter, since there only is one variable.
    @Override
    public void beforeVariableChanged(Object entity, String name) {
        unassign((Task) entity);
    }

    @Override
    public void afterVariableChanged(Object entity, String name) {
        assign((Task) entity);
    }

    void unassign(final Object obj) {
        throw new AssertionError("Impossible: entity neither Task nor Equipment: " + obj);
    }

    void assign(final Object obj) {
        throw new AssertionError("Impossible: entity neither Task nor Equipment: " + obj);
    }

    @Override
    public void beforeEntityRemoved(Object entity) {
        throw new AssertionError("beforeEntityRemoved");
        // Entities are never added or removed. Ignore.
    }

    @Override
    public void afterEntityRemoved(Object entity) {
        throw new AssertionError("afterEntityRemoved");
        // Entities are never added or removed. Ignore.
    }

    @Override
    public void beforeEntityAdded(Object entity) {
        throw new AssertionError("beforeEntityAdded");
        // Entities are never added or removed. Ignore.
    }

    @Override
    public void afterEntityAdded(Object entity) {
        throw new AssertionError("afterEntityAdded");
        // Entities are never added or removed. Ignore.
    }

}
