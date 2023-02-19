package se.ltu.alb.salbp.model;

import java.util.*;
import java.util.function.Function;
import static java.util.stream.Collectors.joining;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.ProblemFactProperty;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ch.rfin.alb.AlbInstance;
import se.ltu.alb.util.Util;

// TODO: check that stations are sorted??

/**
 * An Assembly Plan assigns tasks to assembly line stations.
 * @author Christoffer Fink
 */
@PlanningSolution
public class AssemblyPlan implements Comparable<AssemblyPlan> {

    @PlanningEntityCollectionProperty
    protected List<Step> tasks;
    @ProblemFactCollectionProperty
    protected List<Station> stations;
    @ProblemFactProperty
    public OptionalInt cycleTime = OptionalInt.empty();
    @PlanningScore
    protected HardMediumSoftLongScore score;

    /** OptaPlanner requires a no-arg constructor. */
    public AssemblyPlan() { }

    public AssemblyPlan(List<Step> tasks, List<Station> stations) {
        this.tasks = tasks;
        this.stations = stations;
        if (!Util.isTopSorted(tasks, Step::taskDependencies)) {
            System.out.println("!! Warning: tasks were not sorted!");
        }
    }

    public static AssemblyPlan copyOf(AssemblyPlan plan) {
        var tasks = new ArrayList<Step>(plan.tasks().size());
        plan.tasks().forEach(task -> tasks.add(Step.copyOf(task)));
        var stations = new ArrayList<Station>(plan.stations());
        var copy = new AssemblyPlan(tasks, stations);
        copy.cycleTime = plan.cycleTime;
        return copy;
    }

    // --- OptaPlanner stuff ---

    @ValueRangeProvider(id = "stations")
    public List<Station> stations() {
        return stations;    // FIXME: make sure they are sorted!
    }

    // --- end of OptaPlanner stuff ---

    @Deprecated(forRemoval = true)
    public List<Step> steps() {
        return List.copyOf(tasks);
    }

    public List<Step> tasks() {
        return tasks;
    }

    @Deprecated(forRemoval = true)
    public void tasks(List<Step> tasks) {
        this.tasks = tasks;
    }

    public OptionalInt cycleTime() {
        return cycleTime;
    }

    public AssemblyPlan cycleTime(int cycleTime) {
        return cycleTime(OptionalInt.of(cycleTime));
    }

    public AssemblyPlan cycleTime(OptionalInt cycleTime) {
        this.cycleTime = cycleTime;
        return this;
    }

    public HardMediumSoftLongScore score() {
        return score;
    }

    public void score(HardMediumSoftLongScore score) {
        this.score = score;
    }

    // Note that this cannot be cached, since it changes as assignments are
    // made during solving.
    // TODO: Right, but we CAN make it a shadow variable!
    public SortedMap<Station, Collection<Step>> stationAssignments() {
        var assignments = new TreeMap<Station,Collection<Step>>();
        for (final var task : tasks) {
            final var station = task.station();
            if (station == null) {
                continue;
            }
            if (!assignments.containsKey(station)) {
                assignments.put(station, new ArrayList<Step>());
            }
            assignments.get(station).add(task);
        }
        return assignments;
    }

    public Collection<Step> assignments(final Station station) {
        return stationAssignments().get(station); // REFACTOR?
    }

    public long totalTime(Station station) {
        return stationAssignments()
            .getOrDefault(station, Collections.emptyList())
            .stream()
            .mapToInt(Step::time)
            .sum();
    }

    // TODO: simplify for SALBP!
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        var assignments = stationAssignments();
        for (final var station : stations()) {
            final var tasks = assignments.getOrDefault(station, Collections.emptySet());
            StringBuilder ssb = new StringBuilder();
            int totalTime = 0;
            for (final var task : tasks) {
                String dependencies = task
                    .taskDependencies()
                    .stream()
                    .map(Step::id)
                    .map(String::valueOf)
                    .collect(joining(","));
                ssb.append("  task " + task.id())
                   .append(" (time: " + task.time() + ")")
                   .append("\n")
                   .append("    depends on {" + dependencies + "}")
                   .append("\n");
                totalTime += task.time();
            }
            sb.append("station " + station.number());
            sb.append(" (time: " + totalTime + ")");
            sb.append("\n");
            sb.append(ssb);
        }
        return sb.toString().trim();
    }

    /**
     * Compares assembly plans based on their scores.
     */
    @Override
    public int compareTo(final AssemblyPlan that) {
        return score.compareTo(that.score());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (final var task : tasks) {
            sb.append(task.id())
                .append(":")
                .append(task.station())
                .append(", ");
        }
        return stationAssignments().toString();
    }

    /**
     * Convert an AlbInstance represneting a SALBP (type 1 or 2) instance to
     * an AssemblyBlan.
     */
    public static AssemblyPlan fromAlb(AlbInstance alb) {
        if (alb.cycleTime.isPresent() && alb.stations.isEmpty()) {
            return fromAlb1(alb);
        } else if (alb.cycleTime.isEmpty() && alb.stations.isPresent()) {
            return fromAlb2(alb);
        }
        throw new IllegalArgumentException("Not supported");
    }

    /**
     * Convert an AlbInstance represneting a SALBP-1 instance to an AssemblyBlan.
     */
    public static AssemblyPlan fromAlb1(AlbInstance alb) {
        // Build the problem instance.
        AssemblyPlanBuilder builder = AssemblyPlanBuilder.builder();
        alb.taskDependencies.forEach((id, deps) ->
            builder.step(id, alb.taskTimes.get(id), deps)
        );
        final int cycleTime = alb.cycleTime.getAsInt();
        // We could estimate an upper limit on the number of stations needed
        // and use that instead. However, with the right configuration, this
        // is not necessary. So we can use this simpler upper bound that is
        // guaranteed to be sufficiently large.
        final int stations = alb.tasks();
        builder.stations(stations);

        AssemblyPlan unsolved = builder.build();
        unsolved.cycleTime(cycleTime);
        return unsolved;
    }

    /**
     * Convert an AlbInstance represneting a SALBP-2 instance to an AssemblyBlan.
     */
    public static AssemblyPlan fromAlb2(AlbInstance alb) {
        AssemblyPlanBuilder builder = AssemblyPlanBuilder.builder();
        alb.taskDependencies.forEach((id, deps) ->
            builder.step(id, alb.taskTimes.get(id), deps)
        );
        final int stations = alb.stations.getAsInt();
        builder.stations(stations);
        return builder.build();
    }


}
