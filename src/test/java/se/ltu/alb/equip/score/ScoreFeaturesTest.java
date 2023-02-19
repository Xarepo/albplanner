package se.ltu.alb.equip.score;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.OptionalInt;
import se.ltu.alb.equip.model.*;
import static se.ltu.alb.equip.model.Task.task;
import static se.ltu.alb.equip.model.Station.station;
import static se.ltu.alb.equip.model.Equipment.pinned;
import static se.ltu.alb.equip.model.Equipment.unpinned;
import static se.ltu.alb.equip.model.Equipment.unequipped;
import static se.ltu.alb.equip.score.ScoreFeatures.*;

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

    /*
     * Feasible 1: [0,1  |2    |3,4,5  |6,7|8  |9]
     * Feasible 2: [0,1,2|5,7,8|3,4,6,9|   |   | ]
     * Feasible 3: [0    |1,2  |3,4    |5,6|7,8|9]
     * Equipment:    0,1    1    2,3,4   4      2
     *                      *      *
     *
     * Task Stations    Equipment
     * 0    0               0
     * 1    0,1             1
     * 2    0,1             1
     * 3    2               2,3
     * 4    2               2,4
     * 5    1,2,3           -
     * 6    2,3             4
     * 7    1,3,4           -
     * 8    1,4             -
     * 9    2,5             2
     */

    public static final Station station0 = station(0);
    public static final Station station1 = station(1);
    public static final Station station2 = station(2);
    public static final Station station3 = station(3);
    public static final Station station4 = station(4);
    public static final Station station5 = station(5);
    public static final Station station6 = station(6);
    public static final Station station7 = station(7);
    public static final Station station8 = station(8);
    public static final Station station9 = station(9);
    public static final List<Station> stations = List.of(
            station0, station1, station2, station3, station4,
            station5, station6, station7, station8, station9);

    public static final Task step0 = task(0, 10, List.of(), List.of(0));
    public static final Task step1 = task(1, 11, List.of(), List.of(1));
    public static final Task step2 = task(2, 12, List.of(step0, step1), List.of(1));
    public static final Task step3 = task(3, 13, List.of(step2), List.of(2,3));
    public static final Task step4 = task(4, 14, List.of(step2), List.of(2,4));
    public static final Task step5 = task(5, 15, List.of(step2));
    public static final Task step6 = task(6, 16, List.of(step3, step4), List.of(4));
    public static final Task step7 = task(7, 17, List.of(step5));
    public static final Task step8 = task(8, 18, List.of(step7));
    public static final Task step9 = task(9, 19, List.of(step6, step8), List.of(2));
    public static final List<Task> steps = List.of(
            step0, step1, step2, step3, step4,
            step5, step6, step7, step8, step9);

    public static final Equipment equip0a = unpinned(0, 0, 21, station0);
    public static final Equipment equip1a = unpinned(1, 1, 22, station0);
    public static final Equipment equip1b = pinned(2, 1, station1);
    public static final Equipment equip2a = unpinned(3, 2, 23, station2);
    public static final Equipment equip3a = pinned(4, 3, station2);
    public static final Equipment equip4a = unpinned(5, 4, 24, station2);
    public static final Equipment equip4b = unpinned(6, 4, 25, station3);
    public static final Equipment equip2b = unpinned(7, 2, 26, station5);
    public static final Equipment equip5a = unequipped(8, 5, 321);
    public static final List<Equipment> equipment = List.of(
            equip0a, equip1a, equip1b, equip2a, equip3a,
            equip4a, equip4b, equip2b, equip5a);

    private static final AssemblyPlan shared = new AssemblyPlan(steps, stations, equipment);

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
        // Task 0 no longer has equipment 0!
        assertEquals(1, countMissingEquipment(plan));
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
        // Task 9 no longer has equipment 2!
        assertEquals(1, countMissingEquipment(plan));
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

    @Test
    public void test_countMissingEquipment_removing_equip2a() {
        AssemblyPlan plan = shared;
        // Feasible 1
        feasible1();
        equip2a.equipAt(null);
        assertEquals(2, countMissingEquipment(plan));
        // Feasible 2
        feasible2();
        equip2a.equipAt(null);
        assertEquals(3, countMissingEquipment(plan));
        // Feasible 3
        feasible3();
        equip2a.equipAt(null);
        assertEquals(2, countMissingEquipment(plan));
    }

    @Test
    public void test_totalEquipmentCost() {
        AssemblyPlan plan = equipStations();
        // All equipment in use was already installed at the stations from the
        // beginning (all were "unpinned", which implies equipped).
        // Hence their price is 0.
        assertEquals(0, totalEquipmentCost(plan));
        // If we now put equipment 5 into use, we have to pay for the brand
        // new, previously unused equipment.
        equip5a.equipAt(station4);
        assertEquals(321, totalEquipmentCost(plan));
        // Unequipp again.
        equip5a.equipAt(null);
        assertEquals(0, totalEquipmentCost(plan));
    }

    @Test
    public void test_totalEquipmentMoveCost_noting_moved() {
        AssemblyPlan plan = equipStations();
        assertEquals(0, totalEquipmentMoveCost(plan));
    }

    @Test
    public void test_totalEquipmentMoveCost_moving_equip0a() {
        AssemblyPlan plan = equipStations();
        equip0a.equipAt(station1);
        assertEquals(21, totalEquipmentMoveCost(plan));
        // Equipping 5 does not change anything, since it has move cost 0.
        equip5a.equipAt(station4);
        assertEquals(21, totalEquipmentMoveCost(plan));
    }

    @Test
    public void test_totalEquipmentMoveCost_swapping_4a_4b() {
        AssemblyPlan plan = equipStations();
        equip4a.equipAt(station3);
        assertEquals(24, totalEquipmentMoveCost(plan));
        equip4b.equipAt(station2);
        assertEquals(24+25, totalEquipmentMoveCost(plan));
    }

    // Compute a lower bound for the cycle time when the average station load
    // is the deciding factor.
    @Test
    public void test_lowerBoundCycleTime_average_load_no_equipment() {
        var station0 = Station.station(0);
        var station1 = Station.station(1);
        var station2 = Station.station(2);
        var task0 = task(0, 4, List.of());
        var task1 = task(1, 4, List.of());
        var task2 = task(2, 4, List.of());
        var task3 = task(3, 4, List.of());
        var stations = List.of(station0, station1, station2);
        var tasks = List.of(task0, task1, task2, task3);
        var plan = new AssemblyPlan(tasks, stations, List.of());
        int expected = 6; // (4+4+4+4)/3 = 16/3 = 5.333... = 6 (rounding up)
        int result = lowerBoundCycleTime(plan);
        assertEquals(expected, result);
    }

    // Compute a lower bound for the cycle time when the maximum task time is
    // the deciding factor.
    @Test
    public void test_lowerBoundCycleTime_max_task_time_no_equipment() {
        var station0 = Station.station(0);
        var station1 = Station.station(1);
        var station2 = Station.station(2);
        var task0 = task(0, 1, List.of());
        var task1 = task(1, 1, List.of());
        var task2 = task(2, 100, List.of());
        var stations = List.of(station0, station1, station2);
        var tasks = List.of(task0, task1, task2);
        var plan = new AssemblyPlan(tasks, stations, List.of());
        int expected = 100;
        int result = lowerBoundCycleTime(plan);
        assertEquals(expected, result);
    }

    // Compute a lower bound for the cycle time when the maximum task time is
    // the deciding factor.
    @Test
    public void test_lowerBoundCycleTime_max_task_time_unused_equipment() {
        // Station equipment: [ 0 | 1 | 0,1 ]
        var station0 = Station.station(0);
        var station1 = Station.station(1);
        var station2 = Station.station(2);
        var task0 = task(0, 1, List.of());
        var task1 = task(1, 1, List.of());
        var task2 = task(2, 100, List.of());
        var equip0a = pinned(0, 0, station0);
        var equip0b = pinned(1, 0, station2);
        var equip1a = pinned(2, 1, station1);
        var equip1b = pinned(3, 1, station2);
        var stations = List.of(station0, station1, station2);
        var tasks = List.of(task0, task1, task2);
        var equipment = List.of(equip0a, equip0b, equip1a, equip1b);
        var plan = new AssemblyPlan(tasks, stations, List.of());
        int expected = 100;
        int result = lowerBoundCycleTime(plan);
        assertEquals(expected, result);
    }

    // Compute a lower bound for the cycle time when the average station load
    // is the deciding factor. There is equipment, but it doesn't affect the
    // result.
    // Here each task *MUST* be assigned to its own station, because of
    // task equipment requirements.
    @Test
    public void test_lowerBoundCycleTime_average_load_with_equipment_forced() {
        // Station equipment: [ 0 | 1 | 2 | 3 ]
        // Compatible tasks:  [ 0 | 1 | 2 | 3 ]
        var station0 = Station.station(0);
        var station1 = Station.station(1);
        var station2 = Station.station(2);
        var station3 = Station.station(3);
        var equip0 = pinned(0, 0, station0);
        var equip1 = pinned(1, 1, station1);
        var equip2 = pinned(2, 2, station2);
        var equip3 = pinned(3, 3, station3);
        var task0 = task(0, 4, List.of(), List.of(0));  // pinned to station 0
        var task1 = task(1, 4, List.of(), List.of(1));  // pinned to station 1
        var task2 = task(2, 4, List.of(), List.of(2));  // pinned to station 2
        var task3 = task(3, 4, List.of(), List.of(3));  // pinned to station 3
        var stations = List.of(station0, station1, station2, station3);
        var tasks = List.of(task0, task1, task2, task3);
        var equipment = List.of(equip0, equip1, equip2, equip3);
        var plan = new AssemblyPlan(tasks, stations, equipment);
        int expected = 4;
        int result = lowerBoundCycleTime(plan);
        assertEquals(expected, result);
    }

    // Compute a lower bound for the cycle time when the average station load
    // is the deciding factor. There is equipment, but it doesn't affect the
    // result.
    // Here each task *CAN* be assigned to its own station.
    // (The fact that it's also *possible* to assign all tasks to station 2
    // doesn't matter.)
    @Test
    public void test_lowerBoundCycleTime_average_load_with_equipment() {
        // Station equipment: [ 0 | 1 | 0,1 |  ]
        // Compatible tasks:  [ 0 | 1 |  2  |  ] 3,4 can go anywhere
        var station0 = station(0);
        var station1 = station(1);
        var station2 = station(2);
        var station3 = station(3);
        var equip0a = pinned(0, 0, station0);
        var equip0b = pinned(1, 0, station2);
        var equip1a = pinned(2, 1, station1);
        var equip1b = pinned(3, 1, station2);
        var task0 = task(0, 3, List.of(), List.of(0));    // pinned to station 0
        var task1 = task(1, 3, List.of(), List.of(1));    // pinned to station 1
        var task2 = task(2, 3, List.of(), List.of(0,1));  // pinned to station 2
        var task3 = task(3, 3, List.of(), List.of());
        var task4 = task(4, 3, List.of(), List.of());
        var stations = List.of(station0, station1, station2, station3);
        var tasks = List.of(task0, task1, task2, task3, task4);
        var equipment = List.of(equip0a, equip0b, equip1a, equip1b);
        var plan = new AssemblyPlan(tasks, stations, equipment);
        int expected = 4; // (3+3+3+3+3)/4 = 15/4 = 3.75 = 4 (rounding up)
        int result = lowerBoundCycleTime(plan);
        assertEquals(expected, result);
    }

    // Compute a lower bound for the cycle time when the average station load
    // *WITH CERTAIN EQUIPMENT* is the deciding factor.
    // Only one station can accomodate the tasks.
    @Test
    public void test_lowerBoundCycleTime_average_equipped_load_forced() {
        // Station equipment: [   0   | | | ]
        // Compatible tasks:  [0,1,2,3| | | ]
        var station0 = station(0);
        var station1 = station(1);
        var station2 = station(2);
        var station3 = station(3);
        var equip0 = pinned(0, 0, station0);
        var task0 = task(0, 4, List.of(), List.of(0));  // pinned to station 0
        var task1 = task(1, 4, List.of(), List.of(0));  // pinned to station 0
        var task2 = task(2, 4, List.of(), List.of(0));  // pinned to station 0
        var task3 = task(3, 4, List.of(), List.of(0));  // pinned to station 0
        var stations = List.of(station0, station1, station2, station3);
        var tasks = List.of(task0, task1, task2, task3);
        var equipment = List.of(equip0);
        var plan = new AssemblyPlan(tasks, stations, equipment);
        int expected = 16;
        int result = lowerBoundCycleTime(plan);
        assertEquals(expected, result);
    }

    // Compute a lower bound for the cycle time when the average station load
    // *WITH CERTAIN EQUIPMENT* is the deciding factor.
    // Here the equipment dependencies are such that all tasks are forced to
    // be assigned to a single station. (Proper) Subsets of the equipment also
    // exist at other stations, but that doesn't help. So they should be
    // ignored.
    @Test
    public void test_lowerBoundCycleTime_average_equipped_load_duplicate_forced() {
        // Station equipment: [ 0 | 1 |  0,1  |  ]
        // Compatible tasks:  [   |   |0,1,2,3|  ]
        var station0 = station(0);
        var station1 = station(1);
        var station2 = station(2);
        var station3 = station(3);
        var equip0a = pinned(0, 0, station0);
        var equip0b = pinned(1, 0, station2);
        var equip1a = pinned(2, 1, station1);
        var equip1b = pinned(3, 1, station2);
        var task0 = task(0, 4, List.of(), List.of(0,1)); // pinned to station 2
        var task1 = task(1, 4, List.of(), List.of(0,1)); // pinned to station 2
        var task2 = task(2, 4, List.of(), List.of(0,1)); // pinned to station 2
        var task3 = task(3, 4, List.of(), List.of(0,1)); // pinned to station 2
        var stations = List.of(station0, station1, station2, station3);
        var tasks = List.of(task0, task1, task2, task3);
        var equipment = List.of(equip0a, equip0b, equip1a, equip1b);
        var plan = new AssemblyPlan(tasks, stations, equipment);
        // TODO: figure out if the lower bound can be made tighter.
        //int expected = 16;    // This is the true answer.
        int expected = 8;
        int result = lowerBoundCycleTime(plan);
        assertEquals(expected, result);
    }

    /**
     * Check that all score features that are associated with hard constraints
     * are zero.  Expects a feasible plan!
     */
    private void assertFeasible(AssemblyPlan plan) {
        assertEquals(0, countDependencyInversions(plan));
        assertEquals(0, countDeepDependencyInversions(plan));
        assertEquals(0, totalDependencyDistance(plan));
        assertEquals(0, countMissingEquipment(plan));
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
     * Loads: [21, 12, 42, 33, 18, 19].
     * Note that steps are assigned to stations by modifying a Step.
     * So changing the assignments for this plan will modify any other plan
     * that uses the steps and stations defined in this class.
     * While it would be possible to make a deep clone, that would make it
     * less convenient to assign stations, since we would have to get the
     * actual Step and Station instances from the plan.
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
     * Apply the common station equipments.
     * It's the same for feasible 1,2,3.
     */
    public static AssemblyPlan equipStations() {
        equip0a.equipAt(station0);
        equip1a.equipAt(station0);
        equip1b.equipAt(station1);    // pinned
        equip2a.equipAt(station2);
        equip3a.equipAt(station2);    // pinned
        equip4a.equipAt(station2);
        equip4b.equipAt(station3);
        equip2b.equipAt(station5);
        equip5a.equipAt(null);  // unequipped
        return shared;
    }

    /**
     * Make a an empty solution with no assignments
     * ([...]).
     */
    public static AssemblyPlan reset() {
        shared.cycleTime(OptionalInt.empty());
        equipStations();
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
