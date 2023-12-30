package de.rccookie.json;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;

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
    boolean load(Path file);

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
    default boolean load(String file) {
        return load(Path.of(file));
    }

    /**
     * Stores this json structure in the given file. The file will be cleared
     * if it exists, otherwise a new file will be created.
     *
     * @param file The file to store the object in
     * @return Weather the storing was successful
     */
    default boolean store(File file) {
        try {
            Json.store(this, file);
            return true;
        } catch(UncheckedIOException e) {
            return false;
        }
    }

    /**
     * Stores this json structure in the given file. The file will be cleared
     * if it exists, otherwise a new file will be created.
     *
     * @param file The file to store the object in
     * @return Weather the storing was successful
     */
    default boolean store(Path file) {
        try {
            Json.store(this, file);
            return true;
        } catch(UncheckedIOException e) {
            return false;
        }
    }

    /**
     * Stores this json structure in the given file. The file will be cleared
     * if it exists, otherwise a new file will be created.
     *
     * @param file The file to store the object in
     * @return Weather the storing was successful
     */
    default boolean store(String file) {
        return store(Path.of(file));
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
     * Creates a deep copy of this json structure by creating a new
     * instance with the content also cloned.
     *
     * @return A copy of this json structure
     */
    JsonStructure clone();

    /**
     * Returns a new json structure of the same type as this one, with
     * the given object merged recursively. The object must also be of the same type
     * as this structure, or <code>null</code>. This instance will not
     * be modified.
     *
     * @param otherStructure The structure to be merged with this one
     * @return A deep copy of this structure with the given structure merged into it
     * @throws IllegalArgumentException If this structure cannot be merged with the
     *                                  given object, e.g. because a json structure
     *                                  has to be merged with a primitive (or string)
     *                                  at top level or in a recursive merge
     */
    JsonStructure merge(@Nullable Object otherStructure);

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
