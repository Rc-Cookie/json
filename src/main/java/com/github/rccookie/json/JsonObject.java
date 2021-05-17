package com.github.rccookie.json;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.github.rccookie.json.Json.INDENT;

/**
 * Represents an abstract json object. A json object can hold any
 * type of value, but everything that is not a number, a boolean
 * value or {@code null} will be converted to a string when generating
 * the json string, using {@link Object#toString()}.
 */
public class JsonObject extends HashMap<String, Object> implements JsonElement {



    /**
     * Creates a new, empty json object.
     */
    public JsonObject() {

    }

    /**
     * Creates a new json object with the given mappings
     *
     * @param copy The map describing the mappings to do.
     */
    public JsonObject(Map<? extends String, ?> copy) {
        super(copy);
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



    /**
     * Returns the value associated with the given key. If no value is
     * associated with this key ({@code null} is a value) a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The value associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                in this json object
     */
    public Object get(String key) throws NoSuchElementException {
        if(!containsKey(key))
            throw new NoSuchElementException();
        return super.get(key);
    }

    /**
     * Returns the json object associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a json object, a {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The json object associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a json object
     */
    public JsonObject getObject(String key) throws NoSuchElementException {
        try {
            return (JsonObject) get(key);
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the json array associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a json array, a {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The json array associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a json array
     */
    public JsonArray getArray(String key) throws NoSuchElementException {
        try {
            return (JsonArray) get(key);
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the string associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a string, a {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The string associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a string
     */
    public String getString(String key) throws NoSuchElementException {
        try {
            return (String) get(key);
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the long associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} is not a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The long associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public long getLong(String key) throws NoSuchElementException {
        try {
            return ((Number)get(key)).longValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the integer associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} is not a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The integer associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public int getInt(String key) throws NoSuchElementException {
        try {
            return ((Number)get(key)).intValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the short associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} is not a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The short associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public short getShort(String key) throws NoSuchElementException {
        try {
            return ((Number)get(key)).shortValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the byte associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} is not a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The byte associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public byte getByte(String key) throws NoSuchElementException {
        try {
            return ((Number)get(key)).byteValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the double associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} is not a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The double associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public double getDouble(String key) throws NoSuchElementException {
        try {
            return ((Number)get(key)).doubleValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the float associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} is not a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The float associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public float getFloat(String key) throws NoSuchElementException {
        try {
            return ((Number)get(key)).floatValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the boolean associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a boolean ({@code null} is not a boolean), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The boolean associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a boolean
     */
    public boolean getBool(String key) throws NoSuchElementException {
        try {
            return (boolean) get(key);
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the long associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} IS a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The long associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public Long getNullableLong(String key) throws NoSuchElementException {
        try {
            Object num = get(key);
            return num != null ? ((Number)num).longValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the integer associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} IS a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The integer associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public Integer getNullableInt(String key) throws NoSuchElementException {
        try {
            Object num = get(key);
            return num != null ? ((Number)num).intValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the short associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} IS a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The short associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public Short getNullableShort(String key) throws NoSuchElementException {
        try {
            Object num = get(key);
            return num != null ? ((Number)num).shortValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the byte associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} IS a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The byte associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public Byte getNullableByte(String key) throws NoSuchElementException {
        try {
            Object num = get(key);
            return num != null ? ((Number)num).byteValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the double associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} IS a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The double associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public Double getNullableDouble(String key) throws NoSuchElementException {
        try {
            Object num = get(key);
            return num != null ? ((Number)num).doubleValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the float associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a number ({@code null} IS a number), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The float associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a number
     */
    public Float getNullableFloat(String key) throws NoSuchElementException {
        try {
            Object num = get(key);
            return num != null ? ((Number)num).floatValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the boolean associated with the given key. If no value is
     * associated with this key ({@code null} is a value) or the given value
     * is not a boolean ({@code null} IS a boolean), a
     * {@link NoSuchElementException} will be thrown.
     *
     * @param key The key to get the value for
     * @return The boolean associated with the key
     * @throws NoSuchElementException If no mapping for the given key exists
     *                                or the value is not a boolean
     */
    public Boolean getNullableBool(String key) throws NoSuchElementException {
        try {
            return (Boolean) get(key);
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
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
    public boolean store(File file) {
        return Json.store(this, file);
    }
}
