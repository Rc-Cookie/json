package de.rccookie.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;

/**
 * Returns an iterator which is also an {@link Iterable} itself.
 *
 * @param <T> The type of elements to iterate over
 */
public interface IterableIterator<T> extends Iterable<T>, Iterator<T> {

    /**
     * An empty iterable iterator. Use {@link #empty()} to get it cast
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

        @Override
        public Spliterator<Object> spliterator() {
            return Spliterators.emptySpliterator();
        }

        @Override
        public void forEachRemaining(Consumer<? super Object> action) { }

        @Override
        public void forEach(Consumer<? super Object> action) { }
    };

    /**
     * Returns an iterator over the elements of this iterable, which
     * is the object itself.
     *
     * @return This iterator
     */
    @NotNull
    @Override
    default IterableIterator<T> iterator() {
        return this;
    }

    /**
     * Returns a sequential steam over all remaining elements iterator. If the stream requests
     * an element, it will not be available through {@link #next()} no more.
     *
     * @return A stream over all the iterators remaining elements
     */
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }


    /**
     * Returns an empty iterable iterator.
     *
     * @param <T> The type (has no effect)
     * @return {@link #EMPTY}
     */
    @SuppressWarnings("unchecked")
    @NotNull
    static <T> IterableIterator<T> empty() {
        return (IterableIterator<T>) EMPTY;
    }

    @NotNull
    static <T> IterableIterator<T> of(@NotNull Iterator<T> iterator) {
        if(Objects.requireNonNull(iterator, "iterator") instanceof IterableIterator)
            return (IterableIterator<T>) iterator;
        return new IterableIterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }
        };
    }

    @NotNull
    static <T> IterableIterator<T> iterator(@NotNull Iterable<T> iterable) {
        return of(Objects.requireNonNull(iterable, "iterable").iterator());
    }
}
