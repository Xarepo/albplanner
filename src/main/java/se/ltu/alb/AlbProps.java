package se.ltu.alb;

import java.util.Collection;
import java.util.Map;
import static java.util.function.Predicate.not;

import ch.rfin.alb.AlbInstance;
import static ch.rfin.util.Streams.values;

/**
 * Compute various properties and transformations of ALB instances.
 * @author Christoffer Fink
 */
public class AlbProps {

    /** Return the number of different equipment types stations are equipped with. */
    public static int countStationEquipmentTypes(AlbInstance eqalb) {
        return (int) values(eqalb.stationEquipment())
            .flatMap(Collection::stream)
            .distinct()
            .count();
    }

    /** Count the number of stations that have equipment. */
    public static int countEquippedStations(AlbInstance eqalb) {
        return (int) values(eqalb.stationEquipment())
            .filter(not(Collection::isEmpty))
            .count();
    }

    /** Count the total number of equipment instances at all stations. */
    public static int countEquipmentAtStations(AlbInstance eqalb) {
        return values(eqalb.stationEquipment())
            .mapToInt(Collection::size)
            .sum();
    }

    /** Return the largest number of equipment at any single station. */
    public static int maxEquipmentAtStation(AlbInstance eqalb) {
        return values(eqalb.stationEquipment())
            .mapToInt(Collection::size)
            .max().orElse(0);
    }

    /**
     * Return the smallest number of equipment at any single station
     * <strong>with equipment</strong>, or 0 if no station has equipment.
     */
    public static int minEquipmentAtStation(AlbInstance eqalb) {
        return values(eqalb.stationEquipment())
            .mapToInt(Collection::size)
            .min().orElse(0);
    }

    /** Return the number of different element types tasks depend on. */
    public static int countTaskEquipmentTypes(AlbInstance eqalb) {
        return (int) values(eqalb.taskEquipment())
            .flatMap(Collection::stream)
            .distinct()
            .count();
    }

    /** Count the number of tasks that require equipment. */
    public static int countEquipmentDependentTasks(AlbInstance eqalb) {
        return (int) values(eqalb.taskEquipment())
            .filter(not(Collection::isEmpty))
            .count();
    }

    /** Count the total number of equipment instances tasks depend on. */
    public static int countEquipmentRequiredByTasks(AlbInstance eqalb) {
        return values(eqalb.taskEquipment())
            .mapToInt(Collection::size)
            .sum();
    }

    /** Return the largest number of equipment any task requires. */
    public static int maxEquipmentRequiredByTask(AlbInstance eqalb) {
        return values(eqalb.taskEquipment())
            .mapToInt(Collection::size)
            .max().orElse(0);
    }

    /**
     * Return the smallest number of equipment required by any task
     * <strong>that has equipment requirements</strong>, or 0 if no task
     * has equipment requirements.
     */
    public static int minEquipmentRequiredByTask(AlbInstance eqalb) {
        return values(eqalb.taskEquipment())
            .mapToInt(Collection::size)
            .min().orElse(0);
    }


    /**
     * Map equipment types to the collections of stations that are equipped
     * with them.
     */
    public static Map<Integer,Collection<Integer>> stationsOfferingEquipment(AlbInstance eqalb) {
        throw new UnsupportedOperationException("FIXME: Not implemented.");
    }

    /**
     * Map equipment types to the collections of tasks that require them.
     */
    public static Map<Integer,Collection<Integer>> tasksRequiringEquipment(AlbInstance eqalb) {
        throw new UnsupportedOperationException("FIXME: Not implemented.");
    }

}
