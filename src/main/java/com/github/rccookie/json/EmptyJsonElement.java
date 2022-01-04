package com.github.rccookie.json;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

enum EmptyJsonElement implements JsonElement {

    INSTANCE;

    @Override
    public String toString() {
        return "<No value>";
    }

    @Override
    public Object get() {
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
    public boolean isLong() {
        return false;
    }

    @Override
    public boolean isInt() {
        return false;
    }

    @Override
    public boolean isDouble() {
        return false;
    }

    @Override
    public boolean isFloat() {
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
    public JsonElement or(Object ifNotPresent) {
        return new FullJsonElement(Objects.requireNonNull(ifNotPresent));
    }

    @Override
    public JsonElement orElse(JsonElement useIfNotPresent) {
        return Objects.requireNonNull(Objects.requireNonNull(useIfNotPresent));
    }

    @Override
    public JsonElement orElse(Supplier<JsonElement> useIfNotPresent) {
        return Objects.requireNonNull(Objects.requireNonNull(useIfNotPresent.get()));
    }

    @Override
    public JsonElement orGet(Supplier<?> getIfNotPresent) {
        return new FullJsonElement(Objects.requireNonNull(getIfNotPresent.get()));
    }

    @Override
    public JsonElement nullOr(Object ifNotPresent) {
        return new FullJsonElement(ifNotPresent);
    }

    @Override
    public JsonElement nullOrElse(JsonElement useIfNotPresent) {
        return Objects.requireNonNull(useIfNotPresent);
    }

    @Override
    public JsonElement nullOrElse(Supplier<JsonElement> useIfNotPresent) {
        return Objects.requireNonNull(useIfNotPresent.get());
    }

    @Override
    public JsonElement nullOrGet(Supplier<?> getIfNotPresent) {
        return new FullJsonElement(getIfNotPresent.get());
    }

    @Override
    public JsonElement orNull() {
        return new FullJsonElement(null);
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
