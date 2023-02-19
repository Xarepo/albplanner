package ch.rfin.alb;

import java.util.*;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static ch.rfin.util.Functions.dec;
import ch.rfin.util.Pairs;
import static ch.rfin.util.Pairs.stream;
import static ch.rfin.util.Pairs.pairsToMap;

/*
 * Changes from old (strict) AlbInstance:
 * - Basically, anything goes. Use the validator to check. Leave it to the
 *   builder to (by default) validate.
 * - Add support for arbitrary properties (mapping names to objects).
 * - Add support for unparsed sections (mapping tags to lines (lists of strings)).
 * - Pinning (equipment to stations, tasks to stations)
 *
 * Maybe:
 * - Add problemType field (also add to parser)
 */

/**
 * Intermediate representation of an assembly line balancing problem.
 * This is the parsed representation of an ALB file. It will likely
 * need to be converted to a more specific model.
 * <p>
 * By default, no sanity checking or validation is performed. So it is possible
 * to represent arbitrary, even nonsensical instances.
 * Use {@link #validatedOrFail()} or {@link #validOrFail()} to validate.
 * It is also possible to build valid instances with
 * {@link ch.rfin.alb.AlbBuilder#buildValid()}.
 * <p>
 * <strong>Indexing modes: 1-based vs 0-based</strong>.
 * By the definition of the ALB file format, counting starts at 1.
 * So tasks, stations, and equipment are all numbered 1, 2, …, N.
 * But it's often convenient to start at 0 instead, so that the numbers
 * can be used as indices.
 * Therefore {@link #zeroBased()} and {@link #oneBased()} allow an instance to
 * be translated from one version to the other.
 * Note that, while both modes are supported, exactly one of them has to be
 * used consistently. So you can't have task numbers starting at 0 and
 * stations starting at 1, for example.
 *
 * @author Christoffer Fink
 */
public final class AlbInstance {    // Might allow subclassing in the future.
    public final OptionalInt tasks;
    public final OptionalInt cycleTime;
    public final OptionalInt stations;
    public final OptionalInt equipments;
    public final Map<Integer,Integer> taskTimes;
    public final Map<Integer,Collection<Integer>> taskDependencies;
    public final Map<Integer,Collection<Integer>> taskEquipment;
    // Extensions
    public final Map<Integer,Collection<Integer>> stationEquipment;
    public final Map<Integer,Integer> pinnedTasks;
    public final Map<Integer,Integer> pinnedEquipment;
    public final Map<Integer,Integer> equipmentCounts; // instances per type
    public final Map<Integer,Integer> equipmentPrices;
    public final Map<Integer,Integer> equipmentMoveCosts;
    public final OptionalInt problemType;
    // Extra extensions
    public final Map<String,Object> properties;

    // TODO: Do we really need all of these constructors?

    private AlbInstance(
            int tasks,
            OptionalInt cycleTime,
            OptionalInt stations,
            OptionalInt equipments,
            Map<Integer,Integer> taskTimes,
            Map<Integer,Collection<Integer>> taskDependencies,
            Map<Integer,Collection<Integer>> taskEquipment,
            Map<Integer,Collection<Integer>> stationEquipment
        ) {
        this(OptionalInt.of(tasks),
             cycleTime,
             stations,
             equipments,
             taskTimes,
             taskDependencies,
             taskEquipment,
             stationEquipment);
    }

    private AlbInstance(
            OptionalInt tasks,
            OptionalInt cycleTime,
            OptionalInt stations,
            OptionalInt equipments,
            Map<Integer,Integer> taskTimes,
            Map<Integer,Collection<Integer>> taskDependencies,
            Map<Integer,Collection<Integer>> taskEquipment,
            Map<Integer,Collection<Integer>> stationEquipment
        ) {
        this(tasks,
             cycleTime,
             stations,
             equipments,
             taskTimes,
             taskDependencies,
             taskEquipment,
             stationEquipment,
             Map.of(),
             Map.of(),
             Map.of(),
             Map.of(),
             Map.of(),
             OptionalInt.empty(),
             Map.of());
    }

    private AlbInstance(
            OptionalInt tasks,
            OptionalInt cycleTime,
            OptionalInt stations,
            OptionalInt equipments,
            Map<Integer,Integer> taskTimes,
            Map<Integer,Collection<Integer>> taskDependencies,
            Map<Integer,Collection<Integer>> taskEquipment,
            Map<Integer,Collection<Integer>> stationEquipment,
            Map<Integer,Integer> pinnedTasks,
            Map<Integer,Integer> pinnedEquipment,
            Map<Integer,Integer> equipmentCounts,
            Map<Integer,Integer> equipmentPrices,
            Map<Integer,Integer> equipmentMoveCosts,
            OptionalInt problemType,
            Map<String,Object> properties
        ) {
        this.tasks = tasks;
        this.cycleTime = cycleTime;
        this.stations = stations;
        this.equipments = equipments;
        this.taskTimes = Map.copyOf(taskTimes);
        this.taskDependencies = Map.copyOf(taskDependencies);
        this.taskEquipment = Map.copyOf(taskEquipment);
        this.stationEquipment = Map.copyOf(stationEquipment);
        this.pinnedTasks = pinnedTasks;
        this.pinnedEquipment = pinnedEquipment;
        this.equipmentCounts = equipmentCounts;
        this.equipmentPrices = equipmentPrices;
        this.equipmentMoveCosts = equipmentMoveCosts;
        this.problemType = problemType;
        this.properties = properties;
    }

    private AlbInstance() {
        this(OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    private AlbInstance(int tasks, Map<Integer,Integer> taskTimes) {
        this(tasks, OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), taskTimes, Map.of(), Map.of(), Map.of());
    }

    public static AlbInstance albInstance() {
        return new AlbInstance();
    }


    /**
     * Check whether the instance is valid -- intended for use with {@code assert}.
     * Note that this method <strong>NEVER</strong> returns false. If validation
     * fails, the validator will throw some exception. The point of returning
     * a boolean is that it makes it easy to make validation be conditioned
     * on assertions being enabled. Example use:
     * <pre>
     * // ... create/modify instance ...
     * assert instance.validOrFail(); // only checked if assertions enabled
     * // ... use instance ...
     * </pre>
     * @return true if valid, else throws exception
     * @see #validatedOrFail()
     * @see Validator
     */
    public boolean validOrFail() {
        return Validator.validateStrict(this);
    }

    /**
     * Force validation and then return the instance.
     * Makes it convenient to perform validation as a last step (or even an
     * intermediate step) in a chain of modifications. Example use:
     * <pre>
     * return instance
     *         .cycleTime(123)
     *         .stations(20)
     *         .validatedOrFail();
     * </pre>
     * @see #validOrFail()
     * @see Validator
     */
    public AlbInstance validatedOrFail() {
        Validator.validateStrict(this);
        return this;
    }

    public boolean isZeroBased() {
        return minTaskId() == 0;
    }

    public boolean isOneBased() {
        return minTaskId() == 1;
    }

    private int minTaskId() {
        // Make a set of all the task IDs.
        final Set<Integer> taskNumbers = taskNumbers(
                taskTimes, taskDependencies, taskEquipment);
        // Now examine the set.
        return taskNumbers.stream().min(Integer::compare).get();
    }

    /**
     * Collect all task numbers (wherever they are mentioned) into a set.
     */
    private static Set<Integer> taskNumbers(
            final Map<Integer,Integer> taskTimes,
            final Map<Integer,Collection<Integer>> taskDependencies,
            final Map<Integer,Collection<Integer>> taskEquipment
    ) {
        final Set<Integer> numbers = new HashSet<>(taskTimes.size());
        // Put all known task numbers into the set.
        numbers.addAll(taskTimes.keySet());
        numbers.addAll(taskDependencies.keySet());
        taskDependencies
            .values()
            .forEach(numbers::addAll);
        numbers.addAll(taskEquipment.keySet());
        return numbers;
    }

    /**
     * Collect all equipment numbers (wherever they are mentioned) into a set.
     */
    private static Set<Integer> equipNumbers(
            final Map<Integer,Collection<Integer>> taskEquipment,
            final Map<Integer,Collection<Integer>> stationEquipment
    ) {
        final Set<Integer> numbers = new HashSet<>(taskEquipment.size());
        // Put all known equipment numbers into the set.
        taskEquipment
            .values()
            .forEach(numbers::addAll);
        stationEquipment
            .values()
            .forEach(numbers::addAll);
        return numbers;
    }

    /**
     * Collect all equipment numbers (wherever they are mentioned) into a set.
     */
    private static Set<Integer> stationNumbers(
            final Map<Integer,Collection<Integer>> stationEquipment
    ) {
        return stationEquipment.keySet();
    }

    public static AlbInstance albInstance(int tasks, Map<Integer, Integer> taskTimes) {
        return new AlbInstance(tasks, taskTimes);
    }

    public AlbInstance tasks(int tasks) {
        return tasks(OptionalInt.of(tasks));
    }

    public AlbInstance tasks(OptionalInt tasks) {
        return new AlbInstance(tasks, cycleTime, stations, equipments, taskTimes, taskDependencies, taskEquipment, stationEquipment);
    }

    public AlbInstance cycleTime(int cycleTime) {
        return cycleTime(OptionalInt.of(cycleTime));
    }

    public AlbInstance cycleTime(OptionalInt cycleTime) {
        return new AlbInstance(tasks, cycleTime, stations, equipments, taskTimes, taskDependencies, taskEquipment, stationEquipment);
    }

    public AlbInstance stations(int stations) {
        return stations(OptionalInt.of(stations));
    }

    public AlbInstance stations(OptionalInt stations) {
        return new AlbInstance(tasks, cycleTime, stations, equipments, taskTimes, taskDependencies, taskEquipment, stationEquipment);
    }

    public AlbInstance equipmentTypes(int equipments) {
        return equipmentTypes(OptionalInt.of(equipments));
    }

    public AlbInstance equipmentTypes(OptionalInt equipments) {
        return new AlbInstance(tasks, cycleTime, stations, equipments, taskTimes, taskDependencies, taskEquipment, stationEquipment);
    }

    public AlbInstance taskTimes(Map<Integer,Integer> taskTimes) {
        return new AlbInstance(tasks, cycleTime, stations, equipments, taskTimes, taskDependencies, taskEquipment, stationEquipment);
    }

    /**
     * Specify how tasks depend on other tasks.
     * The keys are task IDs. The values are the collections of IDs of tasks
     * that the key task depends on, i.e., the tasks that must precede the key
     * task.
     * @param taskDependencies map tasks to the tasks they depend on
     */
    public AlbInstance taskDependencies(Map<Integer,Collection<Integer>> taskDependencies) {
        return new AlbInstance(tasks, cycleTime, stations, equipments, taskTimes, taskDependencies, taskEquipment, stationEquipment);
    }

    public AlbInstance taskEquipment(Map<Integer,Collection<Integer>> taskEquipment) {
        return new AlbInstance(tasks, cycleTime, stations, equipments, taskTimes, taskDependencies, taskEquipment, stationEquipment);
    }

    /**
     * Specify the equippment that is available at stations.
     * The keys are station IDs. The values are the collections of equipment
     * types that are available at the key station.
     * @param stationEquipment map stations to equipment types
     */
    public AlbInstance stationEquipment(Map<Integer,Collection<Integer>> stationEquipment) {
        return new AlbInstance(tasks, cycleTime, stations, equipments, taskTimes, taskDependencies, taskEquipment, stationEquipment);
    }

    public int tasks() {
        return tasks.getAsInt();
    }

    public OptionalInt cycleTime() {
        return cycleTime;
    }

    public OptionalInt stations() {
        return stations;
    }

    public OptionalInt equipmentTypes() {
        return equipments;
    }

    public Map<Integer,Integer> taskTimes() {
        return taskTimes;
    }

    /**
     * Return the task dependencies.
     * @see #taskDependencies(Map)
     */
    public Map<Integer,Collection<Integer>> taskDependencies() {
        return taskDependencies;
    }

    public Map<Integer,Collection<Integer>> taskEquipment() {
        return taskEquipment;
    }

    public Map<Integer,Collection<Integer>> stationEquipment() {
        return stationEquipment;
    }

    // TODO: need to think through how this would be used
    @SuppressWarnings("unchecked")
    public <T> T prop(String key) {
        return (T) properties.get(key);
    }

    // XXX: Given that it's possible to check whether a conversion is needed or
    // not, making it a NOP if already in the desired indexing mode risks
    // masking bugs.
    // But making zeroBased() idempotent is also nice. Hmmm.
    // FIXME: technically just decrements the IDs, which doesn't necessarily
    // make them 0. (Although that IS true IF the instance is valid.)
    /**
     * Turn a 1-based instance to 0-based.
     */
    public AlbInstance zeroBased() {
        if (isZeroBased()) {
            return this;
        }
        final Map<Integer,Integer> taskTimes
            = decrementKeys(this.taskTimes);
        final Map<Integer,Collection<Integer>> taskDependencies
            = decrementKeys(decrementValues(this.taskDependencies));
        final Map<Integer,Collection<Integer>> taskEquipment
            = decrementKeys(decrementValues(this.taskEquipment));
        final Map<Integer,Collection<Integer>> stationEquipment
            = decrementKeys(decrementValues(this.stationEquipment));
        return new AlbInstance(
                tasks, cycleTime, stations, equipments,
                taskTimes, taskDependencies, taskEquipment, stationEquipment);
    }

    /**
     * Turn a 0-based instance to 1-based.
     */
    public AlbInstance oneBased() {
        if (isOneBased()) {
            return this;
        }
        throw new UnsupportedOperationException("Not implemented.");
    }

    private <T> Map<Integer,T> decrementKeys(Map<Integer,T> map) {
        return stream(map)
            .map(Pairs.map_1(dec()))
            .collect(pairsToMap());
    }

    private <T> Map<T,Collection<Integer>> decrementValues(Map<T,Collection<Integer>> map) {
        return map
            .keySet()
            .stream()
            .collect(
                    toMap(
                        key -> key,
                        key -> map.get(key)
                            .stream()
                            .map(dec())
                            .collect(toUnmodifiableSet())
            ));
    }


    @Override
    public int hashCode() {
        return Objects.hash(
                tasks, cycleTime, stations, equipments,
                taskTimes, taskDependencies, taskEquipment, stationEquipment);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AlbInstance that = (AlbInstance) obj;
        return Objects.equals(this.stations, that.stations)
            && Objects.equals(this.equipments, that.equipments)
            && Objects.equals(this.tasks, that.tasks)
            && Objects.equals(this.cycleTime, that.cycleTime)
            && Objects.equals(this.taskTimes, that.taskTimes)
            && Objects.equals(this.taskDependencies, that.taskDependencies)
            && Objects.equals(this.taskEquipment, that.taskEquipment)
            && Objects.equals(this.stationEquipment, that.stationEquipment);
    }

}
