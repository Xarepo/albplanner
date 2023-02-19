package se.ltu.alb.equip.algo;

import java.util.*;
import java.util.stream.IntStream;
import se.ltu.alb.equip.model.*;
import ch.rfin.util.Bits;
import ch.rfin.util.ImmutableBits;
import static ch.rfin.util.Misc.add;
import static ch.rfin.util.Collections.increment;

/**
 * Represents problem facts and current working solution.
 * This is primarily intended for computing and caching information that might
 * be needed by various other components like filters/selectors, move
 * generators, score calculators, etc.
 * <p>
 * This class can be considered experimental.
 * Some functionality may be missing or work inconsistently.
 * It may be a better solution to replace or complement this with some
 * shadow variable(s).
 * @author Christoffer Fink
 */
public class State {

    public enum ProblemType {
        /** Pure type 1 -- assembly line is completely unknown. */
        TYPE1,
        /** Pure type 2 -- assembly line is completely given. */
        TYPE2,
        /** Type 1 and 2 hybrid -- assembly line is partially given. */
        MIXED;
    }

    // task → station (assigned)
    // task → equipments (dependencies)
    // task → tasks (dependencies)
    // equipment → stations (assigned)
    // equipment → tasks (dependent)
    // station → tasks (assigned)
    // station → equipments (assigned)

    /** Problem type/variant. Always static. */
    public final ProblemType type;
    public final int stations;
    /** Map task ID to its task time. Always static. */
    public Map<Integer, Integer> taskTimes = new HashMap<>();
    /** Map task ID to its task dependencies (predecessors). Always static. */
    public Map<Integer, Bits> taskPredecessors = new HashMap<>();
    /** Map task ID to its dependent task (successors). Always static. */
    public Map<Integer, Bits> taskSuccessors = new HashMap<>();
    /** Map task ID to its assigned station number. Always dynamic. */
    public Map<Integer, Integer> taskToStation = new HashMap<>();
    /** Map task ID to its equipment dependencies. Always static. */
    public Map<Integer, Bits> taskToEquipments = new HashMap<>();
    /** Map equipment type to its equipped station numbers. Static for type 2. */
    public Map<Integer, Bits> equipmentToStations = new HashMap<>();
    /** Map equipment type to dependent tasks. Always static. */
    public Map<Integer, Bits> equipmentTypeToTasks = new HashMap<>();
    /** Map sets of equipment types to sets of tasks depending on them. Always static. */
    public Map<Bits, Bits> equipmentsToDependentTasks = new HashMap<>();
    /** Station number to assigned tasks. Always dynamic. */
    public Map<Integer, Bits> stationToTasks = new HashMap<>();
    /** Station number to equipped types. Static for type 2. */
    public Map<Integer, Bits> stationToInstalledEquipments = new HashMap<>();
    /** Station number to needed equipment types. Always dynamic. */
    public Map<Integer, Bits> stationToNeededEquipments = new HashMap<>();
    /** Station number to station time. Always dynamic. */
    public Map<Integer, Integer> stationTimes = new HashMap<>();
    /** The set of pinned (and assigned) equipment. Always static. */
    public Bits pinnedEquipment = ImmutableBits.empty();
    /** The set of movable equipment. Always static. */
    public Bits movableEquipment = ImmutableBits.empty();
    /** The set of equipment assigned to stations. Static for type 2. */
    public Bits installedEquipment = ImmutableBits.empty();
    /** The set of equipment that has been moved. Dynamic for mixed. */
    public Bits movedEquipment = ImmutableBits.empty();


    private State(ProblemType type, int stations) {
        this.type = type;
        this.stations = stations;
    }

    public static State init(AssemblyPlan plan) {
        ProblemType type = null; // FIXME
        int stations = plan.stations().size();
        return new State(type, stations).reset(plan);
    }

    public State reset(AssemblyPlan plan) {
        plan.tasks().forEach(this::processTask);
        plan.equipment().forEach(this::processEquipment);
        return this;
    }

    public State resetStatic(AssemblyPlan plan) {
        for (final var task : plan.tasks()) {
        }
        return this;
    }

    public IntStream compatibleStations(Bits equipReq) {
        assert !equipReq.isEmpty();
        return equipReq.stream()
            .mapToObj(equipmentToStations::get)
            .reduce(Bits::and)
            .orElse(ImmutableBits.empty())
            .stream();
    }

    public int firstCompatibleStation(Bits equipReq) {
        assert !equipReq.isEmpty();
        return equipReq.stream()
            .mapToObj(equipmentToStations::get)
            .reduce(Bits::and)
            .orElse(ImmutableBits.empty())
            .asBigInteger()
            .getLowestSetBit();
    }

    public int lastCompatibleStation(Bits equipReq) {
        assert !equipReq.isEmpty();
        return equipReq.stream()
            .mapToObj(equipmentToStations::get)
            .reduce(Bits::and)
            .orElse(ImmutableBits.empty())
            .asBigInteger()
            .bitLength() - 1;
    }

    private void processTask(Task task) {
        processTaskDependencies(task);
        processEquipmentDependencies(task);
        processTaskAssignment(task);
        increment(taskTimes, task.id(), task.time());
    }

    private void processTaskDependencies(Task task) {
        final int taskId = task.id();
        if (!taskPredecessors.containsKey(taskId)) {
            taskPredecessors.put(taskId, ImmutableBits.empty());
        }
        if (!taskSuccessors.containsKey(taskId)) {
            taskSuccessors.put(taskId, ImmutableBits.empty());
        }
        for (final var dep : task.taskDependencies()) {
            final int id = dep.id();
            add(taskPredecessors, taskId, id);
            add(taskSuccessors, id, taskId);
        }
    }

    private void processEquipmentDependencies(Task task) {
        final var bits = Bits.bits(task.equipmentDependencies());
        taskToEquipments.put(task.id(), bits);
        add(equipmentsToDependentTasks, bits, task.id());
        for (final var type : task.equipmentDependencies()) {
            add(equipmentTypeToTasks, type, task.id());
        }
    }

    private void processTaskAssignment(Task task) {
        var station = task.station();
        if (station == null) {
            return;
        }
        final int taskId = task.id();
        final int stationNr = station.number();
        taskToStation.put(taskId, stationNr);
        add(stationToTasks, stationNr, taskId);
        var equipment = taskToEquipments.get(taskId);
        assert equipment != null;
        add(stationToNeededEquipments, stationNr, equipment);
    }

    private void processEquipment(Equipment equipment) {
        final var station = equipment.station();
        if (station == null) {
            return;
        }
        final int type = equipment.type();
        final int stationNr = station.number();
        add(equipmentToStations, type, stationNr);
        add(stationToInstalledEquipments, stationNr, type);
        final int equipId = equipment.id();
        if (equipment.pinned()) {
            pinnedEquipment = pinnedEquipment.set(equipId);
        } else {
            movableEquipment = movableEquipment.set(equipId);
        }
        installedEquipment = installedEquipment.set(equipId);
    }

    public State assignTask(int taskID, int stationNr) {
        throw new UnsupportedOperationException("FIXME: Not implemented.");
    }

    public static Map<Integer, Bits> taskToTasks(AssemblyPlan plan) {
        Map<Integer, Bits> taskPredecessors = new HashMap<>();
        for (final var task : plan.tasks()) {
            final Iterable<Integer> dependencies = () -> task
                .taskDependencies()
                .stream()
                .map(Task::id)
                .iterator();
            final var bits = Bits.bits(dependencies);
            taskPredecessors.put(task.id(), bits);
        }
        return taskPredecessors;
    }

    public static Map<Integer, Bits> taskToEquipments(AssemblyPlan plan) {
        Map<Integer, Bits> taskToEquipments = new HashMap<>();
        for (final var task : plan.tasks()) {
            final var bits = Bits.bits(task.equipmentDependencies());
            taskToEquipments.put(task.id(), bits);
        }
        return taskToEquipments;
    }

    public static Map<Integer, Integer> taskToStation(AssemblyPlan plan) {
        Map<Integer, Integer> taskToStation = new HashMap<>();
        for (final var task : plan.tasks()) {
            var station = task.station();
            if (station == null) {
                continue;
            }
            taskToStation.put(task.id(), station.number());
        }
        return taskToStation;
    }

}
