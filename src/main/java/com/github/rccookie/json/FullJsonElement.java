package com.github.rccookie.json;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents a json element with a value (possibly {@code null}) present.
 */
class FullJsonElement implements JsonElement {

    final Object value;

    FullJsonElement(Object value) {
        this.value = Json.extractJson(value);
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }

    @Override
    public Iterator<JsonElement> iterator() {
        return value == null ? EmptyJsonElement.EMPTY_ITERATOR : asArray().elements();
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super JsonElement> action) {
        JsonObject object = get();
        if(object != null)
            object.forEach((t, u) -> action.accept(t, JsonElement.wrapNullable(u)));
    }

    @Override
    public Object toJson() {
        return value;
    }

    @Override
    public <T> T as(Class<T> type) {
        return JsonDeserialization.deserialize(type, this);
    }

    @Override
    public Stream<JsonElement> stream() {
        return value == null ? Stream.empty() : StreamSupport.stream(spliterator(), false);
    }

    @Override
    public int size() {
        return value == null ? 0 : ((JsonStructure) value).size();
    }

    @Override
    public boolean contains(Object o) {
        return value != null && ((JsonStructure) value).contains(o);
    }

    @Override
    public boolean containsKey(String keyOrIndex) {
        return value != null && ((JsonStructure) value).containsKey(keyOrIndex);
    }

    @Override
    public boolean containsValue(Object o) {
        return value != null && ((JsonElement) value).containsValue(o);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof FullJsonElement && Objects.equals(((FullJsonElement) o).value, value))
                || Objects.equals(o, value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get() {
        return (T) value;
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
    public Number asNumber() {
        return (Number) value;
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
        return (Boolean) value;
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
    public boolean isNumber() {
        return value == null || value instanceof Number;
    }

    @Override
    public boolean isBool() {
        return value == null || value instanceof Boolean;
    }


    @Override
    public JsonElement get(String key) {
        return value == null ? JsonElement.EMPTY : ((JsonObject) value).getElement(key);
    }

    @Override
    public JsonElement get(int index) {
        if(value == null) {
            if(index < 0) throw new IndexOutOfBoundsException(index);
            return JsonElement.EMPTY;
        }
        return ((JsonArray) value).getElement(index);
    }

    @Override
    public JsonElement getPath(String path) {
        return value == null ? JsonElement.EMPTY : ((JsonStructure) value).getPath(path);
    }

    @Override
    public JsonElement getPath(Object... path) {
        if(path.length == 0) return this;
        return value == null ? JsonElement.EMPTY : ((JsonStructure) value).getPath(path);
    }


    @Override
    public <T> T or(T ifNotPresent) {
        return value == null ? Objects.requireNonNull(ifNotPresent) : get();
    }

    @Override
    public <T> T or(Class<T> type, T ifNotPresent) {
        return value == null ? Objects.requireNonNull(ifNotPresent) : as(type);
    }

    @Override
    public <T> T orGet(Supplier<T> getIfNotPresent) {
        return value == null ? Objects.requireNonNull(getIfNotPresent.get()) : get();
    }

    @Override
    public <T> T orGet(Class<T> type, Supplier<T> getIfNotPresent) {
        return value == null ? Objects.requireNonNull(getIfNotPresent.get()) : as(type);
    }

    @Override
    public <T> T orElse(JsonElement useIfNotPresent) {
        return value == null ? Objects.requireNonNull(useIfNotPresent).get() : get();
    }

    @Override
    public <T> T orElse(Class<T> type, JsonElement useIfNotPresent) {
        return value == null ? Objects.requireNonNull(useIfNotPresent).get() : as(type);
    }

    @Override
    public <T> T orElse(Supplier<JsonElement> useIfNotPresent) {
        return value == null ? Objects.requireNonNull(useIfNotPresent.get()).get() : get();
    }

    @Override
    public <T> T orElse(Class<T> type, Supplier<JsonElement> useIfNotPresent) {
        return value == null ? Objects.requireNonNull(useIfNotPresent.get()).get() : as(type);
    }

    @Override
    public <T> T nullOr(T ifNotPresent) {
        return get();
    }

    @Override
    public <T> T nullOr(Class<T> type, T ifNotPresent) {
        return as(type);
    }

    @Override
    public <T> T nullOrGet(Supplier<T> getIfNotPresent) {
        return get();
    }

    @Override
    public <T> T nullOrGet(Class<T> type, Supplier<T> getIfNotPresent) {
        return as(type);
    }

    @Override
    public <T> T nullOrElse(JsonElement useIfNotPresent) {
        return get();
    }

    @Override
    public <T> T nullOrElse(Class<T> type, JsonElement useIfNotPresent) {
        return as(type);
    }

    @Override
    public <T> T nullOrElse(Supplier<JsonElement> useIfNotPresent) {
        return get();
    }

    @Override
    public <T> T nullOrElse(Class<T> type, Supplier<JsonElement> useIfNotPresent) {
        return as(type);
    }

    @Override
    public <T> T orNull() {
        return get();
    }

    @Override
    public <T> T orNull(Class<T> type) {
        return as(type);
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
