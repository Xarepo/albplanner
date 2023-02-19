package ch.rfin.util;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import static java.util.stream.Collectors.toList;

/**
 * Randomization utility.
 * Extends {@code java.util.Random} with some useful methods.
 * There are three main features compared to Random:
 * <ol>
 * <li>Generate integers and doubles using both lower and upper bounds.</li>
 * <li>Generate booleans (true/false) with some probability.</li>
 * <li>Collections: Shuffling, sampling, and selecting/removing elements.</li>
 * </ol>
 * Methods that work on collections generally leave the argument unmodified,
 * unless specifically indicated otherwise, such as
 * {@link #removeRandomElement(List)} or {@link #shuffleInPlace(List)}.
 * <p>
 * For the purpose of asymptotic analysis, a List is assumed to be an ArrayList
 * or similar.  In other words, constant-time random access, but linear-time
 * search and remove. For arbitrary List implementations, the running time
 * may be different.
 * <p>
 * Tips: If you want to repeatedly remove an element from some set of values
 * (hence the order of values in the set doesn't matter), then the most
 * efficient way is to place them in a List and use
 * {@link #removeRandomElementUnordered(List)}.
 * @version 1.0
 * @author Christoffer Fink
 */
@SuppressWarnings("serial")
public class Rng extends Random {

    public Rng() {
        this(System.nanoTime());
    }

    public Rng(final long seed) {
        super(seed);
    }

    public Rng(final Random seedSource) {
        this(seedSource.nextLong());
    }

    /** Uniformly random in [min, max] (hence min == max is allowed). */
    public int nextIntInclusive(final int min, final int maxInclusive) {
        return nextInt(maxInclusive - min + 1) + min;
    }

    /** Uniformly random in [min, max[ (hence min == max is NOT allowed). */
    public int nextInt(final int min, final int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Must have min < max");
        }
        return nextIntInclusive(min, max-1);
    }

    /** Uniformly random in [min, max], but max is returned only when min == max. */
    public double nextDouble(final double min, final double max) {
        if (min > max) {
            throw new IllegalArgumentException("Must have min <= max");
        }
        return min + (max - min)*nextDouble();
    }

    /**
     * Generate {@code true} with 50% probability -- i.e., flip a fair coin.
     */
    public boolean nextBoolean() {
        return nextDouble() < 0.5;
    }

    /**
     * Generate {@code true} with the given probability -- i.e., flip a biased coin.
     * @throws IllegalArgumentException if the probability is not in [0,1].
     */
    public boolean nextBoolean(final double probability) {
        if (probability > 1.0 || probability < 0.0) {
            throw new IllegalArgumentException(probability + " not in [0,1]");
        }
        return nextDouble() < probability;
    }

    /**
     * Stream the elements in the list in random order.
     * Time: Θ(n log n).
     */
    public <T> Stream<T> randomOrder(List<T> xs) {
        return shuffle(xs).stream();
    }

    /**
     * Stream the elements in the iterable in random order.
     * Note that this method is <strong>not</strong> lazy -- it will consume
     * the iterable before returning the Stream.
     * Time: Θ(n log n).
     */
    public <T> Stream<T> randomOrder(Iterable<T> xs) {
        List<T> items = new ArrayList<>();
        xs.forEach(items::add);
        shuffleInPlace(items);
        return items.stream();
    }

    /**
     * Stream the elements in the stream in random order.
     * Note that this method is <strong>not</strong> lazy -- it will consume
     * the stream before returning the new Stream.
     * Time: Θ(n log n).
     */
    public <T> Stream<T> randomOrder(final Stream<T> xs) {
        return randomOrder(() -> xs.iterator());
    }

    /**
     * Remove and return an element from the list in Θ(1) time.
     * This method is more efficient than
     * {@link #removeRandomElement(List)}
     * because it swaps in the last element to fill the gap when removing an
     * element, instead of shifting all the elements over. So the running time
     * is constant instead of linear.  However, that means the remaining
     * elements may not be in the same order as before the call!
     * <strong>Note:</strong> Mutates the given {@code list}!
     * @throws IllegalArgumentException if the collection is empty
     */
    public <T> T removeRandomElementUnordered(final List<T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Cannot remove an element from an empty list");
        }
        final int index = nextInt(list.size());
        final T elem = list.get(index);
        list.set(index, list.get(list.size() - 1));
        list.remove(list.size()-1);
        return elem;
    }

    /**
     * Remove and return an element from the list in Θ(n) time.
     * This method is less efficient than
     * {@link #removeRandomElementUnordered(List)}
     * because it preserves the relative order among the remaining elements.
     * So the running time is linear instead of constant.
     * <strong>Note:</strong> Mutates the given {@code list}!
     * @throws IllegalArgumentException if the collection is empty
     */
    public <T> T removeRandomElement(final List<T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Cannot remove an element from an empty list");
        }
        final int index = nextInt(list.size());
        return list.remove(index);
    }

    /**
     * Remove and return an element from the collection.
     * Time: Θ(n).
     * <strong>Note:</strong> Mutates the given {@code collection}!
     * Does not require that the iterator returned by {@code col.iterator()} supports
     * {@code remove()}.
     * @throws IllegalArgumentException if the collection is empty
     */
    public <T> T removeRandomElement(final Collection<T> col) {
        if (col.isEmpty()) {
            throw new IllegalArgumentException("Cannot remove an element from an empty collection");
        }
        final Iterator<T> iter = col.iterator();
        final int index = nextInt(col.size()) - 1;
        for (int i = 0; i < index; i++) {
            iter.next();
        }
        final T result = iter.next();
        col.remove(result);
        return result;
    }

    /**
     * Return an element from the list.
     * Time: Θ(1).
     * @throws IllegalArgumentException if the list is empty
     */
    public <T> T selectRandomElement(final List<T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Cannot select an element from an empty list");
        }
        final int index = nextInt(list.size());
        return list.get(index);
    }

    /**
     * Return an element from the collection.
     * Time: Θ(n).
     * @throws IllegalArgumentException if the collection is empty
     */
    public <T> T selectRandomElement(final Collection<T> col) {
        if (col.isEmpty()) {
            throw new IllegalArgumentException("Cannot select an element from an empty collection");
        }
        final int index = nextInt(col.size());
        return col.stream().skip(index).findFirst().get();
    }

    // Note: we could have a shuffle(List) overload, so that the list can be
    // used directly with Collections.shuffle(...) without first having to make
    // a list. But then we would still need to make a copy anyway to avoid
    // modifying the input collection.
    /** Return a shuffled copy of the collection. */
    public <T> List<T> shuffle(final Collection<T> col) {
        final List<T> copy = new ArrayList<>(col);
        return shuffleInPlace(copy);
    }

    /**
     * Shuffle the given list.
     * This is a convenience method for using
     * {@link java.util.Collections#shuffle(List)}.
     * <strong>Note:</strong> Mutates the given {@code list}!
     * @return the input list after shuffling it
     */
    public <T> List<T> shuffleInPlace(final List<T> list) {
        java.util.Collections.shuffle(list, this);
        return list;
    }

    // TODO: is there a need for a
    // randomStrictSublist(final List<T> list, final int size)
    // method that produces an actual, contiguous, sublist?

    /**
     * Random subsequence -- preserves relative element order.
     * Time: Θ(n log n).
     */
    public <T> List<T> randomSubsequence(final List<T> list, final int size) {
        if (size > list.size()) {
            throw new IllegalArgumentException("Size too large");
        }
        // Make a list of all indices.
        final List<Integer> indices = IntStream
            .range(0, list.size())
            .boxed()
            .collect(toList());
        // Shuffle it.
        shuffleInPlace(indices);
        // Then use the first size indices in sorted order to select elements.
        return indices.stream()
            .limit(size)
            .sorted()
            .map(list::get)
            .collect(toList());
    }

    // TODO: add a version that allows repetitions so that we can always get a
    // result of the desired size.
    /**
     * Like {@link #randomSubset(Collection, int)} but returns a list instead
     * of a set. Since the result is a list rather than a set, this means that
     * the result can contain duplicates (if there are duplicates in the
     * original collection).
     * If {@code size > col.size()}, then the returned list will have the same
     * size as {@code col}, and the result is equivalent to calling
     * {@link #shuffle(Collection)}.
     * Time: Θ(n log n).
     */
    public <T> List<T> randomSubsetList(final Collection<T> col, final int size) {
        return shuffle(col).subList(0, Math.min(size, col.size()));
    }

    // Note: The reason we have to put the collection into a HashSet before
    // calling randomSubsetList is the following.
    // Suppose we didn't make a set first, and just used col directly.
    // What if the collection has plenty of (non-duplicated) elements,
    // then we pass it to randomSubsetList, which happens to pick out a bunch of
    // duplicates, and then we put that into a HashSet, which causes the duplicates
    // to be removed, resulting in a smaller size than requested?
    // That would be kinda annoying.
    // XXX:
    // If the collection is NOT a set AND contains duplicates, the size of the
    // returned set may be less than the requested {@code size}.
    // This is because the method returns a Set, and so there is no way to
    // keep duplicates.
    /**
     * Return a subset with up to {@code size} elements.
     * If the collection contains at least {@code size}
     * <strong>distinct</strong> elements, then the set will have the requested
     * size. However, if the number of distinct elements is smaller, then the
     * size of the set will equal the number of distinct elements.
     */
    public <T> Set<T> randomSubset(final Collection<T> col, final int size) {
        // TODO: this is correct, but we can probably be more efficient.
        // At the moment, we make a copy here by putting the collection into
        // a set (what if it already is a set?), then randomSubsetList calls
        // shuffle, which copies the collection into a new list, and then we
        // put the result in a new set.
        // So that's 3 copies. Maybe we can get away with just 1?
        return new HashSet<>(randomSubsetList(new HashSet<>(col), size));
    }

}
