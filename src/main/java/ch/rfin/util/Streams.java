package ch.rfin.util;

import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Some tools for working with Streams and Iterators/Iterables.
 * Unless otherwise noted, all streams, iterators, and iterables are
 * constructed/processed lazily.  So these methods can be used with infinite
 * streams/iterables.
 * <p>
 * There is some overlap and even duplication of the functionality provided by
 * {@code ch.rfin.util.Pairs}.
 * @author Christoffer Fink
 */
public class Streams {

    public static class PrimitiveInt {

        /**
         * Like {@code IntStream.range()}, but the parameters can be anything.
         * For example, {@code range(3, -2) → [3,2,1,0,-1]}.
         * Returns an empty stream if {@code endExcl ≤ startIncl}.
         */
        public static IntStream range(final int startIncl, final int endExcl) {
            final int step = endExcl > startIncl ? 1 : -1;
            return range(startIncl, endExcl, step);
        }

        /**
         * Returns a stream of ints in the specified range, in increments of
         * the given step size.
         */
        public static IntStream range(final int startIncl, final int endExcl, final int step) {
            final IntPredicate pred = endExcl > startIncl && step > 0
                ? i -> i < endExcl
                : i -> i > endExcl;
            return IntStream.iterate(
                    startIncl,
                    pred,
                    i -> i+step);
        }

        /**
         * {@code [0, 1, 2, …, n-2, n-1]} -
         * Count from 0 (inclusive) to n (exclusive).
         */
        public static IntStream countUpTo(final int n) {
            return IntStream.range(0, n);
        }

        /**
         * {@code [0, 1, 2, …, Integer.MAX_VALUE]} -
         * Count from 0 (inclusive) to Integer.MAX_VALUE (inclusive).
         */
        public static IntStream countUp() {
            return countUpFrom(0);
        }

        /** Count from n (inclusive) to Integer.MAX_VALUE (inclusive). */
        public static IntStream countUpFrom(final int n) {
            return IntStream.rangeClosed(n, Integer.MAX_VALUE);
        }

        /** Count from n (exclusive) to 0 (inclusive). */
        public static IntStream countDownFrom(final int n) {
            return range(n-1, -1);
        }

        /** Make an int stream out of an Iterable of Integers. */
        public static IntStream intStream(final Collection<Integer> it) {
            return it.stream().mapToInt(Integer::intValue);
        }

        /** Make an int stream out of an Iterable of Integers. */
        public static IntStream intStream(final Iterable<Integer> it) {
            Iterator<Integer> iter = it.iterator();
            if (!iter.hasNext()) {
                return IntStream.empty();
            }
            return Streams.stream(it).mapToInt(Integer::intValue);
        }

        /** Returns a stream of indicies. */
        public static <T> IntStream indices(final List<T> xs) {
            return countUpTo(xs.size());
        }

        /** Returns a stream of indicies in reversed (descending) order. */
        public static <T> IntStream indicesDescending(final List<T> xs) {
            return countDownFrom(xs.size());
        }

    }

    public static class BoxedInt {

        /** Count from 0 (inclusive) to n (exclusive). */
        public static Stream<Integer> countUpTo(final int n) {
            return PrimitiveInt.countUpTo(n).boxed();
        }

        /** Count from 0 (inclusive) to Integer.MAX_VALUE (inclusive). */
        public static Stream<Integer> countUp() {
            return countUpFrom(0);
        }

        /** Count from n (inclusive) to Integer.MAX_VALUE (inclusive). */
        public static Stream<Integer> countUpFrom(final int n) {
            return PrimitiveInt.countUpFrom(n).boxed();
        }

        /** Count from n (exclusive) to 0 (inclusive). */
        public static Stream<Integer> countDownFrom(final int n) {
            return PrimitiveInt.countDownFrom(n).boxed();
        }

        public static <T> Stream<Integer> indices(final List<T> xs) {
            return PrimitiveInt.indices(xs).boxed();
        }

    }


    public static IntStream intStream(Iterable<Integer> ints) {
        return stream(ints).mapToInt(i -> i);
    }

    public static IntStream intStream(Collection<Integer> ints) {
        return ints.stream().mapToInt(i -> i);
    }

    /**
     * Make a stream out of an Iterable.
     * This overload exists for consistency (and efficiency).
     * All Collections are Iterable, but not all Iterables are Collections.
     * This way, the same method can be used in either case, and we can
     * fall back on the existing (and presumably more efficient)
     * {@code collection.stream()} for Collections.
     */
    public static <T> Stream<T> stream(final Collection<T> it) {
        return it.stream();
    }

    /** Make a stream out of an Iterable. */
    public static <T> Stream<T> stream(final Iterable<T> it) {
        return StreamSupport.stream(it.spliterator(), false);
    }

    /** Make a stream out of an Iterator. */
    public static <T> Stream<T> stream(final Iterator<T> it) {
        return stream(() -> it);
    }

    /**
     * Make a stream Iterable.
     * An Iterable has an {@code iterator()} method. A Stream does have such a
     * method, but it's not declared to implement the Iterable interface. So
     * even though it has the necessary method, the type system doesn't allow
     * it to be used as an Iterable.
     */
    public static <T> Iterable<T> iterable(final Stream<T> xs) {
        return () -> xs.iterator();
    }

    /**
     * {@code [x₀, x₁, x₂, …] → [(0,x₀), (1,x₁), (2,x₂), …]} -
     * Converts an iterable to a stream of pairs where the first value is the
     * index of the value, like {@code enumerate()} in Python.
     * @param xs a list of values [x₀, x₁, x₂, ...].
     * @return a stream of index-value pairs [(0,x₀), (1,x₁), (2,x₂), …].
     */
    public static <T> Stream<Pair<Integer,T>> enumerate(final List<T> xs) {
        return BoxedInt.indices(xs).map(Pairs.withId(xs::get));
    }

    /**
     * {@code [x₀, x₁, x₂, …] → [(0,x₀), (1,x₁), (2,x₂), …]} -
     * Converts an iterable to a stream of pairs where the first value is the
     * index of the value, like {@code enumerate()} in Python.
     * Most useful with Lists so that its elements can be iterated together with
     * their indices.
     * @param xs an iterable of values [x₀, x₁, x₂, …].
     * @return a stream of index-value pairs [(0,x₀), (1,x₁), (2, x₂), …].
     */
    public static <T> Stream<Pair<Integer,T>> enumerate(final Iterable<T> xs) {
        return Pairs.enumerate(xs);
    }

    /**
     * {@code [x₀, x₁, x₂, …] → [(0,x₀), (1,x₁), (2,x₂), …]} -
     * Converts an iterable to a stream of pairs where the first value is the
     * index of the value, like {@code enumerate()} in Python.
     */
    public static <T> Stream<Pair<Integer,T>> enumerate(final Stream<T> xs) {
        return enumerate(iterable(xs));
    }

    /**
     * {@code {k₀:v₀, k₁:v₁, k₂:v₂, …} → [(k₀,v₀), (k₁,v₁), (k₂,v₂), …]} -
     * Converts a Map to a stream of key-value pairs, like {@code items()}
     * in Python.
     * This duplicates {@link Pairs#stream(Map)}.
     * @return Stream of key-value pairs [(k₀,v₀), (k₁,v₁), (k₂,v₂), …]
     */
    public static <K,V> Stream<Pair<K,V>> items(final Map<K,V> map) {
        return Pairs.stream(map);
    }

    /**
     * {@code {k₀:v₀, k₁:v₁, k₂:v₂, …} → [k₀:v₀, k₁:v₁, k₂:v₂, …]} -
     * Converts a Map to a stream of key-value pairs, like {@code items()}
     * in Python.
     * @return Stream of key-value pairs [(k₀,v₀), (k₁,v₁), (k₂,v₂), …]
     */
    public static <K,V> Stream<Map.Entry<K,V>> entries(final Map<K,V> xs) {
        return xs.entrySet().stream();
    }

    /**
     * {@code {k₀:v₀, k₁:v₁, k₂:v₂, …} → [k₀, k₁, k₂, …]}.
     */
    public static <K,V> Stream<K> keys(final Map<K,V> xs) {
        return xs.keySet().stream();
    }

    /**
     * {@code {k₀:v₀, k₁:v₁, k₂:v₂, …} → [v₀, v₁, v₂, …]}.
     */
    public static <K,V> Stream<V> values(final Map<K,V> xs) {
        return xs.values().stream();
    }


    /**
     * {@code (f, [s₁, s₂, …]) → [(f,s₁), (f,s₂), …]} - same as
     * {@link #pairsWithFirst(Object, Stream)} but takes an iterable instead
     * of a Stream.
     */
    public static <T,S> Stream<Pair<T,S>> pairsWithFirst(final T first, final Iterable<S> seconds) {
        return pairsWithFirst(first, stream(seconds));
    }

    /**
     * {@code (f, [s₁, s₂, …]) → [(f,s₁), (f,s₂), …]} -
     * pairs up a single item with multiple other items, putting the single item
     * as the first in each pair.
     * @param first a single item to be put second in the pairs
     * @param seconds a stream of values [s₁, s₂, …] to be put second in the pairs
     * @return a Stream of f-sᵢ pairs [(f,s₁), (f,s₂), …]
     */
    public static <T,S> Stream<Pair<T,S>> pairsWithFirst(final T first, final Stream<S> seconds) {
        return seconds.map((S s) -> Pair.of(first, s));
    }

    /**
     * {@code (s, [f₁, f₂, …]) → [(f₁,s), (f₂,s), …]} - same as
     * {@link #pairsWithSecond(Object, Stream)} but takes an iterable instead
     * of a Stream.
     */
    public static <T,S> Stream<Pair<T,S>> pairsWithSecond(final S second, final Iterable<T> firsts) {
        return pairsWithFirst(second, firsts).map(Pair::swap);
    }

    /**
     * {@code (s, [f₁, f₂, …]) → [(f₁,s), (f₂,s), …]} -
     * pairs up a single item with multiple other items, putting the single item
     * as the second in each pair.
     * @param firsts a stream of values [f₁, f₂, …] to be put first in the pairs
     * @param second a single item to be put second in the pairs
     * @return a Stream of fᵢ-s pairs [(f₁,s), (f₂,s), …]
     */
    public static <T,S> Stream<Pair<T,S>> pairsWithSecond(final S second, final Stream<T> firsts) {
        return pairsWithFirst(second, firsts).map(Pair::swap);
    }

    /**
     * {@code [x₀, x₁, x₂, …] → [(x₀,x₁), (x₁,x₂), (x₂,x₃), …]},
     * like {@code zip(xs[:-1], xs[1:])} in Python.
     * Exactly equivalent to {@code Pairs.streamAdjacent(xs)}.
     */
    public static <T> Stream<Pair<T,T>> adjacentPairs(final Iterable<T> xs) {
        return Pairs.streamAdjacent(xs);
    }

    /**
     * {@code [(x₀,x₁), (x₂,x₃), …] → [x₀, x₁, x₂, x₃, …]} - the inverse of
     * {@code Pairs.stream(xs)}.
     */
    public static <T> Stream<T> concatPairs(final Stream<Pair<T,T>> pairs) {
        final Iterator<Pair<T,T>> streamIter = pairs.iterator();
        if (!streamIter.hasNext()) {
            return Stream.empty();
        }
        final Iterable<T> iterable = () -> new Iterator<T>() {
            Pair<T,T> nextPair = streamIter.next();
            int items = 2;
            @Override
            public boolean hasNext() {
                if (items > 0) {
                    return true;
                } else {
                    return streamIter.hasNext();
                }
            }
            @Override
            public T next() {
                if (items == 0) {
                    nextPair = streamIter.next();
                    items = 2;
                }
                items--;
                if (items == 1) {
                    return nextPair._1;
                } else if (items == 0) {
                    return nextPair._2;
                } else {
                    throw new NoSuchElementException("No such element.");
                }
            }
        };
        return stream(iterable);
    }

    /**
     * {@code ([x₀, x₁, x₂, …], [y₀, y₁, y₂, …]) → [(x₀,y₀), (x₁,y₁), (x₂,y₂), …]} -
     * Zip two streams into one, like {@code zip()} in Python.
     * @param xs a Stream of values [x₀, x₁, x₂, …].
     * @param ys a Stream of values [y₀, y₁, y₂, …].
     * @return a Stream of x-y pairs [(x₀,y₀), (x₁,y₁), (x₂,y₂), …].
     */
    public static <T,S> Stream<Pair<T,S>> zip(final Stream<T> xs, final Stream<S> ys) {
        return stream(zip(iterable(xs), iterable(ys)));
    }

    /**
     * {@code ([x₀, x₁, x₂, …], [y₀, y₁, y₂, …]) → [(x₀,y₀), (x₁,y₁), (x₂,y₂), …]} -
     * Zip two iterables into one stream, like {@code zip()} in Python.
     * @param xs an Iterable of values [x₀, x₁, x₂, …].
     * @param ys an Iterable of values [y₀, y₁, y₂, …].
     * @return a Stream of x-y pairs [(x₀,y₀), (x₁,y₁), (x₂,y₂), …].
     */
    public static <T1,T2> Stream<Pair<T1,T2>> zipAsStream(
            final Iterable<T1> xs,
            final Iterable<T2> ys)
    {
        return Pairs.zip(xs, ys);
    }

    public static Stream<String> split(final String s, final String sep) {
        return Stream.of(s.split(sep));
    }


    // TODO: Do these belong here? They neither take nor produce a Stream.
    // But would it make sense to have a separate util class for Iterator/Iterable
    // or zipping?

    /**
     * {@code ([x₀, x₁, x₂, …], [y₀, y₁, y₂, …]) → [(x₀,y₀), (x₁,y₁), (x₂,y₂), …]} -
     * Zip two iterables into one, like {@code zip()} in Python.
     * @param xs an Iterator of values [x₀, x₁, x₂, …].
     * @param ys an Iterator of values [y₀, y₁, y₂, …].
     * @return an Iterator of x-y pairs [(x₀,y₀), (x₁,y₁), (x₂,y₂), …].
     */
    public static <T,S> Iterator<Pair<T,S>> zip(final Iterator<T> xs, final Iterator<S> ys) {
        return new Iterator<Pair<T,S>>() {
            @Override
            public boolean hasNext() {
                return xs.hasNext() && ys.hasNext();
            }

            @Override
            public Pair<T,S> next() {
                return Pair.of(xs.next(), ys.next());
            }
        };
    }

    /**
     * {@code ([x₀, x₁, x₂, …], [y₀, y₁, y₂, …]) → [(x₀,y₀), (x₁,y₁), (x₂,y₂), …]} -
     * Zip two iterables into one, like {@code zip()} in Python.
     * @param xs an Iterable of values [x₀, x₁, x₂, …].
     * @param ys an Iterable of values [y₀, y₁, y₂, …].
     * @return an Iterable of x-y pairs [(x₀,y₀), (x₁,y₁), (x₂,y₂), …].
     */
    public static <T,S> Iterable<Pair<T,S>> zip(final Iterable<T> xs, final Iterable<S> ys) {
        return () -> zip(xs.iterator(), ys.iterator());
    }

}
