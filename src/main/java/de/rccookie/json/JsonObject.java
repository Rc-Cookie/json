package de.rccookie.json;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an abstract json object. A json object can hold any type of
 * valid json value, everything that is not a number, a boolean value, a
 * json structure or {@code null} has to implement {@link JsonSerializable}.
 * Like json objects this map does not allow the {@code null} key.
 */
public class JsonObject implements Map<String, Object>, JsonStructure {

    /**
     * Contents of the json object.
     */
    private final Map<String, Object> data = new LinkedHashMap<>();

    /**
     * Creates a new, empty json object.
     */
    public JsonObject() { }

    /**
     * Creates a new json object with the given mappings
     *
     * @param copy The map describing the mappings to do.
     */
    public JsonObject(Map<?,?> copy) {
        copy.forEach((k,v) -> put(Objects.toString(Objects.requireNonNull(k, "Json objects don't permit 'null' as key")), v));
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
        this(Json.parse(jsonString).asObject());
    }

    /**
     * Creates a new json object by parsing the given json formatted
     * file. If the file only contains "null" or an
     * {@link IOException IOException} occurs during parsing,
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
     * Creates a new json object from the given key-value pairs. There
     * must be exactly as many keys as values, and all keys have to be
     * strings. This method exists for convenient initialization of
     * json objects.
     *
     * @param keyValuePairs The keys and values, alternating
     * @throws IllegalArgumentException If there aren't as many keys as
     *                                  values
     * @throws ClassCastException If a key is not a string
     */
    public JsonObject(Object... keyValuePairs) {
        if(keyValuePairs.length % 2 != 0)
            throw new IllegalArgumentException("There must be exactly as many keys as values");
        for(int i=0; i<keyValuePairs.length; i+=2)
            put((String) keyValuePairs[i], keyValuePairs[i+1]);
    }



    /**
     * Creates a deep copy of this json object by creating a new
     * instance with the content also cloned.
     *
     * @return A copy of this json object
     */
    @Override
    public JsonObject clone() {
        JsonObject copy = new JsonObject();
        forEach((k,v) -> copy.put(k, v instanceof JsonStructure ? ((JsonStructure) v).clone() : v));
        return copy;
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
        return Json.toString(this);
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
        return Json.toString(this);
    }


    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return data.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return data.put(Objects.requireNonNull(key, "Json objects don't permit 'null' as key"), Json.serialize(value));
    }

    @Override
    public Object remove(Object key) {
        return data.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return data.keySet();
    }

    @NotNull
    @Override
    public Collection<Object> values() {
        return data.values();
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return data.entrySet();
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return data.putIfAbsent(Objects.requireNonNull(key, "Json objects don't permit 'null' as key"), Json.serialize(value));
    }

    @Override
    public Object compute(String key, @NotNull BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return data.compute(Objects.requireNonNull(key, "Json objects don't permit 'null' as key"), (k,v) -> Json.serialize(remappingFunction.apply(k,v)));
    }

    @Override
    public Object computeIfAbsent(String key, @NotNull Function<? super String, ?> mappingFunction) {
        return data.computeIfAbsent(Objects.requireNonNull(key, "Json objects don't permit 'null' as key"), k -> Json.serialize(mappingFunction.apply(k)));
    }

    @Override
    public Object computeIfPresent(String key, @NotNull BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return data.computeIfPresent(Objects.requireNonNull(key, "Json objects don't permit 'null' as key"), (k,v) -> Json.serialize(remappingFunction.apply(k,v)));
    }

    /**
     * Returns whether this json object contains the specified key.
     * This is equivalent to {@link #containsKey(Object)}.
     *
     * @param key The key to test for containment
     * @return Whether this json object contains the given key
     */
    @Override
    public boolean contains(Object key) {
        return containsKey(key);
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
        return JsonElement.wrap(get(key));
    }

    @Override
    public JsonElement getPath(String path) {
        return getPath(Json.parsePath(path));
    }

    @Override
    public JsonElement getPath(Object... path) {
        if(path.length == 0) return asElement();
        return getElement(path[0].toString()).getPath(Arrays.copyOfRange(path, 1, path.length));
    }



    public void combine(JsonObject other) {
        other.forEach((k,v) -> {
            if(v == null) putIfAbsent(k, null);
            else if(v instanceof JsonObject) {
                JsonObject current = getObject(k);
                if(current == null) put(k, v);
                else current.combine((JsonObject) v);
            }
            else if(v instanceof JsonArray) {
                JsonArray current = getArray(k);
                if(current == null) put(k, v);
                else current.combine((JsonArray) v);
            }
            else if(!containsKey(k)) put(k, v);
        });
    }

    @Override
    public JsonObject merge(@Nullable Object otherStructure) {
        if(otherStructure != null && !(otherStructure instanceof JsonObject))
            throw new IllegalArgumentException("Cannot merge JsonObject and "+otherStructure.getClass().getSimpleName());
        return merge((JsonObject) otherStructure);
    }

    @Override
    public JsonStructure filter(@Nullable Object filter, boolean allowMissing) {
        if(!(filter instanceof JsonObject))
            return this;

        JsonObject filtered = new JsonObject();
        ((JsonObject) filter).forEach((k,v) -> {
            if(!containsKey(k)) {
                if(!allowMissing)
                    throw new IllegalArgumentException("Key '"+k+"' from filter does not exist in data");
                return;
            }
            Object val = get(k);
            filtered.put(k, val instanceof JsonStructure ? ((JsonStructure) val).filter(v, allowMissing) : val);
        });
        return filtered;
    }

    /**
     * Returns a new json object with the given object merged recursively as follows:
     * <ul>
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
     * This instance itself will not be modified.
     *
     * @param other The json object to be merged with this one
     * @return A deep copy of this json object with the given json object merged into it
     * @throws IllegalArgumentException If this json object cannot be merged with the
     *                                  given object, e.g. because a json structure
     *                                  has to be merged with a primitive (or string)
     *                                  at top level or in a recursive merge
     */
    public JsonObject merge(@Nullable JsonObject other) {
        if(other == null)
            return clone();

        JsonObject merged = new JsonObject();
        for(String key : keySet()) {
            Object val = get(key);
            if(!other.containsKey(key))
                merged.put(key, val instanceof JsonStructure ? ((JsonStructure) val).clone() : val);
            else {
                Object otherVal = other.get(key);
                if(val == null)
                    merged.put(key, otherVal instanceof JsonStructure ? ((JsonStructure) otherVal).clone() : otherVal);
                else if(val instanceof JsonStructure)
                    merged.put(key, ((JsonStructure) val).merge(otherVal));
                else merged.put(key, val);
            }
        }
        other.forEach((k,v) -> {
            if(!merged.containsKey(k))
                merged.put(k, v instanceof JsonStructure ? ((JsonStructure) v).clone() : v);
        });
        return merged;
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
        return (JsonObject) get(key);
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
        return (JsonArray) get(key);
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
        return (String) get(key);
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
        return (Boolean) get(key);
    }



    /**
     * Assigns the value of the given json formatted file to this object.
     * If an {@link IOException} occurres, the content of this json object
     * will only be cleared.
     *
     * @param file The file to load from
     * @return Weather any assignment was done
     * @throws JsonParseException If the file does not follow json syntax
     * @throws NullPointerException If the file contains the content {@code null}
     * @throws ClassCastException If the file is valid but does not represent
     *                            a json object
     */
    @Override
    public boolean load(File file) throws JsonParseException {
        clear();
        try {
            putAll(Json.load(file).asObject());
            return true;
        } catch(UncheckedIOException e) {
            return false;
        }
    }

    @Override
    public boolean load(Path file) {
        clear();
        try {
            putAll(Json.load(file).asObject());
            return true;
        } catch(UncheckedIOException e) {
            return false;
        }
    }
}
