package se.ltu.alb.equip.algo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
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
import se.ltu.alb.equip.algo.State;

public class StateTest {

    /*
     * 0   3
     *  \ /  \
     *   2-4--6
     *  / \     \
     * 1   5-7-8-9
     *
     * Equipment at stations:
     *      [ 0,1 | 1 | 2,3,4 | 4 |  | 2 ]
     * Pinned:      *     *
     *
     * Task     Comp. Stations    Equipment
     * 0            0               0
     * 1            0,1             1
     * 2            0,1             1
     * 3            2               2,3
     * 4            2               2,4
     * 5            1,2,3           -
     * 6            2,3             4
     * 7            1,3,4           -
     * 8            1,4             -
     * 9            2,5             2
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

    static {
        task0.station(station0);
        task1.station(station0);
        task2.station(station1);
        task3.station(station1);    // not feasible
        task4.station(station2);
    }
    private static final AssemblyPlan plan = new AssemblyPlan(tasks, stations, equipment);
    private static final State state = State.init(plan);

    @Test
    public void testTaskPredecessors() {
        final var expected = Map.of(
            0, bits(List.of()),
            1, bits(List.of()),
            2, bits(List.of(0, 1)),
            3, bits(List.of(2)),
            4, bits(List.of(2)),
            5, bits(List.of(2)),
            6, bits(List.of(3, 4)),
            7, bits(List.of(5)),
            8, bits(List.of(7)),
            9, bits(List.of(6, 8))
        );
        assertEquals(expected, state.taskPredecessors);
    }

    @Test
    public void testTaskSuccessors() {
        final var expected = Map.of(
            0, bits(List.of(2)),
            1, bits(List.of(2)),
            2, bits(List.of(3, 4, 5)),
            3, bits(List.of(6)),
            4, bits(List.of(6)),
            5, bits(List.of(7)),
            6, bits(List.of(9)),
            7, bits(List.of(8)),
            8, bits(List.of(9)),
            9, bits(List.of())
        );
        assertEquals(expected, state.taskSuccessors);
    }

    @Test
    public void testTaskToEquipments() {
        final var expected = Map.of(
            0, bits(List.of(0)),
            1, bits(List.of(1)),
            2, bits(List.of(1)),
            3, bits(List.of(2, 3)),
            4, bits(List.of(2, 4)),
            5, bits(List.of()),
            6, bits(List.of(4)),
            7, bits(List.of()),
            8, bits(List.of()),
            9, bits(List.of(2))
        );
        assertEquals(expected, state.taskToEquipments);
    }

    @Test
    public void testTaskToStation() {
        final var expected = Map.of(
            0, 0,
            1, 0,
            2, 1,
            3, 1,
            4, 2
        );
        assertEquals(expected, state.taskToStation);
    }

    @Test
    public void testEquipmentToStations() {
        final var expected = Map.of(
            0, bits(List.of(0)),
            1, bits(List.of(0, 1)),
            2, bits(List.of(2, 5)),
            3, bits(List.of(2)),
            4, bits(List.of(2, 3))
        );
        assertEquals(expected, state.equipmentToStations);
    }

    @Test
    public void testEquipmentTypeToTasks() {
        final var expected = Map.of(
            0, bits(List.of(0)),
            1, bits(List.of(1, 2)),
            2, bits(List.of(3, 4, 9)),
            3, bits(List.of(3)),
            4, bits(List.of(4, 6))
        );
        assertEquals(expected, state.equipmentTypeToTasks);
    }

    @Test
    public void testEquipmentToDependentTasks() {
        final var expected = Map.of(
            bits(List.of()), bits(List.of(5, 7, 8)),    // {}: {5, 7, 8}
            bits(List.of(0)), bits(List.of(0)),         // {0}:   {0}
            bits(List.of(1)), bits(List.of(1, 2)),      // {1}:   {1,2}
            bits(List.of(2)), bits(List.of(9)),         // {2}:   {9}
            bits(List.of(4)), bits(List.of(6)),         // {4}:   {6}
            bits(List.of(2, 3)), bits(List.of(3)),      // {2,3}: {3}
            bits(List.of(2, 4)), bits(List.of(4))       // {2,4}: {4}
        );
        assertEquals(expected, state.equipmentsToDependentTasks);
    }

    @Test
    public void testStationToTasks() {
        final var expected = Map.of(
            0, bits(List.of(0, 1)),
            1, bits(List.of(2, 3)),
            2, bits(List.of(4))
        );
        assertEquals(expected, state.stationToTasks);
    }

    @Test
    public void testStationToNeededEquipments() {
        final var expected = Map.of(
            0, bits(List.of(0, 1)),     // task 0 needs eq 0; task 1 needs eq 1
            1, bits(List.of(1, 2, 3)),  // task 2 needs eq 1; task 3 needs eq 2,3
            2, bits(List.of(2, 4))      // task 4 needs eq 2,4
        );
        assertEquals(expected, state.stationToNeededEquipments);
    }

    @Test
    public void testStationToInstalledEquipments() {
        final var expected = Map.of(
            0, bits(List.of(0, 1)),
            1, bits(List.of(1)),
            2, bits(List.of(2, 3, 4)),
            3, bits(List.of(4)),
            5, bits(List.of(2))
        );
        assertEquals(expected, state.stationToInstalledEquipments);
    }

    @Test
    public void testCompatibleStations_task3_should_be_compatible_with_station_2() {
        Bits req = state.taskToEquipments.get(task3.id());
        List<Integer> reqTypes = req.stream().boxed().collect(toList());
        List<Integer> reqExpected = List.of(2, 3);
        assertEquals(reqExpected, reqTypes);
        Bits stationsThatHave2 = state.equipmentToStations.get(2);
        assertEquals(bits(List.of(2, 5)), stationsThatHave2);
        Bits stationsThatHave3 = state.equipmentToStations.get(3);
        assertEquals(bits(List.of(2)), stationsThatHave3);
        Bits stationsThatHaveBoth = stationsThatHave2.and(stationsThatHave3);
        assertEquals(bits(List.of(2)), stationsThatHaveBoth);
        List<Integer> expected = List.of(2);
        List<Integer> stationNumbersWithBoth = stationsThatHaveBoth.stream()
            .boxed()
            .collect(toList());
        assertEquals(expected, stationNumbersWithBoth);
        List<Integer> result = state.compatibleStations(req).boxed().collect(toList());
        assertEquals(expected, result);
    }

    @Test
    public void testFirstCompatibleStation_task3_should_be_compatible_with_station_2() {
        Bits req = state.taskToEquipments.get(task3.id());
        int expected = 2;
        int result = state.firstCompatibleStation(req);
        assertEquals(expected, result);
    }

    @Test
    public void testLastCompatibleStation_task3_should_be_compatible_with_station_2() {
        Bits req = state.taskToEquipments.get(task3.id());
        int expected = 2;
        int result = state.lastCompatibleStation(req);
        assertEquals(expected, result);
    }

    @Test
    public void testCompatibleStations_task1_should_be_compatible_with_stations_0_1() {
        Bits req = state.taskToEquipments.get(task1.id());
        List<Integer> expected = List.of(0, 1);
        List<Integer> result = state.compatibleStations(req).boxed().collect(toList());
        assertEquals(expected, result);
    }

    @Test
    public void testFirstCompatibleStation_task1_should_be_compatible_with_stations_0_1() {
        Bits req = state.taskToEquipments.get(task1.id());
        int expected = 0;
        int result = state.firstCompatibleStation(req);
        assertEquals(expected, result);
    }

    @Test
    public void testLastCompatibleStation_task1_should_be_compatible_with_stations_0_1() {
        Bits req = state.taskToEquipments.get(task1.id());
        int expected = 1;
        int result = state.lastCompatibleStation(req);
        assertEquals(expected, result);
    }

    @Disabled("FIXME")
    @Test
    public void testCompatibleStations_task5_should_be_compatible_with_all_stations() {
        Bits req = state.taskToEquipments.get(task5.id());
        List<Integer> expected = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<Integer> result = state.compatibleStations(req).boxed().collect(toList());
        assertEquals(expected, result);
    }

    @Test
    public void testPinnedEquipment() {
        final var expected = bits(List.of(2, 4));
        assertEquals(expected, state.pinnedEquipment);
    }

    // FIXME: test movable equipment (but what counts as "movable"!?)

    @Test
    public void testInstalledEquipment() {
        final var expected = bits(List.of(0, 1, 2, 3, 4, 5, 6, 7));
        assertEquals(expected, state.installedEquipment);
    }

}
