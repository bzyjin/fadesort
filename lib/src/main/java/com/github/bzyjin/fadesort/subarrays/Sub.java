package com.github.bzyjin.fadesort;
/** End of imports. */


/**
 * @author  bzyjin
 * @version 1.0
 *
 * A structure that represents a subarray.
 */
class Sub<T> {

    /** Store a reference to the superarray. */
    T[] arr;

    /** The interval bounds. */
    int left, right;

    /** Constructor. */
    Sub(T[] arr, int left, int right) {
        this.arr = arr;
        this.left = left;
        this.right = right;
    }

    /**
     * @return  the size of this Sub.
     */
    int size() {
        return this.right - this.left;
    }

}
