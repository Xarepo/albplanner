package ch.rfin.util;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.function.IntUnaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

/**
 * Hodgepodge of various utilities.
 * @author Christoffer Fink
 */
public class Misc {

    /** {@code xs[index]++}. Increment the element at {@code index} by 1. */
    public static void increment(final List<Integer> xs, final int index) {
        increment(xs, index, 1);
    }

    /**
     * {@code xs[index] += inc}.
     * Increment the element at {@code index} by {@code inc}.
     */
    public static void increment(final List<Integer> xs, final int index, final int inc) {
        xs.set(index, xs.get(index) + inc);
    }


    /** 
     * {@code map[key]++}. Increment the element at {@code key} by 1.
     * The key does not have to be present - the value defaults to 0.
     */
    public static <K> void increment(final Map<K,Integer> map, final K key) {
        increment(map, key, 1);
    }

    /**
     * {@code map[key] += inc}.
     * Increment the element at {@code key} by {@code inc}.
     * The key does not have to be present - the value defaults to 0.
     */
    public static <K> void increment(final Map<K,Integer> map, final K key, final int inc) {
        map.put(key, map.getOrDefault(key, 0) + inc);
    }

    /**
     * {@code map[key] += inc}.
     * Increment the element at {@code key} by {@code inc}.
     * The key does not have to be present - the value defaults to 0.
     */
    public static <K> void increment(final Map<K,Double> map, final K key, final double inc) {
        map.put(key, map.getOrDefault(key, 0.0) + inc);
    }

    /**
     * {@code map[key].add(elem)}.
     * Adds the element to the collection that is mapped to by the key.
     * The mapping defaults to an ArrayList if the key does not already map to a collection.
     */
    public static <K,V> Map<K,Collection<V>> add(final Map<K,Collection<V>> map, final K key, final V elem) {
        return add(map, key, elem, ArrayList::new);
    }

    /**
     * {@code map[key].add(elem)}.
     * Adds the element to the collection that is mapped to by the key.
     */
    public static <K,V> Map<K,Collection<V>> add(final Map<K,Collection<V>> map, final K key, final V elem, Supplier<Collection<V>> col) {
        if (!map.containsKey(key)) {
            map.put(key, col.get());
        }
        map.get(key).add(elem);
        return map;
    }

    /**
     * {@code map[key].add(elem)}.
     */
    public static <K> Map<K,Bits> add(final Map<K,Bits> map, final K key, final int elem) {
        Bits bits = map.getOrDefault(key, ImmutableBits.empty());
        map.put(key, bits.set(elem));
        return map;
    }

    /**
     * {@code map[key].add(set)}.
     */
    public static <K> Map<K,Bits> add(final Map<K,Bits> map, final K key, final Bits set) {
        Bits bits = map.getOrDefault(key, ImmutableBits.empty());
        map.put(key, bits.union(set));
        return map;
    }

    /**
     * Useful for doing {@code for (var keyValuePair : items(list))}.
     */
    public static <K,V> Iterable<Pair<K,V>> items(final Map<K,V> map) {
        return () -> map.entrySet().stream().map(Pairs::pairFrom).iterator();
    }

    public static BitSet bitSet(Collection<Integer> ints) {
        BitSet bits = new BitSet(ints.size());
        ints.forEach(bits::set);
        return bits;
    }

    public static BitSet bitSet(Iterable<Integer> ints) {
        BitSet bits = new BitSet();
        ints.forEach(bits::set);
        return bits;
    }

    // --- Argument validation ---

    public static void checkNonNull(OptionalInt param, String name) {
        if (param != null) {
            return;
        }
        final String msg = String
            .format("%s was null. Use an empty optional instead of null!", name);
        throw new IllegalArgumentException(msg);
    }

    public static void checkNonNull(Map<?,?> param, String name) {
        if (param != null) {
            return;
        }
        final String msg = String
            .format("%s was null. Use an empty map instead of null!", name);
        throw new IllegalArgumentException(msg);
    }

    public static void checkPositive(OptionalInt param, String name) {
        if (param.isEmpty()) {
            return;
        }
        final int val = param.getAsInt();
        if (val > 0) {
            return;
        }
        final String msg = String
            .format("%s (%d) must be positive", name, val);
        throw new IllegalArgumentException(msg);
    }

    public static void checkPositive(int param, String name) {
        if (param > 0) {
            return;
        }
        final String msg = String
            .format("%s (%d) must be positive", name, param);
        throw new IllegalArgumentException(msg);
    }

    // ===== Optionals =====

    public static int get(OptionalInt n) {
        return n.getAsInt();
    }

    public static int get(OptionalInt n, int fallback) {
        return n.orElse(fallback);
    }

    public static double get(OptionalDouble n) {
        // WTF is this crap?  Now they're just trolling us...
        // What else would it be? Are we gonna get a
        // optional.getAsString() some day!?
        return n.getAsDouble();
    }

    public static double get(OptionalDouble n, double fallback) {
        // I'm surprised they didn't name it
        // .orElseUseThisDoubleAsDefault(d)
        return n.orElse(fallback);
    }

    public static <T> T get(Optional<T> n) {
        return n.get();
    }

    public static <T> T get(Optional<T> n, T fallback) {
        return n.orElse(fallback);
    }

    // --- Factories ---

    public static OptionalInt optionalInt(int n) {
        return OptionalInt.of(n);
    }

    public static OptionalInt optionalInt() {
        return OptionalInt.empty();
    }

    public static OptionalDouble optionalDouble(double n) {
        return OptionalDouble.of(n);
    }

    public static OptionalDouble optionalDouble() {
        return OptionalDouble.empty();
    }

    // TODO: What if we want an Optional<Integer>? Use optionalInt(n) instead?
    // Or default to OptionalInt and force Optional<Integer> using optionalBoxed(n)?
    public static OptionalInt optional(int n) {
        return OptionalInt.of(n);
    }

    // TODO: What if we want an Optional<Double>? Use optionalDouble(n) instead?
    // Or default to OptionalInt and force Optional<Integer> using optionalBoxed(n)?
    public static OptionalDouble optional(double n) {
        return OptionalDouble.of(n);
    }

    public static <T> Optional<T> optional(T x) {
        return Optional.of(x);
    }

    public static <T> Optional<T> nullable(T x) {
        return Optional.ofNullable(x);
    }

    public static <T> Optional<T> optional() {
        return Optional.empty();
    }

    public static Optional<Integer> optionalBoxed(int n) {
        return Optional.of(n);
    }

    public static Optional<Double> optionalBoxed(double n) {
        return Optional.of(n);
    }

}
