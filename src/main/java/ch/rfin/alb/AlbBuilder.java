package ch.rfin.alb;

import java.util.*;
import static ch.rfin.util.Misc.checkPositive;
import static ch.rfin.util.Exceptions.illegalArg;

/* TODO: This describes a desirable builder. Which of these are actually true!?
 *
 * Enforces
 * - There is a single indexing mode that applies to all numbers
 *   (tasks, stations, equipments).
 *   It's either 0-based or 1-based.
 * - Task, station, and equipment numbers must
 *   - start with 0 or 1
 *   - be contiguous
 *   - match the number of tasks/stations/equipments set, if set
 * - No circular task dependencies.
 *
 * Instances can be described incrementally by adding facts about individual
 * tasks/stations, etc.
 * Or they can be described in bigger chunks, such as all information about
 * a single task/station.
 * Finally, they can be described in a way that mirrors the ALB file format.
 * That is, a whole section is described at once.
 */

/**
 * General ALB instance builder.
 *
 * @author Christoffer Fink
 */
public class AlbBuilder {
    private OptionalInt tasks = OptionalInt.empty();
    private OptionalInt cycleTime = OptionalInt.empty();
    private OptionalInt stations = OptionalInt.empty();
    private OptionalInt equipmentTypes = OptionalInt.empty();
    private Map<Integer,Integer> taskTimes = new HashMap<>();
    private Map<Integer,Collection<Integer>> dependencies = new HashMap<>();
    private Map<Integer,Collection<Integer>> taskEquipment = new HashMap<>();
    // Extensions
    private Map<Integer,Collection<Integer>> stationEquipment = new HashMap<>();
    private Map<String,Object> properties = new HashMap<>();

    // Auxilliary state information for integrity checking.
    private Collection<Integer> taskIds = new HashSet<>();
    private Map<Integer,Collection<Integer>> deepDependencies = new HashMap<>();
    private Map<Integer,Collection<Integer>> deepPrecedence = new HashMap<>();

    public static AlbBuilder albBuilder() {
        return new AlbBuilder();
    }

    // TODO: Do we actually need this? Hidden for now.
    /**
     * Build without validation.
     */
    private AlbInstance buildRaw() {
        return AlbInstance.albInstance()
            .tasks(tasks)
            .taskTimes(taskTimes)
            .taskDependencies(dependencies)
            .cycleTime(cycleTime)
            .stations(stations);
    }

    /**
     * Build and validate.
     */
    public AlbInstance buildValid() {
        var nrTasks = tasks.orElseGet(this::inferNumberOfTasks);
        return AlbInstance.albInstance()
            .tasks(nrTasks)
            .taskTimes(taskTimes)
            .taskDependencies(dependencies)
            .cycleTime(cycleTime)
            .stations(stations)
            .validatedOrFail();
    }

    /**
     * Specify the number of tasks.
     * Setting this is optional. If no value is specified, it will be inferred
     * from the actual tasks that are added. However, if set, it can be used
     * as an integrity check.
     * The number of tasks must be positive.
     * @param tasks the number of tasks in this problem
     * @throws IllegalArgumentException if number of tasks is not positive
     */
    public AlbBuilder tasks(int tasks) {
        checkPositive(tasks, "number of tasks");
        this.tasks = OptionalInt.of(tasks);
        return this;
    }

    /**
     * Specify the cycle time. Only relevant for type 1 planning.
     * The cycle time must be positive.
     * @param cycleTime the target cycle time
     * @throws IllegalArgumentException if cycleTime is not positive
     */
    public AlbBuilder cycleTime(int cycleTime) {
        checkPositive(cycleTime, "cycle time");
        this.cycleTime = OptionalInt.of(cycleTime);
        return this;
    }

    /**
     * Specify the number of stations. Only relevant for type 2 planning.
     * The number of stations must be positive.
     * @param stations the number of statons
     * @throws IllegalArgumentException if number of stations is not positive
     */
    public AlbBuilder stations(int stations) {
        checkPositive(stations, "number of stations");
        this.stations = OptionalInt.of(stations);
        return this;
    }

    /**
     * Specify the number of equipment types.
     * @param equipmentTypes the number of equipment types
     * @throws IllegalArgumentException if number of equipment types is not positive
     */
    public AlbBuilder equipmentTypes(int equipmentTypes) {
        checkPositive(equipmentTypes, "number of equipment types");
        this.equipmentTypes = OptionalInt.of(equipmentTypes);
        return this;
    }

    // --- Task dependencies / precedence ---

    /** 
     * Specify a single task dependency.
     * dependentTask depends on dependeeTask.
     * None of the tasks have to already be known. Which means it's
     * OK to depend on a task that has not explicitly been added.
     * However, the dependency must not create a cycle.
     * @param after the task that depends on the second task
     * @param before the task that the first task depends on
     * @throws IllegalArgumentException if the dependency creates a cycle.
     */
    public AlbBuilder taskDependency(int after, int before) {
        return addTaskId(after)
            .addTaskId(before)
            .addDependency(after, before);
    }

    /**
     * The before task must precede the after task.
     * This is exactly equivalent to
     * {@link #taskDependency(int, int) taskDependency(after, before)}.
     * @throws IllegalArgumentException if the dependency creates a cycle.
     */
    public AlbBuilder taskPrecedence(int before, int after) {
        return taskDependency(after, before);
    }

    /**
     * Specify multiple dependencies of a task.
     * @throws IllegalArgumentException if the dependency creates a cycle.
     */
    public AlbBuilder taskDependencies(int after, Collection<Integer> before) {
        if (before.isEmpty()) {
            addTaskId(after);
        } else {
            before.forEach(id -> taskDependency(after, id));
        }
        return this;
    }

    /* Specify the task time and all task dependencies of a task. */
    //public AlbBuilder _task(int task, int time, Collection<Integer> dependencies);

    /*
     * Specify the task time and all dependencies (on other tasks and
     * equipment) of a task.
     */
    //public AlbBuilder task(int task, int time, Collection<Integer> dependencies, Collection<Integer> equipment);

    /* Specify all task dependencies. */
    //public AlbBuilder taskDependencies(Map<Integer, Collection<Integer>> dependencies);
    // We could either just take the map as it is (possibly making a
    // defensive copy) or loop through it and add each dependency
    // individually by, for example, calling dependency().
    // Which makes integrity checking easier?
    // Also, what if some dependencies have been set individually before?
    // Should they be overridden or are these new ones added on top?

    // --- Task time ---

    /** Specify all task times. */
    public AlbBuilder taskTimes(Map<Integer, Integer> taskTimes) {
        this.taskTimes.putAll(taskTimes);
        return addTaskIds(taskTimes.keySet());
    }

    /** Specify task time for a specific task. */
    public AlbBuilder taskTime(int task, int time) {
        taskTimes.put(task, time);
        return addTaskId(task);
    }

    /* Specify all station equipment, for all stations. */
    //public AlbBuilder stationEquipment(Map<Integer, Collection<Integer>> equipmentType);

    /* Specify all station equipment for a specific station. */
    //public AlbBuilder stationEquipment(int station, Collection<Integer> equipmentTypes);

    /* Add one equipment to a station. */
    //public AlbBuilder stationEquipment(int station, int equipment);

    /** Add a property. */
    public AlbBuilder property(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    /** Specify full properties map. */
    public AlbBuilder properties(Map<String, Object> properties) {
        this.properties.putAll(properties);
        return this;
    }

    private int inferNumberOfTasks() {
        return taskIds.size();
    }

    /**
     * Register that a task with this ID exists.
     * If unknown, adds it to the set of known IDs and makes
     * empty entries in the dependency/precedence tables.
     */
    private AlbBuilder addTaskId(int id) {
        if (taskIds.contains(id)) {
            return this;
        }
        taskIds.add(id);
        assert !dependencies.containsKey(id);
        assert !deepDependencies.containsKey(id);
        assert !deepPrecedence.containsKey(id);
        dependencies.put(id, new ArrayList<>());
        deepDependencies.put(id, new ArrayList<>());
        deepPrecedence.put(id, new ArrayList<>());
        return this;
    }

    private AlbBuilder addTaskIds(Collection<Integer> ids) {
        ids.forEach(this::addTaskId);
        return this;
    }

    /* Dependency DAG
     * Dependencies: {1: [2,3], 2: [4], 3: [5], 4: [6], 5: [7,6], 6: [8],   7: [],  8: []}
     * Precedence:   {1: [],    2: [1], 3: [1], 4: [2], 5: [3],   6: [4,5], 7: [5], 8: [6]}
     *
     *   2--4
     *  /    \
     * 1      6--8
     *  \    /
     *   3--5--7
     *
     * Transitive (deep) dependencies
     * {1: [2,3,4,5,6,7,8], 2: [4,6,8], 3: [5,6,7,8], 4: [6,8], 5: [6,7,8], 6: [8]}
     *
     *  2 4 6      4           6         6     6
     *   \|/       |           |        /      |
     *    1--8     2--8     5--3--7    4       5--7    6--8
     *   /|\       |           |        \      |
     *  3 5 7      6           8         8     8
     *
     * Transitive (deep) precedence
     * {8: [1,2,3,4,5,6], 7: [1,3,5], 6: [1,2,3,4,5], 5: [1,3], 4: [1,2], 3: [1], 2: [1]}
     *
     *  1 2 3      1           2         1       1
     *   \|/       |           |        /       /
     *    8        7--5     1--6--3    5       4    3--1    2--1
     *   /|\       |          / \       \       \
     *  4 5 6      3         4   5       3       2
     */
    private AlbBuilder addDependency(int after, int before) {
        if (deepDependencies.get(before).contains(after)) {
            final String msg = "Making %d depend on %d would introduce a "
                + "circular dependency, because %d already (transitively) "
                + "depends on %d.";
            illegalArg(msg, after, before, before, after);
        }

        // for i in [after] + predecessors[after]:
        //   dependencies[i] += [before] + dependencies[before]
        //
        // for i in [before] + dependencies[before]:
        //   predecessors[i] += [after] + predecessors[after]

        Set<Integer> allBefore = new HashSet<>(deepDependencies.get(before));
        allBefore.add(before);
        Set<Integer> allAfter = new HashSet<>(deepPrecedence.get(after));
        allAfter.add(after);

        for (final int i : allAfter) {
            deepDependencies.get(i).addAll(allBefore);
        }
        for (final int i : allBefore) {
            deepPrecedence.get(i).addAll(allAfter);
        }
        dependencies.get(after).add(before);

        return this;
    }

}
