package ch.rfin.util;

import java.util.stream.Stream;
import static java.util.stream.Collectors.joining;

/**
 * Printing utilities.
 * Do
 * <pre>
 * printf("Sorted a list of %d elements in %d seconds", list.size(), ms/1000);
 * </pre>
 * instead of
 * <pre>
 * System.out.println("Sorted a list of " + list.size() + " elements in " + ms/1000 + " seconds");
 * // or
 * System.out.println(String.format("Sorted a list of %d elements in %d seconds", list.size(), ms/1000));
 * </pre>
 * <p>
 * The formatting works as described in the documentation for
 * {@link java.lang.String#format String.format()}.
 * @author Christoffer Fink
 */
public class Printing {

    /**
     * Print the concatenation of multiple values, like {@code print(x, y, z)} in Python.
     */
    public static void print(Object ... vals) {
        String s = Stream.of(vals)
            .map(String::valueOf)
            .collect(joining(" "));
        System.out.println(s);
    }

    /**
     * Print a formatted string to standard out.
     */
    public static void printf(String fmt, Object ... vals) {
        System.out.println(String.format(fmt, vals));
    }

    /**
     * Print a formatted string to standard error.
     */
    public static void eprintf(String fmt, Object ... vals) {
        System.err.println(String.format(fmt, vals));
    }

}
