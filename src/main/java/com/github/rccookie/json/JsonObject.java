package com.github.rccookie.json;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.rccookie.json.Json.INDENT;

/**
 * Represents an abstract json object. A json object can hold any
 * type of value, but everything that is not a number, a boolean
 * value or {@code null} will be converted to a string when generating
 * the json string, using {@link Object#toString()}. Like json objects
 * this map does not allow the {@code null} key.
 */
public class JsonObject extends HashMap<String, Object> implements JsonStructure {



    /**
     * Creates a new, empty json object.
     */
    public JsonObject() { }

    /**
     * Creates a new json object with the given mappings
     *
     * @param copy The map describing the mappings to do.
     */
    public JsonObject(Map<? extends String, ?> copy) {
        super(checkNoNullKeys(copy));
    }

    /**
     * Creates a new json object by parsing the given json formatted
     * string. If the json string only contains "null" the json object
     * will be empty. If the file is not formatted properly in json
     * syntax an {@link JsonParseException} will be thrown.
     *
     * @param jsonString The json formatted string
     */
    public JsonObject(String jsonString) throws JsonParseException {
        this(Json.parseObject(jsonString));
    }

    /**
     * Creates a new json object by parsing the given json formatted
     * file. If the file only contains "null" or an
     * {@link java.io.IOException IOException} occurres during parsing,
     * the json object will be empty. If the file is not formatted
     * properly in json syntax an {@link JsonParseException} will be
     * thrown.
     *
     * @param file The file to load from
     */
    public JsonObject(File file) throws JsonParseException {
        load(file);
    }



    /**
     * Creates a shallow copy of this json object by creating a new
     * instance with the same content (which will not be cloned).
     *
     * @return A copy of this json object
     */
    @Override
    public JsonObject clone() {
        return new JsonObject(this);
    }

    /**
     * Converts this json object into a json string. Any values that are
     * not a number, a boolean, {@code false} or already a string will be
     * displayed as their {@link Object#toString()} value.
     * <p>If this object or any of the contained elements contains itself
     * a {@link NestedJsonException} will be thrown.
     *
     * @return The json string representing this object
     * @throws NestedJsonException If the json object or one of it's
     *                             contained json elements contains
     *                             itself
     */
    @Override
    public String toString() throws NestedJsonException {
        return toString(Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    String toString(Set<Object> blacklist, int level) {
        if(isEmpty()) return "{}";

        StringBuilder string = new StringBuilder();
        string.append('{');
        blacklist.add(this);

        for(Entry<String, Object> member : entrySet()) {
            if (blacklist.contains(member.getValue()))
                throw new NestedJsonException();
            string.append('\n').append(INDENT.repeat(level + 1));
            string.append(Json.stringFor(member.getKey())).append(": ").append(Json.stringFor(member.getValue(), blacklist, level + 1)).append(',');
        }

        string.deleteCharAt(string.length() - 1).append('\n').append(INDENT.repeat(level)).append('}');
        blacklist.remove(this); // Allow same instances 'next to each other'
        return string.toString();
    }



    @Override
    public Object put(String key, Object value) {
        return super.put(Objects.requireNonNull(key), value);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        super.putAll(checkNoNullKeys(m));
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return super.putIfAbsent(Objects.requireNonNull(key), value);
    }

    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return super.compute(Objects.requireNonNull(key), remappingFunction);
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
        return super.computeIfAbsent(Objects.requireNonNull(key), mappingFunction);
    }

    @Override
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return super.computeIfPresent(Objects.requireNonNull(key), remappingFunction);
    }

    private static Map<? extends String, ?> checkNoNullKeys(Map<? extends String, ?> m) {
        m.forEach((k,$) -> Objects.requireNonNull(k));
        return m;
    }



    /**
     * @deprecated Use {@link #get(String)} instead for any json interaction.
     *             If the "pure" value is needed use {@link #getAnything(String)}.
     */
    @Override
    @Deprecated
    public Object get(Object key) {
        // This method must return the actual value to conform as map. Otherwise
        // not the object that was mapped would be returned
        return super.get(key);
    }

    /**
     * Returns the value mapped to the specified key, wrapped in a
     * {@link JsonElement} with the default value {@code null}. If no mapping
     * exists for the given key an empty json element will be returned.
     * <p>This method never returns {@code null}.
     *
     * @param key The key to get the value for
     * @return A json element as described above
     */
    public JsonElement get(String key) {
        return getOrDefault(key, null);
    }

    /**
     * Returns the value mapped to the specified key, wrapped in a
     * {@link JsonElement} with given the default value. If no mapping
     * exists for the given key an empty json element will be returned.
     * <p>This method never returns {@code null}.
     *
     * @param key The key to get the value for
     * @param defaultValue The default value returned if at some point
     *                     no value is present
     * @return A json element as described above
     */
    public JsonElement getOrDefault(String key, Object defaultValue) {
        return containsKey(Objects.requireNonNull(key)) ? new JsonElement(super.get(key), defaultValue) : new JsonElement.EmptyJsonElement(defaultValue);
    }

    /**
     * Returns the value mapped to the specified key, wrapped in a
     * {@link JsonElement} with given the default value generator. If no
     * mapping exists for the given key an empty json element will be
     * returned.
     * <p>This method never returns {@code null}.
     *
     * @param key The key to get the value for
     * @param defaultGetter A generator that will be used to generate a
     *                      default value if at some point no value is
     *                      present and the value is requested
     * @return A json element as described above
     */
    public JsonElement getOrDefaultGet(String key, Supplier<Object> defaultGetter) {
        return containsKey(Objects.requireNonNull(key)) ? new JsonElement(super.get(key), defaultGetter) : new JsonElement.EmptyJsonElement(defaultGetter);
    }



    /**
     * Returns the value mapped to the given key, or {@code null} if no mapping
     * exists.
     *
     * @param key The key to get the value for
     * @return The mapped value, or {@code null}
     */
    public Object getAnything(String key) {
        return get(key).asAnything();
    }

    /**
     * Returns the json object mapped to the given key, or {@code null} if no mapping
     * exists. If a mapping exists but the given value is not a json object,
     * a {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The mapped json object, or {@code null}
     */
    public JsonObject getObject(String key) {
        return get(key).asObject();
    }

    /**
     * Returns the json array mapped to the given key, or {@code null} if no mapping
     * exists. If a mapping exists but the given value is not a json array,
     * a {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The mapped json array, or {@code null}
     */
    public JsonArray getArray(String key) {
        return get(key).asArray();
    }

    /**
     * Returns the string to the given key, or {@code null} if no mapping
     * exists. If a mapping exists but the given value is not a string,
     * a {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The mapped string, or {@code null}
     */
    public String getString(String key) {
        return get(key).asString();
    }

    /**
     * Returns the long to the given key, or {@code null} if no mapping
     * exists. If a mapping exists but the given value is not a number,
     * a {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The mapped long, or {@code null}
     */
    public Long getLong(String key) {
        return get(key).asLong();
    }

    /**
     * Returns the integer to the given key, or {@code null} if no mapping
     * exists. If a mapping exists but the given value is not a number,
     * a {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The mapped integer, or {@code null}
     */
    public Integer getInt(String key) {
        return get(key).asInt();
    }

    /**
     * Returns the short to the given key, or {@code null} if no mapping
     * exists. If a mapping exists but the given value is not a number,
     * a {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The mapped short, or {@code null}
     */
    public Short getShort(String key) {
        return get(key).asShort();
    }

    /**
     * Returns the byte to the given key, or {@code null} if no mapping
     * exists. If a mapping exists but the given value is not a number,
     * a {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The mapped byte, or {@code null}
     */
    public Byte getByte(String key) {
        return get(key).asByte();
    }

    /**
     * Returns the double to the given key, or {@code null} if no mapping
     * exists. If a mapping exists but the given value is not a number,
     * a {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The mapped double, or {@code null}
     */
    public Double getDouble(String key) {
        return get(key).asDouble();
    }

    /**
     * Returns the float to the given key, or {@code null} if no mapping
     * exists. If a mapping exists but the given value is not a number,
     * a {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The mapped float, or {@code null}
     */
    public Float getFloat(String key) {
        return get(key).asFloat();
    }

    /**
     * Returns the boolean to the given key, or {@code null} if no mapping
     * exists. If a mapping exists but the given value is not a boolean,
     * a {@link ClassCastException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The mapped boolean, or {@code null}
     */
    public Boolean getBool(String key) {
        return get(key).asBool();
    }



    /**
     * Assigns the value of the given json formatted file to this object.
     * If the file only contains "null" or an {@link java.io.IOException}
     * occurres, the content of this json object will only be cleared.
     *
     * @param file The file to load from
     * @return Weather any assignment was done
     * @throws JsonParseException If the file does not follow json syntax
     *                            or describes an array instead of an object
     */
    @Override
    public boolean load(File file) throws JsonParseException {
        JsonObject o = Json.loadObject(file);
        clear();
        if(o == null || o.isEmpty()) return false;
        putAll(o);
        return true;
    }

    /**
     * Stores this json object in the given file. The file will be cleared
     * if it exists, otherwise a new file will be created.
     *
     * @param file The file to store the object in
     * @return Weather the storing was successful
     */
    @Override
    public boolean store(File file) {
        return Json.store(this, file);
    }
}
