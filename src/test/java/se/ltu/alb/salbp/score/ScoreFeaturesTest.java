package se.ltu.alb.salbp.score;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.OptionalInt;
import se.ltu.alb.salbp.model.*;
import static se.ltu.alb.salbp.model.Step.step;
import static se.ltu.alb.salbp.score.ScoreFeatures.*;

/**
 * Test score features.
 * Every test case examines some solution to a problem with this dependency DAG:
 * <pre>
 * 0   3
 *  \ /  \
 *   2-4--6
 *  / \     \
 * 1   5-7-8-9
 *
 * Dependencies:
 * {0:[], 1:[], 2:[0,1], 3:[2], 4:[2], 5:[2], 6:[3,4], 7:[5], 8:[7], 9:[6,8]}
 * </pre>
 */
public class ScoreFeaturesTest {

    public static final Step step0 = step(0, List.of(), 10, null);
    public static final Step step1 = step(1, List.of(), 11, null);
    public static final Step step2 = step(2, List.of(step0, step1), 12, null);
    public static final Step step3 = step(3, List.of(step2), 13, null);
    public static final Step step4 = step(4, List.of(step2), 14, null);
    public static final Step step5 = step(5, List.of(step2), 15, null);
    public static final Step step6 = step(6, List.of(step3, step4), 16, null);
    public static final Step step7 = step(7, List.of(step5), 17, null);
    public static final Step step8 = step(8, List.of(step7), 18, null);
    public static final Step step9 = step(9, List.of(step6, step8), 19, null);
    public static final List<Step> tasks = List.of(
            step0, step1, step2, step3, step4,
            step5, step6, step7, step8, step9);

    public static final Station station0 = Station.station(0);
    public static final Station station1 = Station.station(1);
    public static final Station station2 = Station.station(2);
    public static final Station station3 = Station.station(3);
    public static final Station station4 = Station.station(4);
    public static final Station station5 = Station.station(5);
    public static final Station station6 = Station.station(6);
    public static final Station station7 = Station.station(7);
    public static final Station station8 = Station.station(8);
    public static final Station station9 = Station.station(9);
    public static final List<Station> stations = List.of(
            station0, station1, station2, station3, station4,
            station5, station6, station7, station8, station9);
    private static final AssemblyPlan shared = new AssemblyPlan(tasks, stations);

    @Test
    public void test_feasible_1() {
        assertFeasibleStrict(feasible1());
    }

    @Test
    public void test_feasible_2() {
        assertFeasible(feasible2());
    }

    @Test
    public void test_feasible_3() {
        assertFeasible(feasible3());
    }

    @Test
    public void feasible_1_to_012_etc() {
        final AssemblyPlan plan = feasible1();
        // Move step 2 forward.
        step2.station(station0);  // [0,1,2|...]
        assertFeasible(plan);
        assertEquals(2, countDependencyInversionsStrict(plan));
    }

    @Test
    public void feasible_1_to_12_0_etc() {
        final AssemblyPlan plan = feasible1();
        // Move task 2 forward and task 0 back.
        step2.station(station0);  // [0,1,2|...]
        step0.station(station1);  // [1,2|0|...] (Inversion! 2 depends on 0)
        assertEquals(1, countDependencyInversions(plan));
        assertEquals(1, countDeepDependencyInversions(plan));
        assertEquals(1, totalDependencyDistance(plan));
    }

    @Test
    public void feasible_1_to_019_etc() {
        final AssemblyPlan plan = feasible1();
        // Move step 9 forward.
        step9.station(station0);  // [0,1,9|...] (Inversion! 9 depends on 6,8)
        assertEquals(2, countDependencyInversions(plan));
        assertEquals(7, countDeepDependencyInversions(plan));
        // Task 9 is 4 stations from task 8 and 3 stations from task 6.
        assertEquals(4+3, totalDependencyDistance(plan));
    }

    @Test
    public void test_totalStationsUsed_1() {
        final AssemblyPlan plan = feasible1();
        assertEquals(6, totalStationsUsed(plan));
        step9.station(station6);
        // Check that gaps are also counted.
        assertEquals(7, totalStationsUsed(plan));
        step8.station(station8);
        assertEquals(9, totalStationsUsed(plan));
    }

    @Test
    public void test_totalStationsUsed_empty_solution() {
        final AssemblyPlan plan = reset();
        assertEquals(0, totalStationsUsed(plan));
    }

    // Check that leading empty stations are also counted.
    @Test
    public void test_totalStationsUsed_3() {
        final AssemblyPlan plan = feasible3();
        // Move task 0 to station 7.
        step0.station(station7);
        assertEquals(8, totalStationsUsed(plan));
    }

    @Test
    public void test_countUsedStations() {
        final AssemblyPlan plan = withGaps();
        // [__|1,2|__|__|3|4,5|6|__|__|__]
        assertEquals(4, countUsedStations(plan));
    }

    @Test
    public void test_countUnusedStations() {
        final AssemblyPlan plan = withGaps();
        // [__|1,2|__|__|3|4,5|6|__|__|__]
        assertEquals(6, countUnusedStations(plan));
    }

    @Test
    public void test_totalStationsUsed() {
        final AssemblyPlan plan = withGaps();
        // [__|1,2|__|__|3|4,5|6|__|__|__]
        assertEquals(7, totalStationsUsed(plan));
    }

    @Test
    public void test_countStationGaps() {
        final AssemblyPlan plan = withGaps();
        // [__|1,2|__|__|3|4,5|6|__|__|__]
        assertEquals(3, countStationGaps(plan));
    }

    @Test
    public void test_countCycleTimeViolations_empty() {
        AssemblyPlan plan = reset().cycleTime(1);
        assertEquals(0, countCycleTimeViolations(plan));
    }

    @Test
    public void test_countCycleTimeViolations_no_violations() {
        AssemblyPlan plan = feasible1().cycleTime(100);
        assertEquals(0, countCycleTimeViolations(plan));
    }

    @Test
    public void test_countCycleTimeViolations_all_violate() {
        AssemblyPlan plan = feasible1().cycleTime(11);
        assertEquals(6, countCycleTimeViolations(plan));
    }

    @Test
    public void test_countCycleTimeViolations_some_violate() {
        AssemblyPlan plan = feasible1().cycleTime(18);
        // 21, 42, 33, 19
        assertEquals(4, countCycleTimeViolations(plan));
    }

    @Test
    public void test_totalExcessLoad_empty() {
        AssemblyPlan plan = reset().cycleTime(1);
        assertEquals(0, totalExcessLoad(plan));
    }

    @Test
    public void test_totalExcessLoad_no_violations() {
        AssemblyPlan plan = feasible1().cycleTime(100);
        assertEquals(0, totalExcessLoad(plan));
    }

    @Test
    public void test_totalExcessLoad_all_violate() {
        AssemblyPlan plan = feasible1().cycleTime(11);
        // Station times: 21, 12, 42, 33, 18, 19
        // Excess: 10 + 1 + 31 + 22 + 7 + 8 = 79
        assertEquals(79, totalExcessLoad(plan));
    }

    @Test
    public void test_totalExcessLoad_some_violate() {
        AssemblyPlan plan = feasible1().cycleTime(18);
        // Station times (above cycle time): 21, 42, 33, 19
        // Excess: 3 + 24 + 15 + 1 = 43
        assertEquals(43, totalExcessLoad(plan));
    }

    @Test
    public void test_sumSquaredStationLoads_empty() {
        AssemblyPlan plan = reset();
        assertEquals(0, sumSquaredStationLoads(plan));
    }

    @Test
    public void test_sumSquaredStationLoads() {
        AssemblyPlan plan = feasible1();
        // Station times: 21, 12, 42, 33, 18, 19
        // Squared: 441 + 144 + 1764 + 1089 + 324 + 361 = 4123
        assertEquals(4123, sumSquaredStationLoads(plan));
    }


    /**
     * Check that all score features that are associated with hard constraints
     * are zero.  Expects a feasible plan!
     */
    private void assertFeasible(AssemblyPlan plan) {
        assertEquals(0, countDependencyInversions(plan));
        assertEquals(0, countDeepDependencyInversions(plan));
        assertEquals(0, totalDependencyDistance(plan));
    }

    /**
     * Check that all score features that are associated with hard constraints
     * are zero when considering strict precedence.  Expects a feasible plan!
     */
    private void assertFeasibleStrict(AssemblyPlan plan) {
        assertFeasible(plan);
        assertEquals(0, countDependencyInversionsStrict(plan));
    }

    /**
     * Make a (strictly) feasible solution with assignments: 
     * [0,1|2|3,4,5|6,7|8|9].
     * Station times: [21, 12, 42, 33, 18, 19].
     * Note that tasks are assigned to stations by modifying a Task.
     * So changing the assignments for this plan will modify any other plan
     * that uses the tasks and stations defined in this class.
     * While it would be possible to make a deep clone, that would make it
     * less convenient to assign stations, since we would have to get the
     * actual Task and Station instances from the plan.
     */
    public static AssemblyPlan feasible1() {
        reset();
        step0.station(station0);  // [0
        step1.station(station0);  // [0,1
        step2.station(station1);  // [0,1|2
        step3.station(station2);  // [0,1|2|3
        step4.station(station2);  // [0,1|2|3,4
        step5.station(station2);  // [0,1|2|3,4,5
        step6.station(station3);  // [0,1|2|3,4,5|6
        step7.station(station3);  // [0,1|2|3,4,5|6,7
        step8.station(station4);  // [0,1|2|3,4,5|6,7|8
        step9.station(station5);  // [0,1|2|3,4,5|6,7|8|9]
        return shared;
    }

    /**
     * Make a (non-strictly) feasible solution with assignments: 
     * [0,1,2|5,7,8|3,4,6,9].
     */
    public static AssemblyPlan feasible2() {
        reset();
        step0.station(station0);  // [0
        step1.station(station0);  // [0,1
        step2.station(station0);  // [0,1,2
        step5.station(station1);  // [0,1,2|5
        step7.station(station1);  // [0,1,2|5,7
        step8.station(station1);  // [0,1,2|5,7,8
        step3.station(station2);  // [0,1,2|5,7,8|3
        step4.station(station2);  // [0,1,2|5,7,8|3,4
        step6.station(station2);  // [0,1,2|5,7,8|3,4,6
        step9.station(station2);  // [0,1,2|5,7,8|3,4,6,9]
        return shared;
    }

    /**
     * Make a (strictly) feasible solution with assignments: 
     * [0|1,2|3,4|5,6|7,8|9].
     */
    public static AssemblyPlan feasible3() {
        reset();
        step0.station(station0);  // [0
        step1.station(station1);  // [0|1
        step2.station(station1);  // [0|1,2
        step3.station(station2);  // [0|1,2|3
        step4.station(station2);  // [0|1,2|3,4
        step5.station(station3);  // [0|1,2|3,4|5
        step6.station(station3);  // [0|1,2|3,4|5,6
        step7.station(station4);  // [0|1,2|3,4|5,6|7
        step8.station(station4);  // [0|1,2|3,4|5,6|7,8
        step9.station(station5);  // [0|1,2|3,4|5,6|7,8|9]
        return shared;
    }

    /**
     * Make a solution with gaps: 
     * [__|1,2|__|__|3|4,5|6|__|__|__].
     */
    public static AssemblyPlan withGaps() {
        reset();
        // [__|1,2|__|__|3|4,5|6|__|__|__]
        step1.station(station1);
        step2.station(station1);
        step3.station(station4);
        step4.station(station5);
        step5.station(station5);
        step6.station(station6);
        return shared;
    }

    /**
     * Make a an empty solution with no assignments
     * ([...]).
     */
    public static AssemblyPlan reset() {
        shared.cycleTime(OptionalInt.empty());
        step0.station(null);
        step1.station(null);
        step2.station(null);
        step3.station(null);
        step4.station(null);
        step5.station(null);
        step6.station(null);
        step7.station(null);
        step8.station(null);
        step9.station(null);
        return shared;
    }

}
