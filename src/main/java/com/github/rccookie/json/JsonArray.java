package com.github.rccookie.json;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Supplier;

import static com.github.rccookie.json.Json.INDENT;

/**
 * Represents an abstract json array. A json array can hold any
 * type of value, but everything that is not a number, a boolean
 * value or {@code null} will be converted to a string when generating
 * the json string, using {@link Object#toString()}.
 * <p>Json arrays implement {@link java.util.List}, so unlike actual
 * arrays they are not of a fixed size.
 */
public class JsonArray extends ArrayList<Object> implements JsonStructure {



    private static final Supplier<Object> OUT_OF_BOUNDS_THROWER = () -> {
        throw new IndexOutOfBoundsException();
    };



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
     * @deprecated Use {@link #getElement(int)} instead for any json interaction.
     *             If the "pure" value is needed use {@link #getAnything(int)}.
     */
    @Override
    @Deprecated
    public Object get(int index) {
        // This method must return the actual value to conform as list. Otherwise
        // not the object that was set for a specific index would be returned, and
        // for indices out of bounds no IndexOutOfBoundsException would be thrown
        return super.get(index);
    }

    /**
     * Returns the value of the specified index, wrapped in a {@link JsonElement}
     * with the default value {@code null}. If the index is positively out of
     * bounds for this array an empty json element will be returned. If the index
     * is negative an {@link IndexOutOfBoundsException} will be thrown.
     * <p>This method never returns {@code null}.
     *
     * @param index The index to get the value for
     * @return A json element as described above
     *
     * @see #getOrDefault(int, Object)
     * @see #getOrDefaultGet(int, Supplier)
     */
    public JsonElement getElement(int index) {
        return getOrDefault(index, null);
    }

    /**
     * Returns the value of the specified index, wrapped in a {@link JsonElement}
     * with the given default value. If the index is positively out of
     * bounds for this array an empty json element will be returned. If the index
     * is negative an {@link IndexOutOfBoundsException} will be thrown.
     * <p>This method never returns {@code null}.
     *
     * @param index The index to get the value for
     * @param defaultValue The default value if at some point no value is present
     * @return A json element as described above
     *
     * @see #getOrDefault(int, Object)
     * @see #getOrDefaultGet(int, Supplier)
     */
    public JsonElement getOrDefault(int index, Object defaultValue) {
        return index < size() ? new JsonElement(super.get(index), defaultValue) : new JsonElement.EmptyJsonElement(defaultValue);
    }

    /**
     * Returns the value of the specified index, wrapped in a {@link JsonElement}
     * with the given default value generator. If the index is positively out of
     * bounds for this array an empty json element will be returned. If the index
     * is negative an {@link IndexOutOfBoundsException} will be thrown.
     * <p>This method never returns {@code null}.
     *
     * @param index The index to get the value for
     * @param defaultGetter A generator that will be used to generate a default
     *                      value if at some point no value is present and the
     *                      value is requested
     * @return A json element as described above
     *
     * @see #getOrDefault(int, Object)
     * @see #getOrDefaultGet(int, Supplier)
     */
    public JsonElement getOrDefaultGet(int index, Supplier<Object> defaultGetter) {
        return index < size() ? new JsonElement(super.get(index), defaultGetter) : new JsonElement.EmptyJsonElement(defaultGetter);
    }



    /**
     * Returns the object at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The object at the specified index
     */
    public Object getAnything(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asAnything();
    }

    /**
     * Returns the json object at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a json object
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The json object at the specified index
     */
    public JsonObject getObject(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asObject();
    }

    /**
     * Returns the json array at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a json array
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The json array at the specified index
     */
    public JsonArray getArray(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asArray();
    }

    /**
     * Returns the string at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a string
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The string at the specified index
     */
    public String getString(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asString();
    }

    /**
     * Returns the long at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a number
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The long at the specified index
     */
    public Long getLong(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asLong();
    }

    /**
     * Returns the integer at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a number
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The integer at the specified index
     */
    public Integer getInt(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asInt();
    }

    /**
     * Returns the short at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a number
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The short at the specified index
     */
    public Short getShort(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asShort();
    }

    /**
     * Returns the byte at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a number
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The byte at the specified index
     */
    public Byte getByte(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asByte();
    }

    /**
     * Returns the double at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a number
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The double at the specified index
     */
    public Double getDouble(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asDouble();
    }

    /**
     * Returns the float at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a number
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The float at the specified index
     */
    public Float getFloat(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asFloat();
    }

    /**
     * Returns the boolean at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a boolean
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The boolean at the specified index
     */
    public Boolean getBool(int index) {
        return getOrDefaultGet(index, OUT_OF_BOUNDS_THROWER).asBool();
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
    @Override
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
    @Override
    public boolean store(File file) {
        return Json.store(this, file);
    }
}
