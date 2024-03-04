package com.github.bzyjin.fadesort;

import java.util.Comparator;
import static java.lang.Math.max;
import static java.lang.Math.min;
/** End of imports. */


/**
 * @author  bzyjin
 * @version 1.0
 *
 * The current implementation of QuickSort. It is a stable block-partition
 * quicksort that uses a bit array and prefix sum array for block swapping.
 */
final class QuickSort<T> {

    /** Maximum length to be considered a small array. */
    private static final int        SMALL_ARRAY     = 24,

    /** Maximum number of samples allowed for pivot selection. */
                                    SAMPLE_SPACE    = 31,

    /** Minimum size of subarray to warrant local median pivot selection. */
                                    BIG_SELECTION   = 15,

    /** Maximum number of partitions to perform for a restore-partition. */
                                    PARTITION_LIMIT = 32;

    /** Predicates for sorting. */
    private static final Condition  SPOS    = (x) -> x > 0,
                                    POS     = (x) -> x >= 0;

    /** Pivot selection arrays to avoid temporary list creation. */
    private static final Object[]   PIVOTS  = new Object[SAMPLE_SPACE],
                                    TMP     = new Object[3],

    /** Holds temporary subarrays to avoid temporary list creation. */
                                    SUBARR  = new Object[PARTITION_LIMIT];

    /** Hide constructor. */
    private QuickSort() {}

    /**
     * @return  the number of samples to be used in pivot selection for an
     *          array of size [n].
     */
    private static int sampleSize(int n) {
        // return min(SAMPLE_SPACE, max(1, FadeSort.log2F(n) | 1));
        // (alternative equation)
        return min(SAMPLE_SPACE, max(1, 2 + (int) Math.cbrt(n) | 1));
    }

    /**
     * Sort [sub] ascending dictated by [cmp], using a given work array [ext].
     * This is a block partition quicksort implementation which uses a queue
     * rather than recursion.
     */
    static <T> void sort(Sub<T> sub, T[] ext, Comparator<? super T> cmp) {
        T[] arr = sub.arr;
        int guard = sub.left;

        LiteStack<Sub> parts = new LiteStack<>();
        parts.push(sub);

        // Maintain a stack instead of using recursion
        while (parts.hasNext()) {
            Sub<T> cur = parts.pop();

            // For small arrays, use insertion sort
            if (cur.size() <= SMALL_ARRAY) {
                if (cur.left == guard)  InsertionSort.sort(cur, cmp);
                else                    InsertionSort.sortUnguarded(cur, cmp);

                continue;
            }

            // Otherwise, partition
            T piv = selectPivot(arr, cur.left, cur.right, cmp);
            partition(parts, arr, ext, cur.left, cur.right, piv, cmp, SPOS);
        }
    }

    /**
     * Divide [arr] on [start]..<[end] into partitions by comparing with [piv]
     * according to [cmp]. Put elements that match [cnd] on the right; put on
     * the left otherwise. Append results into [parts].
     */
    private static <T> void partition(LiteStack<Sub> parts, T[] arr, T[] ext,
            int start, int end, T piv,
            Comparator<? super T> cmp, Condition cnd) {
        int x   = start,    // ptr in [arr] for items that satisfy [cnd]
            y   = 0,        // ptr in [ext] for items that do not satisfy [cnd]
            n   = end - start,          // size of subarray to sort
            w   = ext.length,           // size of work array
            cap = (n + w - 1) / w + 1,  // max # of blocks
            cnt = 0;                    // count # of blocks

        // 1. Partition into blocks
        BitPSA blocks = new BitPSA(cap);

        scan:
        for (int i = start;;) {
            for (y = 0;;) {
                if (i == end) break scan;
                T cur = arr[i++];

                if (cnd.at(cmp.compare(cur, piv))) {
                    ext[y++] = cur;
                    if (y == w) break;
                }
                else arr[x++] = cur;
            }

            int pre = (x - start) / w,  // # of preceding blocks
                dest = start + pre * w; // destination index

            // Shift the remainder right and copy partitioned block
            FadeSort.move(arr, dest, x, arr, dest + w);
            FadeSort.move(ext, 0, w, arr, dest);

            blocks.set(pre);
            cnt = pre + 1;
            x += w;
        }

        // 2. Special case: no deposits made
        if (cnt == 0 && y != 0 && x != start && cnd != POS) {
            restore(parts, ext, y, arr, x, cmp, SPOS);
            parts.push(new Sub(arr, start, x));
            return;
        }

        cnt = max(cnt, (x - start) / w);

        // 3. Move remainder of 1-blocks into [arr]
        FadeSort.move(ext, 0, y, arr, x);

        // 4. Get size of 0-blocks remainder and clean up the partition
        int rem = x - start - cnt * w,
            mid = arrangePartition(arr, ext, start, blocks, cnt, w, rem);

        // 5. Add necessary intervals to sort
        if (cnd == POS) {
            parts.push(new Sub(arr, start, mid));
        }

        else if (mid == end) {
            partition(parts, arr, ext, start, mid, piv, cmp, POS);
        }

        else {
            parts.push(new Sub(arr, mid, end));
            parts.push(new Sub(arr, start, mid));
        }
    }

    /**
     * Format the [blocks] in [arr] using [ext] as a work array. The block
     * partition starts at [start], has [cnt] blocks of size [w], and the
     * remainder of the partition is of size [r].
     *
     * @return  the starting index of the 1s subarray.
     */
    private static <T> int arrangePartition(T[] arr, T[] ext, int start,
            BitPSA blocks, int cnt, int w, int r) {
        if (cnt <= 0) return start + r;

        blocks.buildSums();
        int cnt0 = cnt - blocks.cardinality(cnt); // Number of zero blocks

        // 1. Apply cycle sort to blocks
        BitPSA status = new BitPSA(cnt);

        for (int i = 0; i < cnt0; ++i) {
            if (!status.get(i)) {
                status.set(i);
                int next = destination(blocks, i, cnt0);

                while (next != i) {
                    FadeSort.swapBlocks(arr, ext,
                            start + i * w, start + next * w, w);

                    status.set(next);
                    next = destination(blocks, next, cnt0);
                }
            }
        }

        // 2. Compute destination for remainder (end of 0-blocks)
        int end0 = start + cnt0 * w;
        if (r == 0) return end0;

        // 3. Rotate remainder to the end of 0-blocks
        int endBlocks = start + cnt * w;

        FadeSort.suspend(arr, endBlocks, endBlocks + r, ext);
        FadeSort.move(arr, end0, endBlocks, arr, end0 + r);
        FadeSort.move(ext, 0, r, arr, end0);

        // 4. Return the index of the start of 1-blocks
        return end0 + r;
    }

    /**
     * Repeatedly partition [ext] on 0..<[count] into [arr] on the interval
     * [ins]..<[ins] + [count]. Uses [cmp] to dictate comparisons, and puts
     * elements that DO NOT match [cnd] into [arr]. Pushes new partitioned
     * subarrays into [parts].
     */
    private static <T> void restore(LiteStack<Sub> parts, T[] ext, int count,
            T[] arr, int ins, Comparator<? super T> cmp, Condition cnd) {
        int end = ins + count - SMALL_ARRAY;

        if (ins >= end) {
            FadeSort.move(ext, 0, count, arr, ins);
            parts.push(new Sub(arr, ins, ins + count));
            return;
        }

        int cnt = 0;

        // 1. Partition external array into main array at most 3 times
        while (ins < end && cnt < PARTITION_LIMIT) {
            int y = 0;
            T piv = selectPivot(ext, 0, count, cmp);

            // a. Single partition
            for (int i = 0; i < count; ++i) {
                T cur = ext[i];

                if (cnd.at(cmp.compare(cur, piv)))  ext[y++]    = cur;
                else                                arr[ins++]  = cur;
            }

            // b. Update partitions
            SUBARR[cnt++] = new Sub(arr, ins - count + y, ins);
            count = y;
        }

        // 2. Copy the remaining elements as the last partition
        FadeSort.move(ext, 0, count, arr, ins);
        SUBARR[cnt++] = new Sub(arr, ins, ins + count);

        // 3. Add back the partitioned subarrays
        while (cnt --> 0) parts.push((Sub) SUBARR[cnt]);
    }

    /**
     * @return  the destination index for binary cycle sort from index [i],
     *          with [cnt0] zeroes and binary array context [blocks].
     */
    private static <T> int destination(BitPSA blocks, int i, int cnt0) {
        int offset = blocks.cardinality(i);
        return blocks.get(i) ? cnt0 + offset : i - offset;
    }

    /**
     * @return  an element that estimates the median element in [arr] on
     *          [start]..<[end], dictated by [cmp].
     */
    private static <T> T selectPivot(T[] arr, int start, int end,
            Comparator<? super T> cmp) {
        int amount = sampleSize(end - start),
            w = (end - start) / (amount + 1);

        for (int i = 1; i <= amount; ++i) {
            int ind = start + i * w;

            PIVOTS[i - 1] = amount >= BIG_SELECTION ?
                    localMedian(arr, ind, cmp) : arr[ind];
        }

        InsertionSort.sort((T[]) PIVOTS, 0, amount, cmp);
        return (T) PIVOTS[amount >> 1];
    }

    /**
     * @return  the median element in [arr] at indices [i] - 1, [i], and [i] + 1
     *          dictated by [cmp].
     * Preconditions:   [i] > 0 && [i] < [arr].length - 1
     */
    private static <T> T localMedian(T[] arr, int i,
            Comparator<? super T> cmp) {
        // 1. Get local values
        TMP[0] = arr[i - 1];
        TMP[1] = arr[i];
        TMP[2] = arr[i + 1];

        // 2. Sort with a 3-value sorting network; stability doesn't matter
        if (cmp.compare((T) TMP[0], (T) TMP[1]) > 0) FadeSort.swap(TMP, 0, 1);
        if (cmp.compare((T) TMP[0], (T) TMP[2]) > 0) FadeSort.swap(TMP, 0, 2);
        if (cmp.compare((T) TMP[1], (T) TMP[2]) > 0) FadeSort.swap(TMP, 1, 2);

        // 3. Return median element
        return (T) TMP[1];
    }

}
