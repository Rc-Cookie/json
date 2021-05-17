package com.github.rccookie.json;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.github.rccookie.json.Json.INDENT;

/**
 * Represents an abstract json array. A json array can hold any
 * type of value, but everything that is not a number, a boolean
 * value or {@code null} will be converted to a string when generating
 * the json string, using {@link Object#toString()}.
 * <p>Json arrays implement {@link java.util.List}, so unlike actual
 * arrays they are not of a fixed size.
 */
public class JsonArray extends ArrayList<Object> implements JsonElement {



    /**
     * Creates a new, empty json array.
     */
    public JsonArray() { }

    /**
     * Creates a new json array with all the content from the given
     * collection.
     *
     * @param copy The collection to copy
     */
    public JsonArray(Collection<?> copy) {
        super(copy);
    }

    /**
     * Creates a new json array by parsing the given json formatted
     * string. If the json string only contains "null" the json array
     * will be empty. If the file is not formatted properly in json
     * syntax an {@link JsonParseException} will be thrown.
     *
     * @param jsonString The json formatted string
     */
    public JsonArray(String jsonString) throws JsonParseException {
        this(Json.parseArray(jsonString));
    }

    /**
     * Creates a new json array by parsing the given json formatted
     * file. If the file only contains "null" or an
     * {@link java.io.IOException IOException} occurres during parsing,
     * the json array will be empty. If the file is not formatted
     * properly in json syntax an {@link JsonParseException} will be
     * thrown.
     *
     * @param file The file to load from
     */
    public JsonArray(File file) throws JsonParseException {
        load(file);
    }



    /**
     * Creates a shallow copy of this json array by creating a new
     * instance with the same content (which will not be cloned).
     *
     * @return A copy of this json array
     */
    @Override
    public JsonArray clone() {
        return new JsonArray(this);
    }

    /**
     * Converts this json array into a json string. Any values that are
     * not a number, a boolean, {@code false} or already a string will be
     * displayed as their {@link Object#toString()} value.
     * <p>If this array or any of the contained elements contains itself
     * a {@link NestedJsonException} will be thrown.
     *
     * @return The json string representing this array
     * @throws NestedJsonException If the json array or one of it's
     *                             contained json elements contains
     *                             itself
     */
    @Override
    public String toString() {
        return toString(Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    String toString(Set<Object> blacklist, int level) {
        if(isEmpty()) return "[]";

        blacklist.add(this);
        StringBuilder string = new StringBuilder();
        string.append('[');
        for(Object o : this) {
            if(blacklist.contains(o))
                throw new NestedJsonException();
            string.append('\n').append(INDENT.repeat(level + 1));
            string.append(Json.stringFor(o, blacklist, level + 1)).append(',');
        }

        string.deleteCharAt(string.length() - 1).append('\n').append(INDENT.repeat(level)).append(']');
        return string.toString();
    }



    /**
     * Returns the json object at the specified index.
     * <p>If the value at the specified index is not a json object,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The json object at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a json object
     */
    public JsonObject getObject(int index) throws NoSuchElementException {
        try {
            return (JsonObject) get(index);
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the json array at the specified index.
     * <p>If the value at the specified index is not a json array,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The json array at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a json array
     */
    public JsonArray getArray(int index) throws NoSuchElementException {
        try {
            return (JsonArray) get(index);
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the string at the specified index.
     * <p>If the value at the specified index is not a string,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The string at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a string
     */
    public String getString(int index) throws NoSuchElementException {
        try {
            return (String) get(index);
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the long at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The long at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public long getLong(int index) throws NoSuchElementException {
        try {
            return ((Number)get(index)).longValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the integer at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The integer at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public int getInt(int index) throws NoSuchElementException {
        try {
            return ((Number)get(index)).intValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the short at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The short at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public short getShort(int index) throws NoSuchElementException {
        try {
            return ((Number)get(index)).shortValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the byte at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The byte at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public byte getByte(int index) throws NoSuchElementException {
        try {
            return ((Number)get(index)).byteValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the double at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The double at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public double getDouble(int index) throws NoSuchElementException {
        try {
            return ((Number)get(index)).doubleValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the float at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The float at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public float getFloat(int index) throws NoSuchElementException {
        try {
            return ((Number)get(index)).floatValue();
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the boolean at the specified index.
     * <p>If the value at the specified index is not a boolean,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The boolean at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a boolean
     */
    public boolean getBool(int index) throws NoSuchElementException {
        try {
            return (boolean) get(index);
        } catch(ClassCastException | NullPointerException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the long at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The long at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public Long getNullableLong(int index) throws NoSuchElementException {
        try {
            Object num = get(index);
            return num != null ? ((Number)num).longValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the integer at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The integer at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public Integer getNullableInt(int index) throws NoSuchElementException {
        try {
            Object num = get(index);
            return num != null ? ((Number)num).intValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the short at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The short at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public Short getNullableShort(int index) throws NoSuchElementException {
        try {
            Object num = get(index);
            return num != null ? ((Number)num).shortValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the byte at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The byte at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public Byte getNullableByte(int index) throws NoSuchElementException {
        try {
            Object num = get(index);
            return num != null ? ((Number)num).byteValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the double at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The double at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public Double getNullableDouble(int index) throws NoSuchElementException {
        try {
            Object num = get(index);
            return num != null ? ((Number)num).doubleValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the float at the specified index.
     * <p>If the value at the specified index is not a number,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The float at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a number
     */
    public Float getNullableFloat(int index) throws NoSuchElementException {
        try {
            Object num = get(index);
            return num != null ? ((Number)num).floatValue() : null;
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the boolean at the specified index.
     * <p>If the value at the specified index is not a boolean,
     * a {@link NoSuchElementException} will be thrown.
     *
     * @param index The index to get the value at
     * @return The boolean at the given index
     * @throws NoSuchElementException If the value at the specified index
     *                                is not a boolean
     */
    public Boolean getNullableBool(int index) throws NoSuchElementException {
        try {
            return (Boolean) get(index);
        } catch(ClassCastException e) {
            throw new NoSuchElementException();
        }
    }



    /**
     * Assigns the value of the given json formatted file to this array.
     * If the file only contains "null" or an {@link java.io.IOException}
     * occurres, the content of this json array will only be cleared.
     *
     * @param file The file to load from
     * @return Weather any assignment was done
     * @throws JsonParseException If the file does not follow json syntax
     *                            or describes an object instead of an array
     */
    public boolean load(File file) throws JsonParseException {
        clear();
        JsonArray a = Json.loadArray(file);
        if(a == null) return false;
        addAll(a);
        return true;
    }

    /**
     * Stores this json array in the given file. The file will be cleared
     * if it exists, otherwise a new file will be created.
     *
     * @param file The file to store the array in
     * @return Weather the storing was successful
     */
    public boolean store(File file) {
        return Json.store(this, file);
    }
}
