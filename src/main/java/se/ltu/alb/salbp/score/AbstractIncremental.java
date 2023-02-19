package se.ltu.alb.salbp.score;

import java.util.*;
import org.optaplanner.core.api.score.calculator.IncrementalScoreCalculator;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import se.ltu.alb.salbp.model.*;
import static ch.rfin.util.Functions.min;
import static ch.rfin.util.Functions.max;
import static ch.rfin.util.Functions.ceil;
import static ch.rfin.util.Printing.printf;

// We need two base classes. One that makes it easy to extend in salbp1/2 and
// one that is more general and works only with integers (task IDs and station
// numbers, etc) that is independent of model.
//
// In other words, it's useful to gather what is common to type 1 and 2, and
// it's also useful to gather what is common to any ALB problem.

/*
 * Naming conventions:
 * - computeXYZ methods a score component/feature from scratch by examining
 *   the current solution in its entirety.
 * - updateXYZ methods update a score component/feature based on a single
 *   change.
 */

/**
 * Abstract Incremental Score Calculator for use in either SALBP-1 or SALBP-2.
 * Computes the relevant score features incrementally, but does not actually
 * compute a final Score. In other words,
 * {@link IncrementalScoreCalculator#calculateScore()} must be overridden.
 * <p>
 * Note: Currently computes features for both SALBP-1 and SALBP-2, and extending
 * classes choose which to use in the score.
 * It might be both cleaner and faster to outsource the computations of some of
 * these to the extending classes.
 * @author Christoffer Fink
 */
public abstract class AbstractIncremental implements IncrementalScoreCalculator<AssemblyPlan, HardMediumSoftLongScore> {

    public boolean addLowerBound = false;
    public double lowerBoundMarginFactor = 1.0;

    /* FIXME: This is not actually true!
     * If enabled, the lower bound on the number of stations or cycle time
     * (depending on whether SALBP type 1 or 2 is being solved) is added to the
     * medium score.
     */
    /**
     * Set whether adding the lower bound is enabled
     * (<strong>currently type 1 only</strong>).
     * If enabled, the lower bound on the number of stations
     * is added to the medium score.
     * This means a score of 0 (or above) is considered the best
     * possible score, which can be set as a best score limit.
     * Then the solver can stop early when the lower bound has been achieved.
     */
    public void setAddLowerBound(boolean flag) {
        this.addLowerBound = flag;
    }

    /**
     * Sets a margin factor for the lower bound.
     * This margin allows the best score limit to be triggered when the
     * solution is <em>close</em> to the lower bound.
     * For example, to add a 10% margin, use factor {@code 1.1}.
     * If the lower bound is 100, a 10 % margin would mean that
     * 110 is added to the score.
     */
    public void setLowerBoundMarginFactor(double factor) {
        setAddLowerBound(true);
        this.lowerBoundMarginFactor = factor;
    }

    // TODO: instead of handling the two different indexing modes, maybe
    // just require that it's always 0-based?
    // Leave it up to the caller to make sure the problem is expressed
    // as required.
    // It should be trivial to translate to 0-based and back.
    // Actually, they would also have to be consecutive, which also shouldn't
    // be a problem.

    // ----- State -----

    protected int dependencyInversions; // hard
    protected int cycleTimeViolations;  // hard
    protected int stationsUsed;         // medium (SALBP-1)
    protected int cycleTime;            // medium (SALBP-2)
    protected int excessLoad;           // soft
    protected long squaredLoads;        // soft
    protected int stationsLB;           // lower bound on stations
    protected int cycleTimeLB;          // lower bound on cycle time
    // Note:
    // excessLoad is only relevant for SALBP-1, and only when there are
    // cycle time violations. So if a custom construction procedure is used
    // that produces feasible solutions, it's not relevant.
    // squaredLoads is relevant for both SALBP-1 and SALBP-2, but in opposite
    // ways. For SALBP-1 it's a reward (+); for SALBP-2 it's a penalty (-).
    // It's a measure of unevenness, which means that, for SALBP-1, it's
    // a measure of how much tasks are packed into a small number of stations.
    // In SALBP-2, we want to minimize it, since it means the cycle time is
    // minimized.

    // Excess load is used as a more fine-grained version of cycle time
    // violations.

    // Note that we need to allocate one extra slot so that we can use
    // 1-based task numbers, leaving index 0 empty.
    /**
     * Map stations to their total task times.
     * Use station numbers as the index. The values are station loads.
     */
    protected int[] stationTimes;
    /**
     * Map tasks to their assigned station.
     * Use task IDs as the index. The values are station numbers.
     */
    protected int[] stationAssignments;

    // ----- Problem facts -----

    /** Map tasks to the tasks they depend on. */
    protected int[][] taskDependencies;
    /** Map each step to the steps that depend on it. */
    protected int[][] precedence;

    protected int minTaskId = Integer.MAX_VALUE;
    protected int maxTaskId = Integer.MIN_VALUE;
    protected int minStationNr = Integer.MAX_VALUE;
    protected int maxStationNr = Integer.MIN_VALUE;
    protected int tasks = -1;
    protected int stations = -1;
    protected int targetCycleTime = -1;

    // ----- Auxilliary state -----

    protected boolean initialized = false;
    protected int expecting;
    protected AssemblyPlan workingSolution = null;

    // ----- Settings -----

    /**
     * Include unassigned tasks when counting inversions.
     * That is, rather than only conting tasks that are assigned to the wrong
     * station, also count those that have not been assigned to ANY station.
     * While doing so may in a sense be more accurate, it only applies during
     * construction, and then it ruins the Initializing Score Trend.
     * If missing dependencies are NOT counted, then the trend is ONLY_DOWN,
     * since assigning the task is either feasible (neutral) or not (negative).
     * This is a valuable optimization.
     * If missing dependencies ARE counted, then the score can improve when
     * missing tasks are assigned.
     */
    protected boolean countMissingDepAsInversion = false;

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
    /**
     * Do one-time initialization.
     * This is for setting up the state the first time we see a problem.
     * Resetting the state for the same problem is done in
     * {@link #reset(AssemblyPlan)}.
     */
    protected void init(final AssemblyPlan plan) {
        if (initialized) {
            return; // Already initialized. Skip.
        }
        tasks = plan.tasks().size();
        stations = plan.stations().size();
        targetCycleTime = plan.cycleTime().orElse(-1);
        stationTimes = new int[stations+1];
        taskDependencies = new int[tasks+1][];
        stationAssignments = new int[tasks+1];
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
        if (addLowerBound) {
            if (targetCycleTime > -1) {
                double totalTaskTime = plan.tasks().stream()
                    .mapToDouble(Step::time)
                    .sum();
                double lowerBound = totalTaskTime / targetCycleTime;
                stationsLB = ceil(lowerBound * lowerBoundMarginFactor);
            }
        }
        initialized = true; // Only initialize once.
    }

    /**
     * Do resetting that needs to happen on every reset.
     */
    protected void reset(final AssemblyPlan plan) {
        // Must be initialized before we can reset.
        assert initialized;
        // The calculator assumes that it will be used for the same problem
        // instance. Check that we're (probably) resetting the same instance.
        assert tasks == plan.tasks().size();
        //assert stations == plan.stations().size();    // Not compatible with strip unused
        // Reset...
        workingSolution = plan;
        for (int i = 0; i < stationTimes.length; i++) {
            stationTimes[i] = 0;
        }
        for (int i = 0; i < tasks; i++) {
            stationAssignments[i] = -1; // not assigned
        }
        for (final var task : plan.tasks()) {
            final var station = task.station();
            if (station == null) {
                continue;
            }
            final int taskId = task.id();
            final int stationNr = task.station().number();
            stationAssignments[taskId] = stationNr;
            stationTimes[stationNr] += task.time();
        }
        // Compute score.
        computeScoreFromScratch();
    }

    //*
    private void computeScoreFromScratch() {
        // Compute each score feature separately.
        computeDependencyInversions();
        computeCycleTimeViolations();
        computeStationsUsed();
        computeExcessLoad();    // XXX: remove?
        computeSquaredLoads();
        computeCycleTime();
        // TODO:
        // Inline the computations here for efficiency.
    }
    //*/

    // Inlined version of computeScoreFromScratch
    // Computes
    // - dependencyInversions
    // - cycleTimeViolations
    // - stationsUsed
    // - excessLoad
    // but not squaredLoads (TODO)
    /*
    private void computeScoreFromScratch() {
        dependencyInversions = 0;
        for (int i = minTaskId; i <= maxTaskId; i++) {
            final int here = stationAssignments[i];
            if (here != -1) {
                dependencyInversions += countInversionsForward(i);
            }
        }
        cycleTimeViolations = 0;
        stationsUsed = 0;
        excessLoad = 0;
        for (int i = 0; i < stationTimes.length; i++) {
            if (stationTimes[i] > 0) {
                stationsUsed++;
                if (stationTimes[i] > targetCycleTime) {
                    cycleTimeViolations++;
                    excessLoad += (stationTimes[i] - targetCycleTime);
                }
            }
        }
    }
    //*/

    // Before.
    void unassign(final Step task, final Station station) {
        final int taskId = task.id();
        final int stationNr = station.number();
        final int oldLoad = stationTimes[stationNr];
        final int newLoad = oldLoad - task.time();
        // Update station times.
        stationTimes[station.number] = newLoad;
        squaredLoads -= 1L * oldLoad * oldLoad;
        squaredLoads += 1L * newLoad * newLoad;
        // Update stations used.
        // This is only necessary when this station becomes empty.
        if (oldLoad > 0 && newLoad == 0) {
            stationsUsed--;
        }
        if (targetCycleTime > 0) {
            if (oldLoad > targetCycleTime && newLoad <= targetCycleTime) {
                cycleTimeViolations--;
            }
            if (oldLoad > targetCycleTime) {
                excessLoad -= oldLoad - targetCycleTime;  // Remove old excess.
                if (newLoad > targetCycleTime) {
                    excessLoad += newLoad - targetCycleTime;  // Add new excess, if any.
                }
            }
        } else {
            // Update cycle time.
            // This is only (potentially) necessary when this station has the
            // max load (hence defines the cycle time).
            if (oldLoad == cycleTime) {
                computeCycleTime();
            }
        }

        // Don't need to handle stationAssignments here. That's after.

        // Now handle precedence constraints.

        // Update inversions.
        dependencyInversions -= countInversions(task);
    }

    void assignNull(Step task) {
    }

    // After.
    void assign(Step task, Station station) {
        final int stationNr = station.number;
        final int taskId = task.id();
        final int oldLoad = stationTimes[stationNr];
        final int newLoad = oldLoad + task.time();
        // Update station times.
        stationTimes[stationNr] += task.time();
        squaredLoads += (1L * newLoad * newLoad) - (1L * oldLoad * oldLoad);
        // Update stations used.
        if (oldLoad == 0 && newLoad > 0) {
            stationsUsed++;
        }
        if (targetCycleTime > 0) {
            // Update cycle time violations.
            if (oldLoad <= targetCycleTime && newLoad > targetCycleTime) {
                cycleTimeViolations++;
            }
            if (newLoad > targetCycleTime) {
                if (oldLoad > targetCycleTime) {
                    excessLoad -= oldLoad - targetCycleTime;  // Remove old excess, if any.
                }
                excessLoad += newLoad - targetCycleTime;  // Add new excess.
            }
        } else {
            // Update cycle time.
            // This is only (potentially) necessary when a station load
            // rises ABOVE the current cycle time.
            if (newLoad > cycleTime) {
                cycleTime = newLoad;  // Ah! No need to recompute from scratch!
            }
        }

        stationAssignments[taskId] = stationNr;

        // Now handle precedence constraints.

        // Update inversions.
        dependencyInversions += countInversions(task);
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

    // Note that this depends on stationAssignments being up to date.
    private int countInversions(Step task) {
        if (task.station() == null) {
            return 0;
        }
        return countInversions(task.id());
    }

    // Note that this depends on stationAssignments being up to date.
    private int countInversions(final int id) {
        final int here = stationAssignments[id];
        if (here == -1) {
            return 0;
        }
        int inversions = 0;
        for (final int dependency : taskDependencies[id]) {
            final int there = stationAssignments[dependency];
            if (there > here || (countMissingDepAsInversion && there == -1)) {
                // if -1, the step we depend on is not "before", because it's
                // not anywhere.
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

    /**
     * Compute the inversions from the perspective of this task.
     * In other words, count the number of its dependencies that are not met.
     * We don't consider the number of tasks that might depend on this task.
     * They will get a chance to complain that this task is misplaced when
     * it's their turn.
     * Note that this depends on stationAssignments being up to date.
     */
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

    private void updateInversions() {
    }

    /**
     * Compute sum of squared station loads from scratch.
     * This is only necessary when resetting the working solution.
     * Incremental changes are taken care of in the (un)assign methods.
     */
    private void computeSquaredLoads() {
        squaredLoads = 0;
        for (int i = 0; i < stationTimes.length; i++) {
            final int load = stationTimes[i];
            squaredLoads += load*load;
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

    // This is used when computing from scratch.
    private void computeCycleTimeViolations() {
        cycleTimeViolations = 0;
        for (int i = 0; i < stationTimes.length; i++) {
            if (stationTimes[i] > targetCycleTime) {
                cycleTimeViolations++;
            }
        }
    }

    /**
     * Compute number of stations used from scratch.
     */
    private void computeStationsUsed() {
        stationsUsed = 0;
        for (int i = 0; i < stationTimes.length; i++) {
            if (stationTimes[i] > 0) {
                stationsUsed++;
            }
        }
    }

    private void computeExcessLoad() {
        excessLoad = 0;
        for (int i = 0; i < stationTimes.length; i++) {
            if (stationTimes[i] > targetCycleTime) {
                excessLoad += (stationTimes[i] - targetCycleTime);
            }
        }
    }

    public long getDependencyInversions() {
        return dependencyInversions;
    }

    public long getCycleTimeViolations() {
        return cycleTimeViolations;
    }

    public long getStationsUsed() {
        return stationsUsed;
    }

    public long getExcessLoad() {
        return excessLoad;
    }

    public long getSquaredStationTimes() {
        return squaredLoads;
    }

    // The variable name doesn't matter, since there only is one variable.
    @Override
    public void beforeVariableChanged(Object entity, String name) {
        var task = (Step) entity;
        expecting = task.id();  // For catching testing errors.
        var station = task.station();
        if (station != null) {
            unassign(task, station);
        }
    }

    @Override
    public void afterVariableChanged(Object entity, String name) {
        var task = (Step) entity;
        assert task.id() == expecting;  // Catch testing errors.
        var station = task.station();
        if (station != null) {
            assign(task, station);
        }
    }

    @Override
    public void beforeEntityRemoved(Object entity) {
        System.out.println("beforeEntityRemoved");
        throw new AssertionError("beforeEntityRemoved");
        // Entities are never added or removed. Ignore.
    }

    @Override
    public void afterEntityRemoved(Object entity) {
        System.out.println("afterEntityRemoved");
        throw new AssertionError("afterEntityRemoved");
        // Entities are never added or removed. Ignore.
    }

    @Override
    public void beforeEntityAdded(Object entity) {
        System.out.println("beforeEntityAdded");
        throw new AssertionError("beforeEntityAdded");
        // Entities are never added or removed. Ignore.
    }

    @Override
    public void afterEntityAdded(Object entity) {
        System.out.println("afterEntityAdded");
        throw new AssertionError("afterEntityAdded");
        // Entities are never added or removed. Ignore.
    }

}
