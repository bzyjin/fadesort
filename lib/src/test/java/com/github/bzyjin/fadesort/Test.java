package com.github.bzyjin.fadesort;

import java.util.Arrays;
import java.util.Comparator;
import java.lang.Math;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** End of imports. */


class FadeSortTest {

    static Random rand = new Random();

    @Test void testStability() {
        System.out.println("Checking stability...");

        int n = 9 * 1000;

        Integer[]   seed    = new Integer[] {3, 0, 6, 4, 7, 8, 1, 5, 2},
                    arr     = new Integer[n],
                    to_sort = new Integer[n],
                    exp     = new Integer[n];

        for (int i = 0; i < n; ++i) {
            arr[i] = seed[i % 9];
        }

        for (int i = 0; i < n; ++i) {
            exp[i] = (i * 3 / n) * 3 + i % 3;
        }

        Comparator<Integer> cmp = (a, b) -> (int) a / 3 - (int) b / 3;

        Arrays.sort(arr, 979, 4934, cmp);

        for (int q = 1; n / q >= FadeSort.MINIMUM_BUFFER_SIZE; q <<= 1) {
            System.out.print("space = n / " + q + "");

            System.arraycopy(arr, 0, to_sort, 0, n);
            FadeSort.sort(to_sort, new Integer[n / q], cmp);

            assertTrue(Arrays.equals(to_sort, exp));

            System.out.println("\tsorted stably.");
        }

        seed = null;
        arr = null;
        to_sort = null;
        exp = null;
        System.gc();
    }

    @Test void testRegularSorting() {
        System.out.println("Testing sorting + performance...\n");

        int n = 1_000_000,
            tests = 10;

        Integer[] arr = new Integer[n];
        Integer[] aux = new Integer[256];

        // Initialize values uniformly
        for (int i = 0; i < n; ++i) {
            arr[i] = i + 1;
        }

        for (int test = 1; test <= tests; ++test) {
            System.out.println("(" + test + "/" + tests + ")");
            System.out.println("Creating inputs...");

            shuffle(arr, 0, n);

            // Create sorted runs
            for (int i = n; i > 50000; i >>= 1) {
                if (rand.nextBoolean()) Arrays.sort(arr, i >> 1, i);
            }

            System.out.println("Sorting...");

            FadeSort.sort(arr, aux, Comparator.<Integer>naturalOrder());

            checkSorted(arr, Comparator.<Integer>naturalOrder());
            System.out.println("Sorted correctly.\n");
        }

        arr = null;
        aux = null;
        System.gc();
    }

    <T> void checkSorted(T[] arr, Comparator<? super T> cmp) {
        int i = 1;
        for (; i < arr.length && cmp.compare(arr[i - 1], arr[i]) <= 0; ++i);

        // if (i != arr.length) System.out.println(Arrays.toString(arr));
        assertTrue(i == arr.length);
    }

    <T> void shuffle(T[] arr, int start, int end) {
        for (int i = start + 2; i < end; ++i) {
            int j = rand.nextInt(i - start) + start;

            T tmp   = arr[i];
            arr[i]  = arr[j];
            arr[j]  = tmp;
        }
    }
}
