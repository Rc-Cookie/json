package com.github.rccookie.json;

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
        register(Number.class,        JsonElement::asNumber);
        register(Byte.class, j -> j.asNumber().byteValue());
        register(Short.class, j -> j.asNumber().shortValue());
        register(Integer.class,       JsonElement::asInt);
        register(Long.class,          JsonElement::asLong);
        register(Float.class,         JsonElement::asFloat);
        register(Double.class,        JsonElement::asDouble);
        register(String.class,        JsonElement::asString);
        register(JsonStructure.class, JsonElement::asStructure);
        register(JsonObject.class,    JsonElement::asObject);
        register(JsonArray.class,     JsonElement::asArray);
        register(JsonElement.class, j -> j);
    }

    /**
     * Deserializers that may not be overridden.
     */
    private static final Set<Class<?>> FIXED_TYPES = Collections.unmodifiableSet(new HashSet<>(DESERIALIZERS.keySet()));


    /**
     * Registers the given deserializer for the specified type.
     *
     * @param type The type the deserializer deserializes to
     * @param deserializer The json deserializer
     */
    public static <T> void register(Class<T> type, Function<JsonElement, T> deserializer) {
        DESERIALIZERS.put(checkType(Objects.requireNonNull(type)), Objects.requireNonNull(deserializer));
    }

    /**
     * Unregisters the deserializer for the specified type
     *
     * @param type The type to remove the deserializer for
     * @return Whether a deserializer was present previously
     */
    public static boolean unregister(Class<?> type) {
        return DESERIALIZERS.remove(checkType(Objects.requireNonNull(type))) != null;
    }

    /**
     * Returns whether the given type has a deserializer registered.
     * <p>This will force initialize the specified type.
     *
     * @param type The type to check for
     * @return Whether the type has a deserializer registered
     */
    public static boolean isRegistered(Class<?> type) {
        initType(type);
        return DESERIALIZERS.containsKey(type);
    }

    /**
     * Returns the deserializer mapped for the specified type, or
     * {@code null} if no deserializer is mapped.
     * <p>This will force initialize the specified type.
     *
     * @param type The type to get the deserializer for
     * @return The deserializer for the type, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> Function<JsonElement, T> getDeserializer(Class<T> type) {
        initType(type);
        return (Function<JsonElement, T>) DESERIALIZERS.get(Objects.requireNonNull(type));
    }

    /**
     * Deserializes the given json data into the given type using
     * the mapped deserializer.
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
        Function<JsonElement,T> deserializer = getDeserializer(type);
        if(deserializer == null)
            throw new IllegalStateException("No deserializer for type " + type + " registered");
        return deserializer.apply(data);
    }

    /**
     * Force initializes the specified type to ensure it had time to
     * register itself for deserialization. If the type is already initialized
     * this will do nothing.
     *
     * @param type The type to initialize
     */
    private static void initType(Class<?> type) {
        try { Class.forName(type.getName(), true, type.getClassLoader()); }
        catch(ClassNotFoundException e) { throw new AssertionError(); }
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
        if(FIXED_TYPES != null && FIXED_TYPES.contains(type))
            throw new IllegalArgumentException("The deserializer for " + type + " cannot be changed");
        return type;
    }
}
