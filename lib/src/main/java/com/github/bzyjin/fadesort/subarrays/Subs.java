package com.github.bzyjin.fadesort;
/** End of imports. */


/**
 * @author  bzyjin
 * @version 1.0
 *
 * Flags for the possible states of a subarray.
 */
enum Subs {

    DESC, NON_DESC, UNSORTED;

    /**
     * @return  a joined Sub from [a] and [b].
     * Preconditions:   [a] and [b] are adjacent with [a] on the left.
     *                  [a] and [b] are in the same array.
     */
    static <T> Sub<T> join(Sub<T> a, Sub<T> b) {
        return new Sub(a.arr, a.left, b.right);
    }

}
