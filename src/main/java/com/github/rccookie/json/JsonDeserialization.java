package com.github.rccookie.json;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility class to handle automatic deserialization of json values.
 */
public final class JsonDeserialization {

    private JsonDeserialization() {
        throw new UnsupportedOperationException("JsonSerializer does not allow instances");
    }

    /**
     * Registered deserializers.
     */
    private static final Map<Class<?>, Function<JsonElement,?>> DESERIALIZERS = new HashMap<>();
    static {
        register(Boolean.class,       JsonElement::asBool);
        register(boolean.class,       JsonElement::asBool);
        register(Number.class,        JsonElement::asNumber);
        register(Byte.class,          j -> j.asNumber().byteValue());
        register(byte.class,          j -> j.asNumber().byteValue());
        register(Short.class,         j -> j.asNumber().shortValue());
        register(short.class,         j -> j.asNumber().shortValue());
        register(Integer.class,       JsonElement::asInt);
        register(int.class,           JsonElement::asInt);
        register(Long.class,          JsonElement::asLong);
        register(long.class,          JsonElement::asLong);
        register(Float.class,         JsonElement::asFloat);
        register(float.class,         JsonElement::asFloat);
        register(Double.class,        JsonElement::asDouble);
        register(double.class,        JsonElement::asDouble);
        register(String.class,        JsonElement::asString);
        register(JsonStructure.class, JsonElement::asStructure);
        register(JsonObject.class,    JsonElement::asObject);
        register(JsonArray.class,     JsonElement::asArray);
        register(JsonElement.class,   j -> j);
        register(Character.class,     j -> j.asString().charAt(0));
        register(char.class,          j -> j.asString().charAt(0));
    }

    /**
     * Deserializers that may not be overridden.
     */
    private static final Set<Class<?>> FIXED_TYPES = Collections.unmodifiableSet(new HashSet<>(DESERIALIZERS.keySet()));


    /**
     * Registers a custom deserializer for the specified type.
     *
     * @param type The type the deserializer deserializes to
     * @param deserializer The json deserializer
     */
    public static <T> void register(Class<T> type, Function<JsonElement, T> deserializer) {
        DESERIALIZERS.put(checkType(Objects.requireNonNull(type)), Objects.requireNonNull(deserializer));
    }

    /**
     * Deserializes the given json data into the given type using
     * the mapped deserializer or the constructor of the type with
     * one argument of type {@link JsonElement}.
     *
     * @param type The type to deserialize to
     * @param data The data to deserialize
     * @return The deserialized value
     * @throws IllegalStateException If no deserializer is registered for
     *                               the specified type
     * @throws NoSuchElementException If the json element is empty
     */
    public static <T> T deserialize(Class<T> type, JsonElement data) {
        if(Objects.requireNonNull(data).isEmpty())
            throw new NoSuchElementException("No data to deserialize present");
        if(type.isArray()) {
            int length = data.size();
            Class<?> componentType = type.getComponentType();
            @SuppressWarnings("unchecked")
            T array = (T) Array.newInstance(componentType, length);
            for(int i=0; i<length; i++)
                Array.set(array, i, deserialize(componentType, data.get(i)));
            return array;
        }
        return getDeserializer(type).apply(data);
    }

    /**
     * Ensures that the deserializer of the given type is allowed to be
     * modified.
     *
     * @param type The type to check
     * @return The type
     * @throws IllegalArgumentException If the type's deserializer is not
     *                                  allowed to be modified
     */
    private static Class<?> checkType(Class<?> type) throws IllegalArgumentException {
        //noinspection ConstantConditions
        if(FIXED_TYPES != null && FIXED_TYPES.contains(type) || type.isEnum())
            throw new IllegalArgumentException("The deserializer for " + type + " cannot be changed");
        return type;
    }

    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    private static <T> Function<JsonElement, T> getDeserializer(Class<T> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch(ClassNotFoundException e) {
            throw new AssertionError();
        }
        Function<JsonElement, T> deserializer = (Function<JsonElement, T>) DESERIALIZERS.get(Objects.requireNonNull(type));
        if(deserializer != null) return deserializer;
        if(type.isEnum())
            return json -> (T) getEnumInstance(type, json.asString());

        Constructor<T> ctor = (Constructor<T>) Arrays.stream(type.getDeclaredConstructors())
                .peek(c -> c.setAccessible(true))
                .filter(c -> c.isAnnotationPresent(JsonCtor.class))
                .findAny()
                .orElse(null);

        if(ctor == null) {
            try {
                Constructor<T> altCtor = type.getDeclaredConstructor(JsonElement.class);
                altCtor.setAccessible(true);
                deserializer = j -> {
                    try { return altCtor.newInstance(j); }
                    catch(Exception e) { throw new RuntimeException(e); }
                };
                DESERIALIZERS.put(type, deserializer);
                return deserializer;
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("The type " + type + " is not marked from json deserialization");
            }
        }

        JsonCtor props = ctor.getAnnotation(JsonCtor.class);
        Class<?>[] paramTypes = ctor.getParameterTypes();
        Type packaging = props.type();

//            ctor.getParameters()[0].getName();

        // Json object packaged
        if(packaging == Type.OBJECT || (packaging == Type.AUTO && props.value().length != 0)) {
            String[] keys = props.value();
            if(keys.length != paramTypes.length)
                throw new IllegalArgumentException("The annotated constructor of "+type+" has the wrong number of parameters");
            deserializer = j -> {
                Object[] params = new Object[keys.length];
                for(int i=0; i<params.length; i++)
                    params[i] = deserialize(paramTypes[i], j.get(keys[i]));
                try { return ctor.newInstance(params); }
                catch(Exception e) { throw new RuntimeException(e); }
            };
        }
        // Json array packaged
        else if(packaging == Type.ARRAY || (packaging == Type.AUTO && props.indices().length != 0)) {
            int[] indices = props.indices();
            if(indices.length == 0) {
                // No indices specified -> use index of parameter
                deserializer = j -> {
                    Object[] params = new Object[paramTypes.length];
                    for (int i = 0; i < params.length; i++)
                        params[i] = deserialize(paramTypes[i], j.get(i));
                    try { return ctor.newInstance(params); }
                    catch (Exception e) { throw new RuntimeException(e); }
                };
            }
            else {
                if(indices.length != paramTypes.length)
                    throw new IllegalArgumentException("The annotated constructor of " + type + " has the wrong number of parameters");
                deserializer = j -> {
                    Object[] params = new Object[indices.length];
                    for (int i = 0; i < params.length; i++)
                        params[i] = deserialize(paramTypes[i], j.get(indices[i]));
                    try { return ctor.newInstance(params); }
                    catch (Exception e) { throw new RuntimeException(e); }
                };
            }
        }
        // Not packaged
        else {
            if(paramTypes.length != 1)
                throw new IllegalArgumentException("The annotated json of "+type+" constructor may have exactly one parameter");
            deserializer = j -> {
                try { return ctor.newInstance(deserialize(paramTypes[0], j)); }
                catch(Exception e) { throw new RuntimeException(e); }
            };
        }
        DESERIALIZERS.put(type, deserializer);
        return deserializer;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E getEnumInstance(Class<?> type, String name) {
        return Enum.valueOf((Class<E>) type, name);
    }
}
