package com.github.bzyjin.fadesort;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import static java.lang.Math.max;
import static java.lang.Math.min;
/** End of imports. */


/**
 * @author  bzyjin
 * @version 1.0
 *
 * The current implementation of Fadesort. It uses a merge strategy targeting
 * three-way merges, and is backed by a block-partition quicksort for "unsorted"
 * data.
 */
final class FadeSort<T> {

    /** Minimum buffer size to mitigate performance degradation. */
    static final int MINIMUM_BUFFER_SIZE = 24;

    /** Hide constructor. */
    private FadeSort() {}

    //==========================================================================
    //                                                          UTILITY METHODS
    //==========================================================================

    /**
     * @return  the base 2 logarithm of [n] rounded down to the nearest integer.
     */
    static int log2F(int n) {
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    /**
     * @return  a buffer with minimum size of type <T>.
     */
    static <T> T[] getMinimalBuffer() {
        return (T[]) new Object[MINIMUM_BUFFER_SIZE];
    }

    /**
     * @return  the minimum length of a sorted Range to be considered for
     *          natural merging for an array of size [n].
     */
    static int minimumRunLength(int n) {
        return n <= 256 ? 4 + (n >> 6) : (int) (Math.sqrt(n) / 1);
    }

    /**
     * Swap items in [arr] at indices [i] and [j].
     */
    static <T> void swap(T[] arr, int i, int j) {
        T  tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    /**
     * Swap two blocks in [arr] of length [size] starting at index [i] and [j]
     * respectively, and do it with [ext] as a work array.
     */
    static <T> void swapBlocksSafe(T[] arr, T[] ext, int i, int j, int size) {
        int dst = j - i,
            max = ext.length;

        for (int k = size / max; k --> 0; i += max) {
            swapBlocks(arr, ext, i, i + dst, max);
        }
        swapBlocks(arr, ext, i, i + dst, size % max);
    }

    /**
     * Swap two blocks in [arr] of length [size] starting at index [i] and [j]
     * respectively, and do it with [ext] as a work array.
     *
     * Preconditions:   [size] <= [ext].length
     */
    static <T> void swapBlocks(T[] arr, T[] ext, int i, int j, int size) {
        suspend(arr, i, i + size, ext);
        move(arr, j, j + size, arr, i);
        move(ext, 0, size, arr, j);
    }

    /**
     * Reverse [arr] on [start]..<[end] in-place.
     */
    static <T> void reverse(T[] arr, int start, int end) {
        while (end --> start) {
            swap(arr, start++, end);
        }
    }

    /**
     * Reverse [sub] in-place.
     */
    static <T> void reverse(Sub<T> sub) {
        reverse(sub.arr, sub.left, sub.right);
    }

    /**
     * Put the subarray [start]..<[end] in [arr] into [ext] at index 0.
     *
     * @return  the resulting Sub in [ext].
     */
    static <T> Sub suspend(T[] arr, int start, int end, T[] ext) {
        move(arr, start, end, ext, 0);
        return new Sub(ext, 0, end - start);
    }

    /**
     * Put [sub] into [ext] at index 0.
     *
     * @return  the resulting Sub in [ext].
     */
    static <T> Sub suspend(Sub sub, T[] ext) {
        return suspend(sub.arr, sub.left, sub.right, ext);
    }

    /**
     * Move the subarray [start]..<[end] in [a] into [b] at index [i].
     */
    static <T> void move(T[] a, int start, int end, T[] b, int i) {
        System.arraycopy(a, start, b, i, end - start);
    }

    /**
     * @return  the lesser of [a] and [b], dictated by [cmp].
     */
    static <T> T minOf(T a, T b, Comparator<? super T> cmp) {
        return cmp.compare(a, b) < 0 ? a : b;
    }

    //==========================================================================
    //                                                         DRIVER ALGORITHM
    //==========================================================================

    /**
     * Sort [arr] ascending using the provided auxiliary array [ext] and a
     * comparator [cmp], on [start]..<[end].
     */
    static <T> void sort(T[] arr, T[] ext, int start, int end,
            Comparator<? super T> cmp) {
        // 1. Ensure inputs are valid
        if (start < 0) {
            throw new ArrayIndexOutOfBoundsException(start);
        }

        if (end > arr.length) {
            throw new ArrayIndexOutOfBoundsException(end);
        }

        if (start > end) {
            throw new IllegalArgumentException(
                    "start(" + start + ") > end(" + end + ")");
        }

        if (ext.length < MINIMUM_BUFFER_SIZE && ext.length < arr.length) {
            System.out.println(
                    "For performance, using minimum buffer size "
                    + MINIMUM_BUFFER_SIZE + ".");
            ext = FadeSort.<T>getMinimalBuffer();
        }

        // 2. Find all runs from left to right as TypedSubs
        int minLen = minimumRunLength(end - start);
        Sub[] runs = createRuns(arr, start, end, minLen, cmp);

        // 3. Sort the runs according to their type and filter them
        runs = formatRuns((TypedSub[]) runs, ext, start, cmp);

        // 4. Merge the runs
        Merges.mergeRuns(runs, 0, runs.length - 1, ext, cmp);
    }

    /**
     * Sort [arr] ascending using the provided auxiliary array [ext] and a
     * comparator [cmp] on 0..<[arr].length.
     */
    static <T> void sort(T[] arr, T[] ext, Comparator<? super T> cmp) {
        sort(arr, ext, 0, arr.length, cmp);
    }

    /**
     * Sort [arr] ascending by [cmp] on 0..<[arr].length.
     */
    static <T> void sort(T[] arr, Comparator<? super T> cmp) {
        // Since buffer is not provided, use the minimum space possible.
        sort(arr, FadeSort.<T>getMinimalBuffer(), 0, arr.length, cmp);
    }

    /**
     * Sort [lst] ascending using the provided auxiliary array [ext] and a
     * comparator [cmp], on 0..<[lst].size().
    */
    static <T> void sort(List<T> lst, T[] ext, Comparator<? super T> cmp) {
        T[] arr = lst.toArray((T[]) new Object[lst.size()]);
        sort(arr, ext, cmp);

        ListIterator i = lst.listIterator();
        for (int j = 0; j < arr.length; ++j) {
            i.next();
            i.set(arr[j]);
        }
    }

    /**
     * Sort [lst] ascending using a comparator [cmp], on 0..<[lst].size().
    */
    static <T> void sort(List<T> lst, Comparator<? super T> cmp) {
        sort(lst, FadeSort.<T>getMinimalBuffer(), cmp);
    }

    /**
     * Sort [arr] ascending using the provided auxiliary array [ext] on
     * [start]..<[end].
     */
    static <T extends Comparable<? super T>> void sort(T[] arr, T[] ext,
            int start, int end) {
        sort(arr, ext, start, end, (a, b) -> a.compareTo(b));
    }

    /**
     * Sort [arr] ascending using the provided auxiliary array [ext] on
     * 0..<[arr].length.
     */
    static <T extends Comparable<? super T>> void sort(T[] arr, T[] ext) {
        sort(arr, ext, 0, arr.length);
    }

    /**
     * Sort [arr] ascending by [cmp] on 0..<[arr].length.
     */
    static <T extends Comparable<? super T>> void sort(T[] arr) {
        // Since buffer is not provided, use the minimum space possible.
        sort(arr, (T[]) new Object[MINIMUM_BUFFER_SIZE]);
    }

    /**
     * Sort [lst] ascending using the provided auxiliary array [ext] and a
     * comparator [cmp], on 0..<[lst].size().
    */
    static <T extends Comparable<? super T>> void sort(List<T> lst, T[] ext) {
        sort(lst, ext, (a, b) -> a.compareTo(b));
    }

    /**
     * Sort [lst] ascending using a comparator [cmp], on 0..<[lst].size().
    */
    static <T extends Comparable<? super T>> void sort(List<T> lst) {
        sort(lst, (a, b) -> a.compareTo(b));
    }

    //==========================================================================
    //                                                   NATURAL INPUT HANDLING
    //==========================================================================

    /**
     * Sort [sub] using work array [ext], dictated by [cmp].
     */
    private static <T> void sortRun(TypedSub sub, T[] ext,
            Comparator<? super T> cmp) {
        switch (sub.type) {
            case NON_DESC: {
                return;
            }

            case DESC: {
                reverse(sub);
                return;
            }

            case UNSORTED: {
                QuickSort.sort(sub, ext, cmp);
                return;
            }
        }
    }

    /**
     * Sort all runs in [runs] ascending by [cmp], using [ext] as a work array.
     */
    private static <T> Sub<T>[] formatRuns(TypedSub<T>[] runs, T[] ext,
            int leftBound, Comparator<? super T> cmp) {
        if (runs.length == 0) return new Sub[0];

        T[] arr = runs[0].arr;  // save reference to arr for performance

        ArrayDeque<Sub> res = new ArrayDeque<>();
        res.offer(runs[0]);

        // 1. Sort and filter runs
        sortRun(runs[0], ext, cmp);
        for (int i = 1; i < runs.length; ++i) {
            Sub cur = runs[i];

            sortRun((TypedSub) cur, ext, cmp);
            if (cmp.compare(arr[cur.left - 1], arr[cur.left]) <= 0) {
                res.peekLast().right = cur.right;
            }

            else {
                res.offer(cur);
            }
        }

        return res.toArray(new Sub[res.size()]);
    }

    /**
     * Find the (left-first) next sorted subarray in [arr] on [start]..<[end],
     * according to [cmp].
     *
     * @return  that subarray as a TypedSub.
     */
    private static <T> TypedSub nextSortedSub(T[] arr, int start, int end,
            Comparator<? super T> cmp) {
        int i = start;
        boolean desc = cmp.compare(arr[i + 1], arr[i]) < 0;

        for (++i; i < end - 1; ++i) {
            if (cmp.compare(arr[i + 1], arr[i]) < 0 != desc) break;
        }

        return new TypedSub(arr, start, min(end, ++i),
                desc ? Subs.DESC : Subs.NON_DESC);
    }

    /**
     * Find the (left-first) next sorted subarray in [arr] on [start]..<[end],
     * according to [cmp] that has length at least [minLen]. Skip checking once
     * [minLen] is greater than the maximum size of this subarray.
     *
     * @return  that subarray as a TypedSub.
     * Preconditions:   [minLen] >= 2
     */
    private static <T> TypedSub nextRun(T[] arr, int start, int end,
            int minLen, Comparator<? super T> cmp) {
        while (start + minLen <= end) {
            TypedSub next = nextSortedSub(arr, start, end, cmp);

            if (next.size() >= minLen) return next;
            start = next.right;
        }

        return new TypedSub(arr, end, end, Subs.NON_DESC);
    }

    /**
     * Find all runs of all types from left to right in [arr] on [start]..<[end]
     * where sorted runs must be of length at least [minLen]. Sortedness is
     * dictated by [cmp].
     *
     * @return  those runs as TypedSub objects.
     */
    private static <T> TypedSub[] createRuns(T[] arr, int start, int end,
            int minLen, Comparator<? super T> cmp) {
        ArrayList<TypedSub> runs = new ArrayList<>();

        for (int i = start; i < end;) {
            TypedSub next = nextRun(arr, i, end, minLen, cmp);

            if (next.left != i) {
                TypedSub gap = new TypedSub(
                        arr, i, next.left, Subs.UNSORTED);
                runs.add(gap);
            }

            runs.add(next);
            i = next.right;
        }

        int len = runs.size();
        if (len >= 1 && runs.get(len - 1).left == end) {
            runs.remove(--len);
        }

        return runs.toArray(new TypedSub[len]);
    }

}
