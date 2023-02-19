package ch.rfin.test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extra assertions.
 * @author Christoffer Fink
 */
public class Assertions {

    public static void assertThrowsException(Runnable code) {
        assertThrows(Throwable.class, () -> code.run());
    }

    public static void assertThrowsNpe(Runnable code) {
        assertThrows(NullPointerException.class, () -> code.run());
    }

    public static void assertThrowsIllegalArg(Runnable code) {
        assertThrows(IllegalArgumentException.class, () -> code.run());
    }

    public static void assertThrowsIllegalState(Runnable code) {
        assertThrows(IllegalStateException.class, () -> code.run());
    }

}
