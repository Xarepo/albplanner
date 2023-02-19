package ch.rfin.util;

import java.util.BitSet;
import java.util.stream.IntStream;
import java.math.BigInteger;
import static java.math.BigInteger.ZERO;

// Note: Currently merely wraps a BigInteger.
// So the implementation could probably be made more efficient.
/**
 * Immutable bit set.
 * @author Christoffer Fink
 */
public final class ImmutableBits implements Bits {

    public static final ImmutableBits empty = of(BigInteger.ZERO);

    private final BigInteger bits;

    private ImmutableBits(BigInteger bits) {
        assert bits != null;
        if (!(bits.compareTo(ZERO) >= 0)) {
            final String msg = String.format(
                    "bits (%s) must be >= 0",
                    bits);
            throw new IllegalArgumentException(msg);
        }
        this.bits = bits;
    }

    public static ImmutableBits empty() {
        return empty;
    }

    public static ImmutableBits of(BigInteger bits) {
        return new ImmutableBits(bits);
    }

    public static ImmutableBits of(long bits) {
        return of(BigInteger.valueOf(bits));
    }

    @Override
    public boolean isEmpty() {
        return bits.equals(BigInteger.ZERO);
    }
    @Override
    public int size() {
        return bits.bitCount();
    }

    @Override
    public ImmutableBits and(Bits set) {
        return of(bits.and(set.asBigInteger()));
    }
    @Override
    public ImmutableBits nand(Bits set) {
        throw new UnsupportedOperationException("Not implemented.");    // TODO
    }
    @Override
    public ImmutableBits or(Bits set) {
        return of(bits.or(set.asBigInteger()));
    }
    @Override
    public ImmutableBits xor(Bits set) {
        return of(bits.xor(set.asBigInteger()));
    }

    @Override
    public ImmutableBits set(int index) {
        return of(bits.setBit(index));
    }
    @Override
    public ImmutableBits flip(int index) {
        return of(bits.flipBit(index));
    }
    @Override
    public boolean get(int index) {
        return bits.testBit(index);
    }

    @Override
    public boolean intersects(Bits set) {
        return !ZERO.equals(bits.and(set.asBigInteger()));
    }
    
    /** Is this bit set a subset of the given set? */
    @Override
    public boolean subsetOf(Bits set) {
        return bits.equals(bits.and(set.asBigInteger()));
    }

    @Override
    public BitSet asBitSet() {
        //return BitSet.valueOf(bits.toByteArray());    // XXX: Does not work!
        // XXX: This works, but it seems inefficient (?)
        BitSet bitset = new BitSet(size());
        BigInteger tmp = bits;
        while (!tmp.equals(ZERO)) {
            final int i = tmp.getLowestSetBit();
            bitset.set(i);
            tmp = tmp.clearBit(i);
        }
        return bitset;
    }

    @Override
    public BigInteger asBigInteger() {
        return bits;
    }

    // Using default implementations of
    // Bits intersection(Bits set)
    // Bits union(Bits set)
    // IntStream stream()

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Bits)) {
            return false;
        }
        Bits other = (Bits) obj;
        return bits.equals(other.asBigInteger());
    }

    @Override
    public int hashCode() {
        return bits.hashCode();
    }

    @Override
    public String toString() {
        return asBitSet().toString();
    }

}
