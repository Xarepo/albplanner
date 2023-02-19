package se.ltu.alb.equip.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import static se.ltu.alb.equip.model.Equipment.pinned;
import static se.ltu.alb.equip.model.Task.task;
import static se.ltu.alb.equip.model.Station.station;
import static java.util.stream.Collectors.toSet;
import ch.rfin.alb.AlbInstance;
import ch.rfin.util.Bits;
import static ch.rfin.util.Bits.bits;

public class AssemblyPlanTest {

    // TODO: test more things using this instance.
    @Test
    public void test_equippedSets_singletons() {
        // Station equipment: [ 0 | 1 | 2 | 3 ]
        // Compatible tasks:  [ 0 | 1 | 2 | 3 ]
        var station0 = station(0);
        var station1 = station(1);
        var station2 = station(2);
        var station3 = station(3);
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

        Set<Bits> expected = Set.of(
                bits(List.of(0)),
                bits(List.of(1)),
                bits(List.of(2)),
                bits(List.of(3)));
        Set<Bits> result = plan.equippedSets().collect(toSet());
        assertEquals(expected, result);
    }

    // TODO: test more things using this instance.
    @Test
    public void test_equippedSets_moop() {
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

        Set<Bits> expected = Set.of(
                bits(List.of(0)),
                bits(List.of(1)),
                bits(List.of(0,1)),
                bits(List.of()));
        Set<Bits> result = plan.equippedSets().collect(toSet());
        assertEquals(expected, result);
    }

    // TODO: test more things using this instance.
    @Test
    public void test_equippedSets_identical_equipment() {
        // Station equipment: [ 0 | 0 | 0 | 0 ]
        // Compatible tasks:  [ 0 | 0 | 0 | 0 ]
        var station0 = station(0);
        var station1 = station(1);
        var station2 = station(2);
        var station3 = station(3);
        var equip0a = pinned(0, 0, station0);
        var equip0b = pinned(1, 0, station1);
        var equip0c = pinned(2, 0, station2);
        var equip0d = pinned(3, 0, station3);
        var task0 = task(0, 3, List.of(), List.of(0));  // Doesn't matter.

        var stations = List.of(station0, station1, station2, station3);
        var tasks = List.of(task0);
        var equipment = List.of(equip0a, equip0b, equip0c, equip0d);
        var plan = new AssemblyPlan(tasks, stations, equipment);

        Set<Bits> expected = Set.of(bits(List.of(0)));  // all 4 are the same
        Set<Bits> result = plan.equippedSets().collect(toSet());
        assertEquals(expected, result);
    }

    @Test
    public void test_equippedSets_fromAlb() {
        // Station equipment: [ 0 | 1 | 0,1 |  ]
        // Compatible tasks:  [ 0 | 1 |  2  |  ] 3,4 can go anywhere

        final int tasks = 4;
        final int stations = 4;
        final int equipmentTypes = 2;
        Map<Integer,Integer> taskTimes = Map.of(0, 3, 1, 3, 2, 3, 3, 3);
        Map<Integer,Collection<Integer>> taskDependencies = Map.of(
                0, List.of(), 1, List.of(), 2, List.of(), 3, List.of());
        Map<Integer,Collection<Integer>> taskEquipment = Map.of(
                0, List.of(0),
                1, List.of(1),
                2, List.of(0,1),
                3, List.of());
        Map<Integer,Collection<Integer>> stationEquipment = Map.of(
                0, List.of(0),
                1, List.of(1),
                2, List.of(0,1),
                3, List.of());
        var alb = AlbInstance.albInstance(tasks, taskTimes)
            .stations(stations)
            .equipmentTypes(equipmentTypes)
            .taskDependencies(taskDependencies)
            .taskEquipment(taskEquipment)
            .stationEquipment(stationEquipment);
        var plan = AssemblyPlan.fromAlb(alb);

        Set<Bits> expected = Set.of(
                bits(List.of(0)),
                bits(List.of(1)),
                bits(List.of(0,1)),
                bits(List.of())
                );
        Set<Bits> result = plan.equippedSets().collect(toSet());
        assertEquals(expected, result);
    }

}
