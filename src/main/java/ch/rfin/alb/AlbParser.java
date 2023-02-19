package ch.rfin.alb;

import java.util.*;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.*;
import static java.util.function.Predicate.not;
import ch.rfin.util.Pair;
import ch.rfin.util.Pairs;
import static ch.rfin.util.Pairs.pairsToMap;
import static java.util.stream.Collectors.toList;
import static ch.rfin.alb.AlbInstance.albInstance;

/**
 * Parser for the ALB format.
 * @author Christoffer Fink
 */
public interface AlbParser<P> {

    public default P parseString(String instance) {
        return parse(instance.lines());
    }

    public default P parseFile(String fileName) throws IOException {
        return parse(Paths.get(fileName));
    }

    public default P parse(Path file) throws IOException {
        return parse(Files.lines(file));
    }

    public P parse(Stream<String> lines);

}

// TODO: extensions:
// - station equipment
//    (Let type 2 also apply to station equipment)
// - models/products/work piece (lists the tasks that belong to it).
// - represent a solution
// - workpiece movement speed
// - pinned equipment
// - pinning tasks

// TODO:
// - Apparently comments can follow the tag. So we need to handle those.
// - Should be able to switch between dependency/precedence.
// - Should be able to switch between 0-based or 1-based numbering/indexing.
// - Settings for
//   - Strict parsing
//   - Validation (automatically validate the parsed instance)
//   - Whether to inject empty dependencies
// - Unparsed sections
/**
 * Parser for the ALB format.
 * This early version has some shortcomings relative to the format definition.
 * For now, the focus has been to enable importing the actual ALB files in the
 * standard data set, rater than arbitrary ALB files that adhere to the format.
 * <ul>
 * <li>Does not support comments after tags.</li>
 * <li>Only supports the following tags:
 *   <ul>
 *   <li>number of tasks</li>
 *   <li>cycle time</li>
 *   <li>number of stations</li>
 *   <li>task times</li>
 *   <li>precedence relations</li>
 *   <li>end</li>
 *   </ul>
 * </ul>
 * @author Christoffer Fink
 * @version 0.1.0
 */
class DefaultAlbParser implements AlbParser<AlbInstance> {
    // Standard tags.
    public static final String N_TASKS_TAG = "<number of tasks>";   // Mandatory
    public static final String CYCLE_TIME_TAG = "<cycle time>";
    public static final String N_STATIONS_TAG = "<number of stations>";
    public static final String TASK_TIMES_TAG = "<task times>";     // Mandatory
    public static final String PRECEDENCE_TAG = "<precedence relations>";
    public static final String EQUIPMENTS_TAG = "<number of equipments>";
    public static final String END_TAG = "<end>";
    // Extensions.
    public static final String STATION_EQUIP_TAG = "<station equipments>"; // ext
    // TODO: add remaining tags
    public static final Set<String> TAGS = Set.of(
        N_TASKS_TAG, CYCLE_TIME_TAG, N_STATIONS_TAG, TASK_TIMES_TAG,
        PRECEDENCE_TAG,
        STATION_EQUIP_TAG,
        END_TAG
    );

    @Override
    public AlbInstance parseString(String instance) {
        return parse(instance.lines());
    }

    @Override
    public AlbInstance parseFile(String fileName) throws IOException {
        return parse(Paths.get(fileName));
    }

    @Override
    public AlbInstance parse(Path file) throws IOException {
        return parse(Files.lines(file));
    }

    @Override
    public AlbInstance parse(Stream<String> lines) {
        return parse(parseSections(lines));
    }

    private static AlbInstance parse(Map<String,List<String>> sections) {
        int tasks = Integer.parseInt(sections.get(N_TASKS_TAG).get(0));
        final OptionalInt cycleTime = getCycleTime(sections);
        final var deps = parseDependencies(sections.get(PRECEDENCE_TAG), tasks);
        final var times = parseTaskTimes(sections.get(TASK_TIMES_TAG));
        final OptionalInt stations = getStations(sections);
        var alb = albInstance(tasks, times)
            .taskDependencies(deps)
            .cycleTime(cycleTime);
        if (stations.isPresent()) {
            alb = alb.stations(stations);
        }
        return alb;
    }

    /**
     * Parses the raw sections as strings.
     * Returns a Map that maps the section tag to the list of Strings that
     * belong to that tag.
     */
    public static Map<String,List<String>> parseSections(Stream<String> lines) {
        return parseSections(lines.filter(not(String::isEmpty)).iterator());
    }

    // Expects no blank lines.
    // Expects the first line to be a tag. We get a null pointer exception otherwise!
    // Does not require there to be an end tag.
    private static Map<String,List<String>> parseSections(Iterator<String> lines) {
        final Map<String,List<String>> sections = new HashMap<>();
        List<String> section = null;
        while (lines.hasNext()) {
            final String line = lines.next();
            if (isTag(line)) {
                if (isEndTag(line)) {
                    break;
                }
                section = new ArrayList<>();
                sections.put(line, section);
            } else {
                section.add(line);
            }
        }
        return sections;
    }

    // Each line is of the form
    // i,j
    // which says that task i has to happen before task j.
    // So j depends on i.
    /**
     * Parse the dependencies section.
     * Returns a dependency graph as a map that maps tasks to the collection
     * of tasks it depends on.
     * Tasks that do not depend on any other tasks have their IDs map to an
     * empty Collection. (This means that the key set of the map is the set of
     * vertices in the graph.)
     * Note that numbering starts from 1.
     * <strong>Note:</strong> the ALB format specifies precedence constraints,
     * not dependencies.
     * <em>So this method flips the order.</em>
     */
    public static Map<Integer,Collection<Integer>> parseDependencies(List<String> deps, int tasks) {
        final Map<Integer,Collection<Integer>> graph = new HashMap<>(tasks);
        for (int i = 1; i <= tasks; i++) {
            graph.put(i, new HashSet<>());
        }
        deps.stream()
            .map(time -> parseIntPair(time, ","))
            .forEach(pair -> graph.get(pair._2).add(pair._1));
        return graph;
    }

    // Each line is of the form `i:tᵢ` or `i tᵢ`
    /** Parse the task times section. Maps task number to task time. */
    public static Map<Integer,Integer> parseTaskTimes(List<String> times) {
        return times.stream()
            .map(time -> parseIntPair(time, List.of(" ", ":")))
            .collect(pairsToMap());
    }

    // Each line is of the form `i:eᵢ,e₂,…`
    /**
     * Parse the task equipment requirements section.
     * Maps task number to equipment numbers.
     */
    public static Map<Integer,? extends Collection<Integer>> parseEquipmentDependencies(List<String> deps) {
        return deps.stream()
            .map(dep -> split(dep, ":"))
            .map(Pairs.map_1(Integer::parseInt))
            .map(Pairs.map_2(Stream::of))
            .map(Pairs.map_2(ss -> ss.map(Integer::parseInt)))
            .map(Pairs.map_2(ss -> ss.collect(toList())))
            .collect(pairsToMap());
    }

    /**
     * Like {@link #split(String, Iterable)}, but takes multiple candidate
     * separators.
     * This exists to support variations in the file format.
     * For example, newer versions use {@code "i:tᵢ"} to specify task times,
     * while older versions use {@code "i tᵢ"}.
     * @throws IllegalArgumentException if no candidate separator exists in the string
     */
    private static Pair<String,String> split(String s, Iterable<String> seps) {
        for (final var sep : seps) {
            if (s.indexOf(sep) > -1) {
                return split(s, sep);
            }
        }
        throw new IllegalArgumentException("No separator worked.");
    }

    /**
     * Like {@code s.split(sep)}, but returns a Pair instead of an array.
     * @throws IllegalArgumentException if the split does not yield 2 fields.
     */
    private static Pair<String,String> split(String s, String sep) {
        final String[] ss = s.split(sep);
        if (ss.length != 2) {
            final String msg = "Expected the string to split into two fields!";
            throw new IllegalArgumentException(msg);
        }
        return Pair.of(ss[0], ss[1]);
    }

    /** Like {@link #split(String, String)}, but parses the 2 fields as ints. */
    private static Pair<Integer,Integer> parseIntPair(String s, String sep) {
        return parseIntPair(s, List.of(sep));
    }

    private static Pair<Integer,Integer> parseIntPair(String s, Iterable<String> seps) {
        return split(s, seps).map(Integer::parseInt, Integer::parseInt);
    }

    /**
     * Must <strong>exactly</strong> match one of the known tags.
     * Hence there cannot be extra white space, and the case must match.
     */
    private static boolean isTag(final String line) {
        return TAGS.contains(line);
    }

    private static boolean isEndTag(final String line) {
        return END_TAG.equals(line);
    }

    private static OptionalInt getCycleTime(Map<String,List<String>> sections) {
        return getOptionalInt(sections, CYCLE_TIME_TAG);
    }

    private static OptionalInt getStations(Map<String,List<String>> sections) {
        return getOptionalInt(sections, N_STATIONS_TAG);
    }

    private static OptionalInt getOptionalInt(Map<String,List<String>> sections, String key) {
        List<String> tmp = sections.get(key);
        if (tmp == null || tmp.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(tmp.get(0)));
    }

}
