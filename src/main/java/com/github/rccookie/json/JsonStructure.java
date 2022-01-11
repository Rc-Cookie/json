package com.github.rccookie.json;

import java.io.File;
import java.io.IOException;

/**
 * Superclass of {@link JsonObject} and {@link JsonArray}.
 */
public interface JsonStructure extends Cloneable {

    /**
     * Returns true if this json structure is an instance of {@link JsonObject},
     * just as returned by the statement
     * <pre>
     *     jsonStructure instanceof JsonObject
     * </pre>
     *
     * @return Whether this json structure is a json object
     */
    default boolean isObject() {
        return this instanceof JsonObject;
    }

    /**
     * Returns true if this json structure is an instance of {@link JsonArray},
     * just as returned by the statement
     * <pre>
     *     jsonStructure instanceof JsonArray
     * </pre>
     *
     * @return Whether this json structure is a json object
     */
    default boolean isArray() {
        return this instanceof JsonArray;
    }

    /**
     * Returns this json structure wrapped in a {@link JsonElement}.
     *
     * @return This json structure as json element
     */
    default JsonElement asElement() {
        return JsonElement.wrap(this);
    }

    /**
     * Returns the number of elements in this structure.
     *
     * @return The size of this structure
     */
    int size();

    /**
     * Returns, whether this json structure contains the given
     * element.
     *
     * @param o The element to test for containment
     * @return Whether this json structure contains the given element
     */
    boolean contains(Object o);

    /**
     * Returns whether this json structure contains the given
     * key or index.
     *
     * @param keyOrIndex The key or index to check for containment for
     * @return Whether this json structure contains the given key or index
     */
    boolean containsKey(Object keyOrIndex);

    /**
     * Returns, whether this json structure contains the given
     * value.
     *
     * @param o The value to test for containment
     * @return Whether this json structure contains the given value
     */
    boolean containsValue(Object o);

    /**
     * Assigns the value of the given json formatted file to this structure.
     * If an {@link IOException} occurres, the content of this json structure
     * will only be cleared.
     *
     * @param file The file to load from
     * @return Weather any assignment was done
     * @throws JsonParseException If the file does not follow json syntax
     * @throws NullPointerException If the file contains the content {@code null}
     * @throws ClassCastException If the file is valid but does not represent
     *                            the required type of json structure
     */
    boolean load(File file);

    /**
     * Stores this json structure in the given file. The file will be cleared
     * if it exists, otherwise a new file will be created.
     *
     * @param file The file to store the object in
     * @return Weather the storing was successful
     */
    boolean store(File file);

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
    String toString();

    /**
     * Converts this json array into a json string. Any values that are
     * not a number, a boolean, {@code false} or already a string will be
     * displayed as their {@link Object#toString()} value.
     * <p>If this array or any of the contained elements contains itself
     * a {@link NestedJsonException} will be thrown.
     *
     * @param formatted Weather the json string should be formatted with
     *                  indents and newlines
     * @return The json string representing this array
     * @throws NestedJsonException If the json array or one of it's
     *                             contained json elements contains
     *                             itself
     */
    String toString(boolean formatted);

    /**
     * Creates a shallow copy of this json object by creating a new
     * instance with the same content (which will not be cloned).
     *
     * @return A copy of this json object
     */
    JsonStructure clone();

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
}
