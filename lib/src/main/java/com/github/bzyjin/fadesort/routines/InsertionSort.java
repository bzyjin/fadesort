package com.github.bzyjin.fadesort;

import java.util.Comparator;
/** End of imports. */


/**
 * @author  bzyjin
 * @version 1.0
 *
 * The current implementation of insertion sort.
 */
final class InsertionSort<T> {

    /** Hide constructor. */
    private InsertionSort() {}

    /**
     * Sort [arr] on [start]..<[end] with a classical implementation of guarded
     * insertion sort in ascending order dictated by [cmp].
     */
    static <T> void sort(T[] arr, int start, int end,
            Comparator<? super T> cmp) {
        for (int i = start + 1; i < end; ++i) {
            T ref = arr[i];

            int j = i;
            while (j != start && cmp.compare(ref, arr[j - 1]) < 0) {
                arr[j] = arr[--j];
            }

            arr[j] = ref;
        }
    }

    /**
     * Sort [sub] with a classical implementation of guarded insertion sort in
     * ascending order dictated by [cmp].
     */
    static <T> void sort(Sub<T> sub, Comparator<? super T> cmp) {
        sort(sub.arr, sub.left, sub.right, cmp);
    }

    /**
     * Sort [arr] on [start]..<[end] with a classical implementation of
     * unguarded insertion sort in ascending order dictated by [cmp].
     */
    static <T> void sortUnguarded(T[] arr, int start, int end,
            Comparator<? super T> cmp) {
        for (int i = start + 1; i < end; ++i) {
            T ref = arr[i];

            int j = i;
            while (cmp.compare(ref, arr[j - 1]) < 0) {
                arr[j] = arr[--j];
            }

            arr[j] = ref;
        }
    }

    /**
     * Sort [sub] with a classical implementation of unguarded insertion sort in
     * ascending order dictated by [cmp].
     */
    static <T> void sortUnguarded(Sub<T> sub, Comparator<? super T> cmp) {
        sortUnguarded(sub.arr, sub.left, sub.right, cmp);
    }

}
