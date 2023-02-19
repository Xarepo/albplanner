package se.ltu.alb.equip.test;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import ch.rfin.util.Pair;
import ch.rfin.util.Pairs;
import ch.rfin.util.Rng;
import ch.rfin.alb.AlbInstance;
import se.ltu.alb.salbp.algo.RandomType1;
import se.ltu.alb.equip.test.ProblemGenerator.Parameters;
import static ch.rfin.util.Functions.ceil;
import static ch.rfin.util.Functions.min;
import static ch.rfin.util.Functions.max;
import static ch.rfin.util.Streams.keys;
import static ch.rfin.util.Streams.values;
import static ch.rfin.util.Misc.get;
import static ch.rfin.util.Pairs.pairsToMap;
import static ch.rfin.util.Exceptions.assertion;

// TODO: should probably make an AbstractGenerator or GeneratorTemplate
// using the template pattern.

/**
 * An EqALB Problem Generator/Converter that implements a station-centric
 * algorithm.
 * It explicitly controls the number of stations (as a [min,max] range or
 * a fraction) that have equipment, and the number of equipment types they
 * have (as a [min,max] range).
 * While Parameters can be prepared externally and passed in, this class
 * also offers methods for setting the specific parameters it expects.
 * @author Christoffer Fink
 */
public class GeneratorType2A extends ProblemGenerator {

    private GeneratorType2A(Parameters param) {
        super(param);
    }

    /** Make a generator with the provided parameters. */
    public static GeneratorType2A of(Parameters param) {
        return new GeneratorType2A(param);
    }

    /**
     * Make a generator that uses the default parameters and provided RNG seed.
     * @see #defaultParameters(long)
     */
    public static GeneratorType2A ofDefault(long seed) {
        return of(defaultParameters(seed));
    }

    /**
     * Reasonable default parameters for generating type 2 problems with this
     * generator.
     * Half the stations have [1,3] equipment types.
     * Half the equipment types are unique (half are reused).
     * Half the tasks at a station depend on available equipment.
     * Each such task depends on half of the available equipment.
     */
    public static Parameters defaultParameters(long seed) {
        return ProblemGenerator.Parameters.of(seed)
            .problemType(2)
            .stationHasEquipment(0.5)   // Half the stations have equipment
            .minEquipmentPerStation(1)  // at least 1
            .maxEquipmentPerStation(3)  // at most 3.
            .taskNeedsEquipment(0.5)    // Half the tasks depend on...
            .taskNeedsEquipmentAtStation(0.5)   // ...half of the equipment.
            .fractionUnique(0.5);       // Half the equipment has unique type.
    }

    /**
     * Set the fraction of stations that should have some equipment.
     * <p>
     * This setting is mutually exclusive with
     * {@link #stationsWithEquipment(int, int)}
     */
    public GeneratorType2A stationsWithEquipment(double fraction) {
        param = param.stationHasEquipment(fraction);
        return this;
    }

    /**
     * Set the range of stations that should have equipment.
     * Use min = max to set a definite (deterministic) number.
     * <p>
     * This setting is mutually exclusive with
     * {@link #stationsWithEquipment(double)}
     */
    public GeneratorType2A stationsWithEquipment(int min, int max) {
        param = param
            .minStationsWithEquipment(min)
            .maxStationsWithEquipment(max);
        return this;
    }

    /**
     * Set the fraction of tasks (at an equipped station) that depends on
     * some of the equipment available at the station.
     * <em>Note that this applies to tasks at an equipped station, not to all
     * tasks in general.</em>
     * If set to 0.0, no tasks will be dependent on any equipment.
     * If set to 1.0, all tasks at an equipped station will depend on some of
     * the available equipment.
     * <p>
     * This setting is optional and will default to 1.0.
     */
    public GeneratorType2A tasksWithEquipment(double fraction) {
        param = param.taskNeedsEquipment(fraction);
        return this;
    }

    /**
     * Set the fraction of available equipment that a task should require.
     * <em>Note that this applies to tasks at an equipped station, not to all
     * tasks in general.</em>
     * If set to 0.0, the task will not depend on any equipment!
     * If set to 1.0, the task will depend on all available equipment.
     * <p>
     * This setting is mutually exclusive with
     * {@link #equipmentPerTask(int, int)}.
     */
    public GeneratorType2A equipmentPerTask(double fraction) {
        param = param.taskNeedsEquipmentAtStation(fraction);
        return this;
    }

    /**
     * Set the range of available equipment that a task should require.
     * <em>Note that this applies to tasks at an equipped station, not to all
     * tasks in general.</em>
     * <p>
     * This setting is mutually exclusive with
     * {@link #equipmentPerTask(double)}.
     */
    public GeneratorType2A equipmentPerTask(int min, int max) {
        param = param
            .minEquipmentPerTask(min)
            .maxEquipmentPerTask(max);
        return this;
    }

    /**
     * Set the fraction of equipment that shares the same type.
     * This controls the number of different equipment types.
     * If set to 0.0, equipment types are reused as much as possible.
     * So any two stations will always offer overlapping equipment.
     * (The number of types will equal the largest number of equipments
     * available at any station.)
     * If set to 1.0, every equipment will have a unique type.
     * Hence no two stations offer overlapping equipment.
     */
    public GeneratorType2A uniqueEquipmentTypes(double fraction) {
        param = param.fractionUnique(fraction);
        return this;
    }

    // Given any two of {equipment, equipment types, equipment per type},
    // the third can be derived.
    // Given `stations with equipment` and `equipment per station`,
    // `equipment` can be derived. However, that leaves `equipment types`
    // and `equipment per type` unspecified.
    // Given `stations with equipment` and `fraction unique`,
    // `equipment types` can be derived.

    /*
     * stations [ | | | ... | | ]
     * - Select the subset that should have equipment.
     * stationsToEquip [x| |x| ... |x| ] → [ | | ]
     * - Decide how many equipment types each should have
     * stationEquipCount [ | | ] → [2|1|3]
     *
     * [ | | | ... | | ] → [x| |x| ... |x| ] → [ | | ] → [2|1|3]
     *
     * [12 ]   [oo ]   [12 ]
     * [3  ] ← [o  ] → [1  ]
     * [456]   [ooo]   [123]
     * Middle: Stations with placeholders for equipment.
     * Left: Every equipment is of a unique type. (One extreme.)
     * Right: Stations share as many equipment types as possible. (This is
     * the other extreme.)
     */

    /**
     * Generate pure Type 2 problems with the station-centered method.
     * Needs
     * - stationHasEquipment XOR (min and/or max) stationsWithEquipment.
     * - equipmentPerStation (min/max both mandatory)
     * - taskNeedsEquipmentAtStation XOR (min and/or max) equipmentPerTask.
     * - fractionUnique
     *
     * Also (optionally) uses
     * - taskNeedsEquipment for controlling the fraction of tasks at an equipped
     *   station that should be made dependent on the available equipment.
     *   Defaults to 1.0 (all tasks at a station) if missing.
     *
     * stationHasEquipment or stationsWithEquipment control the number of
     * stations to equip. (stationHasEquipment is treated as a fraction.)
     * min/max stationsWithEquipment <strong>must not</strong> be greater than
     * the number of stations.
     * min defaults to 0 (assuming max is given).
     * max defaults to the number of stations (assuming min is given).
     *
     * taskNeedsEquipmentAtStation or min/max EquipmentPerTask control the
     * number of the available equipment to make each task depend on.  The
     * actual number is always capped to the available equipment.  In other
     * words, min/max EquipmentPerTask are suggestions that are overridden by
     * the actual number of equipment available at the corresponding station.
     *
     * fractionUnique controls the number of equipment types.
     *
     * - All equipment types are instantiated.
     *   (Which means they are equipped at one or more stations.)
     * - All equipment is pinned.
     *   (No equipment is assigned to a station but not pinned.)
     *   XXX: currently, AlbInstance cannot differentiate pinned/unpinned!
     */
    @Override   // FIXME don't need to override. Inherit from parent.
    public AlbInstance convert(AlbInstance original) {
        Map<Integer,Integer> solution = solve(original);
        assert solution.size() == original.tasks();
        assert solution.values().size() >= 1;
        return convert(original, solution);
    }

    public AlbInstance convert(AlbInstance original, Map<Integer,Integer> solution) {
        Parameters tweaked = validateAndTweak(param, original, solution);
        return convert(tweaked, original, solution);
    }

    private Parameters validateAndTweak(Parameters tweaked, AlbInstance original, Map<Integer,Integer> solution) {
        final var param = Parameters.of(this.param); // copy
        final int nrStations = (int) values(solution).distinct().count();
        // Validate parameters.
        assert param.valid();
        assert param.stationHasEquipment().isPresent()
            != (param.minStationsWithEquipment().isPresent()
                    || param.maxStationsWithEquipment().isPresent());   // XOR
        assert param.minEquipmentPerStation().isPresent();
        assert param.maxEquipmentPerStation().isPresent();
        // How many tasks at the station?
        //assert param.taskNeedsEquipment().isPresent(); // Use 1.0 if missing.
        // How many equipments at the station?
        assert param.taskNeedsEquipmentAtStation().isPresent()
            != (param.minEquipmentPerTask().isPresent()
                || param.maxEquipmentPerTask().isPresent());    // XOR
        /* TODO: Maybe...
         * min/max StationsWithEquipment are soft lower/upper limits
         * (making it larger than the number of stations does no harm).
         * Or maybe relax max but be strict on min?
         */
        param.minStationsWithEquipment()
            .ifPresent(n -> { assert n <= nrStations; });
        param.maxStationsWithEquipment()
            .ifPresent(n -> { assert n <= nrStations; });

        // Tweak params as necessary.
        if (param.stationHasEquipment().isPresent()) {
            final double frac = param.stationHasEquipment().getAsDouble();
            param.stationsWithEquipment(ceil(nrStations * frac));
        } else {
            final int n = nrStations;
            final int min = min(n, param.minStationsWithEquipment().orElse(n));
            param.minStationsWithEquipment(min);
            final int max = min(n, param.maxStationsWithEquipment().orElse(n));
            param.maxStationsWithEquipment(max);
        }
        return param;
    }

    /**
     * Convert the problem using validated and possibly tweaked parameters.
     */
    private AlbInstance convert(Parameters tweaked, AlbInstance original, Map<Integer,Integer> solution) {
        final int nrStations = (int) values(solution).distinct().count();
        // Map stations to the number of types to equip them with.
        final var stationEquipCounts = stationEquipmentCounts(tweaked, solution);
        // Map stations to collections of equipment types.
        final var stationEquipment = equipStations(stationEquipCounts);
        // Map tasks to collections of equipment types.
        final var taskEquipment = equipTasks(stationEquipment, solution);

        // Make sure all tasks declare their equipment dependencies!
        mapToEmpty(taskEquipment, original.tasks());
        // And stations declare equipment.
        mapToEmpty(stationEquipment, nrStations);

        // Remap equipment type numbers to be consecutive, starting with 0.
        var idMapper = consecutiveMapper(stationEquipment);
        var finalStationEquipment = remapIds(stationEquipment, idMapper);
        var finalTaskEquipment = remapIds(taskEquipment, idMapper);

        final int equipmentTypeCount = (int) values(finalStationEquipment)
            .flatMap(Collection::stream).distinct().count();

        return original
            .stations(nrStations)
            .equipmentTypes(equipmentTypeCount)
            .stationEquipment(finalStationEquipment)
            .taskEquipment(finalTaskEquipment)
            .validatedOrFail();
    }

    /** Make tasks depend on equipment. */
    private Map<Integer,Collection<Integer>> equipTasks(Map<Integer,Collection<Integer>> stationEquipment, Map<Integer,Integer> solution) {
        final var taskEquipment = new HashMap<Integer, Collection<Integer>>();
        Map<Integer,Set<Integer>> assignments = Pairs.stream(solution)
            .collect(groupingBy(Pair::get_2, mapping(Pair::get_1, toSet())));
        // Loop through each station that has equipment.
        // Choose some subset of the tasks.
        // Make each of those tasks depend on a subset of the equipment at the
        // station.
        for (final int station : stationEquipment.keySet()) {
            final Set<Integer> tasks = assignments.get(station);
            final Collection<Integer> types = stationEquipment.get(station);
            final Set<Integer> taskSubset = taskSubset(param, tasks);
            for (final var task : taskSubset) {
                taskEquipment.put(task, equipmentSubset(param, types));
            }
        }
        return taskEquipment;
    }

    /**
     * Make a map that maps stations to the number of equipment types they
     * should be equipped with.
     */
    private Map<Integer,Integer> stationEquipmentCounts(Parameters tweaked, Map<Integer,Integer> solution) {
        final int n = tweaked.stationsWithEquipment();
        // These stations will be equipped with some number of equipment.
        final Set<Integer> stationsToEquip
            = rng.randomSubset(Set.copyOf(solution.values()), n);
        assert stationsToEquip.size() == n;
        // Decide how many equipment types to assign to each of the stations.
        return stationsToEquip
            .stream()
            .map(station -> Pair.of(station, tweaked.equipmentPerStation()))
            .peek(p -> { assertion(p._2 > 0, "station %d was 0", p._1); })
            .collect(pairsToMap());
    }

    /**
     * Returns a map that maps station numbers to equipment types.
     * Given a map that maps station numbers to the number of equipment types
     * to equip that station with, return a map that maps station numbers to
     * collections of equipment types.
     * <p>
     * Note that this is a key function in the generation/conversion algorithm.
     * It is not a general utility method.
     * @param counts station number ↦ equipment count
     */
    private Map<Integer,Collection<Integer>> equipStations(Map<Integer,Integer> counts) {
        // This version works. It has the advantage that we never run out of
        // types. The disadvantage is that we may use too many types. We
        // may also use too many instances of the same type. This can be
        // fixed by filtering on use count, but then of course we'll use
        // even more types.
        // This solution might be good enough, or we might want to investigate
        // the idea above.
        final Map<Integer,Collection<Integer>> stationEquipment = new HashMap<>();
        PriorityQueue<Pair<Integer,Integer>> queue
            = new PriorityQueue<>(secondReversed());
        Pairs.stream(counts).forEach(queue::add);
        int nextUnusedType = 0;
        Set<Integer> usedTypes = new HashSet<>();
        while (!queue.isEmpty()) {
            final Pair<Integer,Integer> stationCount = queue.remove();
            final int station = stationCount._1;
            final int count = stationCount._2;
            // The types equipped at this stations.
            final Collection<Integer> equipped = new HashSet<>();
            stationEquipment.put(station, equipped);
            // TODO: maybe filter out the types that have been used too often?
            Collection<Integer> candidates = new ArrayList<>(usedTypes);
            final double prob = get(param.fractionUnique());
            for (int i = 0; i < count; i++) {
                int type = candidates.isEmpty() || rng.nextBoolean(prob)
                    ? nextUnusedType++
                    : rng.removeRandomElement(candidates);
                equipped.add(type);
            }
            usedTypes.addAll(equipped);
        }
        return stationEquipment;
    }

    /**
     * Make a function that remaps the numbers in the value collections of the
     * map such that they are consecutive, starting from 0.
     * One could think of this as compacting the numbers toward 0.
     * The numbers in the collections are presumably equipment types.
     */
    private Function<Integer,Integer> consecutiveMapper(Map<Integer,Collection<Integer>> map) {
        Map<Integer,Integer> numMap = new HashMap<>();
        Set<Integer> distinct = values(map)
            .flatMap(Collection::stream)
            .collect(toSet());
        int id = 0;
        for (final var x : distinct) {
            numMap.put(x, id++);
        }
        return numMap::get;
    }

    /**
     * Modifies the collections of IDs (the values in the map) according to the
     * mapping function.
     * Note that the keys are left untouched.
     * The map is assumed to map station numbers to equipment types.
     */
    private Map<Integer, Collection<Integer>> remapIds(Map<Integer, Collection<Integer>> map, Function<Integer,Integer> mapper) {
        return keys(map)
            .collect(toMap(key -> key, key ->
                        map.get(key).stream()
                        .map(mapper).collect(toSet())));
    }

    /**
     * Make sure very number in [0,n[ maps to a Collection.
     * Existing collections are left untouched.
     */
    private void mapToEmpty(Map<Integer,Collection<Integer>> map, int n) {
        for (int i = 0; i < n; i++) {
            if (!map.containsKey(i)) {
                map.put(i, Set.of());
            }
        }
    }

    /**
     * Returns a random task subset of size based on taskNeedsEquipment,
     * defaulting to the whole set if that parameter is not set.
     */
    private Set<Integer> taskSubset(Parameters param, Set<Integer> set) {
        if (!param.taskNeedsEquipment().isPresent()) {
            return set;
        }
        final double frac = get(param.taskNeedsEquipment());
        final int size = ceil(set.size() * frac);
        return param.rng().randomSubset(set, size);
    }

    /**
     * Returns a random equipment subset of size based on
     * taskNeedsEquipmentAtStation or min/max equipmentPerTask, limited
     * to the size of the set.
     */
    private Set<Integer> equipmentSubset(Parameters param, Collection<Integer> set) {
        final int size = equipmentSubsetSize(param, set.size());
        return param.rng().randomSubset(set, size);
    }

    // Technically we don't need to be so careful with the size limit since
    // rng will simply cap the size of the subset at the size of the set.
    /**
     * Returns a random equipment count based on either
     * taskNeedsEquipmentAtStation or min/max equipmentPerTask, limiting
     * the min/max based on the maxSize limit.
     */
    private int equipmentSubsetSize(Parameters param, int maxSize) {
        if (param.taskNeedsEquipmentAtStation().isPresent()) {
            return ceil(maxSize * get(param.taskNeedsEquipmentAtStation()));
        }
        final int min = min(maxSize, get(param.minEquipmentPerTask(), maxSize));
        final int max = min(maxSize, get(param.maxEquipmentPerTask(), maxSize));
        return param.generate(min, max);
    }

    /**
     * This method is necessary because of how ridiculously terrible Java's
     * type system is.
     * Can't do
     * <pre>
     * .sorted(Comparator.comparing(Pair::get_2).reversed())
     * </pre>
     * Can't do
     * <pre>
     * Comparator<Pair<Integer,Integer>> comp = Comparator
     *     .comparing(Pair::get_2)
     *     .reversed();
     * // ...
     * .sorted(comp)
     * </pre>
     * CAN do
     * <pre>
     * Comparator<Pair<Integer,Integer>> comp = Comparator.comparing(Pair::get_2);
     * // ...
     * .sorted(comp.reversed())
     * </pre>
     */
    private static Comparator<Pair<Integer,Integer>> secondReversed() {
        //return Comparator.comparing(Pair::get_2).reversed();  // NOPE
        // Holy shit Java type system sucks! It's incredible! Just amazing.
        Comparator<Pair<Integer,Integer>> tmp = Comparator.comparing(Pair::get_2);
        return tmp.reversed();
    }

}
