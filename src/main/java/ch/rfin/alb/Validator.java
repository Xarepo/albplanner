package ch.rfin.alb;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.OptionalInt;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static ch.rfin.util.Functions.dec;
import ch.rfin.util.Pairs;
import static ch.rfin.util.Pairs.stream;
import static ch.rfin.util.Pairs.pairsToMap;

/**
 * Checks an AlbInstance for consistency.
 * Failed validation throws exceptions.
 * Future versions will likely have settings.
 * @author Christoffer Fink
 */
public class Validator {

    public static boolean validateStrict(AlbInstance alb) {
        validate(alb.tasks,
             alb.cycleTime,
             alb.stations,
             alb.equipments,
             alb.taskTimes,
             alb.taskDependencies,
             alb.taskEquipment,
             alb.stationEquipment);
        return true;
    }

    // Return strict validator. For now there is only one mode.
    public static Validator strict() {
        return new Validator();
    }

    private static boolean validate(
            OptionalInt tasks,
            OptionalInt cycleTime,
            OptionalInt stations,
            OptionalInt equipments,
            Map<Integer,Integer> taskTimes,
            Map<Integer,Collection<Integer>> taskDependencies,
            Map<Integer,Collection<Integer>> taskEquipment,
            Map<Integer,Collection<Integer>> stationEquipment
        ) {
        return validate(tasks.getAsInt(),
             cycleTime,
             stations,
             equipments,
             taskTimes,
             taskDependencies,
             taskEquipment,
             stationEquipment);
    }

    private static boolean validate(
            int tasks,
            OptionalInt cycleTime,
            OptionalInt stations,
            OptionalInt equipments,
            Map<Integer,Integer> taskTimes,
            Map<Integer,Collection<Integer>> taskDependencies,
            Map<Integer,Collection<Integer>> taskEquipment,
            Map<Integer,Collection<Integer>> stationEquipment
        ) {
        if (tasks <= 0) {
            final String msg = String
                .format("Number of tasks must be positive, was %d", tasks);
            throw new IllegalArgumentException(msg);
        }
        // A null optional!? You should be ashamed of yourself!
        checkNonNull(cycleTime, "cycle time");
        checkNonNull(stations, "stations");
        checkNonNull(equipments, "equipments");
        checkNonNull(taskTimes, "task times");
        checkNonNull(taskDependencies, "task dependencies");
        checkNonNull(taskEquipment, "task equipment");
        checkPositive(cycleTime, "cycle time");
        checkPositive(stations, "number of stations");
        checkPositive(equipments, "number of equipments"); // possibly allow 0
        if (!taskTimes.isEmpty()) {
            assert tasks == taskTimes.size()
                : "must have a task time for each task";
        }
        if (!taskDependencies.isEmpty()) {
            assert tasks == taskDependencies.size()
                : "must specify task dependencies for each task";
        }
        if (!taskEquipment.isEmpty()) {
            assert tasks == taskEquipment.size()
                : "must specify equipment dependencies for each task";
        }
        if (!stationEquipment.isEmpty() && stations.isPresent()) {
            assert stations.getAsInt() == stationEquipment.size()
                : "must specify equipment available at each station";
        }
        final Set<Integer> taskNumbers = taskNumbers(
                taskTimes, taskDependencies, taskEquipment);
        // Now examine the set.
        final int uniqueTasks = (int) taskNumbers.stream().distinct().count();
        final int minTask = taskNumbers.stream().min(Integer::compare).get();
        final int maxTask = taskNumbers.stream().max(Integer::compare).get();
        checkTaskNumbers(tasks, uniqueTasks, minTask, maxTask);
        final boolean zeroBased = minTask == 0;
        // TODO: also check the integrity of the
        // - task times
        //    - keys (nodes) must match the number of tasks
        //    - values must be subsets of the keys
        //    - the union of all values must equal the keys
        // - dependencies
        //    - keys (nodes) must match the number of tasks
        // - equipment dependencies
        //    - keys (nodes) must match the number of tasks
        // Of course, the keys in all these maps bust be the same set, so
        // that they all agree on what the set of tasks is.
        checkEquipNumbers(zeroBased, equipments, taskEquipment, stationEquipment);
        checkStationNumbers(zeroBased, stations, stationEquipment);
        return true;
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
     * Enforce restrictions on task numbers.
     * This catches lots of errors, but it doesn't necessarily give a lot of
     * information about where the error is.
     */
    private static boolean checkTaskNumbers(
            final int tasks, final int unique, final int min, final int max
    ) {
        if (unique != tasks) {
            String msg = String
                .format("There are %d tasks but %d task numbers",
                        tasks, unique);
            throw new IllegalArgumentException(msg);
        }
        if (min == 0) {
            assert max == tasks - 1;  // FIXME: throw exception
        } else if (min == 1) {  // FIXME: throw exception
            assert max == tasks;
        } else {
            String msg = String
                .format("Lowest task number must be 0 or 1, but it was",
                        min);
            throw new IllegalArgumentException(msg);
        }
        return min == 0;
    }

    private static void checkEquipNumbers(
            final boolean zeroBased,
            final OptionalInt equipments,
            final Map<Integer,Collection<Integer>> taskEquipment,
            final Map<Integer,Collection<Integer>> stationEquipment
    ) {
        final Set<Integer> numbers = equipNumbers(taskEquipment, stationEquipment);
        if (equipments.isEmpty()) {
            assert numbers.isEmpty();
            return;
        }
        if (numbers.isEmpty()) {
            return;
        }
        final int equip = equipments.getAsInt();
        final int unique = (int) numbers.stream().distinct().count();
        final int min = numbers.stream().min(Integer::compare).get();
        final int max = numbers.stream().max(Integer::compare).get();
        checkEquipNumbers(zeroBased, equip, unique, min, max);
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
     * Enforce restrictions on equipment numbers.
     * This catches lots of errors, but it doesn't necessarily give a lot of
     * information about where the error is.
     */
    private static void checkEquipNumbers(
            final boolean zeroBased,
            final int equip, final int unique, final int min, final int max
    ) {
        if (unique != equip) {
            String msg = String
                .format("There are %d equipments but %d equipment numbers",
                        equip, unique);
            throw new IllegalArgumentException(msg);
        }
        if (min == 0) {
            if (max != equip - 1) {
                String msg = String
                    .format("min = %d, equipments = %d. So max (%d) should be %d",
                            min, equip, max, (equip - 1));
                throw new IllegalArgumentException(msg);
            }
            assert max == equip - 1;  // FIXME: throw exception
        } else if (min == 1) {  // FIXME: throw exception
            assert max == equip;
        } else {
            String msg = String
                .format("Lowest equipment number must be 0 or 1, but it was", min);
            throw new IllegalArgumentException(msg);
        }
        assert zeroBased == (min == 0);  // indexing mode consistency
    }

    private static void checkStationNumbers(
            final boolean zeroBased,
            final OptionalInt stations,
            final Map<Integer,Collection<Integer>> stationEquipment
    ) {
        final Set<Integer> numbers = stationNumbers(stationEquipment);
        if (stations.isEmpty()) {
            //assert numbers.isEmpty(); // FIXME: should this be here!? Not sure!
            return;
        }
        if (numbers.isEmpty()) {
            return;
        }
        final int stat = stations.getAsInt();
        final int unique = (int) numbers.stream().distinct().count();
        final int min = numbers.stream().min(Integer::compare).get();
        final int max = numbers.stream().max(Integer::compare).get();
        checkStationNumbers(zeroBased, stat, unique, min, max);
    }

    /**
     * Collect all equipment numbers (wherever they are mentioned) into a set.
     */
    private static Set<Integer> stationNumbers(
            final Map<Integer,Collection<Integer>> stationEquipment
    ) {
        return stationEquipment.keySet();
    }

    /**
     * Enforce restrictions on station numbers.
     * This catches lots of errors, but it doesn't necessarily give a lot of
     * information about where the error is.
     */
    private static void checkStationNumbers(
            final boolean zeroBased,
            final int stations, final int unique, final int min, final int max
    ) {
        if (unique != stations) {
            String msg = String
                .format("There are %d stations but %d stations numbers",
                        stations, unique);
            throw new IllegalArgumentException(msg);
        }
        if (min == 0) {
            assert max == stations - 1;  // FIXME: throw exception
        } else if (min == 1) {  // FIXME: throw exception
            assert max == stations;
        } else {
            String msg = String
                .format("Lowest station number must be 0 or 1, but it was", min);
            throw new IllegalArgumentException(msg);
        }
        assert zeroBased == (min == 0);  // indexing mode consistency
    }


    private static void checkNonNull(OptionalInt param, String name) {
        if (param != null) {
            return;
        }
        final String msg = String
            .format("%s was null. Use an empty optional instead of null!", name);
        throw new IllegalArgumentException(msg);
    }

    private static void checkNonNull(Map<?,?> param, String name) {
        if (param != null) {
            return;
        }
        final String msg = String
            .format("%s was null. Use an empty map instead of null!", name);
        throw new IllegalArgumentException(msg);
    }

    private static void checkPositive(OptionalInt param, String name) {
        if (param.isEmpty()) {
            return;
        }
        final int val = param.getAsInt();
        if (param.getAsInt() > 0) {
            return;
        }
        final String msg = String
            .format("%s (%d) must be positive", name, val);
        throw new IllegalArgumentException(msg);
    }

}
