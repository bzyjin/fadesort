package com.github.bzyjin.fadesort;
/** End of imports. */


/**
 * @author  bzyjin
 * @version 1.0
 *
 * A predicate, basically.
 */
interface Condition {

    /**
     * @return  true iff this predicate should be true for [x].
     */
    boolean at(int x);

}
