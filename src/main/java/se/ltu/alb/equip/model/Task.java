package se.ltu.alb.equip.model;

import java.util.*;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.solution.ProblemFactProperty;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import se.ltu.alb.equip.algo.TaskSort1;

/**
 * A task in the assembly plan, represents the smallest unit of work.
 * Tasks are performed at assembly stations. A task may depend on other
 * tasks and may require certain equipment (which a station must provide).
 * @author Christoffer Fink
 */
@PlanningEntity(difficultyWeightFactoryClass = TaskSort1.class)
public class Task implements Comparable<Task> {

    @PlanningVariable(valueRangeProviderRefs = {"stations"})
    private Station station;

    // Problem facts.
    private int id;
    private int time;
    private Collection<Task> taskDependencies;    // TODO: rename
    private Collection<Integer> equipmentDependencies;
    private Collection<Task> deepDependencies;

    /** OptaPlanner needs a no-arg constructor. */
    private Task() {
    }

    /**
     * Copy constructor. Defensively copies dependencies.
     */
    private Task(final Task task) {
        // TODO: why defensively copy? And why not copy deep dependencies!?
        this(task.id,
             task.time,
             Set.copyOf(task.taskDependencies),
             Set.copyOf(task.equipmentDependencies),
             task.station);
        this.deepDependencies = task.deepDependencies;
    }

    private Task(int id, int time, Collection<Task> tasks, Collection<Integer> equipment, Station station) {
        if (tasks == null || equipment == null) {
            throw new AssertionError("Impossible: ");
        }
        this.id = id;
        this.time = time;
        this.taskDependencies = tasks;
        this.equipmentDependencies = equipment;
        this.station = station;
    }

    @Deprecated(forRemoval = true)
    public static Task task(int id, Collection<Task> tasks, int time, Station station) {
        return task(id, time, tasks, station);
    }

    public static Task task(int id, int time, Collection<Task> tasks) {
        return task(id, time, tasks, Set.of(), null);
    }

    public static Task task(int id, int time, Collection<Task> tasks, Collection<Integer> equipment) {
        return task(id, time, tasks, equipment, null);
    }

    public static Task task(int id, int time, Collection<Task> tasks, Station station) {
        return task(id, time, tasks, Set.of(), station);
    }

    public static Task task(int id, int time, Collection<Task> tasks, Collection<Integer> equipment, Station station) {
        return new Task(id, time, tasks, equipment, station);
    }

    /**
     * Make a new, independent copy of the given Step.
     * (Defensively copies dependencies.)
     */
    public static Task copyOf(final Task task) {
        return new Task(task);
    }

    // TODO: take more than 2 steps.
    /**
     * Merge two steps into a new step.
     */
    public static Task merge(final int id, final Task step1, final Task step2) {
        if (!Objects.equals(step1.station(), step2.station())) {
            throw new IllegalArgumentException("Both steps must be assigned to the same station (or null)");
        }
        final Set<Task> taskDependencies = new HashSet<>(step1.taskDependencies());
        taskDependencies.addAll(step2.taskDependencies());
        final int time = step1.time() + step2.time();
        return task(id, taskDependencies, time, null);
    }

    public Station station() {
        return station;
    }

    /**
     * For use by custom phases.
     */
    public Task station(Station station) {
        this.station = station;
        return this;
    }

    @ProblemFactProperty
    @PlanningId
    public int id() {
        return id;
    }

    @ProblemFactProperty
    public int time() {
        return time;
    }

    @ProblemFactCollectionProperty
    public Collection<Task> taskDependencies() {
        return taskDependencies;
    }

    @Deprecated(forRemoval = true)
    public Collection<Task> stepDependencies() {
        return taskDependencies();
    }

    @ProblemFactCollectionProperty
    public Collection<Integer> equipmentDependencies() {
        return equipmentDependencies;
    }

    /** Return the immediate and also transitive dependencies. */
    @ProblemFactCollectionProperty
    public Collection<Task> deepDependencies() {
        if (deepDependencies == null) {
            final var tmp = new HashSet<Task>(taskDependencies);
            for (final var step : taskDependencies) {
                tmp.addAll(step.deepDependencies());
            }
            deepDependencies = Set.copyOf(tmp);
        }
        return deepDependencies;
    }

    /** This comparison is for (topological) sorting.
     * When sorted based on this comparison, steps appear AFTER the steps they
     * depend on.
     */
    @Override
    public int compareTo(final Task that) {
        if (deepDependencies().contains(that)) {
            // If that is one of the steps this depends on.
            return 1;
        }
        if (that.deepDependencies().contains(this)) {
            // If this is one of the steps that depends on.
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "task " + id;
    }

    /** Strict type equality. */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        final Task that = (Task) o;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

}
