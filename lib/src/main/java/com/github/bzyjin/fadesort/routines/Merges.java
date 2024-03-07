package com.github.bzyjin.fadesort;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.stream.IntStream;
import static java.lang.Math.max;
import static java.lang.Math.min;
/** End of imports. */


/**
 * @author  bzyjin
 * @version 1.0
 *
 * The current implementation of Fadesort merges.
 */
final class Merges<T> {

    private static final int PREFER_LINEAR_SEARCH = 24;

    /** Hide constructor. */
    private Merges() {}

    /**
     * @return  the index on [first]..[last] of the Sub in [runs] whose left
     *          value is closest to [find].
     */
    private static <T> int bisectRuns(Sub<T>[] runs, int first, int last,
            int find) {
        if (first >= last) return first;
        int i = first;

        // 1. a) Linear search
        if (last - first <= PREFER_LINEAR_SEARCH) {
            for (; i < last && runs[i].left < find; ++i);
        }

        // 1. b) Lower-bound binary search
        else {
            for (int d = last - first; d > 0; d >>= 1) {
                while (i + d < last && runs[i + d].left <= find) i += d;
            }
        }

        // 2. Choose closer of two run boundaries
        return i + (find - runs[i].left <= runs[i].right - find ? 0 : 1);
    }

    /**
     * @return  the radius of rotation for a rotate merge between two subarrays
     *          [a] and [b], according to [cmp].
     */
    private static <T> int mergeRadius(Sub<T> a, Sub<T> b,
            Comparator<? super T> cmp) {
        T[] arrA = a.arr,   arrB = b.arr;
        int i = a.right,    range = min(a.size(), b.size()),
            j = i - 1,      min = i - range;

        for (int d = range; d > 0; d >>= 1) {
            while (i - d >= min && cmp.compare(arrA[i - d], arrB[j + d]) > 0) {
                i -= d;
                j += d;
            }
        }

        return a.right - i;
    }

    /**
     * Merge two subarrays [a] and [b] into [out] where [b].arr == [out].arr,
     * according to [cmp].
     */
    static <T> void mergeUp2(Sub<T> a, Sub<T> b, Sub<T> out,
            Comparator<? super T> cmp) {
        T[] aux = a.arr,        arr = b.arr;        // destructuring
        int start = out.left,   end = out.right,    // ...
            l = a.left,         r = b.left,         // ...
            ml = a.right,       mr = b.right;       // ...

        for (int i = start; i < end && l < ml; ++i) {
            arr[i] = r >= mr || cmp.compare(aux[l], arr[r]) <= 0 ?
                    aux[l++] : arr[r++];
        }
    }

    /**
     * Merge two subarrays [a] and [b] into [out] where [a].arr == [out].arr,
     * according to [cmp].
     */
    static <T> void mergeDown2(Sub<T> a, Sub<T> b, Sub<T> out,
            Comparator<? super T> cmp) {
        T[] arr = a.arr,        aux = b.arr;        // destructuring
        int start = out.left,   end = out.right,    // ...
            l = a.right - 1,    r = b.right - 1,    // ...
            ml = a.left,        mr = b.left;        // ...

        for (int i = end - 1; i >= start && r >= mr; --i) {
            arr[i] = l >= ml && cmp.compare(arr[l], aux[r]) > 0 ?
                    arr[l--] : aux[r--];
        }
    }

    /**
     * Merge two subarrays [a] and [b] into [out] where [a].arr == [b].arr,
     * according to [cmp].
     */
    static <T> void mergeOut2(Sub<T> a, Sub<T> b, Sub<T> out,
            Comparator<? super T> cmp) {
        T[] aux = a.arr,        arr = out.arr;      // destructuring
        int start = out.left,   end = out.right,    // ...
            l = a.left,         r = b.left,         // ...
            ml = a.right,       mr = b.right;       // ...

        int i = start;
        for (; i < end && l < ml && r < mr; ++i) {
            arr[i] = cmp.compare(aux[l], aux[r]) <= 0 ? aux[l++] : aux[r++];
        }

        if (r == mr)    FadeSort.move(aux, l, ml, arr, i);
        else            FadeSort.move(aux, r, mr, arr, i);
    }

    /**
     * Merge two adjacent subarrays [a] and [b] into the same space, using [ext]
     * as a work array. This is done according to [cmp].
     *
     * @return  the resulting subarray as a Sub.
     */
    static <T> Sub<T> mergeIn2(Sub<T> a, Sub<T> b, T[] ext,
            Comparator<? super T> cmp) {
        T[] arr = a.arr;    // destructuring
        Sub res = Subs.join(a, b);

        // 1. Skip if only a swap needs to be done
        if (cmp.compare(arr[a.left], arr[b.right - 1]) > 0) {
            FadeSort.reverse(a);
            FadeSort.reverse(b);
            FadeSort.reverse(res);
            return res;
        }

        // 2. Get rotate merge radius and create new subarrays
        int rad = mergeRadius(a, b, cmp),   midA = a.right - rad,
                                            midB = b.left + rad;
        Sub<T>  s1 = new Sub(arr, a.left, midA),
                s2 = new Sub(arr, midA, a.right),
                s3 = new Sub(arr, b.left, midB),
                s4 = new Sub(arr, midB, b.right);

        // 3. Skip rotation if possible
        if (rad <= ext.length) {
            s2 = FadeSort.suspend(s2, ext);
            mergeDown2(s1, s3, a, cmp);
            mergeUp2(s2, s4, b, cmp);
        }

        // 4. Rotate and merge again
        else {
            FadeSort.swapBlocksSafe(arr, ext, midA, b.left, rad);
            mergeIn2(s1, s2, ext, cmp);
            mergeIn2(s3, s4, ext, cmp);
        }

        return res;
    }

    /**
     * Merge three adjacent subarrays [a], [b], and [c] into the same space by
     * merging [a] and [b] out and then merging the resulting subarray with [c].
     * This is done using [ext] as a work array, and according to [cmp].
     *
     * @return  the resulting subarray as a Sub.
     */
    static <T> Sub<T> mergeUp3(Sub<T> a, Sub<T> b, Sub<T> c,
            T[] ext, Comparator<? super T> cmp) {
        Sub<T>  ab  = new Sub(ext, 0, a.size() + b.size()),
                res = Subs.join(a, c);

        mergeOut2(a, b, ab, cmp);
        mergeUp2(ab, c, res, cmp);
        return res;
    }

    /**
     * Merge three adjacent subarrays [a], [b], and [c] into the same space by
     * merging [b] and [c] out and then merging the resulting subarray with [a].
     * This is done using [ext] as a work array, and according to [cmp].
     *
     * @return  the resulting subarray as a Sub.
     */
    static <T> Sub<T> mergeDown3(Sub<T> a, Sub<T> b, Sub<T> c, T[] ext,
            Comparator<? super T> cmp) {
        Sub<T>  bc  = new Sub(ext, 0, b.size() + c.size()),
                res = Subs.join(a, c);

        mergeOut2(b, c, bc, cmp);
        mergeDown2(a, bc, res, cmp);
        return res;
    }

    /**
     * Merge three adjacent subarrays [a], [b], and [c] into the same space,
     * using [ext] as a work array, and according to [cmp].
     *
     * @return  the resulting subarray as a Sub.
     */
    static <T> Sub<T> mergeIn3(Sub<T> a, Sub<T> b, Sub<T> c, T[] ext,
            Comparator<? super T> cmp) {
        int sA = a.size(),  sB = b.size(),  sC = c.size();

        // Not enough space to merge out
        if (min(sA, sC) + sB > ext.length) {
            return mergeIn2(mergeIn2(a, b, ext, cmp), c, ext, cmp);
        }

        // Enough space
        if (sA < sC)    return mergeUp3(a, b, c, ext, cmp);
        else            return mergeDown3(a, b, c, ext, cmp);
    }

    /**
     * Merge all runs in [runs] from [first] to [last] inclusive, using [ext] as
     * a work array, and according to [cmp].
     *
     * @return  the merged subarray as a Sub.
     */
    static <T> Sub<T> mergeRuns(Sub<T>[] runs, int first, int last,
            T[] ext, Comparator<? super T> cmp) {
        Sub all = Subs.join(runs[first], runs[last]);

        // 1. Too few runs to merge
        if (first >= last) return all;

        // 2. Identify indices of merge points
        int n = all.size(),         start = all.left,
            m = start + n / 2,      l = start + n / 4,
                                    r = start + 3 * n / 4;

        int mid     = bisectRuns(runs, first, last, m),     // [   |   ]
            left    = bisectRuns(runs, first, last, l),     // [ |     ]
            right   = bisectRuns(runs, first, last, r);     // [     | ]

        // 3. Collect array of unique indices
        int[] idx = IntStream
                .of(new int[] {first, left, mid, right, last + 1})
                .distinct()
                .toArray();

        // 4. Merge based on those indices
        switch (idx.length) {
            case 2: {
                mergeIn2(
                        mergeRuns(runs, idx[0], idx[1] - 1, ext, cmp),
                        runs[idx[1]],
                        ext, cmp);
                break;
            }

            case 3: {
                mergeIn2(
                        mergeRuns(runs, idx[0], idx[1] - 1, ext, cmp),
                        mergeRuns(runs, idx[1], idx[2] - 1, ext, cmp),
                        ext, cmp);
                break;
            }

            case 4: {
                mergeIn3(
                        mergeRuns(runs, idx[0], idx[1] - 1, ext, cmp),
                        mergeRuns(runs, idx[1], idx[2] - 1, ext, cmp),
                        mergeRuns(runs, idx[2], idx[3] - 1, ext, cmp),
                        ext, cmp);
                break;
            }

            default: {
                // Merge out left side
                if (runs[idx[2] - 1].right - runs[idx[0]].left <= ext.length) {
                    mergeUp3(
                            mergeRuns(runs, idx[0], idx[1] - 1, ext, cmp),
                            mergeRuns(runs, idx[1], idx[2] - 1, ext, cmp),
                            mergeRuns(runs, idx[2], idx[4] - 1, ext, cmp),
                            ext, cmp);
                }

                // Merge out right side
                else if (all.right - runs[idx[2]].left <= ext.length) {
                    mergeDown3(
                            mergeRuns(runs, idx[0], idx[2] - 1, ext, cmp),
                            mergeRuns(runs, idx[2], idx[3] - 1, ext, cmp),
                            mergeRuns(runs, idx[3], idx[4] - 1, ext, cmp),
                            ext, cmp);
                }

                // No space available for merging out
                else {
                    mergeIn2(
                            mergeRuns(runs, idx[0], idx[2] - 1, ext, cmp),
                            mergeRuns(runs, idx[2], idx[4] - 1, ext, cmp),
                            ext, cmp);
                }
                break;
            }

        }

        // 5. Return the merged subarray
        return all;
    }

}
