package se.ltu.alb.equip.algo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import static java.util.stream.Collectors.toList;
import se.ltu.alb.equip.model.*;
import static se.ltu.alb.equip.model.Task.task;
import static se.ltu.alb.equip.model.Station.station;
import static se.ltu.alb.equip.model.Equipment.pinned;
import static se.ltu.alb.equip.model.Equipment.unpinned;
import static se.ltu.alb.equip.model.Equipment.unequipped;
import ch.rfin.util.Bits;
import static ch.rfin.util.Bits.bits;
import se.ltu.alb.equip.algo.EarlyLateChangeMoveFilter;

public class EarlyLateChangeMoveFilterTest {

    /*
     * 0   3
     *  \ /  \
     *   2-4--6
     *  / \     \
     * 1   5-7-8-9
     *
     * Equipment at stations:
     *      [ 0,1 | 1 | 2,3,4 | 4 |--| 2 |--|--]
     * Pinned:      *     *
     *
     * Task     Comp. Stations    Equipment
     * 0            0               0
     * 1            0,1             1
     * 2            0,1             1
     * 3            2               2,3
     * 4            2               2,4
     * 5            all             -
     * 6            2,3             4
     * 7            all             -
     * 8            all             -
     * 9            2,5             2
     *
     * Effectively pinned: [ 0,{1,2} | {1,2} | 3,4,{6,9} | {6} | -- | {9} | -- | -- ]
     *
     * Earliest:
     *      0 1 2 3 4 5 6 7 8 9
     * 0:   |
     * 1:   |-|
     * 2:   |-|
     * 3:       |
     * 4:       |
     * 5:   |---------|
     * 6:       |-|
     * 7:   |---------|
     * 8:   |---------|
     * 9:       |     |
     *
     *
     * Feasible
     * [ 0,1,2 | 5,7,8 | 3,4,6,9 | -- | -- | -- | -- | -- ]
     * [ 0 | 1,2 | 3,4 | 6 | -- | 5,7,8,9 | -- | -- ]
     *
     * earliest = max(minCompatible, max(earliestPredecessors))
     * latest = min(maxCompatible, min(latestSuccessors))
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

    public static final Task task0 = task(0, 10, List.of(), List.of(0));
    public static final Task task1 = task(1, 11, List.of(), List.of(1));
    public static final Task task2 = task(2, 12, List.of(task0, task1), List.of(1));
    public static final Task task3 = task(3, 13, List.of(task2), List.of(2,3));
    public static final Task task4 = task(4, 14, List.of(task2), List.of(2,4));
    public static final Task task5 = task(5, 15, List.of(task2));
    public static final Task task6 = task(6, 16, List.of(task3, task4), List.of(4));
    public static final Task task7 = task(7, 17, List.of(task5));
    public static final Task task8 = task(8, 18, List.of(task7));
    public static final Task task9 = task(9, 19, List.of(task6, task8), List.of(2));
    public static final List<Task> tasks = List.of(
            task0, task1, task2, task3, task4,
            task5, task6, task7, task8, task9);

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

    private static final AssemblyPlan plan = new AssemblyPlan(tasks, stations, equipment);
    private static final State state = State.init(plan);

    @Test
    public void testEquipmentBasedBounds() {
        var intervals = EarlyLateChangeMoveFilter.stationBounds(plan, 1.0, state); // 1.0 is irrelevant
        // Task 0
        var interval = intervals.get(0);
        assertEquals(0, interval._1);
        assertEquals(0, interval._2);
        // Task 1
        interval = intervals.get(1);
        assertEquals(0, interval._1);
        assertEquals(1, interval._2);
        // Task 2
        interval = intervals.get(2);
        assertEquals(0, interval._1);
        assertEquals(1, interval._2);
        // Task 3
        interval = intervals.get(3);
        assertEquals(2, interval._1);
        assertEquals(2, interval._2);
        // Task 4
        interval = intervals.get(4);
        assertEquals(2, interval._1);
        assertEquals(2, interval._2);
        // Task 5
        interval = intervals.get(5);
        assertEquals(0, interval._1);
        assertEquals(5, interval._2);
        // Task 6
        interval = intervals.get(6);
        assertEquals(2, interval._1);
        assertEquals(3, interval._2);
        // Task 7
        interval = intervals.get(7);
        assertEquals(0, interval._1);
        assertEquals(5, interval._2);
        // Task 8
        interval = intervals.get(8);
        assertEquals(0, interval._1);
        assertEquals(5, interval._2);
        // Task 9
        interval = intervals.get(9);
        assertEquals(2, interval._1);
        assertEquals(5, interval._2);
    }

}
