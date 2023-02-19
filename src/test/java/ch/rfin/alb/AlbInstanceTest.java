package ch.rfin.alb;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import static ch.rfin.test.Assertions.*;
import static ch.rfin.alb.AlbInstance.albInstance;

public class AlbInstanceTest {

    private static final AlbInstance single = singleTask();

    @Test
    public void task_times_should_be_copied() {
        Map<Integer,Integer> taskTimes = new HashMap<>();
        taskTimes.put(1, 123);
        var alb = albInstance(1, taskTimes);
        taskTimes.put(1, 321);
        var copy = alb.taskTimes();
        assertEquals(123, copy.get(1));
    }

    @Test
    public void default_optionals_should_be_empty() {
        // OptionalInt
        assertNotNull(single.cycleTime());
        assertTrue(single.cycleTime().isEmpty());
        assertNotNull(single.stations());
        assertTrue(single.stations().isEmpty());
        assertNotNull(single.equipmentTypes());
        assertTrue(single.equipmentTypes().isEmpty());
        // Empty Maps
        assertNotNull(single.taskDependencies());
        assertTrue(single.taskDependencies().isEmpty());
        assertNotNull(single.taskEquipment());
        assertTrue(single.taskEquipment().isEmpty());
    }

    @Test
    public void should_detect_indexing_mode() {
        final var zeroBased = albInstance(1, Map.of(0, 123));
        assertTrue(zeroBased.isZeroBased());
        assertFalse(zeroBased.isOneBased());
        final var oneBased = albInstance(1, Map.of(1, 123));
        assertTrue(oneBased.isOneBased());
        assertFalse(oneBased.isZeroBased());
    }

    @Test
    public void one_to_zero_based_indexing() {
        final var one = albInstance(4, Map.of(1, 123, 2, 234, 3, 345, 4, 456))
            .taskDependencies(
                    Map.of(
                        1, Set.of(),       // Task 1 depends on no other task.
                        2, Set.of(),       // Task 2 depends on no other task.
                        3, Set.of(1, 2),   // Task 3 depends on tasks 1 and 2.
                        4, Set.of(3)       // Task 4 depends on task 3.
                    )
            )
            .equipmentTypes(2)  // There are 2 equipment types.
            .stations(3)        // There are 3 stations.
            .taskEquipment(
                    Map.of(
                        1, Set.of(2),    // Task 1 needs equipment 2.
                        2, Set.of(1),    // Task 2 needs equipment 1.
                        3, Set.of(1,2),  // Task 3 needs equipment 1 and 2.
                        4, Set.of()      // Task 4 needs no equipment.
                    )
            )
            .stationEquipment(
                    Map.of(
                        1, Set.of(1),    // Station 1 has equipment 1.
                        2, Set.of(1,2),  // Station 2 has equipment 1 and 2.
                        3, Set.of()      // Station 3 has no equipment.
                    )
            );
        final var zero = albInstance(4, Map.of(0, 123, 1, 234, 2, 345, 3, 456))
            .taskDependencies(
                    Map.of(
                        0, Set.of(),       // Task 1 depends on no other task.
                        1, Set.of(),       // Task 2 depends on no other task.
                        2, Set.of(0, 1),   // Task 3 depends on tasks 1 and 2.
                        3, Set.of(2)       // Task 4 depends on task 3.
                    )
            )
            .equipmentTypes(2)  // There are 2 equipment types.
            .stations(3)        // There are 3 stations.
            .taskEquipment(
                    Map.of(
                        0, Set.of(1),    // Task 1 needs equipment 2.
                        1, Set.of(0),    // Task 2 needs equipment 1.
                        2, Set.of(0,1),  // Task 3 needs equipment 1 and 2.
                        3, Set.of()      // Task 4 needs no equipment.
                    )
            )
            .stationEquipment(
                    Map.of(
                        0, Set.of(0),    // Station 1 has equipment 1.
                        1, Set.of(0,1),  // Station 2 has equipment 1 and 2.
                        2, Set.of()      // Station 3 has no equipment.
                    )
            );
        assertEquals(zero, one.zeroBased());
    }

    // Helpers

    private static AlbInstance singleTask() {
        return albInstance(1, Map.of(1, 123));
    }

}
