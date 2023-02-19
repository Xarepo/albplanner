package se.ltu.alb.equip.model;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.function.Function;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.collectingAndThen;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.ProblemFactProperty;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import se.ltu.alb.util.Util;
import ch.rfin.util.Bits;
import ch.rfin.util.Pair;
import ch.rfin.util.Pairs;
import ch.rfin.alb.AlbInstance;
import ch.rfin.util.Graphs;
import static ch.rfin.util.Functions.ceil;
import static ch.rfin.util.Predicates.nonNull;
import static ch.rfin.util.Predicates.not;
import static ch.rfin.util.Printing.printf;

/**
 * An Assembly Plan assigns steps to assembly line stations - it is a
 * sequencing of assembly steps.
 * @author Christoffer Fink
 */
@PlanningSolution
public class AssemblyPlan implements Comparable<AssemblyPlan> {

    @PlanningEntityCollectionProperty
    private Collection<Task> steps;
    @PlanningEntityCollectionProperty
    private Collection<Equipment> equipment;
    @ProblemFactCollectionProperty
    private Collection<Station> stations;
    @ProblemFactProperty
    public OptionalInt cycleTime = OptionalInt.empty();
    @PlanningScore
    private HardMediumSoftLongScore score;

    /** OptaPlanner needs a no-arg constructor. */
    private AssemblyPlan() { }

    public AssemblyPlan(Collection<Task> steps, Collection<Station> stations, Collection<Equipment> equipment) {
        this.steps = steps;
        this.stations = stations;
        this.equipment = equipment;
        assert valid();
    }

    public static AssemblyPlan copyOf(AssemblyPlan plan) {
        var tasks = new ArrayList<Task>(plan.tasks().size());
        plan.tasks().forEach(task -> tasks.add(Task.copyOf(task)));
        var stations = new ArrayList<Station>(plan.stations());
        var equipment = new ArrayList<Equipment>(plan.equipment());
        var copy = new AssemblyPlan(tasks, stations, equipment);
        copy.cycleTime = plan.cycleTime;
        return copy;
    }

    private boolean valid() {
        final long uniqueTaskIds = steps
            .stream().map(Task::id).distinct().count();
        assert steps.size() == uniqueTaskIds;
        final long uniqueEquipmentIds = equipment
            .stream().map(Equipment::id).distinct().count();
        assert equipment.size() == uniqueEquipmentIds;
        final long uniqueStationNumbers = stations
            .stream().map(Station::number).distinct().count();
        assert stations.size() == uniqueStationNumbers;
        Collection<Integer> availableEquipmentTypes =
            equipment.stream().map(Equipment::type).collect(toSet());
        assert steps.stream()
            .map(Task::equipmentDependencies)
            .flatMap(Collection::stream)
            .allMatch(availableEquipmentTypes::contains);
        return true;
    }

    /**
     * Check that this is a valid type 2 problem.
     * If this is a type 2 planning problem, all task equipment dependencies
     * must be satisfied by some station.
     * That means each required equipment set must actually be equipped.
     */
    private boolean validType2() {
        // XXX: this must be commented out while we have the dummy equipment
        // workaround.
        //assert equipment.stream().allMatch(Equipment::pinned);
        // For now, instead check that the dummy equipment is the only one
        // that is unpinned, by check that there is at most 1 unpinned.
        assert equipment.stream().filter(not(Equipment::pinned)).count() <= 1L;
        var equipped = equippedSets().collect(toSet());
        var required = taskEquipmentSets();
        if (!required.allMatch(req -> equipped.stream().anyMatch(req::subsetOf))) {
            System.out.println("equipped: " + equipped);
            var missing = taskEquipmentSets()
                .filter(req -> equipped.stream().noneMatch(req::subsetOf))
                .findFirst().get();
            String msg = String.format(
                    "task equipment requirement %s not offered by any station",
                    missing);
            throw new AssertionError(msg);
        }
        return true;
    }

    /**
     * Check that this is a valid type 1 problem.
     * TODO: what does this mean? What are the restrictions?
     */
    private boolean validType1() {
        // TODO: what about pinned equipment? Do they all have to be unpinned?
        // Do some of them have to be unpinned?
        var equipped = equippedSets().collect(toSet());
        var required = taskEquipmentSets();
        if (!required.allMatch(req -> equipped.stream().anyMatch(req::subsetOf))) {
            System.out.println("equipped: " + equipped);
            var missing = taskEquipmentSets()
                .filter(req -> equipped.stream().noneMatch(req::subsetOf))
                .findFirst().get();
            String msg = String.format(
                    "task equipment requirement %s not offered by any station",
                    missing);
            throw new AssertionError(msg);
        }
        return true;
    }

    public static AssemblyPlan fromAlb(AlbInstance alb) {
        return fromAlbType2(alb);
    }

    // FIXME: make sure this is fully adapted to type 1 problems!
    public static AssemblyPlan fromAlbType1(AlbInstance alb) {
        assert alb.isZeroBased();
        final List<Station> stations = IntStream
            .range(0, stationsUpperBound(alb))
            .mapToObj(Station::station)
            .collect(toList());
        // Map equipment types to the collections of stations equipped with them.
        final Map<Integer,Collection<Station>> equippedAt = new HashMap<>();
        for (final var entry : alb.stationEquipment.entrySet()) {
            final var station = stations.get(entry.getKey());
            final var equips = entry.getValue();
            for (final int equip : equips) {
                var list = equippedAt.get(equip);
                if (list == null) {
                    list = new ArrayList<>();
                    equippedAt.put(equip, list);
                }
                list.add(station);
            }
        }
        final List<Equipment> equipment = new ArrayList<>();
        int equipId = 0;
        // Loop over all equipment types and create the necessary instances.
        // XXX:
        // If stations are equipped, then we know that we need an instance of
        // each type for every station where it is equipped.
        // But if they are not equipped, how many instances do we need!?
        // For now, just create one, but that's not really a solution.
        // This is similar to the problem of setting an upper bound on stations.
        for (int i = 0; i < alb.equipments.orElse(0); i++) {
            final int type = i;
            if (equippedAt.containsKey(type)) {
                for (final var station : equippedAt.get(type)) {
                    //equipment.add(Equipment.unpinned(equipId, type, 0, station));
                    // XXX: assume pinned!
                    equipment.add(Equipment.pinned(equipId, type, station));
                    equipId++;
                }
            } else {
                //*
                // XXX: Only creates one unequipped!
                equipment.add(Equipment.unequipped(equipId, type, 0));
                equipId++;
                //*/
            }
        }
        // Ok, now we have the stations and the equipment.
        // Time for the tasks.
        final Map<Integer,Collection<Integer>> dag = alb.taskDependencies();
        final List<Integer> sortedTaskIds = Graphs.topologicalSort(dag);
        final Map<Integer,Task> tasksById = new HashMap<>();
        final List<Task> tasks = new ArrayList<>(); // sorted
        sortedTaskIds.forEach(id -> {
            var dependencies = dag.get(id)
                .stream()
                .map(tasksById::get)
                .collect(toList());
            final int time = alb.taskTimes.get(id);
            final var equip = alb.taskEquipment.getOrDefault(id, Set.of());
            final var task = Task.task(id, time, dependencies, equip);
            tasksById.put(id, task);
            tasks.add(task);
        });
        var plan = new AssemblyPlan(tasks, stations, equipment);
        assert plan.validType1();
        return plan;
    }

    /**
     * Make an AssemblyPlan from an ALB instance.
     * Does not currently support
     * - equipment price
     * - pinning (currently assumes pinned if equipped!)
     * - equipment type counts (how many instances of a type exists)
     * <p>
     * In other words, this is specialized for pure type-2 planning!
     */
    public static AssemblyPlan fromAlbType2(AlbInstance alb) {
        assert alb.isZeroBased();
        // NOTE: alb.stations() is not the same as <number of stations> in the .alb file!
        // (Instead set by the generator/converter?)
        final int m = alb.stations().isPresent()
            ? alb.stations().getAsInt()
            : stationsUpperBound(alb);
        final List<Station> stations = IntStream
            .range(0, m)
            .mapToObj(Station::station)
            .collect(toList());
        // Map equipment types to the collections of stations equipped with them.
        final Map<Integer,Collection<Station>> equippedAt = new HashMap<>();
        for (final var entry : alb.stationEquipment.entrySet()) {
            final var station = stations.get(entry.getKey());
            final var equips = entry.getValue();
            for (final int equip : equips) {
                var list = equippedAt.get(equip);
                if (list == null) {
                    list = new ArrayList<>();
                    equippedAt.put(equip, list);
                }
                list.add(station);
            }
        }
        final List<Equipment> equipment = new ArrayList<>();
        int equipId = 0;
        // Loop over all equipment types and create the necessary instances.
        // XXX:
        // If stations are equipped, then we know that we need an instance of
        // each type for every station where it is equipped.
        // But if they are not equipped, how many instances do we need!?
        // For now, just create one, but that's not really a solution.
        // This is similar to the problem of setting an upper bound on stations.
        for (int i = 0; i < alb.equipments.orElse(0); i++) {
            final int type = i;
            if (equippedAt.containsKey(type)) {
                for (final var station : equippedAt.get(type)) {
                    //equipment.add(Equipment.unpinned(equipId, type, 0, station));
                    // XXX: assume pinned!
                    equipment.add(Equipment.pinned(equipId, type, station));
                    equipId++;
                }
            } else {
                String msg = String.format(
                        "Assuming type 2 problems, every equipment should be"
                        +" equipped at some station. But equpment type %d"
                        +" was not found in %s", type, equippedAt);
                throw new AssertionError(msg);
            }
        }
        // Ok, now we have the stations and the equipment.
        // Time for the tasks.
        final Map<Integer,Collection<Integer>> dag = alb.taskDependencies();
        final List<Integer> sortedTaskIds = Graphs.topologicalSort(dag);
        final Map<Integer,Task> tasksById = new HashMap<>();
        final List<Task> tasks = new ArrayList<>(); // sorted
        sortedTaskIds.forEach(id -> {
            var dependencies = dag.get(id)
                .stream()
                .map(tasksById::get)
                .collect(toList());
            final int time = alb.taskTimes.get(id);
            final var equip = alb.taskEquipment.getOrDefault(id, Set.of());
            final var task = Task.task(id, time, dependencies, equip);
            tasksById.put(id, task);
            tasks.add(task);
        });
        Collections.sort(tasks, Comparator.comparing(Task::id));
        // XXX: This is a hack to work around an OptaPlanner limitation
        // (PLANNER-2208).  If there are multiple planning entities, and every
        // instance of one entity is pinned (as is the case in pure type 2),
        // then OptaPlanner spams "bailing out of neverEnding selector".
        // (And fails to make any progress?)
        // So we add a dummy equipment that is unpinned and that no task
        // depends on. Then OptaPlanner has something to move around.
        // NOTE: we still get the bailing out warning, but it's much less
        // frequent.
        final int dummyType = alb.equipmentTypes().orElse(0);
        final Station lastStation = stations.get(stations.size() - 1);
        equipment.add(Equipment.dummy(equipId, dummyType, null));
        var plan = new AssemblyPlan(tasks, stations, equipment);
        assert plan.validType2();
        return plan;
    }

    // How many stations are there? Either a number is specified in the problem
    // instance, or we have to decide on some reasonable upper bound.
    private static int stationsUpperBound(AlbInstance alb) {
        if (alb.stations.isPresent()) {
            return alb.stations.getAsInt();
        }
        // If we don't have stations, we should have a cycle time.
        final int cycleTime = alb.cycleTime.getAsInt();
        final int totalTaskTime = alb.taskTimes.values().stream()
            .mapToInt(time -> time)
            .sum();
        final int lowerBound = ceil(2.0 * totalTaskTime / cycleTime);
        if (alb.stationEquipment.size() == 0) {
            // There SHOULDN'T be a problem with equipment screwing up the
            // upper bound if it isn't pinned to stations.
            return lowerBound;
        }
        // XXX: This is more pessimistic than necessary!
        // A tighter upper bound would be based on the tasks that can happen
        // after the last equipped station, excluding those that must
        // necessarily happen before (or at).
        final int lastEquipped = alb.stationEquipment.keySet()
            .stream()
            .max(Integer::compare)
            .get();
        return lastEquipped + lowerBound;
    }

    @ValueRangeProvider(id = "stations")
    public Collection<Station> stations() {
        return stations;    // FIXME: make sure they are sorted!
    }

    public List<Task> tasks() {
        return List.copyOf(steps);  // XXX: really necessary!?
    }

    @Deprecated(forRemoval = true)
    public List<Task> steps() {
        return tasks();
    }

    public Collection<Equipment> equipment() {
        return equipment;
    }

    public void steps(Collection<Task> steps) {
        this.steps = steps;
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

    // TODO: Is there any point in making this a shadow variable?
    /**
     * Returns the current task assignments as a map mapping stations to
     * their assigned tasks.
     */
    public SortedMap<Station, Collection<Task>> stationAssignmentsMap() {
        var assignments = new TreeMap<Station,Collection<Task>>();
        for (final var step : steps) {
            final var station = step.station();
            if (station == null) {
                continue;
            }
            if (!assignments.containsKey(station)) {
                assignments.put(station, new ArrayList<Task>());
            }
            assignments.get(station).add(step);
        }
        return assignments;
    }

    @Deprecated(forRemoval = true)
    public SortedMap<Station, Collection<Task>> stationAssignments() {
        return stationAssignmentsMap();
    }

    /**
     * Returns the current equippment assignments as a map mapping stations
     * to their equipments.
     */
    public SortedMap<Station, Collection<Equipment>> stationEquipmentMap() {
        var assignments = new TreeMap<Station,Collection<Equipment>>();
        stations()
            .forEach(station -> assignments.put(station, new ArrayList<>()));
        for (final var equip : equipment) {
            final var station = equip.station();
            if (station == null) {
                continue;
            }
            assignments.get(station).add(equip);
        }
        assert assignments.size() == stations.size();
        return assignments;
    }

    @Deprecated(forRemoval = true)
    public SortedMap<Station, Collection<Equipment>> stationEquipment() {
        return stationEquipmentMap();
    }

    /**
     * Returns a stream of all stations that are equipped with some equipment.
     */
    public Stream<Station> equippedStations() {
        return equipment.stream()
            .map(Equipment::station)
            .filter(nonNull());
    }

    /**
     * Returns a stream of all equippment that has been equipped at a station.
     */
    public Stream<Equipment> equipped() {
        return equipment.stream().filter(equip -> equip.station() != null);
    }

    /**
     * Returns a stream of all unused equippment.
     */
    public Stream<Equipment> unequipped() {
        return equipment.stream().filter(equip -> equip.station() == null);
    }

    /**
     * Returns a stream of the equipment sets available at some station.
     * For example, if station 1 has equipment types 0 and 1, and station
     * 2 has equipment types 1 and 2, then the stream would contain sets
     * {0,1} and {1,2}.
     */
    public Stream<Bits> equippedSets() {
        return stationEquipment().values().stream()
            .map(eqs -> eqs.stream().map(Equipment::type).collect(toList()))
            .map(Bits::bits);
    }

    public Map<Station, Bits> equippedSetsMap() {
        return equipped()
            .collect(groupingBy(Equipment::station,
                        mapping(Equipment::type,
                            collectingAndThen(toSet(), Bits::bits))));
    }

    // TODO: why is "? extends Bits" necessary here? What do we need to do
    // to get rid of it?
    public Map<Task, ? extends Bits> taskEquipmentSetsMap() {
        return steps
            .stream()
            .map(Pair::of)
            .map(Pairs.map_2(Task::equipmentDependencies))
            .filter(Pairs.test_2(nonNull()))
            .filter(Pairs.test_2(not(Collection::isEmpty)))
            .map(Pairs.map_2(Bits::bits))
            .collect(Pairs.pairsToMap());
    }

    public Stream<Bits> taskEquipmentSets() {
        return steps
            .stream()
            .map(Task::equipmentDependencies)
            .filter(nonNull())
            .filter(not(Collection::isEmpty))
            .map(Bits::bits);
    }

    public Collection<Task> assignments(final Station station) {
        return stationAssignments().get(station); // REFACTOR?
    }

    public long totalTime(Station station) {
        return stationAssignments()
            .getOrDefault(station, Collections.emptyList())
            .stream()
            .mapToInt(Task::time)
            .sum();
    }

    /*
     * Station 0:
     *   Equipped types = <set of equipment types at this station>
     *   Pinned = <set of equipment pinned to this station>
     *   Assigned tasks: (Load = <total assigned task time>, #Tasks = <number of tasks>)
     *     Task n:
     *       Time = <task time>
     *       Task Dependencies = <set of task IDs>
     *       Equipment Dependencies = <set of equipment types>
     *     Task m:
     *     ⋮
     * Station 1:
     * ⋮
     */

    // Always show numbers (IDs, numbers, types) in sorted order
    /*
     * Stations (<count>)...
     *   Station 0:
     *     Equipped types = <list/set of equipment types at this station>
     *     Pinned = <set of equipment pinned to this station>
     *     Assigned tasks = <list of task IDs>
     *     Load = <count>: <total task time>
     *   Station 1:
     *   ⋮
     * Tasks (<count>)...
     *   Task 1:
     *     Time = <task time>
     *     Task Dependencies = <count>: <list of task IDs>
     *     Equipment Dependencies = <count>: <list of equipment types>
     *     Compatible stations = <count>: <list of station numbers>
     *     Assigned to <station number>
     *   Task 1:
     *   ⋮
     * Equipment types (<total instance count>, <type count> types)...
     *   Type 0:
     *     Equipped at = <instance count>: <list of station numbers>
     *   Type 1:
     *   ⋮
     */

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        var assignments = stationAssignments();
        for (final var station : stations()) {
            final var steps = assignments.getOrDefault(station, Collections.emptySet());
            StringBuilder ssb = new StringBuilder();
            int totalTime = 0;
            for (final var step : steps) {
                String dependencies = step
                    .taskDependencies()
                    .stream()
                    .map(Task::id)
                    .map(String::valueOf)
                    .collect(joining(","));
                ssb.append("  step " + step.id())
                   .append(" (time: " + step.time() + ")")
                   .append("\n")
                   .append("    depends on {" + dependencies + "}")
                   .append("\n");
                totalTime += step.time();
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
        for (final var step : steps) {
            sb.append(step.id())
                .append(":")
                .append(step.station())
                .append(", ");
        }
        return stationAssignments().toString();
    }

}
