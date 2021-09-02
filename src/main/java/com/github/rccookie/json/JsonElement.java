package com.github.rccookie.json;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents any json value like an object, an array, a string or
 * similar, or a non-existent value. It is helpful when working with
 * data that may exist, but does not have to, and in that case would
 * get replaced by a default value.
 */
// The main class always represents the state where the value is present,
// the subclass EmptyJsonElement always represents the state where no
// value is present.
public class JsonElement {

    private final Object value;
    final Object defaultValue;
    final Supplier<Object> defaultGetter;



    JsonElement(Object value) {
        this(value, (Object)null);
    }

    JsonElement(Object value, Object defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
        defaultGetter = null;
    }

    JsonElement(Object value, Supplier<Object> defaultGetter) {
        this.value = value;
        this.defaultGetter = Objects.requireNonNull(defaultGetter, "Default getter must not be null");
        defaultValue = null;
    }



    /**
     * Returns the elements value. If no value is present the default value
     * will be returned, or {@code null} if that was not specified.
     *
     * @return This element's value, or the default value
     */
    public Object asAnything() {
        return value;
    }

    /**
     * Returns the elements value as a json structure. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a json structure.
     *
     * @return This element's value, or the default value, as json structure
     */
    public JsonStructure asStructure() {
        return (JsonStructure) asAnything();
    }

    /**
     * Returns the elements value as a json object. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a json object.
     *
     * @return This element's value, or the default value, as json object
     */
    public JsonObject asObject() {
        return (JsonObject) asAnything();
    }

    /**
     * Returns the elements value as a json array. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a json array.
     *
     * @return This element's value, or the default value, as json array
     */
    public JsonArray asArray() {
        return (JsonArray) asAnything();
    }

    /**
     * Returns the elements value as a string. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a string.
     *
     * @return This element's value, or the default value, as string
     */
    public String asString() {
        return (String) asAnything();
    }

    /**
     * Returns the elements value as a long. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a number.
     *
     * @return This element's value, or the default value, as long
     */
    public Long asLong() {
        Object value = asAnything();
        return value != null ? ((Number) value).longValue() : null;
    }

    /**
     * Returns the elements value as an integer. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a number.
     *
     * @return This element's value, or the default value, as integer
     */
    public Integer asInt() {
        Object value = asAnything();
        return value != null ? ((Number) value).intValue() : null;
    }

    /**
     * Returns the elements value as a short. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a number.
     *
     * @return This element's value, or the default value, as short
     */
    public Short asShort() {
        Object value = asAnything();
        return value != null ? ((Number) value).shortValue() : null;
    }

    /**
     * Returns the elements value as a byte. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a number.
     *
     * @return This element's value, or the default value, as byte
     */
    public Byte asByte() {
        Object value = asAnything();
        return value != null ? ((Number) value).byteValue() : null;
    }

    /**
     * Returns the elements value as a double. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a number.
     *
     * @return This element's value, or the default value, as double
     */
    public Double asDouble() {
        Object value = asAnything();
        return value != null ? ((Number) value).doubleValue() : null;
    }

    /**
     * Returns the elements value as a float. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a number.
     *
     * @return This element's value, or the default value, as float
     */
    public Float asFloat() {
        Object value = asAnything();
        return value != null ? ((Number) value).floatValue() : null;
    }

    /**
     * Returns the elements value as a boolean. If no value is present
     * the default value will be returned, or {@code null} if that was not
     * specified.
     * <p>Throws a {@link ClassCastException} if the value, or the default
     * value if the value is not present, is not a boolean.
     *
     * @return This element's value, or the default value, as boolean
     */
    public Boolean asBool() {
        Object value = asAnything();
        return value != null ? (boolean) value : null;
    }

    public boolean isStructure() {
        Object value = asAnything();
        return value == null || value instanceof JsonStructure;
    }

    public boolean isObject() {
        Object value = asAnything();
        return value == null || value instanceof JsonObject;
    }

    public boolean isArray() {
        Object value = asAnything();
        return value == null || value instanceof JsonArray;
    }

    public boolean isString() {
        Object value = asAnything();
        return value == null || value instanceof String;
    }

    public boolean isLong() {
        Object value = asAnything();
        return value == null || value instanceof Long;
    }

    public boolean isInt() {
        Object value = asAnything();
        return value == null || value instanceof Integer;
    }

    public boolean isShort() {
        Object value = asAnything();
        return value == null || value instanceof Short;
    }

    public boolean isByte() {
        Object value = asAnything();
        return value == null || value instanceof Byte;
    }

    public boolean isDouble() {
        Object value = asAnything();
        return value == null || value instanceof Double;
    }

    public boolean isFloat() {
        Object value = asAnything();
        return value == null || value instanceof Float;
    }

    public boolean isBool() {
        Object value = asAnything();
        return value == null || value instanceof Boolean;
    }



    /**
     * Returns a json element containing the value that is mapped to
     * the given key in contained json object.
     * <p>If the json object does not have a mapping for the given key a
     * json element representing an unspecified value which will accept
     * further getting of keys and/or elements at indices and will return
     * the previously specified default value, will be returned.
     * <p>If this element already is an empty json element like described
     * above, it will return itself.
     * <p>If a value is present but is not a json object, a
     * {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The json element as described above. <b>Never {@code null}</b>
     */
    public JsonElement get(String key) {
        return defaultGetter != null ? asObject().getElementOrGet(key, defaultGetter) : asObject().getElementOr(key, defaultValue);
    }

    /**
     * Returns a json element containing the value that is at the specified
     * index in the contained json array.
     * <p>If the json array does not have the given index a
     * json element representing an unspecified value which will accept
     * further getting of keys and/or elements at indices and will return
     * the previously specified default value, will be returned.
     * <p>If this element already is an empty json element like described
     * above, it will return itself.
     * <p>If a value is present but is not a json array, a
     * {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value
     * @return The json element as described above. <b>Never {@code null}</b>
     */
    public JsonElement get(int index) {
        return defaultGetter != null ? asArray().getElementOrGet(index, defaultGetter) : asArray().getElementOr(index, defaultValue);
    }

    public JsonElement getPath(Object... path) {
        if(path.length == 0) return this;
        return defaultGetter != null ? asStructure().getPathOrGet(defaultGetter, path) : asStructure().getPathOr(defaultValue, path);
    }



    /**
     * Returns an optional containing itself if a value is present, otherwise
     * an empty optional.
     *
     * @return An optional containing itself if a value is present
     */
    public Optional<JsonElement> toOptional() {
        return Optional.of(this);
    }

    /**
     * Returns weather a value is present or if the default value would be used
     * ({@code null} is also a value, if it was mapped / set to the responsible
     * key / index).
     *
     * @return Weather a value is present
     */
    public boolean isPresent() {
        return true;
    }

    /**
     * If a value is present, its {@link Object#toString()} result will be
     * returned, otherwise the result for the default value will be returned,
     * or {@code "null"} if no default was specified.
     *
     * @return The toString return of the element's value or default value
     */
    @Override
    public String toString() {
        return Objects.toString(asAnything());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(asAnything());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        return Objects.equals(asAnything(), obj instanceof JsonElement ? ((JsonElement)obj).asAnything() : obj);
    }



    static class EmptyJsonElement extends JsonElement {

        EmptyJsonElement() {
            this((Object)null);
        }

        EmptyJsonElement(Object defaultValue) {
            super(defaultValue, defaultValue);
        }

        EmptyJsonElement(Supplier<Object> defaultGetter) {
            super(null, defaultGetter);
        }



        @Override
        public Object asAnything() {
            return defaultGetter != null ? defaultGetter.get() : defaultValue;
        }

        @Override
        public JsonElement get(String key) {
            if(key == null) throw new NullPointerException();
            return this;
        }

        @Override
        public JsonElement get(int index) {
            if(index < 0) throw new IndexOutOfBoundsException();
            return this;
        }

        @Override
        public JsonElement getPath(Object... path) {
            return this;
        }

        @Override
        public Optional<JsonElement> toOptional() {
            return Optional.empty();
        }

        @Override
        public boolean isPresent() {
            return false;
        }
    }
}
