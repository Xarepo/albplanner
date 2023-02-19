package se.ltu.alb.salbp.model;

import java.util.*;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.solution.ProblemFactProperty;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import se.ltu.alb.salbp.algo.TaskTopSortWeight3;

// Note: This is an old class that could use a rewrite.

/**
 * A task in the assembly plan, represents the smallest unit of work.
 * A task could potentially represent any task that needs to be performed
 * during assembly, but it is primarily intended to represent the mounting
 * of a certain part.
 * Steps are performed at assembly stations. A task may depend on other
 * steps and may require certain resources (which a station must provide).
 * @author Christoffer Fink
 */
@PlanningEntity(difficultyWeightFactoryClass = TaskTopSortWeight3.class)
public class Step implements Comparable<Step> {

    @PlanningVariable(valueRangeProviderRefs = {"stations"})
    protected Station station;

    // Problem facts.
    protected int id;
    protected int time;
    protected Collection<Step> taskDependencies;
    protected Collection<Step> deepDependencies;

    /** OptaPlanner requires a no-arg constructor. */
    public Step() { }

    /**
     * Copy constructor. Defensively copies dependencies.
     */
    private Step(final Step task) {
        this(task.id, Set.copyOf(task.taskDependencies), task.time, task.station);
        this.deepDependencies = task.deepDependencies;
    }

    private Step(final int id, final Collection<Step> taskDependencies, final int time, final Station station) {
        this.id = id;
        this.taskDependencies = taskDependencies;
        this.time = time;
        this.station = station;
    }

    public static Step step(final int id, final Collection<Step> taskDependencies, final int time, final Station station) {
        return new Step(id, taskDependencies, time, station);
    }

    /**
     * Make a new, independent copy of the given Step.
     * (Defensively copies dependencies.)
     */
    public static Step copyOf(final Step task) {
        return new Step(task);
    }

    // TODO: take more than 2 steps.
    /**
     * Merge two steps into a new task.
     */
    public static Step merge(final int id, final Step step1, final Step step2) {
        if (!Objects.equals(step1.station(), step2.station())) {
            throw new IllegalArgumentException("Both steps must be assigned to the same station (or null)");
        }
        final Set<Step> taskDependencies = new HashSet<>(step1.taskDependencies());
        taskDependencies.addAll(step2.taskDependencies());
        final int time = step1.time() + step2.time();
        return step(id, taskDependencies, time, null);
    }


    public Station station() {
        return station;
    }

    /**
     * For use by custom phases.
     */
    public Step station(Station station) {
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

    @Deprecated(forRemoval = true)
    public Collection<Step> stepDependencies() {
        return taskDependencies;
    }

    @ProblemFactCollectionProperty
    public Collection<Step> taskDependencies() {
        return taskDependencies;
    }

    /** Return the immediate and also transitive dependencies. */
    @ProblemFactCollectionProperty
    public Collection<Step> deepDependencies() {
        if (deepDependencies == null) {
            final var tmp = new HashSet<Step>(taskDependencies);
            for (final var task : taskDependencies) {
                tmp.addAll(task.deepDependencies());
            }
            deepDependencies = Set.copyOf(tmp);
        }
        return deepDependencies;
    }

    /**
     * This comparison is for (topological) sorting.
     * When sorted based on this comparison, steps appear AFTER the steps they
     * depend on.
     */
    @Override
    public int compareTo(final Step that) {
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
        final Step that = (Step) o;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

}
