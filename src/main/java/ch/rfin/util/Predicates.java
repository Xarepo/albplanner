package ch.rfin.util;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Various predicates.
 * All methods either return boolean or a predicate.
 * These often make working with Streams cleaner.
 * For example: {@code stream.filter(lessThan(3))} instead of
 * {@code stream.filter(n -> n < 3)},
 * or {@code stream.filter(between(2, 5))} instead of
 * {@code stream.filter(n -> n >= 2 && n <= 5)}.
 * They can also make working with Comparables easier. For example,
 * {@code if (lessThan(b).test(a))} or
 * {@code if (lessThan(a, b))}
 * might be considered clearer than
 * {@code if (a.compareTo(b) < 0)}.
 * <p>
 * Some overloads may seem silly, like {@code zero()} and {@code zero(i)},
 * since it might seem like a method reference such as {@code Predicates::zero}
 * could be used wherever {@code Predicates.zero()} would be used. However,
 * method references can unfortunately not be treated as function literals
 * representing a functional interface instance. So, for example, while
 * {@code (Predicates::zero).negate()} is not allowed,
 * {@code Predicates.zero().negate()} is.
 *
 * @author Christoffer Fink
 */
public class Predicates {

    public static <T,R> Predicate<T> compose(
            Predicate<? super R> p,
            Function<? super T, ? extends R> f)
    {
        return t -> p.test(f.apply(t));
    }

    public static boolean not(final boolean b) {
        return !b;
    }

    public static <T> Predicate<T> not(final Predicate<T> p) {
        return Predicate.not(p);
    }


    public static Predicate<Object> isNull() {
        return o -> o == null;
    }

    public static boolean isNull(Object o) {
        return o == null;
    }

    public static Predicate<Object> nonNull() {
        return o -> o != null;
    }

    public static boolean nonNull(Object o) {
        return o != null;
    }

    public static BiPredicate<Object, Object> equal() {
        return Objects::equals;
    }

    public static Predicate<Object> equal(Object o) {
        return p -> Objects.equals(o, p);
    }

    public static boolean equal(Object o, Object p) {
        return Objects.equals(o, p);
    }


    public static boolean zero(final int i) {
        return i == 0;
    }

    public static boolean positive(final int i) {
        return i > 0;
    }

    public static boolean negative(final int i) {
        return i < 0;
    }

    public static boolean even(final int i) {
        return i % 2 == 0;
    }

    public static boolean odd(final int i) {
        return i % 2 != 0;
    }

    public static Predicate<Integer> zero() {
        return i -> i == 0;
    }

    public static Predicate<Integer> positive() {
        return i -> i > 0;
    }

    public static Predicate<Integer> negative() {
        return i -> i < 0;
    }

    public static Predicate<Integer> even() {
        return i -> i % 2 == 0;
    }

    public static Predicate<Integer> odd() {
        return i -> i % 2 != 0;
    }



    public static boolean zero(final long i) {
        return i == 0;
    }

    public static boolean positive(final long i) {
        return i > 0;
    }

    public static boolean negative(final long i) {
        return i < 0;
    }

    public static boolean zero(final double i) {
        return i == 0;
    }

    public static boolean positive(final double i) {
        return i > 0;
    }

    public static boolean negative(final double i) {
        return i < 0;
    }


    /** {@code lessThan(b).test(a) ⇔ a < b}. */
    public static <T extends Comparable<? super T>> BiPredicate<T,T> lessThan() {
        return (a,b) -> a.compareTo(b) < 0;
    }

    /** {@code atMost(b).test(a) ⇔ a ≤ b}. */
    public static <T extends Comparable<? super T>> BiPredicate<T,T> atMost() {
        return (a,b) -> a.compareTo(b) <= 0;
    }

    /** {@code greaterThan(b).test(a) ⇔ a > b}. */
    public static <T extends Comparable<? super T>> BiPredicate<T,T> greaterThan() {
        return (a,b) -> a.compareTo(b) > 0;
    }

    /** {@code atLeast(b).test(a) ⇔ a ≥ b}. */
    public static <T extends Comparable<? super T>> BiPredicate<T,T> atLeast() {
        return (a,b) -> a.compareTo(b) >= 0;
    }


    /** {@code lessThan(b).test(a) ⇔ a < b}. */
    public static <T extends Comparable<? super T>> Predicate<T> lessThan(final T b) {
        return a -> a.compareTo(b) < 0;
    }

    /** {@code atMost(b).test(a) ⇔ a ≤ b}. */
    public static <T extends Comparable<? super T>> Predicate<T> atMost(final T b) {
        return a -> a.compareTo(b) <= 0;
    }

    /** {@code greaterThan(b).test(a) ⇔ a > b}. */
    public static <T extends Comparable<? super T>> Predicate<T> greaterThan(final T b) {
        return a -> a.compareTo(b) > 0;
    }

    /** {@code atLeast(b).test(a) ⇔ a ≥ b}. */
    public static <T extends Comparable<? super T>> Predicate<T> atLeast(final T b) {
        return a -> a.compareTo(b) >= 0;
    }

    /** {@code between(a, b).test(c) ⇔ a ≤ c ≤ b}. */
    public static <T extends Comparable<? super T>> Predicate<T> between(final T min, final T max) {
        return atLeast(min).and(atMost(max));
    }

    /** {@code outside(a, b).test(c) ⇔ c < a ∨ c > b}. */
    public static <T extends Comparable<? super T>> Predicate<T> outside(final T min, final T max) {
        return between(min, max).negate();
    }


    /** {@code lessThan(a, b) ⇔ a < b}. */
    public static <T extends Comparable<? super T>> boolean lessThan(final T a, final T b) {
        return a.compareTo(b) < 0;
    }

    /** {@code atMost(a, b) ⇔ a ≤ b}. */
    public static <T extends Comparable<? super T>> boolean atMost(final T a, final T b) {
        return a.compareTo(b) <= 0;
    }

    /** {@code greaterThan(a, b) ⇔ a > b}. */
    public static <T extends Comparable<? super T>> boolean greaterThan(final T a, final T b) {
        return a.compareTo(b) > 0;
    }

    /** {@code atLeast(a, b) ⇔ a ≥ b}. */
    public static <T extends Comparable<? super T>> boolean atLeast(final T a, final T b) {
        return a.compareTo(b) >= 0;
    }

    /** {@code between(c, a, b) ⇔ a ≤ c ≤ b}. */
    public static <T extends Comparable<? super T>> boolean between(final T c, final T min, final T max) {
        return atLeast(c, min) && atMost(c, max);
    }

    /** {@code outside(c, a, b) ⇔ c < a ∨ c > b}. */
    public static <T extends Comparable<? super T>> boolean outside(final T c, final T min, final T max) {
        return !(between(c, min, max));
    }

}
