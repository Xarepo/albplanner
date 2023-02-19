package ch.rfin.alb;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import static ch.rfin.test.Assertions.*;

public class AlbBuilderTest {

    @Test
    public void tasks_must_be_positive() {
        var builder = new AlbBuilder();
        assertThrowsIllegalArg(() -> builder.tasks(0));
        assertThrowsIllegalArg(() -> builder.tasks(-1));
    }

    @Test
    public void cycleTime_must_be_positive() {
        var builder = new AlbBuilder();
        assertThrowsIllegalArg(() -> builder.cycleTime(0));
        assertThrowsIllegalArg(() -> builder.cycleTime(-1));
    }

    @Test
    public void stations_must_be_positive() {
        var builder = new AlbBuilder();
        assertThrowsIllegalArg(() -> builder.stations(0));
        assertThrowsIllegalArg(() -> builder.stations(-1));
    }

    @Test
    public void instance_should_have_specifed_number_of_tasks() {
        var alb = AlbBuilder.albBuilder()
            .tasks(1)
            .taskTimes(Map.of(1, 123))
            .buildValid();
        assertEquals(1, alb.tasks());
    }

    @Test
    public void builder_should_infer_number_of_tasks_if_missing() {
        var alb = AlbBuilder.albBuilder()
            .taskTimes(Map.of(1, 123, 2, 321))
            .buildValid();
        assertEquals(2, alb.tasks());
    }

    @Test
    public void specified_number_of_tasks_must_match_actual__more_actual() {
        var builder = AlbBuilder.albBuilder()
            .tasks(1)
            .taskTimes(Map.of(1, 123, 2, 234));
        assertThrowsException(() -> builder.buildValid());
    }

    @Test
    public void specified_number_of_tasks_must_match_actual__less_actual() {
        var builder = AlbBuilder.albBuilder()
            .tasks(2)
            .taskTimes(Map.of(1, 123));
        assertThrowsException(() -> builder.buildValid());
    }

    @Test
    public void tasks_should_have_the_individually_specified_dependencies() {
        var alb = AlbBuilder.albBuilder()
            .taskTime(1, 123)
            .taskDependency(1, 3)
            .taskDependency(2, 3)
            .taskTimes(Map.of(2, 234, 3, 345, 4, 456))
            .taskDependency(3, 4)
            .taskDependency(3, 5)
            .taskTime(5, 567)
            .buildValid();
        Map<Integer, Collection<Integer>> expected = Map.of(
            1, List.of(3), 2, List.of(3), 3, List.of(4, 5), 4, List.of(), 5, List.of()
        );
        assertEquals(expected, alb.taskDependencies());
    }

    @Test
    public void tasks_should_have_the_bulk_specified_dependencies() {
        var alb = AlbBuilder.albBuilder()
            .taskTime(1, 123)
            .taskDependencies(1, List.of(3))
            .taskTime(2, 234)
            .taskDependencies(2, List.of(3))
            .taskTime(3, 345)
            .taskDependencies(3, List.of(4, 5))
            .taskTime(4, 456)
            .taskTime(5, 567)
            .buildValid();
        Map<Integer, Collection<Integer>> expected = Map.of(
            1, List.of(3), 2, List.of(3), 3, List.of(4, 5), 4, List.of(), 5, List.of()
        );
        assertEquals(expected, alb.taskDependencies());
    }

    @Test
    public void tasks_should_have_the_specified_task_times() {
        var alb = AlbBuilder.albBuilder()
            .taskDependency(1, 2)
            .taskTime(1, 123)
            .taskTimes(Map.of(2, 234, 3, 345, 4, 456))
            .taskDependency(2, 3)
            .taskTime(5, 567)
            .buildValid();
        // Check task times.
        Map<Integer, Integer> expectedTaskTimes = Map.of(
            1, 123, 2, 234, 3, 345, 4, 456, 5, 567
        );
        assertEquals(expectedTaskTimes, alb.taskTimes());
        // Also check dependencies.
        Map<Integer, Collection<Integer>> expectedDependencies = Map.of(
            1, List.of(2), 2, List.of(3), 3, List.of(), 4, List.of(), 5, List.of()
        );
        assertEquals(expectedDependencies, alb.taskDependencies());
    }

    @Test
    public void instance_should_have_specified_cycle_time() {
        var alb = AlbBuilder.albBuilder()
            .cycleTime(1234)
            .taskTimes(Map.of(1, 123))
            .buildValid();
        assertEquals(1234, alb.cycleTime().getAsInt());
    }

    @Test
    public void instance_should_have_specified_number_of_stations() {
        var alb = AlbBuilder.albBuilder()
            .stations(23)
            .taskTimes(Map.of(1, 123))
            .buildValid();
        assertEquals(23, alb.stations().getAsInt());
    }

    @Test
    public void circular_dependencies_are_not_allowed__direct() {
        var builder = AlbBuilder.albBuilder()
            .taskDependency(8, 9);
        assertThrowsException(() -> builder.taskDependency(9, 8));
    }

    @Test
    public void circular_dependencies_are_not_allowed__indirect() {
        var builder = AlbBuilder.albBuilder()
            .taskDependency(7, 8)
            .taskDependency(8, 9);
        assertThrowsException(() -> builder.taskDependency(9, 7));
    }

    @Test
    public void adding_a_circular_dependency_throws_and_does_not_modify_builder() {
        var builder = AlbBuilder.albBuilder()
            .taskDependency(1, 3)
            .taskDependency(2, 3)
            .taskDependency(4, 5)
            .taskDependency(4, 6)
            .taskDependency(6, 2);
        assertThrowsException(() -> builder.taskDependency(3, 4));
        // Check that the attempt to add an invalid dependency had no effect.
        var alb = builder.buildValid();
        Map<Integer, Collection<Integer>> expectedDependencies = Map.of(
            1, List.of(3), 2, List.of(3), 3, List.of(),
            4, List.of(5, 6), 5, List.of(),
            6, List.of(2)
        );
        assertEquals(expectedDependencies, alb.taskDependencies());
    }

}
