package com.github.bzyjin.fadesort;
/** End of imports. */


/**
 * @author  bzyjin
 * @version 1.0
 *
 * A bare-bones implementation of a stack.
 */
class LiteStack<T> {

    /** The last added item. */
    private LiteStackNode<T> top = null;

    /**
     * Add an item [obj] to this stack.
     */
    void push(T obj) {
        LiteStackNode<T> node = new LiteStackNode(obj);

        node.next = this.top;
        this.top = node;
    }

    /**
     * Remove the last added item of this stack.
     *
     * @return  the item.
     * Preconditions:   this.hasNext()
     */
    T pop() {
        T res = this.top.val;
        this.top = this.top.next;
        return res;
    }

    /**
     * @return  the last added item of this stack.
     */
    T top() {
        return this.top.val;
    }

    /**
     * @return  true iff the stack has more items.
     */
    boolean hasNext() {
        return this.top != null;
    }

    /**
     * @return  true iff the stack has no more items.
     */
    boolean isEmpty() {
        return this.top == null;
    }

}


/**
 * A single node of a LiteStack.
 */
class LiteStackNode<T> {

    /** The object that this node holds. */
    T val                   = null;

    /** The next node. */
    LiteStackNode<T> next   = null;

    /** Constructor. */
    LiteStackNode(T val) {
        this.val = val;
    }

}
