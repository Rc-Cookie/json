package com.github.rccookie.json;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.github.rccookie.util.IterableIterator;

import org.jetbrains.annotations.NotNull;

import static com.github.rccookie.json.Json.extractJson;

/**
 * Represents an abstract json array. A json object can hold any type of
 * valid json value, everything that is not a number, a boolean value, a
 * json structure or {@code null} has to implement {@link JsonSerializable}.
 * <p>Json arrays implement {@link List}, so unlike actual
 * arrays they are not of a fixed size.
 */
public class JsonArray implements List<Object>, JsonStructure {

    /**
     * The contents of the json array.
     */
    private final List<Object> data = new ArrayList<>();

    /**
     * Creates a new, empty json array.
     */
    public JsonArray() { }

    /**
     * Creates a new json array with all the content from the given
     * collection.
     *
     * @param copy The collection to copy
     */
    public JsonArray(Collection<?> copy) {
        addAll(copy);
    }

    /**
     * Creates a new json array by parsing the given json formatted
     * string. If the json string only contains "null" the json array
     * will be empty. If the file is not formatted properly in json
     * syntax an {@link JsonParseException} will be thrown.
     *
     * @param jsonString The json formatted string
     */
    public JsonArray(String jsonString) throws JsonParseException {
        this(Json.parse(jsonString).asArray());
    }

    /**
     * Creates a new json array by parsing the given json formatted
     * file. If the file only contains "null" or an
     * {@link IOException IOException} occurres during parsing,
     * the json array will be empty. If the file is not formatted
     * properly in json syntax an {@link JsonParseException} will be
     * thrown.
     *
     * @param file The file to load from
     */
    public JsonArray(File file) throws JsonParseException {
        load(file);
    }

    /**
     * Creates a new json array with the given values.
     *
     * @param values The values for the json array
     */
    public JsonArray(Object... values) {
        this.addAll(Arrays.asList(values));
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
    public boolean contains(Object o) {
        return data.contains(o);
    }

    @NotNull
    @Override
    public Iterator<Object> iterator() {
        return data.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return data.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        //noinspection SuspiciousToArrayCall
        return data.toArray(a);
    }

    @Override
    public boolean add(Object o) {
        return data.add(extractJson(o));
    }

    @Override
    public boolean remove(Object o) {
        return data.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return data.containsAll(c);
    }

    @Override
    public void add(int index, Object element) {
        data.add(index, extractJson(element));
    }

    @Override
    public Object remove(int index) {
        return data.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return data.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return data.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<Object> listIterator() {
        return listIterator(0);
    }

    @NotNull
    @Override
    public ListIterator<Object> listIterator(int index) {
        ListIterator<Object> it = data.listIterator(index);
        return new ListIterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                return it.next();
            }

            @Override
            public boolean hasPrevious() {
                return it.hasPrevious();
            }

            @Override
            public Object previous() {
                return it.previous();
            }

            @Override
            public int nextIndex() {
                return it.nextIndex();
            }

            @Override
            public int previousIndex() {
                return it.previousIndex();
            }

            @Override
            public void remove() {
                it.remove();
            }

            @Override
            public void set(Object o) {
                it.set(extractJson(o));
            }

            @Override
            public void add(Object o) {
                it.add(extractJson(o));
            }
        };
    }

    @NotNull
    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        return data.subList(fromIndex, toIndex);
    }

    @Override
    public Object set(int index, Object element) {
        return data.set(index, extractJson(element));
    }

    @Override
    public boolean addAll(Collection<?> c) {
        for(Object o : c) add(o);
        return !c.isEmpty();
    }

    @Override
    public boolean addAll(int index, Collection<?> c) {
        for(Object o : c) add(index++, o);
        return !c.isEmpty();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return data.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return data.retainAll(c);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Object get(int index) {
        return data.get(index);
    }

    /**
     * Creates a shallow copy of this json array by creating a new
     * instance with the same content (which will not be cloned).
     *
     * @return A copy of this json array
     */
    @Override
    public JsonArray clone() {
        return new JsonArray(this);
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
    public String toString() {
        return Json.toString(this);
    }

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
    @Override
    public String toString(boolean formatted) {
        return Json.toString(this, formatted);
    }

    /**
     * Returns whether the given index is within bounds of this json
     * array.
     * <p>If the given value is not a number or {@code null},
     * {@code false} will be returned.
     *
     * @param index The index to check for containment for
     * @return Whether this json array has the specified index
     */
    @Override
    public boolean containsKey(Object index) {
        return index instanceof Number &&
                ((Number) index).intValue() >= 0 &&
                ((Number) index).intValue() < size();
    }

    /**
     * Returns, whether this json array contains the specified value.
     * Equivalent to {@link #contains(Object)}.
     *
     * @param o The value to test for containment
     * @return Whether this json array contains the given value
     */
    @Override
    public boolean containsValue(Object o) {
        return contains(o);
    }

    /**
     * Returns the value of the specified index, wrapped in a {@link JsonElement}
     * with the default value {@code null}. If the index is positively out of
     * bounds for this array an empty json element will be returned. If the index
     * is negative an {@link IndexOutOfBoundsException} will be thrown.
     * <p>This method never returns {@code null}.
     *
     * @param index The index to get the value for
     * @return A json element as described above
     */
    public JsonElement getElement(int index) {
        return index < size() ? JsonElement.wrapNullable(get(index)) : JsonElement.EMPTY;
    }


    @Override
    public JsonElement getPath(String path) {
        return getPath(Json.parsePath(path));
    }

    @Override
    public JsonElement getPath(Object... path) {
        if(path.length == 0) return asElement();
        int index = path[0] instanceof Integer ? (int)path[0] : Integer.parseInt(path[0].toString());
        return getElement(index).getPath(Arrays.copyOfRange(path, 1, path.length));
    }



    public void combine(JsonArray other) {
        for(int i=0; i<other.size(); i++) {
            Object v = other.get(i);
            if(i >= size()) add(v);
            else if(v instanceof JsonObject) {
                JsonObject current = getObject(i);
                if(current == null) set(i, v);
                else current.combine((JsonObject) v);
            }
            else if(v instanceof JsonArray) {
                JsonArray current = getArray(i);
                if(current == null) set(i, v);
                else current.combine((JsonArray) v);
            }
            else if(get(i) == null) set(i, v);
        }
    }



    /**
     * Returns the json object at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a json object
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The json object at the specified index
     */
    public JsonObject getObject(int index) {
        return (JsonObject) get(index);
    }

    /**
     * Returns the json array at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a json array
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The json array at the specified index
     */
    public JsonArray getArray(int index) {
        return (JsonArray) get(index);
    }

    /**
     * Returns the string at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a string
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The string at the specified index
     */
    public String getString(int index) {
        return (String) get(index);
    }

    /**
     * Returns the long at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a number
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The long at the specified index
     */
    public Long getLong(int index) {
        Object value = get(index);
        return value == null ? null : ((Number) value).longValue();
    }

    /**
     * Returns the integer at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a number
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The integer at the specified index
     */
    public Integer getInt(int index) {
        Object value = get(index);
        return value == null ? null : ((Number) value).intValue();
    }

    /**
     * Returns the double at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a number
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The double at the specified index
     */
    public Double getDouble(int index) {
        Object value = get(index);
        return value == null ? null : ((Number) value).doubleValue();
    }

    /**
     * Returns the float at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a number
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The float at the specified index
     */
    public Float getFloat(int index) {
        Object value = get(index);
        return value == null ? null : ((Number) value).floatValue();
    }

    /**
     * Returns the boolean at the specified index in this json array.
     * <p>If the index is negative or greater or equal to the size of this
     * json array, an {@link IndexOutOfBoundsException} will be thrown. If
     * the index is within bounds but the object found is not a boolean
     * a {@link ClassCastException} will be thrown.
     *
     * @param index The index to get the value for
     * @return The boolean at the specified index
     */
    public Boolean getBool(int index) {
        return (Boolean) get(index);
    }


    /**
     * Returns an {@link IterableIterator} over the elements in this
     * array, each wrapped in a {@link JsonElement}.
     *
     * @return An IterableIterator over the json elements of this array
     */
    public IterableIterator<JsonElement> elements() {
        Iterator<JsonElement> it = stream().map(JsonElement::wrapNullable).iterator();
        return new IterableIterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public JsonElement next() {
                return it.next();
            }
        };
    }



    /**
     * Assigns the value of the given json formatted file to this array.
     * If an {@link IOException} occurres, the content of this json array
     * will only be cleared.
     *
     * @param file The file to load from
     * @return Weather any assignment was done
     * @throws JsonParseException If the file does not follow json syntax
     * @throws NullPointerException If the file contains the content {@code null}
     * @throws ClassCastException If the file is valid but does not represent
     *                            a json array
     */
    @Override
    public boolean load(File file) throws JsonParseException {
        clear();
        try {
            addAll(Json.load(file).asArray());
            return true;
        } catch(UncheckedIOException e) {
            return false;
        }
    }

    /**
     * Stores this json array in the given file. The file will be cleared
     * if it exists, otherwise a new file will be created.
     *
     * @param file The file to store the array in
     * @return Weather the storing was successful
     */
    @Override
    public boolean store(File file) {
        try {
            Json.store(this, file);
            return true;
        } catch(UncheckedIOException e) {
            return false;
        }
    }
}
