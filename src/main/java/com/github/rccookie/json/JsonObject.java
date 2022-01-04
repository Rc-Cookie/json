package com.github.rccookie.json;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

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
        this(Json.parseObjectString(jsonString));
    }

    /**
     * Creates a new json object by parsing the given json formatted
     * file. If the file only contains "null" or an
     * {@link java.io.IOException IOException} occurs during parsing,
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
        return toString(Json.DEFAULT_FORMATTED);
    }

    /**
     * Converts this json object into a json string. Any values that are
     * not a number, a boolean, {@code false} or already a string will be
     * displayed as their {@link Object#toString()} value.
     * <p>If this object or any of the contained elements contains itself
     * a {@link NestedJsonException} will be thrown.
     *
     * @param formatted Weather the json string should be formatted with
     *                  indents and newlines
     * @return The json string representing this object
     * @throws NestedJsonException If the json object or one of it's
     *                             contained json elements contains
     *                             itself
     */
    @Override
    public String toString(boolean formatted) {
        return toString(Collections.newSetFromMap(new IdentityHashMap<>()), formatted, 0);
    }

    String toString(Set<Object> blacklist, boolean formatted, int level) {
        if(isEmpty()) return "{}";

        StringBuilder string = new StringBuilder();
        string.append('{');
        blacklist.add(this);

        for(Entry<String, Object> member : entrySet()) {
            if (blacklist.contains(member.getValue()))
                throw new NestedJsonException();
            if(formatted) string.append('\n').append(INDENT.repeat(level + 1));
            string.append(Json.stringFor(member.getKey())).append(':');
            if(formatted) string.append(' ');
            string.append(Json.stringFor(member.getValue(), blacklist, formatted, level + 1)).append(',');
        }

        string.deleteCharAt(string.length() - 1);
        if(formatted) string.append('\n').append(INDENT.repeat(level));
        string.append('}');

        blacklist.remove(this); // Allow same instances 'next to each other'
        return string.toString();
    }



    @Override
    public Object put(String key, Object value) {
        return super.put(Objects.requireNonNull(key, "Json objects don't permit 'null' as key"), value);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        super.putAll(checkNoNullKeys(m));
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return super.putIfAbsent(Objects.requireNonNull(key, "Json objects don't permit 'null' as key"), value);
    }

    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return super.compute(Objects.requireNonNull(key, "Json objects don't permit 'null' as key"), remappingFunction);
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
        return super.computeIfAbsent(Objects.requireNonNull(key, "Json objects don't permit 'null' as key"), mappingFunction);
    }

    @Override
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return super.computeIfPresent(Objects.requireNonNull(key, "Json objects don't permit 'null' as key"), remappingFunction);
    }

    private static Map<? extends String, ?> checkNoNullKeys(Map<? extends String, ?> m) {
        m.forEach((k,$) -> Objects.requireNonNull(k));
        return m;
    }



    /**
     * Returns the value mapped to the specified key, wrapped in a
     * {@link JsonElement}. If no mapping exists for the given key an empty json
     * element will be returned.
     * <p>This method never returns {@code null}.
     *
     * @param key The key to get the value for
     * @return A json element as described above
     */
    public JsonElement getElement(String key) {
        return containsKey(key) ? new FullJsonElement(super.get(key)) : EmptyJsonElement.INSTANCE;
    }

    @Override
    public JsonElement getPath(String path) {
        return getPath(Json.parsePath(path));
    }

    @Override
    public JsonElement getPath(Object... path) {
        if(path.length == 0) return new FullJsonElement(this);
        return getElement(path[0].toString()).getPath(Arrays.copyOfRange(path, 1, path.length));
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
        return getElement(key).asObject();
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
        return getElement(key).asArray();
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
        return getElement(key).asString();
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
        return getElement(key).asLong();
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
        return getElement(key).asInt();
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
        return getElement(key).asDouble();
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
        return getElement(key).asFloat();
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
        return getElement(key).asBool();
    }



    /**
     * Assigns the value of the given json formatted file to this object.
     * If the file only contains "null" or an {@link java.io.IOException}
     * occurs, the content of this json object will only be cleared.
     * If the file defines a json array instead of an object, a
     * {@link ClassCastException} will be thrown.
     *
     * @param file The file to load from
     * @return Weather any assignment was done
     * @throws JsonParseException If the file does not follow json syntax
     */
    @Override
    public boolean load(File file) throws JsonParseException {
        JsonObject o = Json.parseObject(file);
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
