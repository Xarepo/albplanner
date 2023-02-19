package ch.rfin.util;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;

import ch.rfin.util.Pair;
import ch.rfin.util.Pairs;
import ch.rfin.util.Streams;

/**
 * Various collection utilities.
 * @author Christoffer Fink
 */
public class Collections {

    /** Make a list of `size` zeros. */
    public static List<Integer> zeros(final int size) {
        final List<Integer> tmp = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            tmp.add(0);
        }
        return tmp;
    }

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


    public static <T> Map<Integer, T> toMap(final Iterable<T> xs) {
        return Streams.enumerate(xs)
            .collect(Pairs.pairsToMap());
    }

    /**
     * Useful for doing {@code for (var keyValuePair : enumerate(map))}.
     */
    public static <K,V> Iterable<Pair<K,V>> items(final Map<K,V> map) {
        return () -> map.entrySet().stream().map(Pairs::pairFrom).iterator();
    }

    /** Return a list of `size` empty sets. */
    public static <T> List<Collection<T>> emptySets(final int size) {
        return Stream
            .generate(() -> new HashSet<T>(size))
            .limit(size)
            .collect(Collectors.toList());
    }

    /** Return a list of `size` empty list. */
    public static <T> List<Collection<T>> emptyLists(final int size) {
        return Stream
            .generate(() -> new ArrayList<T>(size))
            .limit(size)
            .collect(Collectors.toList());
    }

}
