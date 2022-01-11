package com.github.rccookie.util;

import java.util.Iterator;

/**
 * Returns an iterator which is also an {@link Iterable} itself.
 *
 * @param <T> The type of elements to iterate over
 */
public interface IterableIterator<T> extends Iterable<T>, Iterator<T> {

    /**
     * Returns an iterator over the elements of this iterable, which
     * is the object itself.
     *
     * @return This iterator
     */
    @Override
    default Iterator<T> iterator() {
        return this;
    }
}
