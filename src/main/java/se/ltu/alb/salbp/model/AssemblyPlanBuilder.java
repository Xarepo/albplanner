package se.ltu.alb.salbp.model;

import java.util.*;
import java.util.stream.*;
import java.util.function.Function;
import static java.util.stream.Collectors.*;
import se.ltu.alb.util.Util;
import ch.rfin.util.Graphs;
import ch.rfin.util.Pair;
import static ch.rfin.util.Pair.pair;

/**
 * AssemblyPlan builder.
 * This class currently exists for backward compatibility.
 * It is old and became increasingly outdated. The current version is a
 * heavily stripped back and simplified/cleaned up version, but it is
 * quite hacky in nature.
 * It may be significantly modified or even removed in the future.
 * @author Christoffer Fink
 */
@Deprecated
public class AssemblyPlanBuilder {

    private Collection<Step> steps = new ArrayList<>();
    private Collection<Station> stations = new ArrayList<>();
    private int nextStepId = 0;
    private int nextStationId = 0;

    private Map<Integer, Collection<Integer>> stationAssignments = new TreeMap<>();     // station → [steps]
    private Map<Integer, Collection<Integer>> stepDependencies = new TreeMap<>();       // step → [steps]
    private Map<Integer, Integer> stepAssignments = new TreeMap<>();                    // step → station
    private Map<Integer, Integer> stepTimes = new TreeMap<>();                          // step → time

    private SortedSet<Integer> stepIds = new TreeSet<>();
    private SortedSet<Integer> stationIds = new TreeSet<>();

    // TODO: should be able to maintain a deep dependency map and check for circular dependencies as steps are added!

    public static AssemblyPlanBuilder builder() {
        return new AssemblyPlanBuilder();
    }

    public AssemblyPlan build() {
        var missingDependency = Graphs.missingDependency(stepDependencies);
        if (missingDependency.isPresent()) {
            throw new IllegalStateException("Step " + missingDependency.get() + " is missing!");
        }
        var stations = buildStations();
        var steps = Util.topologicalSort(buildSteps(stations), Step::taskDependencies);
        return new AssemblyPlan(steps, stations.keySet().stream().map(stations::get).collect(toList()));
    }

    private SortedMap<Integer, Station> buildStations() {
        TreeMap<Integer, Station> result = new TreeMap<>();
        stationIds.stream()
            .map(Station::station)
            .forEach(station -> {
                result.put(station.number(), station);
            });
        return result;
    }

    private List<Step> buildSteps(Map<Integer, Station> stations) {
        Map<Integer, Integer> assignments = reverseAssignmentMap();
        Map<Integer, Step> steps = new TreeMap<>();
        List<Integer> sortedIds = Graphs.topologicalSort(stepIds, stepDependencies);
        return sortedIds.stream()
            .map(i -> Step.step(
                        i,
                        getStepDependencies(i, steps),
                        getStepTime(i),
                        assignments.containsKey(i) ? stations.get(assignments.get(i)) : null))
            .peek(step -> steps.put(step.id(), step))
            .collect(toList());
    }

    @Deprecated(forRemoval = true)
    private Map<Integer, Integer> reverseAssignmentMap() {
        return stationAssignments.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream().map(step -> pair(step, entry.getKey())))
            .collect(toMap(Pair::get_1, Pair::get_2));
    }

    private int getStepTime(final int step) {
        return stepTimes.getOrDefault(step, 0);
    }

    private Set<Step> getStepDependencies(int step, Map<Integer, Step> steps) {
        return stepDependencies.get(step).stream()
            .peek(d -> {
                if (!steps.containsKey(d)) {
                    throw new AssertionError("No Step for dependency " + d + " of step " + step);
                }
            })
            .map(steps::get)
            .collect(toSet());
    }

    // Will throw StackOverflow if there are circular dependencies.
    private Set<Integer> getDeepStepDependencies(int step) {
        Set<Integer> dependencies = new TreeSet<>(stepDependencies.get(step));
        Set<Integer> deep = new TreeSet<>(dependencies);
        dependencies.stream()
            .map(this::getDeepStepDependencies)
            .forEach(deep::addAll);
        return deep;
    }

    private Map<Integer, Collection<Integer>> getDeepStepDependencies() {
        return stepIds.stream()
            .collect(toMap(Function.identity(), this::getDeepStepDependencies));
    }

    private AssemblyPlanBuilder addStepDependencies(final int step, final Iterable<Integer> dep) {
        if (!stepDependencies.containsKey(step)) {
            stepDependencies.put(step, new ArrayList<>());
        }
        addAll(stepDependencies.get(step), dep);
        stepIds.add(step);
        addAll(stepIds, dep);
        return this;
    }

    private AssemblyPlanBuilder addStation(final int station) {
        if (!stationAssignments.containsKey(station)) {
            stationAssignments.put(station, new ArrayList<>());
        }
        stationIds.add(station);
        return this;
    }

    private AssemblyPlanBuilder addStationAddignment(final int step, final int station) {
        addStation(station);
        stationAssignments.get(station).add(step);
        stepAssignments.put(step, station);
        return this;
    }

    private AssemblyPlanBuilder addStepTime(final int step, final int time) {
        stepTimes.put(step, time);
        return this;
    }

    public AssemblyPlanBuilder step(
            final int id,
            final int time,
            final Iterable<Integer> dep,
            final int station)
    {
        return step(id, time, dep)
            .addStationAddignment(id, station);
    }

    public AssemblyPlanBuilder step(
            final int id,
            final int time,
            final Iterable<Integer> dep)
    {
        return addStepDependencies(id, dep)
            .addStepTime(id, time);
    }


    /**
     * Add {@code n} stations (with automatically generated number).
     */
    public AssemblyPlanBuilder stations(final int n) {
        int station = nextStationId();
        IntStream.range(station, station+n).forEach(this::addStation);
        return this;
    }

    private int nextStationId() {
        return stationAssignments.keySet().stream().mapToInt(i -> i).max().orElse(0) + 1;
    }

    private <T> void addAll(Collection<T> col, Iterable<T> ts) {
        for (final var t : ts) {
            if (t == null) {
                throw new IllegalArgumentException("null value in iterable!");
            }
            col.add(t);
        }
    }

}
