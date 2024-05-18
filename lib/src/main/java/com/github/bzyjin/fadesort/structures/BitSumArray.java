package com.github.bzyjin.fadesort;
/** End of imports. */


/**
 * @author  bzyjin
 * @version 1.0
 *
 * A bit array backed by a prefix sum array for fast cardinality queries. Only
 * the barebones functionality necessary for block partitioning is implemented,
 * so this is NOT A FULL BIT ARRAY IMPLEMENTATION.
 */
class BitSumArray {

    private static final int    STRING_BITS = 6,
                                STRING_SIZE = 1 << STRING_BITS;

    private long[]  strings;        // container for bit strings
    private int     stringCount;    // number of bit strings
    private int[]   psa;            // prefix sum array data structure
            int     n;              // capacity

    /**
     * @return  the index in 'this.strings' that an overall index of [i]
     *          corresponds to.
     */
    private static int indexOfString(int i) {
        return i >> STRING_BITS;
    }

    /**
     * @return  the index within a string that an overall index of [i]
     *          corresponds to.
     */
    private static int indexInString(int i) {
        // return i % STRING_SIZE;
        return i - ((i >> STRING_BITS) << STRING_BITS);
    }

    /**
     * @return  a mask of 64 bits with the bit at index [i] set to 1 and the
     *          rest set to 0.
     */
    private static long mask(int i) {
        return 1L << i;
    }

    /** Constructor. */
    BitSumArray(int n) {
        this.n              = n;
        this.stringCount    = (n + STRING_SIZE - 1) >> STRING_BITS;
        this.strings        = new long[this.stringCount];
    }

    /**
     * Construct the PSA for fast sum queries.
     */
    void buildSums() {
        this.psa = new int[this.stringCount];

        this.psa[0] = Long.bitCount(this.strings[0]);
        for (int i = 1; i < this.stringCount; ++i) {
            this.psa[i] = Long.bitCount(this.strings[i]) + this.psa[i - 1];
        }
    }

    /**
     * Update the bit at index [i] to be 1 <=> true.
     */
    void set(int i) {
        this.strings[indexOfString(i)] |= mask(i);
    }

    /**
     * @return  true iff the bit at index [i] is 1 <=> true.
     * Preconditions:   0 <= indexOfString([i]) < this.stringCount
     */
    boolean get(int i) {
        return 0 != (this.strings[indexOfString(i)] & mask(i));
    }

    /**
     * @return  the number of bits set to 1 <=> true on the range 0..<[end].
     * Preconditions:   this.buildSums() has been called.
     */
    int cardinality(int end) {
        if (end <= 0) return 0;

        int stringIndex = indexOfString(end);
        return this.psa[stringIndex] -
                Long.bitCount(this.strings[stringIndex] >>> indexInString(end));
    }

}
