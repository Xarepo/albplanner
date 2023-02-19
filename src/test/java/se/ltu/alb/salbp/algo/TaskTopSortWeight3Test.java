package se.ltu.alb.salbp.algo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import se.ltu.alb.salbp.model.Step;
import static se.ltu.alb.salbp.model.Step.step;
import static se.ltu.alb.salbp.algo.TaskTopSortWeight3.compare_c;

public class TaskTopSortWeight3Test {

    @Test
    public void different_layers_task1_first() {
        Step task1 = step(1, Set.of(), 100, null);
        Step task2 = step(2, Set.of(), 100, null);
        Map<Step, Integer> layerMap = Map.of(task1, 0, task2, 1);
        assertEquals(-1, compare_c(layerMap, null, task1, task2));
    }

    @Test
    public void different_layers_task1_second() {
        Step task1 = step(1, Set.of(), 100, null);
        Step task2 = step(2, Set.of(), 100, null);
        Map<Step, Integer> layerMap = Map.of(task1, 1, task2, 0);
        assertEquals(1, compare_c(layerMap, null, task1, task2));
    }

    @Test
    public void different_dependees_task1_first() {
        Step task1 = step(1, Set.of(), 100, null);
        Step task2 = step(2, Set.of(), 100, null);
        Map<Step, Integer> layerMap = Map.of(task1, 0, task2, 0);
        Map<Step, Collection<Step>> dependees = Map.of(
                task1, List.of(task2),
                task2, List.of());
        assertEquals(-1, compare_c(layerMap, dependees, task1, task2));
    }

    @Test
    public void different_dependees_task1_second() {
        Step task1 = step(1, Set.of(), 100, null);
        Step task2 = step(2, Set.of(), 100, null);
        Map<Step, Integer> layerMap = Map.of(task1, 0, task2, 0);
        Map<Step, Collection<Step>> dependees = Map.of(
                task1, List.of(),
                task2, List.of(task1));
        assertEquals(1, compare_c(layerMap, dependees, task1, task2));
    }

    @Test
    public void different_times_task1_first() {
        Step task1 = step(1, Set.of(), 200, null);
        Step task2 = step(2, Set.of(), 100, null);
        Map<Step, Integer> layerMap = Map.of(task1, 0, task2, 0);
        Map<Step, Collection<Step>> dependees = Map.of(
                task1, List.of(),
                task2, List.of());
        assertEquals(-1, compare_c(layerMap, dependees, task1, task2));
    }

    @Test
    public void different_times_task1_second() {
        Step task1 = step(1, Set.of(), 100, null);
        Step task2 = step(2, Set.of(), 200, null);
        Map<Step, Integer> layerMap = Map.of(task1, 0, task2, 0);
        Map<Step, Collection<Step>> dependees = Map.of(
                task1, List.of(),
                task2, List.of());
        assertEquals(1, compare_c(layerMap, dependees, task1, task2));
    }

    @Test
    public void different_IDs_task1_first() {
        Step task1 = step(1, Set.of(), 100, null);
        Step task2 = step(2, Set.of(), 100, null);
        Map<Step, Integer> layerMap = Map.of(task1, 0, task2, 0);
        Map<Step, Collection<Step>> dependees = Map.of(
                task1, List.of(),
                task2, List.of());
        assertEquals(-1, compare_c(layerMap, dependees, task1, task2));
    }

    @Test
    public void different_IDs_task1_second() {
        Step task1 = step(1, Set.of(), 100, null);
        Step task2 = step(0, Set.of(), 100, null);
        Map<Step, Integer> layerMap = Map.of(task1, 0, task2, 0);
        Map<Step, Collection<Step>> dependees = Map.of(
                task1, List.of(),
                task2, List.of());
        assertEquals(1, compare_c(layerMap, dependees, task1, task2));
    }

}
