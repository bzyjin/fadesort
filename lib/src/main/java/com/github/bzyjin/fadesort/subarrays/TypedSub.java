package com.github.bzyjin.fadesort;
/** End of imports. */


/**
 * @author  bzyjin
 * @version 1.0
 *
 * A structure that represents a subarray that is either sorted non-descending,
 * descending, or unsorted.
 */
class TypedSub<T> extends Sub<T> {

    /** The state of the subarray. */
    Subs type;

    /** Constructor. */
    TypedSub(T[] arr, int left, int right, Subs type) {
        super(arr, left, right);
        this.type = type;
    }

}
