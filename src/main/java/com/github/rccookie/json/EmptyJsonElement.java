package com.github.rccookie.json;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Represents json elements with no value present.
 */
enum EmptyJsonElement implements JsonElement {

    INSTANCE;

    static final Iterator<JsonElement> EMPTY_ITERATOR = new Iterator<>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public JsonElement next() {
            throw new NoSuchElementException();
        }
    };

    @Override
    public String toString() {
        // The exception is better because toString() may not only be used for
        // debugging purposes but also to get the string representation without
        // calling get, which assumes that a value is present. Therefore, an
        // exception should be thrown if it is not.
        throw new NoSuchElementException("Cannot generate toString() value because no value is present");
//        return "<No value>";
    }

    @Override
    public Iterator<JsonElement> iterator() {
        return EMPTY_ITERATOR;
    }

    @Override
    public Stream<JsonElement> stream() {
        return Stream.empty();
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super JsonElement> action) {
        // No action
    }

    @Override
    public Object toJson() {
        throw new NoSuchElementException();
    }

    @Override
    public <T> T as(Class<T> type) {
        throw new NoSuchElementException("No data to deserialize present");
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public boolean containsKey(String keyOrIndex) {
        return false;
    }

    @Override
    public boolean containsValue(Object o) {
        return false;
    }

    @Override
    public <T> T get() {
        throw new NoSuchElementException();
    }

    @Override
    public JsonStructure asStructure() {
        throw new NoSuchElementException();
    }

    @Override
    public JsonObject asObject() {
        throw new NoSuchElementException();
    }

    @Override
    public JsonArray asArray() {
        throw new NoSuchElementException();
    }

    @Override
    public String asString() {
        throw new NoSuchElementException();
    }

    @Override
    public Number asNumber() {
        throw new NoSuchElementException();
    }

    @Override
    public Long asLong() {
        throw new NoSuchElementException();
    }

    @Override
    public Integer asInt() {
        throw new NoSuchElementException();
    }

    @Override
    public Double asDouble() {
        throw new NoSuchElementException();
    }

    @Override
    public Float asFloat() {
        throw new NoSuchElementException();
    }

    @Override
    public Boolean asBool() {
        throw new NoSuchElementException();
    }

    @Override
    public boolean isStructure() {
        return false;
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isBool() {
        return false;
    }

    @Override
    public JsonElement get(String key) {
        return this;
    }

    @Override
    public JsonElement get(int index) {
        if(index < 0) throw new IndexOutOfBoundsException(index);
        return this;
    }

    @Override
    public JsonElement getPath(String path) {
        return this;
    }

    @Override
    public JsonElement getPath(Object... path) {
        return this;
    }

    @Override
    public <T> T or(T ifNotPresent) {
        return Objects.requireNonNull(ifNotPresent);
    }

    @Override
    public <T> T orGet(Supplier<T> getIfNotPresent) {
        return Objects.requireNonNull(getIfNotPresent.get());
    }

    @Override
    public <T> T orElse(JsonElement useIfNotPresent) {
        return Objects.requireNonNull(useIfNotPresent.get());
    }

    @Override
    public <T> T orElse(Supplier<JsonElement> useIfNotPresent) {
        return Objects.requireNonNull(useIfNotPresent.get().get());
    }

    @Override
    public <T> T nullOr(T ifNotPresent) {
        return ifNotPresent;
    }

    @Override
    public <T> T nullOrGet(Supplier<T> getIfNotPresent) {
        return getIfNotPresent.get();
    }

    @Override
    public <T> T nullOrElse(JsonElement useIfNotPresent) {
        return useIfNotPresent.get();
    }

    @Override
    public <T> T nullOrElse(Supplier<JsonElement> useIfNotPresent) {
        return useIfNotPresent.get().get();
    }

    @Override
    public <T> T orNull() {
        return null;
    }

    @Override
    public Optional<JsonElement> toOptional() {
        return Optional.empty();
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isNotNull() {
        return false;
    }
}
