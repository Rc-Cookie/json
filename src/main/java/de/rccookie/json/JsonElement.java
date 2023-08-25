package de.rccookie.json;

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

    public static final JsonElement EMPTY = new JsonElement(null);

    private final Object value;

    private JsonElement(Object value) {
        this.value = value;
    }

    @Override
    public Object toJson() {
        return value;
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
        return JsonSerialization.deserialize(type, this);
    }

    /**
     * Returns the elements value as {@link JsonStructure}.
     *
     * @return The value of the json element as json structure
     * @throws ClassCastException If a value is present and is not a json structure
     */
    public JsonStructure asStructure() {
        return (JsonStructure) value;
    }

    /**
     * Returns the elements value as {@link JsonObject}.
     *
     * @return The value of the json element as json object
     * @throws ClassCastException If a value is present and is not a json object
     */
    public JsonObject asObject() {
        return (JsonObject) value;
    }

    /**
     * Returns the elements value as {@link JsonArray}.
     *
     * @return The value of the json element as json array
     * @throws ClassCastException If al value is present and is not a json array
     */
    public JsonArray asArray() {
        return (JsonArray) value;
    }

    /**
     * Returns a list containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to a list, or an empty list
     * @throws ClassCastException If a value is present and is not a json array
     */
    @NotNull
    public <T> List<T> asList(Class<T> contentType) {
        if(value == null) return List.of();
        JsonArray array = (JsonArray) value;
        List<T> list = new ArrayList<>(array.size());
        for(int i=0; i<array.size(); i++)
            list.add(JsonSerialization.deserialize(contentType, array.getElement(i)));
        return list;
    }

    /**
     * Returns a set containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to a set, or an empty set
     * @throws ClassCastException If a value is present and is not a json array
     */
    @NotNull
    public <T> Set<T> asSet(Class<T> contentType) {
        if(value == null) return Set.of();
        JsonArray array = (JsonArray) value;
        Set<T> set = new HashSet<>(array.size());
        for(int i=0; i<array.size(); i++)
            set.add(JsonSerialization.deserialize(contentType, array.getElement(i)));
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
     * @throws ClassCastException If a value is present and is not a json object
     */
    @NotNull
    public <T> Map<String, T> asMap(Class<T> valueType) {
        if(value == null) return Map.of();
        JsonObject obj = (JsonObject) value;
        Map<String,T> map = new HashMap<>(obj.size());
        obj.forEach((k,v) -> map.put(k, JsonElement.wrap(v).as(valueType)));
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
     * @throws ClassCastException If a value is present and is not a json object or array
     */
    public <K,V> Map<K,V> asMap(Class<K> keyType, Class<V> valueType) {
        if(value == null) return Map.of();
        Map<K, V> map = new HashMap<>(size());
        if(value instanceof JsonObject)
            forEach((k, v) -> map.put(JsonElement.wrap(k).as(keyType), v.as(valueType)));
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
     * @throws ClassCastException If a value is present and is not a string
     */
    public String asString() {
        return (String) value;
    }

    /**
     * Returns the elements value as a number.
     *
     * @return The value of the json element as number
     * @throws ClassCastException If a value is present and is not a number
     */
    public Number asNumber() {
        return (Number) value;
    }

    /**
     * Returns the elements value as {@code long}.
     *
     * @return The value of the json element as long
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a value is present and is not a number that can be converted to a long without data loss
     */
    public Long asLong() {
        if(value == null) return null;
        Number n = (Number) value;
        if(value instanceof Long || value instanceof Integer || n.doubleValue() == n.longValue())
            return n.longValue();
        throw new ClassCastException("Lossy conversion from "+n.getClass().getSimpleName()+" to Long");
    }

    /**
     * Returns the elements value as {@code int}.
     *
     * @return The value of the json element as int
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a value is present and
     *                            is not a number
     */
    public Integer asInt() {
        if(value == null) return null;
        Number n = (Number) value;
        if(value instanceof Integer || (value instanceof Long && n.longValue() == n.intValue())
            || ((value instanceof Double || value instanceof Float) && n.doubleValue() == n.intValue()))
            return n.intValue();
        throw new ClassCastException("Lossy conversion from "+n.getClass().getSimpleName()+" to Integer");
    }

    /**
     * Returns the elements value as {@code double}.
     *
     * @return The value of the json element as double
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a value is present and
     *                            is not a number
     */
    public Double asDouble() {
        return value != null ? ((Number) value).doubleValue() : null;
    }

    /**
     * Returns the elements value as {@code float}.
     *
     * @return The value of the json element as float
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a value is present and
     *                            is not a number
     */
    public Float asFloat() {
        if(value == null) return null;
        Number n = (Number) value;
        if(value instanceof Double && n.floatValue() != n.doubleValue())
            throw new ClassCastException("Lossy conversion from Double to Float");
        return n.floatValue();
    }

    /**
     * Returns the elements value as {@code boolean}.
     *
     * @return The value of the json element as boolean
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a value is present and
     *                            is not a boolean
     */
    public Boolean asBool() {
        return (Boolean) value;
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
     * @throws ClassCastException If a value is present and not an
     *                            instance of {@link JsonObject}
     * @throws NullPointerException If the value of this element is {@code null}
     */
    public JsonElement get(String key) {
        return value != null ? of(asObject().get(key)) : EMPTY;
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
     * @throws ClassCastException If a value is present and not an
     *                            instance of {@link JsonArray}
     */
    public JsonElement get(int index) {
        if(value == null) {
            if(index < 0) throw new IndexOutOfBoundsException(index);
            return JsonElement.EMPTY;
        }
        return asArray().getElement(index);
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link JsonStructure}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The structure mapped to the specified key of the contained json object
     * @throws ClassCastException If a value is present and not a json object, or
     *                            the mapped value is not a json structure
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
     * @throws ClassCastException If a value is present and not a json array, or
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
     * @throws ClassCastException If a value is present and not a json object, or
     *                            the mapped value is not a json object
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
     * @throws ClassCastException If a value is present and not a json array, or
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
     * @throws ClassCastException If a value is present and not a json object, or
     *                            the mapped value is not a json array
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
     * @throws ClassCastException If a value is present and not a json array, or
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
     * @throws ClassCastException If a value is present and not a json object, or
     *                            the mapped value is not a string
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
     * @throws ClassCastException If a value is present and not a json array, or
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
     * @throws ClassCastException If a value is present and not a json object, or
     *                            the mapped value is not a number
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
     * @throws ClassCastException If a value is present and not a json array, or
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
     * @throws ClassCastException If a value is present and not a json object, or
     *                            the mapped value is not a number that can be
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
     * @throws ClassCastException If a value is present and not a json array, or
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
     * @throws ClassCastException If a value is present and not a json object, or
     *                            the mapped value is not a number that can be
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
     * @throws ClassCastException If a value is present and not a json array, or
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
     * @throws ClassCastException If a value is present and not a json object, or
     *                            the mapped value is not a number that can be
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
     * @throws ClassCastException If a value is present and not a json array, or
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
     * @throws ClassCastException If a value is present and not a json object, or
     *                            the mapped value is not a number that can be
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
     * @throws ClassCastException If a value is present and not a json array, or
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
     * @throws ClassCastException If a value is present and not a json object, or
     *                            the mapped value is not a boolean value
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
     * @throws ClassCastException If a value is present and not a json array, or
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
        return value != null ? asStructure().getPath(path) : EMPTY;
    }

    /**
     * Returns a {@link JsonElement} with the value mapped at the specified
     * path, or an empty json element if that value does not exist.
     *
     * @param path The path of the value to get
     * @return A json element with the value, or empty
     */
    public JsonElement getPath(Object... path) {
        return value != null ? asStructure().getPath(path) : EMPTY;
    }


    /**
     * Returns the value of this json object, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json object, or the specified value
     */
    public Object or(Object ifNotPresent) {
        return value != null ? value : ifNotPresent;
    }

    /**
     * Returns the value of this json object, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json object, or the specified value
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
     * Returns the value of this json object, or the specified value if none is present.
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
        return value != null ? this : wrap(ifNotPresent);
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
        return value != null ? this : wrap(getIfNotPresent.get());
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
        return asObject().values().stream().filter(Objects::nonNull).map(JsonElement::of).collect(Collectors.toList());
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
        return value == null ? Stream.empty() : asArray().stream().filter(Objects::nonNull).map(JsonElement::of);
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
            if(v != null) action.accept(k, of(v));
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
        return of(Json.serialize(o));
    }

    private static JsonElement of(Object o) {
        return o != null ? new JsonElement(o) : EMPTY;
    }
}
