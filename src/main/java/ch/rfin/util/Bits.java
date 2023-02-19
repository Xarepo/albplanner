package ch.rfin.util;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.math.BigInteger;
import static java.math.BigInteger.ZERO;

// TODO: could we make a bit set from some (arbitrary) enum values?

/**
 * A bit set that shares similarities with both {@link java.util.BitSet} and
 * {@link java.math.BigInteger}, hopefully combining the best of both.
 * Each of them offers only a (proper) subset of the ideal interface.
 * <p>
 * The problem with BitSet is that it's mutable. So, for example, you can't
 * easily get the union or intersection of two bit sets. Not without destroying
 * one of them.
 * The problem with BigInteger is that, while immutable, it really is just a
 * big integer (string of bits) that can be forced into the role of a bit set.
 * BigInteger also has many extraneous methods that don't make sense for a
 * bit set, such as taking the square root or checking primality, etc.
 * <p>
 * The Bits interface is designed to allow both mutable and immutable
 * implementations. While it is intended to be used as a replacement for
 * BitSet (so primarily a set rather than a big integer), it can be converted
 * to/from a BigInteger (and also to/from a BitSet).
 * @author Christoffer Fink
 */
public interface Bits extends Iterable<Integer> {

    @Override
    public default Iterator<Integer> iterator() {
        return stream().boxed().iterator();
    }

    public default Collection<Integer> ints() {
        Collection<Integer> ints = new java.util.ArrayList<>(size());
        for (final Integer i : this) {
            ints.add(i);
        }
        return ints;
    }

    public default boolean isEmpty() {
        return size() == 0;
    }
    public int size();

    public Bits and(Bits set);
    public Bits nand(Bits set);
    public Bits or(Bits set);
    public Bits xor(Bits set);

    public Bits set(int index);
    // TODO: more set(...) variants
    public Bits flip(int index);

    public default boolean get(int index) {
        // TODO: Implement in terms of stream()
        throw new UnsupportedOperationException("Not implemented.");
    }

    public default boolean intersects(Bits set) {
        // TODO: Implement in terms of stream()
        throw new UnsupportedOperationException("Not implemented.");
    }

    public default boolean subsetOf(Bits set) {
        // TODO: Implement in terms of stream()
        throw new UnsupportedOperationException("Not implemented.");
    }

    public default boolean properSubsetOf(Bits set) {
        return size() < set.size() && subsetOf(set);
    }

    public default boolean supersetOf(Bits set) {
        return set.subsetOf(this);
    }

    public default Bits intersection(Bits set) {
        return and(set);
    }

    public default Bits union(Bits set) {
        return or(set);
    }

    /**
     * Returns a stream of indices for which this Bits contains a bit in the
     * set state.
     * <strong>NOTE:</strong> The default implementation uses
     * {@link #asBitSet()}. So implementations must override at least one
     * of these two methods.
     */
    public default IntStream stream() {
        return asBitSet().stream();
    }

    /**
     * Convert this bit set to the equivalent {@link java.util.BitSet}.
     * <strong>NOTE:</strong> The default implementation uses
     * {@link #stream()}. So implementations must override at least one
     * of these two methods.
     */
    public default BitSet asBitSet() {
        BitSet bits = new BitSet(size());
        stream().forEach(bits::set);
        return bits;
    }

    public default BigInteger asBigInteger() {
        if (isEmpty()) {
            return BigInteger.ZERO;
        }
        return new BigInteger(asBitSet().toByteArray());
    }

    // TODO: Make mutable and unmutable versions?
    public static ImmutableBits bits(BitSet bits) {
        if (bits.cardinality() == 0) {
            return bits(ZERO);
        }
        // This doesn't work for some reason. Not sure how BitSet creates
        // byte arrays. It probably isn't contiguous.
        BigInteger tmp = bits
            .stream()
            .boxed()
            .reduce(ZERO, BigInteger::setBit, BigInteger::or);
        return bits(tmp);
    }

    public static ImmutableBits bits(BigInteger bits) {
        return ImmutableBits.of(bits);
    }

    public static ImmutableBits bits(Iterable<Integer> ints) {
        BigInteger tmp = BigInteger.ZERO;
        for (final int i : ints) {
            tmp = tmp.setBit(i);
        }
        return bits(tmp);
    }

    public static ImmutableBits bits(Collection<Integer> ints) {
        BigInteger tmp = ints
            .stream()
            .reduce(ZERO, BigInteger::setBit, BigInteger::or);
        return bits(tmp);
    }

    // TODO: hmm, really?
    /*
    public default boolean asBoolean() {
        return !isEmpty();
    }

    public default Bits plus(int index) {
        return set(index);
    }

    public default Bits plus(Bits set) {
        return union(set);
    }
    */

    // Missing from Bits
    // cardinality()                // replaced by size()
    // clear(index)                 // don't want this
    // clear(from, to)              // don't want this
    // clone()                      // don't want this
    // flip(from, to)
    // get(from, to)
    // length()                     // don't want this !?
    // nextClearBit(from)
    // nextSetBit(from)
    // previousClearBit(from)
    // previousSetBit(from)
    // set(index, bool)
    // set(from, to)
    // set(from, to, bool)
    // toByteArray()                // superfluous?
    // toLongArray()                // superfluous?
    // valueOf(bytes)               // superfluous?
    // valueOf(longs)               // superfluous?
    // valueOf(byteBuffer)          // superfluous?
    // valueOf(longBuffer)          // superfluous?

}
