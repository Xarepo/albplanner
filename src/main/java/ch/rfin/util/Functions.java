package ch.rfin.util;

import java.util.Map;
import java.util.Collection;
import java.util.function.*;

/**
 * Various functions.
 * @author Christoffer Fink
 */
public class Functions {

    /** Alias for {@code Integer.valueOf(i)}. */
    public static Integer box(final int i) {
        return i;
    }

    /** Alias for {@code integer.intValue()}. */
    public static int unbox(final Integer i) {
        return i;
    }

    /** Alias for {@code Long.valueOf(i)}. */
    public static Long box(final long i) {
        return i;
    }

    /** Alias for {@code long.intValue()}. */
    public static long unbox(final Long i) {
        return i;
    }

    /** Alias for {@code Double.valueOf(i)}. */
    public static Double box(final double i) {
        return i;
    }

    /** Alias for {@code double.doubleValue()}. */
    public static double unbox(final Double i) {
        return i;
    }


    public static int abs(final int n) {
        return Math.abs(n);
    }

    public static UnaryOperator<Integer> abs() {
        return n -> Math.abs(n);
    }

    /**
     * Force a number to be within a certain range (inclusive).
     */
    public static int clamp(final int lower, final int upper, final int n) {
        return max(lower, min(upper, n));
    }

    /**
     * Force a number to be within a certain range (inclusive).
     */
    public static UnaryOperator<Integer> clamp(final int lower, final int upper) {
        return n -> clamp(lower, upper, n);
    }

    /**
     * Force a number to be at most equal to an upper limit.
     */
    public static int upperCap(final int upper, final int n) {
        return min(upper, n);
    }

    /**
     * Force a number to be at most equal to an upper limit.
     */
    public static UnaryOperator<Integer> upperCap(final int upper) {
        return n -> upperCap(upper, n);
    }

    /**
     * Force a number to be at least equal to a lower limit.
     */
    public static int lowerCap(final int lower, final int n) {
        return max(lower, n);
    }

    /**
     * Force a number to be at least equal to a lower limit.
     */
    public static UnaryOperator<Integer> lowerCap(final int lower) {
        return n -> lowerCap(lower, n);
    }


    /** Increment (by 1). */
    public static int inc(final int a) {
        return a+1;
    }

    /** Increment (by 1) or saturate at {@code Integer.MAX_VALUE}. */
    public static int incSaturate(final int a) {
        if (a == Integer.MAX_VALUE) {
            return a;
        } else {
            return a+1;
        }
    }

    /** Increment (by 1) or throw if overflowing. */
    public static int incThrow(final int a) {
        if (a == Integer.MAX_VALUE) {
            throw new ArithmeticException(a + " would overflow if incremented");
        } else {
            return a+1;
        }
    }

    /** Increment (by 1). */
    public static UnaryOperator<Integer> inc() {
        return Functions::inc;
    }

    /** Increment (by 1). */
    public static long inc(final long a) {
        return a+1;
    }

    /**
     * Increment by the given amount.
     * @see ch.rfin.util.IntFunctions#incBy(int)
     */
    public static UnaryOperator<Integer> incBy(final int a) {
        return i -> i+a;
    }

    /** Increment by the given amount. */
    public static UnaryOperator<Integer> incBy(final Integer a) {
        return i -> i+a;
    }

    /**
     * Increment by the given amount.
     */
    public static UnaryOperator<Long> incBy(final long a) {
        return i -> i+a;
    }

    /** Increment by the given amount. */
    public static UnaryOperator<Long> incBy(final Long a) {
        return i -> i+a;
    }

    /** Decrement (by 1). */
    public static int dec(final int a) {
        return a-1;
    }

    /** Decrement (by 1). */
    public static UnaryOperator<Integer> dec() {
        return Functions::dec;
    }

    /** Decrement (by 1). */
    public static long dec(final long a) {
        return a-1;
    }

    /** Decrement by the given amount. */
    public static UnaryOperator<Integer> decBy(final int a) {
        return i -> i-a;
    }

    /** Decrement by the given amount. */
    public static UnaryOperator<Integer> decBy(final Integer a) {
        return i -> i-a;
    }

    /** Decrement by the given amount. */
    public static UnaryOperator<Long> decBy(final long a) {
        return i -> i-a;
    }


    public static int min(final int a, final int b) {
        return a < b ? a : b;
    }

    public static int min(final int a, final int b, final int c) {
        return min(min(a, b), c);
    }

    public static int min(final int a, final int b, final int c, final int d) {
        return min(min(a, b), min(c, d));
    }

    public static int min(final int n, final int ... ns) {
        int min = n;
        for (final int m : ns) {
            min = min(min, m);
        }
        return min;
    }


    public static int max(int a, int b) {
        return a < b ? b : a;
    }

    public static int max(int a, int b, int c) {
        return max(max(a, b), c);
    }

    public static int max(int a, int b, int c, int d) {
        return max(max(a, b), max(c, d));
    }

    public static int max(int n, int ... ns) {
        int max = n;
        for (final int m : ns) {
            max = max(n, m);
        }
        return max;
    }


    public static long min(long a, long b) {
        return a < b ? a : b;
    }

    public static long min(long a, long b, long c) {
        return min(min(a, b), c);
    }

    public static long min(long a, long b, long c, long d) {
        return min(min(a, b), min(c, d));
    }

    public static long min(long n, long ... ns) {
        long min = n;
        for (final long m : ns) {
            min = min(n, m);
        }
        return min;
    }


    public static long max(long a, long b) {
        return a < b ? b : a;
    }

    public static long max(long a, long b, long c) {
        return max(max(a, b), c);
    }

    public static long max(long a, long b, long c, long d) {
        return max(max(a, b), max(c, d));
    }

    public static long max(long n, long ... ns) {
        long max = n;
        for (final long m : ns) {
            max = max(n, m);
        }
        return max;
    }


    public static double min(double a, double b) {
        return a < b ? a : b;
    }

    public static double min(double a, double b, double c) {
        return min(min(a, b), c);
    }

    public static double min(double a, double b, double c, double d) {
        return min(min(a, b), min(c, d));
    }

    public static double min(double n, double ... ns) {
        double min = n;
        for (final double m : ns) {
            min = min(n, m);
        }
        return min;
    }


    public static double max(double a, double b) {
        return a < b ? b : a;
    }

    public static double max(double a, double b, double c) {
        return max(max(a, b), c);
    }

    public static double max(double a, double b, double c, double d) {
        return max(max(a, b), max(c, d));
    }

    public static double max(double n, double ... ns) {
        double max = n;
        for (final double m : ns) {
            max = max(n, m);
        }
        return max;
    }



    public static <T extends Comparable<? super T>> BinaryOperator<T> min() {
        return (a,b) -> min(a, b);
    }

    public static <T extends Comparable<? super T>> UnaryOperator<T> min(final T a) {
        return b -> min(a, b);
    }

    public static <T extends Comparable<? super T>> BinaryOperator<T> max() {
        return (a,b) -> max(a, b);
    }

    public static <T extends Comparable<? super T>> UnaryOperator<T> max(final T a) {
        return b -> max(a, b);
    }


    public static <T extends Comparable<? super T>> T min(T a, T b) {
        if (Predicates.lessThan(a, b)) {
            return a;
        } else {
            return b;
        }
    }

    @SafeVarargs
    public static <T extends Comparable<? super T>> T min(T n, T ... ns) {
        T min = n;
        for (final T x : ns) {
            min = min(min, x);
        }
        return min;
    }


    public static <T extends Comparable<? super T>> T max(T a, T b) {
        if (Predicates.greaterThan(a, b)) {
            return a;
        } else {
            return b;
        }
    }

    @SafeVarargs
    public static <T extends Comparable<? super T>> T max(T n, T ... ns) {
        T max = n;
        for (final T x : ns) {
            max = max(max, x);
        }
        return max;
    }

    public static int ceil(final double n) {
        return (int) java.lang.Math.ceil(n);
    }

    public static int floor(final double n) {
        return (int) java.lang.Math.floor(n);
    }

    public static int size(final String s) {
        return s == null ? 0 : s.length();
    }

    public static <T> int size(final Collection<T> xs) {
        return xs == null ? 0 : xs.size();
    }

    public static <K,V> int size(final Map<K,V> xs) {
        return xs == null ? 0 : xs.size();
    }

    public static <T> int size(final T[] xs) {
        return xs == null ? 0 : xs.length;
    }

}
