package ch.rfin.alb;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import static ch.rfin.test.Assertions.*;
import static ch.rfin.alb.AlbInstance.albInstance;
import static ch.rfin.alb.Validator.validateStrict;

public class ValidatorTest {

    private static final AlbInstance single = singleTask();

    @Test
    public void making_an_instance_with_nonpositive_tasks_is_impossible() {
        assertThrowsException(() -> validateStrict(albInstance(0, Map.of())));
        assertThrowsException(() -> validateStrict(albInstance(-1, Map.of())));
    }


    @Test
    public void null_optionals_not_allowed() {
        assertThrowsException(() -> validateStrict(single.cycleTime(null)));
        assertThrowsException(() -> validateStrict(single.stations(null)));
        assertThrowsException(() -> validateStrict(single.equipmentTypes(null)));
    }

    @Test
    public void null_maps_not_allowed() {
        assertThrowsException(() -> validateStrict(albInstance(1, null)));
        assertThrowsException(() -> validateStrict(single.taskDependencies(null)));
        assertThrowsException(() -> validateStrict(single.taskEquipment(null)));
    }

    @Test
    public void cycleTime_must_be_positive() {
        assertThrowsException(() -> validateStrict(single.cycleTime(0)));
        assertThrowsException(() -> validateStrict(single.cycleTime(-1)));
    }

    @Test
    public void stations_must_be_positive() {
        assertThrowsException(() -> validateStrict(single.stations(0)));
        assertThrowsException(() -> validateStrict(single.stations(-1)));
    }

    @Test
    public void equipments_must_be_positive() {
        assertThrowsException(() -> validateStrict(single.equipmentTypes(0)));
        assertThrowsException(() -> validateStrict(single.equipmentTypes(-1)));
    }

    @Test
    public void task_numbers_must_start_with_0_or_1() {
        assertThrowsException(() -> validateStrict(albInstance(1, Map.of(2, 123))));
        assertThrowsException(() -> validateStrict(albInstance(1, Map.of(-1, 123))));
    }

    @Test
    public void task_numbers_must_be_contiguous() {
        assertThrowsException(() -> validateStrict(albInstance(2, Map.of(0, 123, 2, 321))));
    }

    @Test
    public void all_tasks_must_have_task_times() {
        assertThrowsException(() -> validateStrict(albInstance(2, Map.of(0, 123))));
        assertThrowsException(() -> validateStrict(albInstance(2, Map.of(1, 123))));
    }

    @Test
    public void task_numbers_in_task_times_must_match() {
        assertThrowsException(() ->
                validateStrict(albInstance(1, Map.of(0, 123, 1, 321)))  // 1 Doesn't exist.
        );
        assertThrowsException(() ->
                validateStrict(albInstance(1, Map.of(1, 123, 2, 321)))  // 2 Doesn't exist.
        );
        assertThrowsException(() ->
                validateStrict(albInstance(2, Map.of(0, 123, 3, 321)))  // 3 Doesn't exist.
        );
    }

    @Test
    public void task_numbers_in_dependencies_must_match() {
        assertThrowsException(() ->
                validateStrict(
                    singleTask()
                    .taskDependencies(Map.of(2, List.of(1)))) // 2 Doesn't exist.
        );
        assertThrowsException(() ->
                validateStrict(
                    singleTask()
                    .taskDependencies(Map.of(1, List.of(2)))) // 2 Doesn't exist.
        );
    }

    /**
     * If task numbers are 0-based, so must equipment be. Same for 1-based.
     */
    @Test
    public void indexing_mode_must_be_consistent() {
        // 0-based tasks but 1-based task equipment
        assertThrowsException(() ->
                validateStrict(
                    albInstance(1, Map.of(0, 123))
                        .equipmentTypes(3)
                        .taskEquipment(Map.of(0, List.of(1,2,3))))
        );
        // 1-based tasks but 0-based task equipment
        assertThrowsException(() ->
                validateStrict(
                    albInstance(1, Map.of(1, 123))
                        .equipmentTypes(3)
                        .taskEquipment(Map.of(1, List.of(0,1,2))))
        );
        // 1-based tasks but 0-based station in task equipment
        assertThrowsException(() ->
                validateStrict(
                    albInstance(1, Map.of(1, 123))
                        .equipmentTypes(3)
                        .taskEquipment(Map.of(0, List.of(1,2,3))))
        );
        // 1-based tasks but 0-based station in station equipment
        assertThrowsException(() ->
                validateStrict(
                    albInstance(1, Map.of(1, 123))
                        .equipmentTypes(3)
                        .stations(1)
                        .stationEquipment(Map.of(0, List.of(1,2,3))))
        );
        // 1-based tasks but 0-based equipment in task equipment
        assertThrowsException(() ->
                validateStrict(
                    albInstance(1, Map.of(1, 123))
                        .equipmentTypes(3)
                        .stations(1)
                        .taskEquipment(Map.of(1, List.of(0,1,2))))
        );
        // 1-based tasks but 0-based equipment in station equipment
        assertThrowsException(() ->
                validateStrict(
                    albInstance(1, Map.of(1, 123))
                        .equipmentTypes(3)
                        .stations(1)
                        .stationEquipment(Map.of(1, List.of(0,1,2))))
        );
        // OK, no exception!
        validateStrict(
            albInstance(1, Map.of(1, 123))
                .equipmentTypes(3)
                .stations(1)
                .taskEquipment(Map.of(1, List.of(1,2,3)))
                .stationEquipment(Map.of(1, List.of(1,2,3))));
    }

    @Test
    public void equipment_numbers_must_be_contiguous() {
        assertThrowsException(() ->
            validateStrict(
                singleTask()
                    .equipmentTypes(3)
                    .taskEquipment(Map.of(1, List.of(0,3))))
        );
    }

    @Test
    public void station_numbers_must_be_contiguous() {
        assertThrowsException(() ->
            validateStrict(
                singleTask()
                    .equipmentTypes(1)
                    .stations(3)
                    .stationEquipment(Map.of(1, List.of(1), 3, List.of(1))))
        );
    }

    @Test
    public void task_equipment_numbers_must_match() {
        assertThrowsException(() ->
                validateStrict(
                singleTask()
                    .equipmentTypes(3)
                    .taskEquipment(Map.of(1, List.of(0,1,2,3)))) // too many
        );
        assertThrowsException(() ->
            validateStrict(
                singleTask()
                    .equipmentTypes(3)
                    .taskEquipment(Map.of(1, List.of(1,2,3,4)))) // too many
        );
        assertThrowsException(() ->
            validateStrict(
                singleTask()
                    .equipmentTypes(3)
                    .taskEquipment(Map.of(2, List.of(1,2,3)))) // task 2!
        );
        // XXX: This is disallowed for now.
        // Maybe lift this restriction in the future.
        // We could imagine a task defining that it needs X copies of a
        // certain equipment, or something like that.
        assertThrowsException(() ->
            validateStrict(
                singleTask()
                    .equipmentTypes(3)
                    .taskEquipment(Map.of(1, List.of(1,1,1,1)))) // too many
        );
    }

    @Test
    public void station_equipment_numbers_must_match() {
        assertThrowsException(() ->
            validateStrict(
                albInstance(1, Map.of(1, 123))
                    .equipmentTypes(1)
                    .stations(1)
                    .stationEquipment(Map.of(2, List.of(1))))
        );
        assertThrowsException(() ->
            validateStrict(
                albInstance(1, Map.of(1, 123))
                    .equipmentTypes(1)
                    .stations(2)
                    .stationEquipment(Map.of(1, List.of(1))))  // 2 missing
        );
    }

    // Helpers

    private static AlbInstance singleTask() {
        return albInstance(1, Map.of(1, 123));
    }
}
