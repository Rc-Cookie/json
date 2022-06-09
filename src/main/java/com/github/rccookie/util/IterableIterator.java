package com.github.rccookie.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Returns an iterator which is also an {@link Iterable} itself.
 *
 * @param <T> The type of elements to iterate over
 */
public interface IterableIterator<T> extends Iterable<T>, Iterator<T> {

    /**
     * An empty iterable iterator. Use {@link #empty()} to get it casted
     * to a specific type.
     */
    IterableIterator<?> EMPTY = new IterableIterator<>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }
    };

    /**
     * Returns an iterator over the elements of this iterable, which
     * is the object itself.
     *
     * @return This iterator
     */
    @Override
    default IterableIterator<T> iterator() {
        return this;
    }


    /**
     * Returns an empty iterable iterator.
     *
     * @param <T> The type (has no effect)
     * @return {@link #EMPTY}
     */
    @SuppressWarnings("unchecked")
    static <T> IterableIterator<T> empty() {
        return (IterableIterator<T>) EMPTY;
    }
}
