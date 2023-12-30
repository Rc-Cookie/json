package de.rccookie.json;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.rccookie.util.IterableIterator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents any json value like an object, an array, a string or
 * similar, or null, and works similar to {@link Optional}, with the
 * difference that no exception is thrown when trying to retrieve a
 * non-present value; instead <code>null</code> is returned.
 * It is helpful when working with data that may exist, but does not
 * have to, and in that case would get replaced by a default value.
 * <p>A json element cannot differ between no value being present
 * and the value <code>null</code> being present, both will be treated
 * as not present.</p>
 */
public class JsonElement implements Iterable<JsonElement>, JsonSerializable {

    /**
     * A json element with no value, i.e. null.
     */
    public static final JsonElement EMPTY = new JsonElement(null, JsonDeserializer.DEFAULT);

    private final Object value;
    private final JsonDeserializer deserializer;

    private JsonElement(Object value, JsonDeserializer deserializer) {
        this.value = value;
        this.deserializer = Objects.requireNonNull(deserializer, "deserializer");
    }

    @Override
    public Object toJson() {
        return value;
    }

    /**
     * Returns the deserializer used by this json element to deserialize values.
     *
     * @return The deserializer used
     */
    public JsonDeserializer getDeserializer() {
        return deserializer;
    }

    /**
     * Returns a json element which uses the given deserializer to deserialize its
     * value into a specific format. The deserializer will also be used for any
     * json elements created from the returned element, e.g. by calling {@link #get(int)}
     * or {@link #get(String)}.
     *
     * @param deserializer The deserializer to use
     * @return A json element using the given deserializer. Might be this json element if
     *         it is already using the given deserializer.
     */
    public JsonElement withDeserializer(JsonDeserializer deserializer) {
        if(this.deserializer == deserializer)
            return this;
        return new JsonElement(value, deserializer);
    }

    /**
     * Returns a json element which uses the {@link JsonDeserializer#STRICT}, or the
     * {@link JsonDeserializer#STRING_CONVERSION} deserializer to deserialize its
     * value into a specific format. The deserializer will also be used for any
     * json elements created from the returned element, e.g. by calling {@link #get(int)}
     * or {@link #get(String)}.
     *
     * @param strict Whether to enforce strict type checking
     * @return A json element using the specified deserializer. Might be this json element if
     *         it is already using the specified deserializer.
     * @see #withDeserializer(JsonDeserializer)
     */
    public JsonElement strict(boolean strict) {
        return withDeserializer(strict ? JsonDeserializer.STRICT : JsonDeserializer.STRING_CONVERSION);
    }

    /**
     * Returns the elements value, or null.
     *
     * @return The value of the json element
     */
    public Object get() {
        return value;
    }

    /**
     * Returns the elements value as an instance of the given type using the
     * annotated constructor or the deserializer registered using
     * {@link Json#registerDeserializer(Class, Function)} (see there for more info).
     * If no value is present,
     * <ul>
     *     <li>an empty array will be returned if the target type is an array type,</li>
     *     <li>otherwise <code>null</code> will be returned.</li>
     * </ul>
     * If you want <code>null</code> as the default value for an array type, use
     * <code>or(type, null)</code> instead.
     *
     * @param type The type to deserialize the value to
     */
    public <T> T as(Class<T> type) {
        return deserializer.deserialize(type, this);
    }

    /**
     * Returns the elements value as {@link JsonStructure}.
     *
     * @return The value of the json element as json structure
     * @throws ClassCastException If a value is present and is not convertible to a json structure
     */
    public JsonStructure asStructure() {
        return deserializer.asStructure(value);
    }

    /**
     * Returns the elements value as {@link JsonObject}.
     *
     * @return The value of the json element as json object
     * @throws ClassCastException If a value is present and is not convertible to a json object
     */
    public JsonObject asObject() {
        return deserializer.asObject(value);
    }

    /**
     * Returns the elements value as {@link JsonArray}.
     *
     * @return The value of the json element as json array
     * @throws ClassCastException If al value is present and is not a json array
     */
    public JsonArray asArray() {
        return deserializer.asArray(value);
    }

    /**
     * Returns an array containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to an array, or an empty array
     * @throws ClassCastException If a value is present and is not convertible to a json array
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T[] asArray(Class<T> contentType) {
        if(value == null)
            return (T[]) Array.newInstance(contentType, 0);
        JsonArray array = (JsonArray) value;
        T[] arr = (T[]) Array.newInstance(contentType, array.size());
        for(int i=0; i<array.size(); i++)
            arr[i] = deserializer.deserialize(contentType, array.getElement(i).withDeserializer(deserializer));
        return arr;
    }

    /**
     * Returns a list containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to a list, or an empty list
     * @throws ClassCastException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T> List<T> asList(Class<T> contentType) {
        if(value == null) return List.of();
        JsonArray array = (JsonArray) value;
        List<T> list = new ArrayList<>(array.size());
        for(int i=0; i<array.size(); i++)
            list.add(deserializer.deserialize(contentType, array.getElement(i).withDeserializer(deserializer)));
        return list;
    }

    /**
     * Returns a set containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to a set, or an empty set
     * @throws ClassCastException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T> Set<T> asSet(Class<T> contentType) {
        if(value == null) return Set.of();
        JsonArray array = (JsonArray) value;
        Set<T> set = new HashSet<>(array.size());
        for(int i=0; i<array.size(); i++)
            set.add(deserializer.deserialize(contentType, array.getElement(i).withDeserializer(deserializer)));
        return set;
    }

    /**
     * Returns a map containing all entries of the {@link JsonObject} contained
     * in this json element, each value being deserialized to the specified type.
     * If the value of the json element is <code>null</code>, an empty map will be
     * returned.
     *
     * @param valueType The type to deserialize the values to (the keys are strings)
     * @return The json object deserialized to a map, or an empty map
     * @throws ClassCastException If a value is present and is not convertible to a json object
     */
    @NotNull
    public <T> Map<String, T> asMap(Class<T> valueType) {
        if(value == null) return Map.of();
        JsonObject obj = (JsonObject) value;
        Map<String,T> map = new HashMap<>(obj.size());
        obj.forEach((k,v) -> map.put(k, wrap(v, deserializer).as(valueType)));
        return map;
    }

    /**
     * Deserializes this json data as a map. If the value of this json element is
     * a {@link JsonObject}, the keys will be deserialized into the key type and
     * the values to the value type. If the value of this json element is a {@link JsonArray},
     * the elements of the array must be key-value pairs as follows:
     * <pre>
     * [{ "key": 1, "value": "A" }, { "key": 2, "value": "B" }]
     * OR
     * [[1,"A"], [2,"B"]]
     * </pre>
     *
     * @param keyType The type to deserialize keys to
     * @param valueType The type to deserialize values to
     * @return The deserialized map, or an empty map
     * @throws ClassCastException If a value is present and is not convertible to a json object or array
     */
    public <K,V> Map<K,V> asMap(Class<K> keyType, Class<V> valueType) {
        if(value == null) return Map.of();
        Map<K, V> map = new HashMap<>(size());
        if(value instanceof JsonObject)
            forEach((k, v) -> map.put(wrap(k, deserializer).as(keyType), v.as(valueType)));
        else for(JsonElement entry : this) {
            if(entry.isArray())
                map.put(entry.get(0).as(keyType), entry.get(1).as(valueType));
            else map.put(entry.get("key").as(keyType), entry.get("value").as(valueType));
        }
        return map;
    }

    /**
     * Returns the elements value as {@link String}.
     *
     * @return The value of the json element as string
     * @throws ClassCastException If a value is present and is not convertible to a string
     */
    public String asString() {
        return deserializer.asString(value);
    }

    /**
     * Returns the elements value as a number.
     *
     * @return The value of the json element as number
     * @throws ClassCastException If a value is present and is not convertible to a number
     */
    public Number asNumber() {
        return deserializer.asNumber(value);
    }

    /**
     * Returns the elements value as {@code long}.
     *
     * @return The value of the json element as long
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a value is present and is not convertible to a number that can be converted to a long without data loss
     */
    public Long asLong() {
        return deserializer.asLong(value);
    }

    /**
     * Returns the elements value as {@code int}.
     *
     * @return The value of the json element as int
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a value is present and
     *                            is not convertible to a number
     */
    public Integer asInt() {
        return deserializer.asInt(value);
    }

    /**
     * Returns the elements value as {@code double}.
     *
     * @return The value of the json element as double
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a value is present and
     *                            is not convertible to a number
     */
    public Double asDouble() {
        return deserializer.asDouble(value);
    }

    /**
     * Returns the elements value as {@code float}.
     *
     * @return The value of the json element as float
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a value is present and
     *                            is not convertible to a number
     */
    public Float asFloat() {
        return deserializer.asFloat(value);
    }

    /**
     * Returns the elements value as {@code boolean}.
     *
     * @return The value of the json element as boolean
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a value is present and
     *                            is not convertible to a boolean
     */
    public Boolean asBool() {
        return deserializer.asBool(value);
    }


    /**
     * Returns {@code true} if this element contains no value or an instance of {@link JsonStructure}.
     *
     * @return Whether this element contains a json structure
     */
    public boolean isStructure() {
        return value == null || value instanceof JsonStructure;
    }

    /**
     * Returns {@code true} if this element contains no value or an instance of {@link JsonObject}.
     *
     * @return Whether this element contains a json object
     */
    public boolean isObject() {
        return value == null || value instanceof JsonObject;
    }

    /**
     * Returns {@code true} if this element contains no value or an instance of {@link JsonArray}.
     *
     * @return Whether this element contains a json array
     */
    public boolean isArray() {
        return value == null || value instanceof JsonArray;
    }

    /**
     * Returns {@code true} if this element contains no value or an instance of {@link String}.
     *
     * @return Whether this element contains a string
     */
    public boolean isString() {
        return value == null || value instanceof String;
    }

    /**
     * Returns {@code true} if this element contains no value or an instance of {@code Number}.
     *
     * @return Whether this element contains a number
     */
    public boolean isNumber() {
        return value == null || value instanceof Number;
    }

    /**
     * Returns {@code true} if this element contains no value, an instance of {@link Integer},
     * {@link Long}, {@link Short} or {@link Byte}, or an instance of {@link Double} or {@link Float}
     * which can be converted loss-less to a <code>long</code>.
     *
     * @return Whether this element contains an integer number
     */
    public boolean isInteger() {
        if(value == null || value instanceof Long || value instanceof Integer
            || value instanceof Byte || value instanceof Short) return true;
        if(value instanceof Double || value instanceof Float)
            return ((Number) value).doubleValue() == ((Number) value).longValue();
        return false;
    }

    /**
     * Returns {@code true} if this element contains no value or an instance of {@code boolean}.
     *
     * @return Whether this element contains a boolean
     */
    public boolean isBool() {
        return value == null || value instanceof Boolean;
    }


    /**
     * Returns a json element containing the mapping for the given key in the
     * map that is contained in this element.
     * <p>If this element does not have a non-null value, or the contained
     * object does not have a mapping for the specified key, an empty json
     * element will be returned.
     *
     * @param key The key to get the value for
     * @return The value mapped for the key, or an empty json element
     * @throws ClassCastException If a value is present and cannot be converted to a {@link JsonObject}
     * @throws NullPointerException If the value of this element is {@code null}
     */
    public JsonElement get(String key) {
        return value != null ? of(asObject().get(key), deserializer) : EMPTY;
    }

    /**
     * Returns a json element containing the value at the given index in the
     * array that is contained in this element.
     * <p>If this element does not have a non-null value, or the contained
     * array does not have the specified index, an empty json element will
     * be returned.
     *
     * @param index The index to get the value at
     * @return The value at that index, or an empty json element
     * @throws ClassCastException If a value is present and cannot be converted to a {@link JsonArray}
     */
    public JsonElement get(int index) {
        if(value == null) {
            if(index < 0) throw new IndexOutOfBoundsException(index);
            return JsonElement.EMPTY;
        }
        return of(asArray().get(index), deserializer);
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link JsonStructure}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The structure mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a json structure
     */
    public JsonStructure getStructure(String key) {
        return value != null ? (JsonStructure) asObject().get(key) : null;
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as {@link JsonStructure}. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The structure mapped at the specified index of the contained json array
     * @throws ClassCastException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a json structure
     */
    public JsonStructure getStructure(int index) {
        return value != null ? (JsonStructure) asArray().get(index) : null;
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link JsonObject}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The object mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a json object
     */
    public JsonObject getObject(String key) {
        return value != null ? asObject().getObject(key) : null;
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as {@link JsonObject}. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The object mapped at the specified index of the contained json array
     * @throws ClassCastException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a json object
     */
    public JsonObject getObject(int index) {
        return value != null ? asArray().getObject(index) : null;
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link JsonArray}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The array mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a json array
     */
    public JsonArray getArray(String key) {
        return value != null ? asObject().getArray(key) : null;
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as {@link JsonArray}. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The array mapped at the specified index of the contained json array
     * @throws ClassCastException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a json array
     */
    public JsonArray getArray(int index) {
        return value != null ? asArray().getArray(index) : null;
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link String}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The string mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a string
     */
    public String getString(String key) {
        return value != null ? asObject().getString(key) : null;
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as {@link String}. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The string mapped at the specified index of the contained json array
     * @throws ClassCastException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a string
     */
    public String getString(int index) {
        return value != null ? asArray().getString(index) : null;
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link Number}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The number mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a number
     */
    public Number getNumber(String key) {
        return value != null ? (Number) asObject().get(key) : null;
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as {@link Number}. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The number mapped at the specified index of the contained json array
     * @throws ClassCastException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a number
     */
    public Number getNumber(int index) {
        return value != null ? (Number) asArray().get(index) : null;
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as long. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The long mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a number that can be
     *                            converted to a long without loosing data
     */
    public Long getLong(String key) {
        return value != null ? asObject().getLong(key) : null;
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as long. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The long mapped at the specified index of the contained json array
     * @throws ClassCastException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a number that can be
     *                            converted to a long without loosing data
     */
    public Long getLong(int index) {
        return value != null ? asArray().getLong(index) : null;
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as int. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The int mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a number that can be
     *                            converted to an int without loosing data
     */
    public Integer getInt(String key) {
        return value != null ? asObject().getInt(key) : null;
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as int. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The int mapped at the specified index of the contained json array
     * @throws ClassCastException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a number that can be
     *                            converted to an int without loosing data
     */
    public Integer getInt(int index) {
        return value != null ? asArray().getInt(index) : null;
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as double. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The double mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a number that can be
     *                            converted to a double without loosing data
     */
    public Double getDouble(String key) {
        return value != null ? asObject().getDouble(key) : null;
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as double. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The double mapped at the specified index of the contained json array
     * @throws ClassCastException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a number that can be
     *                            converted to a double without loosing data
     */
    public Double getDouble(int index) {
        return value != null ? asArray().getDouble(index) : null;
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as float. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The float mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a number that can be
     *                            converted to a float without loosing data
     */
    public Float getFloat(String key) {
        return value != null ? asObject().getFloat(key) : null;
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as float. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The float mapped at the specified index of the contained json array
     * @throws ClassCastException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a number that can be
     *                            converted to a float without loosing data
     */
    public Float getFloat(int index) {
        return value != null ? asArray().getFloat(index) : null;
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as boolean. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The boolean value mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a boolean value
     */
    public Boolean getBool(String key) {
        return value != null ? asObject().getBool(key) : null;
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as boolean. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The boolean value mapped at the specified index of the contained json array
     * @throws ClassCastException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a boolean value
     */
    public Boolean getBool(int index) {
        return value != null ? asArray().getBool(index) : null;
    }

    /**
     * Returns a {@link JsonElement} with the value mapped at the specified
     * path, or an empty json element if that value does not exist.
     *
     * @param path The path of the value to get
     * @return A json element with the value, or empty
     */
    public JsonElement getPath(String path) {
        return value != null ? asStructure().getPath(path).withDeserializer(deserializer) : EMPTY;
    }

    /**
     * Returns a {@link JsonElement} with the value mapped at the specified
     * path, or an empty json element if that value does not exist.
     *
     * @param path The path of the value to get
     * @return A json element with the value, or empty
     */
    public JsonElement getPath(Object... path) {
        return value != null ? asStructure().getPath(path).withDeserializer(deserializer) : EMPTY;
    }

    /**
     * Returns a {@link JsonElement} which represents the value of this element
     * merged with the value of the other element:
     * <ul>
     *     <li>If one of the elements has no value present, the other value will
     *     be returned (although the returned json element will always use the
     *     deserializer of this element).</li>
     *     <li>If both elements contain json objects or arrays, an element containing
     *     the objects or arrays merged will be returned.</li>
     *     <li>If both elements contain primitive values (including strings), this
     *     element will be returned and the other element will be discarded.</li>
     *     <li>If one of the element contains a json structure and the other one a
     *     primitive value, or one contains an object and the other one an array,
     *     a {@link IllegalArgumentException} will be thrown.</li>
     * </ul>
     * The underlying value of this or the other json element will never be modified,
     * the returned object may contain a copy <b>or the original instance</b> if not modified.
     *
     * @param other The element to merge with
     * @return A json element containing the merge of this and the given element
     * @throws IllegalArgumentException If the elements cannot be merged
     */
    public JsonElement merge(@NotNull JsonElement other) {
        if(value == null)
            return other.withDeserializer(deserializer);
        if(value instanceof JsonStructure)
            return of(((JsonStructure) value).merge(other.value), deserializer);
        if(other.value instanceof JsonStructure)
            throw new IllegalArgumentException("Cannot merge "+value.getClass().getSimpleName()+" with "+other.value.getClass().getSimpleName());
        return this;
    }

    /**
     * Returns a json element with the given object merged recursively as follows:
     * <ul>
     *     <li>If the element has no value present or the given object is null, the other
     *     value will be returned.</li>
     *     <li>If only one of the objects has a given key, that key-value-pair will be
     *     in the result (copied).</li>
     *     <li>If both of the objects have a given key, but one of their values is null,
     *     the other value will be used (copied).</li>
     *     <li>If both of the objects have a json object or a json array mapped to a
     *     given key, the objects / arrays will be merged and that structure assigned to
     *     the key.</li>
     *     <li>If both of the objects have a primitive non-null value (including strings)
     *     mapped to a given key, this object's value will be used, the other object's
     *     value will be discarded.</li>
     *     <li>If both of the objects have a value, but one of them is a json structure and
     *     the other one a primitive type, or one of the values is an object and the other
     *     one an array, an {@link IllegalArgumentException} will be thrown</li>
     * </ul>
     * The underlying value of this or the other json element will never be modified,
     * the returned object may contain a copy <b>or the original instance</b> if not modified.
     *
     * @param other The json object to be merged with this one
     * @return A json element representing the merge of this element's object and the given object
     * @throws ClassCastException If a value is present and is not convertible to a json object
     * @throws IllegalArgumentException If the contained json object cannot be merged with the
     *                                  given object, e.g. because a json structure
     *                                  has to be merged with a primitive (or string)
     *                                  at top level or in a recursive merge
     */
    public JsonElement merge(@Nullable JsonObject other) {
        if(other == null) {
            asObject();
            return this;
        }
        return value != null ? of(asObject().merge(other), deserializer) : of(other, deserializer);
    }

    /**
     * Returns a new json element with the given array merged recursively as follows:
     * <ul>
     *     <li>If the element has no value present or the given array is null, the other
     *     value will be returned.</li>
     *     <li>If one of the arrays is longer than the other one, the extra items
     *     from the longer one will be included in the merge (as copy).</li>
     *     <li>If one of the arrays has null assigned to a given index, the other
     *     array's value at that index will be used (copied).</li>
     *     <li>If both of the arrays have a json object or a json array at a given index,
     *     the objects / arrays will be merged and that structure assigned to the index.</li>
     *     <li>If both of the arrays have a primitive non-null value (including strings)
     *     at a given index, this array's value will be used, the other array's value
     *     will be discarded.</li>
     *     <li>If both of the arrays have a value at the same index, but one of them is
     *     a json structure and the other one a primitive type, or one of the values is
     *     an object and the other one an array, an {@link IllegalArgumentException}
     *     will be thrown</li>
     * </ul>
     * The underlying value of this or the other json element will never be modified,
     * the returned object may contain a copy <b>or the original instance</b> if not modified.
     *
     * @param other The json array to be merged with this one
     * @return A deep copy of this json object with the given json object merged into it
     * @throws ClassCastException If a value is present and is not convertible to a json array
     * @throws IllegalArgumentException If the contained json array cannot be merged with the
     *                                  given array, e.g. because a json structure
     *                                  has to be merged with a primitive (or string)
     *                                  at top level or in a recursive merge
     */
    public JsonElement merge(@Nullable JsonArray other) {
        if(other == null) {
            asArray();
            return this;
        }
        return value != null ? of(asArray().merge(other), deserializer) : of(other, deserializer);
    }


    /**
     * Returns the value of this json element, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     */
    public Object or(Object ifNotPresent) {
        return value != null ? value : ifNotPresent;
    }

    /**
     * Returns the value of this json element as json structure, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws ClassCastException If a value is present and not convertible to a {@link JsonStructure}
     */
    public JsonStructure or(JsonStructure ifNotPresent) {
        return value != null ? asStructure() : null;
    }

    /**
     * Returns the value of this json element as json object, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws ClassCastException If a value is present and not convertible to a {@link JsonObject}
     */
    public JsonObject or(JsonObject ifNotPresent) {
        return value != null ? asObject() : null;
    }

    /**
     * Returns the value of this json element as json array, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws ClassCastException If a value is present and not convertible to a {@link JsonArray}
     */
    public JsonArray or(JsonArray ifNotPresent) {
        return value != null ? asArray() : null;
    }

    /**
     * Returns the value of this json element as an array, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present. Must not be null.
     * @return The value of the json element, or the specified value
     * @throws ClassCastException If a value is present and not convertible to a {@link JsonArray}
     *                            or an element of that array is not convertible to the array's component type
     */
    @SuppressWarnings("unchecked")
    public <T> T[] or(@NotNull T[] ifNotPresent) {
        return value != null ? asArray((Class<T>) Objects.requireNonNull(ifNotPresent, "ifNotPresent").getClass().getComponentType()) : ifNotPresent;
    }

    /**
     * Returns the value of this json element as a String, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws ClassCastException If a value is present and not convertible to a {@link String}
     */
    public String or(String ifNotPresent) {
        return value != null ? asString() : ifNotPresent;
    }

    /**
     * Returns the value of this json element as int, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws ClassCastException If a value is present and not convertible to an int
     */
    public int or(int ifNotPresent) {
        return value != null ? asInt() : ifNotPresent;
    }

    /**
     * Returns the value of this json element as long, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws ClassCastException If a value is present and not convertible to a long
     */
    public long or(long ifNotPresent) {
        return value != null ? asLong() : ifNotPresent;
    }

    /**
     * Returns the value of this json element as float, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws ClassCastException If a value is present and not convertible to a float
     */
    public float or(float ifNotPresent) {
        return value != null ? asFloat() : ifNotPresent;
    }

    /**
     * Returns the value of this json element as double, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws ClassCastException If a value is present and not convertible to a double
     */
    public double or(double ifNotPresent) {
        return value != null ? asDouble() : ifNotPresent;
    }

    /**
     * Returns the value of this json element as boolean, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws ClassCastException If a value is present and not convertible to a boolean
     */
    public boolean or(boolean ifNotPresent) {
        return value != null ? asBool() : ifNotPresent;
    }

    /**
     * Returns the value of this json element, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     */
    public <T> T or(Class<? extends T> type, T ifNotPresent) {
        return value != null ? as(type) : ifNotPresent;
    }

    /**
     * Deserializes this element's value if present, otherwise returns the value
     * returned by the specified supplier.
     *
     * @param getIfNotPresent Supplier for the value to use if no non-null value
     *                        is present
     * @return The json object's value deserialized to the specified type, or the return
     *         value from the supplier
     */
    public Object orGet(Supplier<?> getIfNotPresent) {
        return value != null ? value : getIfNotPresent;
    }

    /**
     * Returns the value of this json element, or the specified value if none is present.
     *
     * @param getIfNotPresent Supplier for the value to use if no non-null value
     *                        is present
     * @return The json object's value deserialized to the specified type, or the return
     *         value from the supplier
     */
    public <T> T orGet(Class<? extends T> type, Supplier<? extends T> getIfNotPresent) {
        return value != null ? as(type) : getIfNotPresent.get();
    }

    /**
     * Returns this element if a value is present, otherwise a json element with the
     * given value. If the alternative value is also a json element, it will be returned
     * directly in case of no value being present.
     *
     * @param ifNotPresent The value or json element to use if no value is present
     * @return This or an element with the given value / the given element
     */
    public JsonElement orElse(Object ifNotPresent) {
        return value != null ? this : wrap(ifNotPresent, deserializer);
    }

    /**
     * Returns this element if a value is present, otherwise a json element with the
     * value returned by the given supplier. If the returned value is also a json element,
     * it will be returned directly in case of no value being present
     *
     * @param getIfNotPresent Supplier for value or element to use if no value is present
     * @return This or the supplied value / element
     */
    public JsonElement orElseGet(Supplier<?> getIfNotPresent) {
        return value != null ? this : wrap(getIfNotPresent.get(), deserializer);
    }


    /**
     * Returns an optional containing this element if a value is present
     * or an empty optional if no value is present
     *
     * @return An optional as described above
     */
    public Optional<JsonElement> toOptional() {
        return value != null ? Optional.of(this) : Optional.empty();
    }

    /**
     * Returns whether a value is present.
     *
     * @return Whether a value is present
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * Returns whether no value is present.
     *
     * @return Whether no value is present
     */
    public boolean isEmpty() {
        return value == null;
    }


    /**
     * Returns an iterator iterating over the elements of the
     * json array contained in this element. If no value is present, an empty
     * iterator will be returned.
     *
     * @return An iterator over the element of the contained array
     * @throws ClassCastException If an element is present, but it
     *                            is not a json array
     */
    @NotNull
    @Override
    public Iterator<JsonElement> iterator() {
        return value != null ? asArray().elements() : IterableIterator.empty();
    }

    /**
     * Returns the keys of the json object contained in this element.
     * If no value is present, an empty iterator will be returned.
     *
     * @return The keys of the contained object
     * @throws ClassCastException If a value is present, but is not a json object
     */
    @NotNull
    public Set<String> keySet() {
        return value != null ? asObject().keySet() : Set.of();
    }

    /**
     * Returns the non-null values of the json object contained in this element.
     * If no value is present, an empty iterator will be returned.
     *
     * @return The non-null values of the contained object
     * @throws ClassCastException If a value is present, but is not a json object
     */
    @NotNull
    public Collection<JsonElement> values() {
        if(value == null) return List.of();
        return asObject().values().stream().filter(Objects::nonNull).map(o -> of(o, deserializer)).collect(Collectors.toList());
    }

    /**
     * Returns a stream over the elements of the json array contained in
     * this element. If no value is present, the stream will be empty.
     *
     * @return A stream over the elements of the contained array
     * @throws ClassCastException If an element is present, but it is not
     *                            a json array
     */
    @NotNull
    public Stream<JsonElement> stream() {
        return value == null ? Stream.empty() : asArray().stream().filter(Objects::nonNull).map(o -> of(o, deserializer));
    }

    /**
     * Returns the size of the contained json structure. If no value is present,
     * 0 will be returned.
     *
     * @return The size of the contained structure
     * @throws ClassCastException If an element is present, but it is
     *                            not a json structure
     */
    public int size() {
        return value != null ? asStructure().size() : 0;
    }

    /**
     * Returns, whether this json structure contains the given
     * object. In case of a json object this will be equivalent
     * to {@code containsKey(o)}. If no value is present, <code>false</code>
     * will be returned.
     *
     * @param o The object to check for containment for
     * @return Whether the contained json structure contains the
     *         specified element
     * @throws ClassCastException If an element is present, but it
     *                            is not a json structure
     */
    public boolean contains(Object o) {
        return value != null && asStructure().contains(o);
    }

    /**
     * Returns, whether this json object contains the given
     * key or index. If no value is present, <code>false</code>
     * will be returned.
     *
     * @param key The key to check for containment for
     * @return Whether the contained json object contains the key or index
     * @throws ClassCastException If an element is present, but it
     *                            is not a json object
     */
    public boolean containsKey(String key) {
        return value != null && asObject().containsKey(key);
    }

    /**
     * Returns, whether this json structure contains the given
     * value. In case of a json array this will be equivalent
     * to {@code contains(o)}. If no value is present, <code>false</code>
     * will be returned.
     *
     * @param o The object to check for containment for
     * @return Whether the contained json structure contains the
     *         specified element
     * @throws ClassCastException If an element is present, but it
     *                            is not a json structure
     */
    public boolean containsValue(Object o) {
        return value != null && asStructure().containsValue(o);
    }

    /**
     * Returns a string representation of this json element, that
     * is, the {@code toString()} value from it's contained object.
     *
     * @return The string representation of the contained object
     */
    @Override
    public String toString() {
        return Objects.toString(value);
    }

    /**
     * Iterates over the non-null entries of this json object. If no value
     * is present, no elements will be iterated.
     *
     * @param action The action to perform on each non-null key-value-pair
     * @throws ClassCastException If a value is present, but
     *                            it is not a json object
     */
    public void forEach(BiConsumer<? super String, ? super JsonElement> action) {
        if(value == null) return;
        asObject().forEach((k,v) -> {
            if(v != null)
                action.accept(k, of(v, deserializer));
        });
    }

    /**
     * Returns the specified object wrapped in a json element,
     * or an empty json element if the object has the value
     * {@code null}.
     *
     * @param o The object to wrap
     * @return A json element with the specified value, or an
     *         empty json element. If the parameter is a json element
     *         itself, it will be returned directly
     */
    public static JsonElement wrap(Object o) {
        return wrap(o, JsonDeserializer.DEFAULT);
    }

    public static JsonElement wrap(Object o, JsonDeserializer deserializer) {
        return of(Json.serialize(o), deserializer);
    }

    private static JsonElement of(Object o, JsonDeserializer deserializer) {
        return o != null ? new JsonElement(o, deserializer) : EMPTY;
    }
}
