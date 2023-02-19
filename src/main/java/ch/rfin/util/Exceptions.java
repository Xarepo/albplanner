package ch.rfin.util;

/**
 * Exception utilities.
 * For methods that take a message ({@code String msg}) and varargs
 * ({@code Object ... vararg}), the message can either be a plain message
 * or a format string.
 * If there are varargs present, the string is treated as a format string, and
 * the varargs are inserted into it.
 * @author Christoffer Fink
 */
public class Exceptions {

    /**
     * Shorthand for conditionally throwing an Assertion Error with a default message.
     * @throws AssertionError if the condition is false
     */
    public static void assertion(boolean condition) {
        assertion(condition, "Not true :(");
    }

    /**
     * Shorthand for conditionally throwing an Assertion Error with a customized message.
     * @param msg either a message or a format string
     * @throws AssertionError if the condition is false
     */
    public static void assertion(boolean condition, String msg, Object ... vararg) {
        if (!condition) {
            final String formatted = String.format(msg, vararg);
            throw new AssertionError(formatted);
        }
    }

    /**
     * Shorthand for throwing an Assertion Error.
     * @param msg either a message or a format string
     * @throws AssertionError always
     */
    public static void assertion(String msg, Object ... vararg) {
        final String formatted = String.format(msg, vararg);
        throw new AssertionError(formatted);
    }

    /**
     * Shorthand for throwing an Illegal Argument Exception.
     * @param msg either a message or a format string
     * @throws IllegalArgumentException always
     */
    public static void illegalArg(final String msg, Object ... vararg) {
        final String formatted = String.format(msg, vararg);
        throw new IllegalArgumentException(formatted);
    }

    /**
     * Shorthand for throwing an Illegal State Exception.
     * @param msg either a message or a format string
     * @throws IllegalStateException always
     */
    public static void illegalState(final String msg, Object ... vararg) {
        final String formatted = String.format(msg, vararg);
        throw new IllegalStateException(formatted);
    }

}
