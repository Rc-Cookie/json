package de.rccookie.json;

import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Utility class to handle automatic deserialization of json values.
 *
 * @deprecated Use {@link Json#registerDeserializer(Class, Function)} and {@link JsonElement#as(Class)} instead
 */
@Deprecated(forRemoval = true)
public final class JsonDeserialization {

    private JsonDeserialization() { }

    /**
     * Registers a custom deserializer for the specified type.
     *
     * @param type The type the deserializer deserializes to
     * @param deserializer The json deserializer
     */
    public static <T> void register(Class<T> type, Function<JsonElement, T> deserializer) {
        Json.registerDeserializer(type, deserializer);
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
        return data.as(type);
    }
}
