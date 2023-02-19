package ch.rfin.util;

import java.util.function.IntUnaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.DoubleToIntFunction;
import java.util.function.ToIntFunction;
import java.util.function.IntFunction;

/**
 * Various int-specific functions.
 * @author Christoffer Fink
 */
public class IntFunctions {

    /** Increment (by 1). */
    public static int inc(final int n) {
        return n + 1;
    }

    /** Increment (by 1). */
    public static IntUnaryOperator inc() {
        return incBy(1);
    }

    /** Increment by the given amount. */
    public static IntUnaryOperator incBy(final int a) {
        return i -> i+a;
    }

    /** Increment by the given amount. */
    public static IntUnaryOperator incBy(final Integer a) {
        return i -> i+a;
    }

    /** Decrement (by 1). */
    public static int dec(final int a) {
        return a-1;
    }

    /** Increment (by 1). */
    public static IntUnaryOperator dec() {
        return decBy(1);
    }

    /** Decrement by the given amount. */
    public static IntUnaryOperator decBy(final int a) {
        return i -> i-a;
    }

    public static IntBinaryOperator plus() {
        return (a,b) -> a + b;
    }

    public static int min(final int a, final int b) {
        return a < b ? a : b;
    }

    public static int max(final int a, final int b) {
        return a < b ? b : a;
    }

    public static IntBinaryOperator min() {
        return IntFunctions::min;
    }

    public static IntBinaryOperator max() {
        return IntFunctions::max;
    }

    public static IntUnaryOperator min(final int a) {
        return b -> Functions.min(a, b);
    }

    public static IntUnaryOperator max(final int a) {
        return b -> Functions.max(a, b);
    }

    public static int ceil(final double n) {
        return (int) java.lang.Math.ceil(n);
    }

    public static int floor(final double n) {
        return (int) java.lang.Math.floor(n);
    }

    public static DoubleToIntFunction ceil() {
        return IntFunctions::ceil;
    }

    public static DoubleToIntFunction floor() {
        return IntFunctions::floor;
    }

    public static int clamp(final int lower, final int upper, final int n) {
        return max(lower, min(upper, n));
    }

    public static int upperCap(final int upper, final int n) {
        return min(upper, n);
    }

    public static int lowerCap(final int lower, final int n) {
        return max(lower, n);
    }

    public static IntUnaryOperator clamp(final int lower, final int upper) {
        return n -> max(lower, min(upper, n));
    }

    public static IntUnaryOperator upperCap(final int upper) {
        return n -> min(upper, n);
    }

    public static IntUnaryOperator lowerCap(final int lower) {
        return n -> max(lower, n);
    }

    public static ToIntFunction<Integer> unbox() {
        return i -> i;
    }

    public static IntFunction<Integer> box() {
        return i -> i;
    }

}
