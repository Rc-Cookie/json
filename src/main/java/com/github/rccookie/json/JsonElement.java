package com.github.rccookie.json;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents any json value like an object, an array, a string or
 * similar, or a non-existent value. It is helpful when working with
 * data that may exist, but does not have to, and in that case would
 * get replaced by a default value.
 */
public interface JsonElement {

    /**
     * Returns the elements value.
     *
     * @return The value of the json element
     * @throws NoSuchElementException If no value is present
     */
    Object get();

    /**
     * Returns the elements value as {@link JsonStructure}.
     *
     * @return The value of the json element as json structure
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a non-null value is present and
     *                            is not a json structure
     */
    JsonStructure asStructure();

    /**
     * Returns the elements value as {@link JsonObject}.
     *
     * @return The value of the json element as json object
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a non-null value is present and
     *                            is not a json object
     */
    JsonObject asObject();

    /**
     * Returns the elements value as {@link JsonArray}.
     *
     * @return The value of the json element as json array
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a non-null value is present and
     *                            is not a json array
     */
    JsonArray asArray();

    /**
     * Returns the elements value as {@link String}.
     *
     * @return The value of the json element as string
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a non-null value is present and
     *                            is not a string
     */
    String asString();

    /**
     * Returns the elements value as {@code long}.
     *
     * @return The value of the json element as long
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a non-null value is present and
     *                            is not a long
     */
    Long asLong();

    /**
     * Returns the elements value as {@code int}.
     *
     * @return The value of the json element as int
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a non-null value is present and
     *                            is not an int
     */
    Integer asInt();

    /**
     * Returns the elements value as {@code double}.
     *
     * @return The value of the json element as double
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a non-null value is present and
     *                            is not a double
     */
    Double asDouble();

    /**
     * Returns the elements value as {@code float}.
     *
     * @return The value of the json element as float
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a non-null value is present and
     *                            is not a float
     */
    Float asFloat();

    /**
     * Returns the elements value as {@code boolean}.
     *
     * @return The value of the json element as boolean
     * @throws NoSuchElementException If no value is present
     * @throws ClassCastException If a non-null value is present and
     *                            is not a boolean
     */
    Boolean asBool();


    /**
     * Returns {@code true} if this element contains a value and the value
     * is either {@code null} or an instance of {@link JsonStructure}.
     *
     * @return Whether this element contains a json structure
     */
    boolean isStructure();

    /**
     * Returns {@code true} if this element contains a value and the value
     * is either {@code null} or an instance of {@link JsonObject}.
     *
     * @return Whether this element contains a json object
     */
    boolean isObject();

    /**
     * Returns {@code true} if this element contains a value and the value
     * is either {@code null} or an instance of {@link JsonArray}.
     *
     * @return Whether this element contains a json array
     */
    boolean isArray();

    /**
     * Returns {@code true} if this element contains a value and the value
     * is either {@code null} or an instance of {@link String}.
     *
     * @return Whether this element contains a string
     */
    boolean isString();

    /**
     * Returns {@code true} if this element contains a value and the value
     * is either {@code null} or an instance of {@code long}.
     *
     * @return Whether this element contains a long
     */
    boolean isLong();

    /**
     * Returns {@code true} if this element contains a value and the value
     * is either {@code null} or an instance of {@code int}.
     *
     * @return Whether this element contains an int
     */
    boolean isInt();

    /**
     * Returns {@code true} if this element contains a value and the value
     * is either {@code null} or an instance of {@code double}.
     *
     * @return Whether this element contains a double
     */
    boolean isDouble();

    /**
     * Returns {@code true} if this element contains a value and the value
     * is either {@code null} or an instance of {@code float}.
     *
     * @return Whether this element contains a float
     */
    boolean isFloat();

    /**
     * Returns {@code true} if this element contains a value and the value
     * is either {@code null} or an instance of {@code boolean}.
     *
     * @return Whether this element contains a boolean
     */
    boolean isBool();


    /**
     * Returns a json element containing the mapping for the given key in the
     * map that is contained in this element.
     * <p>If this element does not have a non-null value, or the contained
     * object does not have a mapping for the specified key, an empty json
     * element will be returned.
     *
     * @param key The key to get the value for
     * @return The value mapped for the key, or an empty json element
     * @throws ClassCastException If a non-null value is present and not an
     *                            instance of {@link JsonObject}
     * @throws NullPointerException If the value of this element is {@code null}
     */
    JsonElement get(String key);

    /**
     * Returns a json element containing the value at the given index in the
     * array that is contained in this element.
     * <p>If this element does not have a non-null value, or the contained
     * array does not have the specified index, an empty json element will
     * be returned.
     *
     * @param index The index to get the value at
     * @return The value at that index, or an empty json element
     * @throws ClassCastException If a non-null value is present and not an
     *                            instance of {@link JsonArray}
     */
    JsonElement get(int index);

    /**
     * Returns a {@link JsonElement} with the value mapped at the specified
     * path, or an empty json element if that value does not exist.
     *
     * @param path The path of the value to get
     * @return A json element with the value, or empty
     */
    JsonElement getPath(String path);

    /**
     * Returns a {@link JsonElement} with the value mapped at the specified
     * path, or an empty json element if that value does not exist.
     *
     * @param path The path of the value to get
     * @return A json element with the value, or empty
     */
    JsonElement getPath(Object... path);


    /**
     * Returns this element if a non-null value is present, or a json element
     * with the given non-null value.
     *
     * @param ifNotPresent The value to use if no non-null value is present
     * @return A json element with a non-null value
     */
    JsonElement or(Object ifNotPresent);

    /**
     * Returns this element if a non-null value is present, or the specified
     * json element if this one is empty.
     *
     * @param useIfNotPresent The element to use if no non-null value is present
     * @return This or the given json element
     */
    JsonElement orElse(JsonElement useIfNotPresent);

    /**
     * Returns this element if a non-null value is present, or gets the specified
     * json element if this one is empty.
     *
     * @param useIfNotPresent Supplier for element to use if no non-null value
     *                        is present
     * @return This or the supplied json element
     */
    JsonElement orElse(Supplier<JsonElement> useIfNotPresent);

    /**
     * Returns this element if a non-null value is present, or a json element
     * with the supplied non-null value.
     *
     * @param getIfNotPresent Supplier for the value to use if no non-null value
     *                        is present
     * @return A json element with a non-null value
     */
    JsonElement orGet(Supplier<?> getIfNotPresent);


    /**
     * Returns this element if a value is present, or a json element with
     * the specified value if no value is present.
     *
     * @param ifNotPresent The value to use if no value is present
     * @return A json element with a value present
     */
    JsonElement nullOr(Object ifNotPresent);

    /**
     * Returns this element if a value is present, or the specified json
     * element if this one is empty.
     *
     * @param useIfNotPresent The element to use if no value is present
     * @return This or the given json element
     */
    JsonElement nullOrElse(JsonElement useIfNotPresent);

    /**
     * Returns this element if a value is present, or gets a json element
     * from the specified supplier if this one is empty.
     *
     * @param useIfNotPresent The json element supplier to use if no value
     *                        is present
     * @return This or the supplied json element
     */
    JsonElement nullOrElse(Supplier<JsonElement> useIfNotPresent);

    /**
     * Returns this element if a value is present, or a json element with
     * the supplier value.
     *
     * @param getIfNotPresent The supplier for the value if no value is
     *                        present
     * @return A json element with a value present
     */
    JsonElement nullOrGet(Supplier<?> getIfNotPresent);

    /**
     * Returns this element if a value is present, or a json element with
     * the value {@code null} if this element is empty.
     *
     * @return A json element with a value present
     */
    JsonElement orNull();


    /**
     * Returns an optional containing this element if a value is present
     * or an empty optional if no value is present
     *
     * @return An optional as described above
     */
    Optional<JsonElement> toOptional();

    /**
     * Returns whether a value is present ({@code null} is also a value!)
     *
     * @return Whether a value is present
     */
    boolean isPresent();

    /**
     * Returns whether no value is present ({@code null} is also a value!)
     *
     * @return Whether no value is present
     */
    boolean isEmpty();

    /**
     * Returns whether a non-null value is present.
     *
     * @return Whether a non-null value is present
     */
    boolean isNotNull();
}
