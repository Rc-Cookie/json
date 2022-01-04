package com.github.rccookie.json;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

class FullJsonElement implements JsonElement {

    final Object value;

    FullJsonElement(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FullJsonElement && Objects.equals(((FullJsonElement) o).value, value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public Object get() {
        return value;
    }

    @Override
    public JsonStructure asStructure() {
        return (JsonStructure) value;
    }

    @Override
    public JsonObject asObject() {
        return (JsonObject) value;
    }

    @Override
    public JsonArray asArray() {
        return (JsonArray) value;
    }

    @Override
    public String asString() {
        return (String) value;
    }

    @Override
    public Long asLong() {
        return value != null ? ((Number) value).longValue() : null;
    }

    @Override
    public Integer asInt() {
        return value != null ? ((Number) value).intValue() : null;
    }

    @Override
    public Double asDouble() {
        return value != null ? ((Number) value).doubleValue() : null;
    }

    @Override
    public Float asFloat() {
        return value != null ? ((Number) value).floatValue() : null;
    }

    @Override
    public Boolean asBool() {
        return value != null ? (boolean) value : null;
    }


    @Override
    public boolean isStructure() {
        return value == null || value instanceof JsonStructure;
    }

    @Override
    public boolean isObject() {
        return value == null || value instanceof JsonObject;
    }

    @Override
    public boolean isArray() {
        return value == null || value instanceof JsonArray;
    }

    @Override
    public boolean isString() {
        return value == null || value instanceof String;
    }

    @Override
    public boolean isLong() {
        return value == null || value instanceof Long;
    }

    @Override
    public boolean isInt() {
        return value == null || value instanceof Integer;
    }

    @Override
    public boolean isDouble() {
        return value == null || value instanceof Double;
    }

    @Override
    public boolean isFloat() {
        return value == null || value instanceof Float;
    }

    @Override
    public boolean isBool() {
        return value == null || value instanceof Boolean;
    }


    @Override
    public JsonElement get(String key) {
        return value == null ? EmptyJsonElement.INSTANCE : ((JsonObject) value).getElement(key);
    }

    @Override
    public JsonElement get(int index) {
        if(value == null) {
            if(index < 0) throw new IndexOutOfBoundsException(index);
            return EmptyJsonElement.INSTANCE;
        }
        return ((JsonArray) value).getElement(index);
    }

    @Override
    public JsonElement getPath(String path) {
        return value == null ? EmptyJsonElement.INSTANCE : ((JsonStructure) value).getPath(path);
    }

    @Override
    public JsonElement getPath(Object... path) {
        if(path.length == 0) return this;
        return value == null ? EmptyJsonElement.INSTANCE : ((JsonStructure) value).getPath(path);
    }


    @Override
    public JsonElement or(Object ifNotPresent) {
        return value == null ? new FullJsonElement(Objects.requireNonNull(ifNotPresent)) : this;
    }

    @Override
    public JsonElement orElse(JsonElement useIfNotPresent) {
        return value == null ? Objects.requireNonNull(useIfNotPresent) : this;
    }

    @Override
    public JsonElement orElse(Supplier<JsonElement> useIfNotPresent) {
        return value == null ? Objects.requireNonNull(useIfNotPresent.get()) : this;
    }

    @Override
    public JsonElement orGet(Supplier<?> getIfNotPresent) {
        return value == null ? new FullJsonElement(Objects.requireNonNull(getIfNotPresent.get())) : this;
    }

    @Override
    public JsonElement nullOr(Object ifNotPresent) {
        return this;
    }

    @Override
    public JsonElement nullOrElse(JsonElement useIfNotPresent) {
        return this;
    }

    @Override
    public JsonElement nullOrElse(Supplier<JsonElement> useIfNotPresent) {
        return this;
    }

    @Override
    public JsonElement nullOrGet(Supplier<?> getIfNotPresent) {
        return this;
    }

    @Override
    public JsonElement orNull() {
        return this;
    }

    @Override
    public Optional<JsonElement> toOptional() {
        return Optional.of(this);
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isNotNull() {
        return value != null;
    }
}
