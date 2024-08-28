package de.rccookie.json;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
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
    public static final JsonElement EMPTY = new JsonElement(null, JsonDeserializer.DEFAULT, "", null);

    private final Object value;
    private final JsonDeserializer deserializer;
    private final String path;
    private final GenericContext context;

    private JsonElement(Object value, JsonDeserializer deserializer, String path, GenericContext context) {
        this.value = value;
        this.deserializer = Objects.requireNonNull(deserializer, "deserializer");
        this.path = Objects.requireNonNull(path, "path");
        this.context = context;
    }

    @Override
    public Object toJson() {
        return value;
    }


    JsonElement pushGenericContext(TypeVariable<?>[] typeVariables, Type[] actualTypes) {
        if(typeVariables.length == 0) return this;
        if(context == null)
            return of(value, deserializer, path, new GenericContext()).pushGenericContext(typeVariables, actualTypes);
        context.pushAllTypeValues(typeVariables, actualTypes);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    JsonElement popGenericContext(TypeVariable<?>[] typeVariables) {
        if(typeVariables.length != 0)
            context.popAllTypeValues(typeVariables);
        return this;
    }

    JsonElement pushCurrentType(Type type) {
        if(context == null)
            return of(value, deserializer, path, new GenericContext()).pushCurrentType(type);
        context.pushCurrentType(type);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    JsonElement popCurrentType() {
        if(context != null)
            context.popCurrentType();
        return this;
    }

    Class<?> currentType() {
        if(context == null)
            throw new IllegalStateException("No deserialization context available");
        return context.currentType();
    }

    public JsonElement withContextOf(JsonElement contextJson) {
        return context == contextJson.context ? this : of(value, deserializer, path, contextJson.context);
    }

    Type getActualType(TypeVariable<?> typeVariable) {
        if(context == null)
            throw new IllegalArgumentException("Missing generic type information about "+typeVariable+" for "+typeVariable.getGenericDeclaration());
        return context.getActualType(typeVariable);
    }

    Class<?> resolveRawType(Type type) {
        if(type instanceof Class<?>)
            return (Class<?>) type;
        if(context == null)
            throw new IllegalArgumentException("Missing generic type information about "+type);
        return context.resolveRawType(type);
    }

    /**
     * Returns the actual values of the generic type parameter of the class that is currently being deserialized,
     * e.g. if serializing to <code>MyClass&le;String></code>, the return value would be <code>[String.class]</code>.
     * This can be used by custom deserializers to deserialize generic fields in a class.
     *
     * <p>If the current class is not generic, an empty array will be returned. If no deserialization is currently
     * in process, an {@link IllegalStateException} will be thrown.</p>
     *
     * @return The actual types of the generic type variables of the class currently being deserialized
     */
    public Type[] typeParameters() {
        return typeParameters(currentType());
    }

    /**
     * Returns the actual values of the generic type parameter of the class that is currently being deserialized,
     * e.g. if serializing to <code>MyClass&le;String></code>, the return value would be <code>[String.class]</code>.
     * This can be used by custom deserializers to deserialize generic fields in a class.
     *
     * <p>If the current class is not generic, an empty array will be returned. If no deserialization is currently
     * in process, an {@link IllegalStateException} will be thrown.</p>
     *
     * @return The actual types of the generic type variables of the class currently being deserialized
     */
    public Class<?>[] rawTypeParameters() {
        TypeVariable<?>[] typeParameters = currentType().getTypeParameters();
        Class<?>[] rawTypes = new Class<?>[typeParameters.length];
        for(int i=0; i<typeParameters.length; i++)
            rawTypes[i] = resolveRawType(getActualType(typeParameters[i]));
        return rawTypes;
    }

    private Type[] typeParameters(Class<?> rawType) {
        TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
        Type[] actualTypes = new Type[typeParameters.length];
        for(int i=0; i<typeParameters.length; i++)
            actualTypes[i] = getActualType(typeParameters[i]);
        return actualTypes;
    }

    /**
     * Returns the actual value of the single generic type parameter of the class that is currently being deserialized,
     * e.g. if serializing to <code>MyClass&le;String></code>, the return value would be <code>String.class</code>.
     * This can be used by custom deserializers to deserialize generic fields in a class with exactly one type parameter.
     *
     * <p>If the current class has no or more than 1 generic parameter, an {@link IllegalArgumentException} will be
     * thrown. If no deserialization is currently in process, an {@link IllegalStateException} will be thrown.</p>
     *
     * @return The actual type of the generic type variable of the class currently being deserialized
     */
    public Type typeParameter() {
        return typeParameter(currentType());
    }

    /**
     * Returns the actual value of the single generic type parameter of the class that is currently being deserialized,
     * e.g. if serializing to <code>MyClass&le;String></code>, the return value would be <code>String.class</code>.
     * This can be used by custom deserializers to deserialize generic fields in a class with exactly one type parameter.
     *
     * <p>If the current class has no or more than 1 generic parameter, an {@link IllegalArgumentException} will be
     * thrown. If no deserialization is currently in process, an {@link IllegalStateException} will be thrown.</p>
     *
     * @return The actual type of the generic type variable of the class currently being deserialized
     */
    public Class<?> rawTypeParameter() {
        return resolveRawType(typeParameter());
    }

    private Type typeParameter(Class<?> rawTypeWithOneGenericParam) {
        TypeVariable<?>[] typeParameters = rawTypeWithOneGenericParam.getTypeParameters();
        if(typeParameters.length != 1)
            throw new IllegalArgumentException("Expected type with 1 generic parameter, got "+rawTypeWithOneGenericParam.toGenericString());
        return getActualType(typeParameters[0]);
    }


    /**
     * Returns the path that was requested to receive this json element.
     *
     * @return The path of this json element
     */
    public String path() {
        return path;
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
        return of(value, deserializer, path, context);
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
        return deserializer.get(this, value);
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
     * @throws JsonDeserializationException If this json element could not be deserialized to the given type
     */
    public <T> T as(Class<T> type) {
        return type.cast(as((Type) type));
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
     * @param rawType The type to deserialize the value to
     * @param typeParameters Generic type parameters of the given (raw) type
     * @throws JsonDeserializationException If this json element could not be deserialized to the given type
     */
    @SuppressWarnings("unchecked")
    public <T> T as(Class<? super T> rawType, Type... typeParameters) {
        return (T) rawType.cast(as(type(rawType, typeParameters)));
    }

    private static Type type(Class<?> rawType, Type... typeParameters) {
        return typeParameters.length != 0 ? TypeBuilder.generic(Objects.requireNonNull(rawType, "rawType"), Objects.requireNonNull(typeParameters, "typeParameters")) : rawType;
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
     * @throws JsonDeserializationException If this json element could not be deserialized to the given type
     */
    @SuppressWarnings("unchecked")
    public <T> T as(Type type) {
        return (T) deserializer.deserialize(Objects.requireNonNull(type, "type"), this, value);
    }

    /**
     * Returns the elements value as {@link JsonStructure}.
     *
     * @return The value of the json element as json structure
     * @throws TypeMismatchException If a value is present and is not convertible to a json structure
     */
    public JsonStructure asStructure() {
        return deserializer.asStructure(this, value);
    }

    /**
     * Returns the elements value as {@link JsonObject}.
     *
     * @return The value of the json element as json object
     * @throws TypeMismatchException If a value is present and is not convertible to a json object
     */
    public JsonObject asObject() {
        return deserializer.asObject(this, value);
    }

    /**
     * Returns the elements value as {@link JsonArray}.
     *
     * @return The value of the json element as json array
     * @throws TypeMismatchException If al value is present and is not a json array
     */
    public JsonArray asArray() {
        return deserializer.asArray(this, value);
    }

    /**
     * Returns an array containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to an array, or an empty array
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T> T[] asArray(Class<T> contentType) {
        return asArray0(contentType, contentType);
    }

    /**
     * Returns an array containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to an array, or an empty array
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @NotNull
    public <T> T[] asArray(Class<? super T> contentType, Type... typeParameters) {
        return (T[]) as(contentType, type(Objects.requireNonNull(contentType, "contentType"), Objects.requireNonNull(typeParameters, "typeParameters")));
    }

    /**
     * Returns an array containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to an array, or an empty array
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @NotNull
    public <T> T[] asArray(Type contentType) {
        return (T[]) as(resolveRawType(Objects.requireNonNull(contentType, "contentType")), contentType);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private <T> T[] asArray0(Class<? super T> contentType, Type genericContentType) {
        if(value == null)
            return (T[]) Array.newInstance(contentType, 0);
        T[] arr = (T[]) Array.newInstance(contentType, size());
        for(int i=0; i<arr.length; i++)
            arr[i] = get(i).as(genericContentType);
        return arr;
    }

    /**
     * Returns a list containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to a list, or an empty list
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T> List<T> asList(Class<T> contentType) {
        return asList((Type) contentType);
    }

    /**
     * Returns a list containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to a list, or an empty list
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T> List<T> asList(Class<? super T> contentType, Type... typeParameters) {
        return asList(type(contentType, typeParameters));
    }

    /**
     * Returns a list containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to a list, or an empty list
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T> List<T> asList(Type contentType) {
        return asCollection(ArrayList::new, Objects.requireNonNull(contentType, "contentType"));
    }

    /**
     * Returns a set containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to a set, or an empty set
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T> Set<T> asSet(Class<T> contentType) {
        return asSet((Type) contentType);
    }

    /**
     * Returns a set containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to a set, or an empty set
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T> Set<T> asSet(Class<? super T> contentType, Type... typeParameters) {
        return asSet(type(contentType, typeParameters));
    }

    /**
     * Returns a set containing all elements of the {@link JsonArray} contained
     * in this json element, each deserialized to the specified type.
     *
     * @param contentType The type to deserialize the elements to
     * @return The json array deserialized to a set, or an empty set
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T> Set<T> asSet(Type contentType) {
        return asCollection(HashSet::new, Objects.requireNonNull(contentType, "contentType"));
    }

    /**
     * Returns a collection of the specified type containing all elements of the {@link JsonArray}
     * contained in this json element, each deserialized to the specified type.
     *
     * @param collectionCtor A constructor for the type of collection that should be returned
     * @param contentType    The type to deserialize the elements to
     * @return The json array deserialized to the specified collection type, or an empty collection of that type
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T, C extends Collection<? super T>> C asCollection(Supplier<C> collectionCtor, Class<T> contentType) {
        return asCollection(collectionCtor, (Type) contentType);
    }

    /**
     * Returns a collection of the specified type containing all elements of the {@link JsonArray}
     * contained in this json element, each deserialized to the specified type.
     *
     * @param collectionCtor A constructor for the type of collection that should be returned
     * @param contentType    The type to deserialize the elements to
     * @return The json array deserialized to the specified collection type, or an empty collection of that type
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T, C extends Collection<? super T>> C asCollection(Supplier<C> collectionCtor, Class<? super T> contentType, Type... typeParameters) {
        return asCollection(collectionCtor, type(contentType, typeParameters));
    }

    /**
     * Returns a collection of the specified type containing all elements of the {@link JsonArray}
     * contained in this json element, each deserialized to the specified type.
     *
     * @param collectionCtor A constructor for the type of collection that should be returned
     * @param contentType    The type to deserialize the elements to
     * @return The json array deserialized to the specified collection type, or an empty collection of that type
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
     */
    @NotNull
    public <T, C extends Collection<? super T>> C asCollection(Supplier<C> collectionCtor, Type contentType) {
        C col = Objects.requireNonNull(collectionCtor, "collectionCtor").get();
        forEachNullable(e -> col.add(e.as(contentType)));
        return col;
    }

    /**
     * Returns a map containing all entries of the {@link JsonObject} contained
     * in this json element, each value being deserialized to the specified type.
     * If the value of the json element is <code>null</code>, an empty map will be
     * returned.
     *
     * @param valueType The type to deserialize the values to (the keys are strings)
     * @return The json object deserialized to a map, or an empty map
     * @throws TypeMismatchException If a value is present and is not convertible to a json object
     */
    @NotNull
    public <T> Map<String, T> asMap(Class<T> valueType) {
        return asCustomMap(HashMap::new, valueType);
    }

    /**
     * Returns a map containing all entries of the {@link JsonObject} contained
     * in this json element, each value being deserialized to the specified type.
     * If the value of the json element is <code>null</code>, an empty map will be
     * returned.
     *
     * @param valueType The type to deserialize the values to (the keys are strings)
     * @return The json object deserialized to a map, or an empty map
     * @throws TypeMismatchException If a value is present and is not convertible to a json object
     */
    @NotNull
    public <T> Map<String, T> asMap(Type valueType) {
        return asCustomMap(HashMap::new, valueType);
    }

    /**
     * Returns a map of the specified implementation type containing all entries of the
     * {@link JsonObject} contained in this json element, each value being deserialized to
     * the specified type. If the value of the json element is <code>null</code>, an empty
     * map will be returned.
     *
     * @param mapCtor   A constructor for the type of map that should be returned
     * @param valueType The type to deserialize the values to (the keys are strings)
     * @return The json object deserialized to a map, or an empty map
     * @throws TypeMismatchException If a value is present and is not convertible to a json object
     */
    @NotNull
    public <T, M extends Map<? super String, ? super T>> M asCustomMap(Supplier<M> mapCtor, Class<T> valueType) {
        return asCustomMap(mapCtor, (Type) valueType);
    }

    /**
     * Returns a map of the specified implementation type containing all entries of the
     * {@link JsonObject} contained in this json element, each value being deserialized to
     * the specified type. If the value of the json element is <code>null</code>, an empty
     * map will be returned.
     *
     * @param mapCtor   A constructor for the type of map that should be returned
     * @param valueType The type to deserialize the values to (the keys are strings)
     * @return The json object deserialized to a map, or an empty map
     * @throws TypeMismatchException If a value is present and is not convertible to a json object
     */
    @NotNull
    public <T, M extends Map<? super String, ? super T>> M asCustomMap(Supplier<M> mapCtor, Type valueType) {
        M map = mapCtor.get();
        forEachNullable((k,v) -> map.put(k, v.as(valueType)));
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
     * @throws TypeMismatchException If a value is present and is not convertible to a json object or array
     */
    public <K,V> Map<K,V> asMap(Class<K> keyType, Class<V> valueType) {
        return asCustomMap(HashMap::new, keyType, valueType);
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
     * @throws TypeMismatchException If a value is present and is not convertible to a json object or array
     */
    public <K,V> Map<K,V> asMap(Type keyType, Type valueType) {
        return asCustomMap(HashMap::new, keyType, valueType);
    }

    /**
     * Deserializes this json data as a map of the specified implementation type. If the value
     * of this json element is a {@link JsonObject}, the keys will be deserialized into the key
     * type and the values to the value type. If the value of this json element is a {@link JsonArray},
     * the elements of the array must be key-value pairs as follows:
     * <pre>
     * [{ "key": 1, "value": "A" }, { "key": 2, "value": "B" }]
     * OR
     * [[1,"A"], [2,"B"]]
     * </pre>
     *
     * @param mapCtor   A constructor for the type of map that should be returned
     * @param keyType   The type to deserialize keys to
     * @param valueType The type to deserialize values to
     * @return The deserialized map, or an empty map
     * @throws TypeMismatchException If a value is present and is not convertible to a json object or array
     */
    public <K,V, M extends Map<? super K, ? super V>> M asCustomMap(Supplier<M> mapCtor, Class<K> keyType, Class<V> valueType) {
        return asCustomMap(mapCtor, (Type) keyType, valueType);
    }

    /**
     * Deserializes this json data as a map of the specified implementation type. If the value
     * of this json element is a {@link JsonObject}, the keys will be deserialized into the key
     * type and the values to the value type. If the value of this json element is a {@link JsonArray},
     * the elements of the array must be key-value pairs as follows:
     * <pre>
     * [{ "key": 1, "value": "A" }, { "key": 2, "value": "B" }]
     * OR
     * [[1,"A"], [2,"B"]]
     * </pre>
     *
     * @param mapCtor   A constructor for the type of map that should be returned
     * @param keyType   The type to deserialize keys to
     * @param valueType The type to deserialize values to
     * @return The deserialized map, or an empty map
     * @throws TypeMismatchException If a value is present and is not convertible to a json object or array
     */
    public <K,V, M extends Map<? super K, ? super V>> M asCustomMap(Supplier<M> mapCtor, Type keyType, Type valueType) {
        M map = mapCtor.get();
        if(value == null) return map;
        if(isObject())
            forEach((k,v) -> map.put(of(k, v.deserializer, path+"[key]", context).as(keyType), v.as(valueType)));
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
     * @throws TypeMismatchException If a value is present and is not convertible to a string
     */
    public String asString() {
        return deserializer.asString(this, value);
    }

    /**
     * Returns the elements value as a number.
     *
     * @return The value of the json element as number
     * @throws TypeMismatchException If a value is present and is not convertible to a number
     */
    public Number asNumber() {
        return deserializer.asNumber(this, value);
    }

    /**
     * Returns the elements value as {@code long}.
     *
     * @return The value of the json element as long
     * @throws NoSuchElementException If no value is present
     * @throws TypeMismatchException If a value is present and is not convertible to a number that can be converted to a long without data loss
     */
    public Long asLong() {
        return deserializer.asLong(this, value);
    }

    /**
     * Returns the elements value as {@code int}.
     *
     * @return The value of the json element as int
     * @throws NoSuchElementException If no value is present
     * @throws TypeMismatchException If a value is present and
     *                            is not convertible to a number
     */
    public Integer asInt() {
        return deserializer.asInt(this, value);
    }

    /**
     * Returns the elements value as {@code double}.
     *
     * @return The value of the json element as double
     * @throws NoSuchElementException If no value is present
     * @throws TypeMismatchException If a value is present and
     *                            is not convertible to a number
     */
    public Double asDouble() {
        return deserializer.asDouble(this, value);
    }

    /**
     * Returns the elements value as {@code float}.
     *
     * @return The value of the json element as float
     * @throws NoSuchElementException If no value is present
     * @throws TypeMismatchException If a value is present and
     *                            is not convertible to a number
     */
    public Float asFloat() {
        return deserializer.asFloat(this, value);
    }

    /**
     * Returns the elements value as {@code boolean}.
     *
     * @return The value of the json element as boolean
     * @throws NoSuchElementException If no value is present
     * @throws TypeMismatchException If a value is present and
     *                            is not convertible to a boolean
     */
    public Boolean asBool() {
        return deserializer.asBool(this, value);
    }


    /**
     * Returns {@code true} if this element contains no value or convertible to a {@link JsonStructure}.
     *
     * @return Whether this element contains a json structure
     */
    public boolean isStructure() {
        return deserializer.isStructure(this, value);
    }

    /**
     * Returns {@code true} if this element contains no value or convertible to a {@link JsonObject}.
     *
     * @return Whether this element contains a json object
     */
    public boolean isObject() {
        return deserializer.isObject(this, value);
    }

    /**
     * Returns {@code true} if this element contains no value or convertible to a {@link JsonArray}.
     *
     * @return Whether this element contains a json array
     */
    public boolean isArray() {
        return deserializer.isArray(this, value);
    }

    /**
     * Returns {@code true} if this element contains no value or convertible to a {@link String}.
     *
     * @return Whether this element contains a string
     */
    public boolean isString() {
        return deserializer.isString(this, value);
    }

    /**
     * Returns {@code true} if this element contains no value or convertible to a {@code Number}.
     *
     * @return Whether this element contains a number
     */
    public boolean isNumber() {
        return deserializer.isNumber(this, value);
    }

    /**
     * Returns {@code true} if this element contains no value, convertible to an {@link Integer},
     * {@link Long}, {@link Short} or {@link Byte}, or convertible to a {@link Double} or {@link Float}
     * which can be converted loss-less to a <code>long</code>.
     *
     * @return Whether this element contains an integer number
     */
    public boolean isInteger() {
        return deserializer.isInteger(this, value);
    }

    /**
     * Returns {@code true} if this element contains no value or convertible to a {@code boolean}.
     *
     * @return Whether this element contains a boolean
     */
    public boolean isBool() {
        return deserializer.isBool(this, value);
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
     * @throws TypeMismatchException If a value is present and cannot be converted to a {@link JsonObject}
     * @throws NullPointerException If the value of this element is {@code null}
     */
    public JsonElement get(String key) {
        return value != null ? deserializer.get(this, value, key) : EMPTY;
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
     * @throws TypeMismatchException If a value is present and cannot be converted to a {@link JsonArray}
     */
    public JsonElement get(int index) {
        if(value == null) {
            if(index < 0) throw new IndexOutOfBoundsException(index);
            return JsonElement.EMPTY;
        }
        return index < size() ? deserializer.get(this, value, index) : EMPTY;
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link JsonStructure}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The structure mapped to the specified key of the contained json object
     * @throws TypeMismatchException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a json structure
     */
    public JsonStructure getStructure(String key) {
        return get(key).asStructure();
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as {@link JsonStructure}. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The structure mapped at the specified index of the contained json array
     * @throws TypeMismatchException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a json structure
     */
    public JsonStructure getStructure(int index) {
        return get(index).asStructure();
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link JsonObject}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The object mapped to the specified key of the contained json object
     * @throws TypeMismatchException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a json object
     */
    public JsonObject getObject(String key) {
        return get(key).asObject();
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as {@link JsonObject}. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The object mapped at the specified index of the contained json array
     * @throws TypeMismatchException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a json object
     */
    public JsonObject getObject(int index) {
        return get(index).asObject();
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link JsonArray}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The array mapped to the specified key of the contained json object
     * @throws TypeMismatchException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a json array
     */
    public JsonArray getArray(String key) {
        return get(key).asArray();
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as {@link JsonArray}. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The array mapped at the specified index of the contained json array
     * @throws TypeMismatchException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a json array
     */
    public JsonArray getArray(int index) {
        return get(index).asArray();
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link String}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The string mapped to the specified key of the contained json object
     * @throws TypeMismatchException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a string
     */
    public String getString(String key) {
        return get(key).asString();
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as {@link String}. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The string mapped at the specified index of the contained json array
     * @throws TypeMismatchException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a string
     */
    public String getString(int index) {
        return get(index).asString();
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as {@link Number}. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The number mapped to the specified key of the contained json object
     * @throws TypeMismatchException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a number
     */
    public Number getNumber(String key) {
        return get(key).asNumber();
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as {@link Number}. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The number mapped at the specified index of the contained json array
     * @throws TypeMismatchException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a number
     */
    public Number getNumber(int index) {
        return get(index).asNumber();
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as long. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The long mapped to the specified key of the contained json object
     * @throws TypeMismatchException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a number that can be
     *                            converted to a long without loosing data
     */
    public Long getLong(String key) {
        return get(key).asLong();
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as long. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The long mapped at the specified index of the contained json array
     * @throws TypeMismatchException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a number that can be
     *                            converted to a long without loosing data
     */
    public Long getLong(int index) {
        return get(index).asLong();
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as int. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The int mapped to the specified key of the contained json object
     * @throws TypeMismatchException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a number that can be
     *                            converted to an int without loosing data
     */
    public Integer getInt(String key) {
        return get(key).asInt();
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as int. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The int mapped at the specified index of the contained json array
     * @throws TypeMismatchException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a number that can be
     *                            converted to an int without loosing data
     */
    public Integer getInt(int index) {
        return get(index).asInt();
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as double. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The double mapped to the specified key of the contained json object
     * @throws TypeMismatchException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a number that can be
     *                            converted to a double without loosing data
     */
    public Double getDouble(String key) {
        return get(key).asDouble();
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as double. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The double mapped at the specified index of the contained json array
     * @throws TypeMismatchException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a number that can be
     *                            converted to a double without loosing data
     */
    public Double getDouble(int index) {
        return get(index).asDouble();
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as float. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The float mapped to the specified key of the contained json object
     * @throws TypeMismatchException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a number that can be
     *                            converted to a float without loosing data
     */
    public Float getFloat(String key) {
        return get(key).asFloat();
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as float. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The float mapped at the specified index of the contained json array
     * @throws TypeMismatchException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a number that can be
     *                            converted to a float without loosing data
     */
    public Float getFloat(int index) {
        return get(index).asFloat();
    }

    /**
     * Returns the value mapped to the specified key of the
     * contained json object as boolean. If the json object does
     * not contain a mapping for that key, <code>null</code> will be returned.
     *
     * @return The boolean value mapped to the specified key of the contained json object
     * @throws TypeMismatchException If a value is present and not convertible to a json object, or
     *                            the mapped value is not convertible to a boolean value
     */
    public Boolean getBool(String key) {
        return get(key).asBool();
    }

    /**
     * Returns the value mapped at the specified index of the
     * contained json array as boolean. If the json array does
     * not contain such an index, <code>null</code> will be returned.
     *
     * @return The boolean value mapped at the specified index of the contained json array
     * @throws TypeMismatchException If a value is present and not convertible to a json array, or
     *                            the value at that index is not a boolean value
     */
    public Boolean getBool(int index) {
        return get(index).asBool();
    }

    /**
     * Returns a {@link JsonElement} with the value mapped at the specified
     * path, or an empty json element if that value does not exist.
     *
     * @param path The path of the value to get
     * @return A json element with the value, or empty
     */
    public JsonElement getPath(String path) {
        return value != null ?
                of(asStructure().getPath(path).value, deserializer, Json.joinPaths(this.path, path), context) :
                EMPTY;
    }

    /**
     * Returns a {@link JsonElement} with the value mapped at the specified
     * path, or an empty json element if that value does not exist.
     *
     * @param path The path of the value to get
     * @return A json element with the value, or empty
     */
    public JsonElement getPath(Object... path) {
        return value != null ?
                of(asStructure().getPath(path).value, deserializer, Json.pathToString(this.path, path), context) :
                EMPTY;
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
        if(isStructure())
            return replacementElement(asStructure().merge(other.value));
        if(other.isStructure())
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
     * @throws TypeMismatchException If a value is present and is not convertible to a json object
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
        return value != null ? replacementElement(asObject().merge(other)) : replacementElement(other);
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
     * @throws TypeMismatchException If a value is present and is not convertible to a json array
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
        return value != null ? replacementElement(asArray().merge(other)) : replacementElement(other);
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
     * @throws TypeMismatchException If a value is present and not convertible to a {@link JsonStructure}
     */
    public JsonStructure or(JsonStructure ifNotPresent) {
        return value != null ? asStructure() : null;
    }

    /**
     * Returns the value of this json element as json object, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws TypeMismatchException If a value is present and not convertible to a {@link JsonObject}
     */
    public JsonObject or(JsonObject ifNotPresent) {
        return value != null ? asObject() : null;
    }

    /**
     * Returns the value of this json element as json array, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws TypeMismatchException If a value is present and not convertible to a {@link JsonArray}
     */
    public JsonArray or(JsonArray ifNotPresent) {
        return value != null ? asArray() : null;
    }

    /**
     * Returns the value of this json element as an array, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present. Must not be null.
     * @return The value of the json element, or the specified value
     * @throws TypeMismatchException If a value is present and not convertible to a {@link JsonArray}
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
     * @throws TypeMismatchException If a value is present and not convertible to a {@link String}
     */
    public String or(String ifNotPresent) {
        return value != null ? asString() : ifNotPresent;
    }

    /**
     * Returns the value of this json element as int, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws TypeMismatchException If a value is present and not convertible to an int
     */
    public int or(int ifNotPresent) {
        return value != null ? asInt() : ifNotPresent;
    }

    /**
     * Returns the value of this json element as long, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws TypeMismatchException If a value is present and not convertible to a long
     */
    public long or(long ifNotPresent) {
        return value != null ? asLong() : ifNotPresent;
    }

    /**
     * Returns the value of this json element as float, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws TypeMismatchException If a value is present and not convertible to a float
     */
    public float or(float ifNotPresent) {
        return value != null ? asFloat() : ifNotPresent;
    }

    /**
     * Returns the value of this json element as double, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws TypeMismatchException If a value is present and not convertible to a double
     */
    public double or(double ifNotPresent) {
        return value != null ? asDouble() : ifNotPresent;
    }

    /**
     * Returns the value of this json element as boolean, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     * @throws TypeMismatchException If a value is present and not convertible to a boolean
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
    public <T> T or(Class<T> type, T ifNotPresent) {
        return or((Type) type, ifNotPresent);
    }

    /**
     * Returns the value of this json element, or the specified value if none is present.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return The value of the json element, or the specified value
     */
    public <T> T or(Type type, T ifNotPresent) {
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
        return value != null ? get() : getIfNotPresent;
    }

    /**
     * Returns the value of this json element, or the specified value if none is present.
     *
     * @param getIfNotPresent Supplier for the value to use if no non-null value
     *                        is present
     * @return The json object's value deserialized to the specified type, or the return
     *         value from the supplier
     */
    public <T> T orGet(Class<T> type, Supplier<? extends T> getIfNotPresent) {
        return orGet((Type) type, getIfNotPresent);
    }

    /**
     * Returns the value of this json element, or the specified value if none is present.
     *
     * @param getIfNotPresent Supplier for the value to use if no non-null value
     *                        is present
     * @return The json object's value deserialized to the specified type, or the return
     *         value from the supplier
     */
    public <T> T orGet(Type type, Supplier<? extends T> getIfNotPresent) {
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
        return value != null ? this : wrap(ifNotPresent, deserializer).withContextOf(this);
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
        return value != null ? this : wrap(getIfNotPresent.get(), deserializer).withContextOf(this);
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
     * Returns an iterator iterating over the non-null elements of the
     * json array contained in this element. If no value is present, an empty
     * iterator will be returned.
     *
     * @return An iterator over the element of the contained array
     * @throws TypeMismatchException If an element is present, but it
     *                            is not a json array
     */
    @NotNull
    @Override
    public Iterator<JsonElement> iterator() {
        Iterator<JsonElement> it = nullableElements().iterator();
        return new Iterator<JsonElement>() {
            JsonElement next = null;
            @Override
            public boolean hasNext() {
                getNext();
                return next != null;
            }

            @Override
            public JsonElement next() {
                getNext();
                if(next == null)
                    throw new NoSuchElementException();
                JsonElement res = next;
                next = null;
                return res;
            }

            public void getNext() {
                while((next == null || next.value == null) && it.hasNext())
                    next = it.next();
            }
        };
    }

    /**
     * Returns an iterable iterating over the elements of the
     * json array contained in this element, including <code>null</code> entries.
     * If no value is present, an empty iterator will be returned.
     *
     * @return An iterator over the element of the contained array
     * @throws TypeMismatchException If an element is present, but it
     *                            is not a json array
     */
    public Iterable<JsonElement> nullableElements() {
        if(value == null) return IterableIterator.empty();
        return deserializer.elements(this, value);
    }

    /**
     * Returns the keys of the json object contained in this element.
     * If no value is present, an empty iterator will be returned.
     *
     * @return The keys of the contained object
     * @throws TypeMismatchException If a value is present, but is not a json object
     */
    @NotNull
    public Set<String> keySet() {
        if(value == null) return Set.of();
        return deserializer.keySet(this, value);
    }

    /**
     * Returns the non-null values of the json object contained in this element.
     * If no value is present, an empty iterator will be returned.
     *
     * @return The non-null values of the contained object
     * @throws TypeMismatchException If a value is present, but is not convertible to a json object
     */
    @NotNull
    public Collection<JsonElement> values() {
        if(value == null) return List.of();
        return deserializer.values(this, value);
    }

    /**
     * Returns a stream over the elements of the json array contained in
     * this element. If no value is present, the stream will be empty.
     *
     * @return A stream over the elements of the contained array
     * @throws TypeMismatchException If an element is present, but it is not convertible to a json array
     */
    @NotNull
    public Stream<JsonElement> stream() {
        if(value == null) return Stream.empty();
        return deserializer.stream(this, value);
    }

    /**
     * Returns the size of the contained json structure. If no value is present,
     * 0 will be returned.
     *
     * @return The size of the contained structure
     * @throws TypeMismatchException If an element is present, but it is
     *                            not a json structure
     */
    public int size() {
        return value != null ? deserializer.size(this, value) : 0;
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
     * @throws TypeMismatchException If an element is present, but it
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
     * @throws TypeMismatchException If an element is present, but it
     *                            is not a json object
     */
    public boolean containsKey(String key) {
        return value != null && keySet().contains(key);
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
     * @throws TypeMismatchException If an element is present, but it
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
        return Objects.toString(get());
    }

    /**
     * Iterates over the non-null entries of this json object. If no value
     * is present, no elements will be iterated.
     *
     * @param action The action to perform on each non-null key-value-pair
     * @throws TypeMismatchException If a value is present, but
     *                            it is not a json object
     */
    public void forEach(BiConsumer<? super String, ? super JsonElement> action) {
        forEachNullable((k,v) -> {
            if(v.isPresent())
                action.accept(k,v);
        });
    }

    /**
     * Iterates over all entries of this json object, including <code>null</code> values. If no value
     * is present, no elements will be iterated.
     *
     * @param action The action to perform on each non-null key-value-pair
     * @throws TypeMismatchException If a value is present, but
     *                            it is not a json object
     */
    public void forEachNullable(BiConsumer<? super String, ? super JsonElement> action) {
        if(value == null) return;
        deserializer.forEach(this, value, action);
    }

    /**
     * Iterates over all non-null elements in this json array. If no value is present, no elements
     * will be iterated.
     *
     * @param action The action to perform in each non-null entry
     * @throws TypeMismatchException If a value is present but is not convertible to a json array
     */
    @Override
    public void forEach(Consumer<? super JsonElement> action) {
        if(value == null) return;
        for(JsonElement e : this)
            if(e.isPresent())
                action.accept(e);
    }

    /**
     * Iterates over all elements in this json array, including <code>null</code> values. If no
     * value is present, no elements will be iterated.
     *
     * @param action The action to perform in each entry
     * @throws TypeMismatchException If a value is present but is not convertible to a json array
     */
    public void forEachNullable(Consumer<? super JsonElement> action) {
        if(value == null) return;
        for(JsonElement e : this)
            action.accept(e);
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
        return of(Json.serialize(o), deserializer, "", null);
    }

    JsonElement subElement(Object field, Object o) {
        return of(o, deserializer, Json.appendToPath(path, field), context);
    }

    private JsonElement replacementElement(Object o) {
        return of(o, deserializer, path, context);
    }

    private static JsonElement of(Object o, JsonDeserializer deserializer, String path, GenericContext context) {
        return o != null || context != null ? new JsonElement(o, deserializer, path, context) : EMPTY;
    }



    private static final class GenericContext {

        private final Deque<Type> currentType = new ArrayDeque<>();
        private final Map<TypeVariable<?>, Deque<Type>> typeVariableValues = new HashMap<>();

        public void pushCurrentType(Type type) {
            currentType.push(resolveRawType(type));
        }

        public void popCurrentType() {
            currentType.pop();
        }

        public Class<?> currentType() {
            Type type = currentType.getFirst();
            if(!(type instanceof Class)) {
                type = resolveRawType(type);
                currentType.pop();
                currentType.push(type);
            }
            return (Class<?>) type;
        }

        public void pushTypeValue(TypeVariable<?> typeVariable, Type value) {
            typeVariableValues.computeIfAbsent(typeVariable, $ -> new ArrayDeque<>()).push(value);
        }

        public void pushAllTypeValues(TypeVariable<?>[] typeVariables, Type[] types) {
            assert typeVariables.length == types.length;
            for(int i=0; i<typeVariables.length; i++)
                pushTypeValue(typeVariables[i], types[i]);
        }

        public void popTypeValue(TypeVariable<?> typeVariable) {
            Deque<Type> stack = typeVariableValues.get(typeVariable);
            stack.pop();
            if(stack.isEmpty())
                typeVariableValues.remove(typeVariable);
        }

        public void popAllTypeValues(TypeVariable<?>[] typeVariables) {
            for(int i=0; i<typeVariables.length; i++)
                popTypeValue(typeVariables[i]);
        }

        public Type getActualType(TypeVariable<?> typeVariable) {
            Deque<Type> stack = typeVariableValues.get(typeVariable);
            if(stack == null)
                throw new IllegalArgumentException("Missing generic type information about "+typeVariable+" for "+typeVariable.getGenericDeclaration());
            return stack.getFirst();
        }

        public Class<?> resolveRawType(Type type) {
            int arrayDepth = 0;
            while(!(type instanceof Class)) {
                if(type instanceof TypeVariable<?>)
                    type = getActualType((TypeVariable<?>) type);
                else if(type instanceof GenericArrayType) {
                    type = ((GenericArrayType) type).getGenericComponentType();
                    arrayDepth++;
                }
                else if(type instanceof WildcardType) {
                    Type[] bounds = ((WildcardType) type).getLowerBounds();
                    if(bounds.length == 0)
                        bounds = ((WildcardType) type).getUpperBounds();
                    type = bounds[0];
                }
                else type = ((ParameterizedType) type).getRawType();
            }
            Class<?> cls = (Class<?>) type;
            for(int i=0; i<arrayDepth; i++)
                cls = Array.newInstance(cls, 0).getClass();
            return cls;
        }
    }
}
