package de.rccookie.json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and writing json.
 *
 * <p><p><h2>Supported syntax</h2>
 * <li>Objects, arrays, numbers, strings, booleans, null
 * <li>Any whitespace/newline formatting
 * <li>Single and multiline comments
 * <li>Character escape sequences - parsing and writing
 * <li>Any top level type
 * <li>Single quotes as double quotes (only in {@link Parse} class)
 *
 * <p><p><h2>Path syntax</h2>
 * <p>Paths describe the path to an element in a json structure.
 * They have two ways of describing the required element:
 * <li>{@code [&lt;key or index&gt;]}
 * <li>{@code .&lt;key or index&gt;}
 * <p>It is recommended to use the brackets only for array indices and
 * the dot only for keys, but it is valid to do it any way. The first
 * dot must be omitted.
 */
public final class Json {

    private Json() {
        throw new UnsupportedOperationException();
    }


    /**
     * The indent used for formatted json strings.
     */
    static String INDENT = "    ";

    /**
     * Whether to format if not specified.
     */
    static boolean DEFAULT_FORMATTED = true;

    /**
     * Encoder to determine whether to escape characters.
     */
    private static final CharsetEncoder CHARSET = StandardCharsets.UTF_8.newEncoder();


    // ---------- String conversion ----------


    /**
     * Converts the given object to a json string. Any objects that
     * are not a valid json type (Maps (no null keys!), Lists and arrays,
     * numbers, booleans, Strings) will be represented as if they were
     * strings with their {@code toString()} value.
     *
     * @param object The object to convert
     * @return The json string representing the object
     */
    public static String toString(Object object) {
        return toString(object, DEFAULT_FORMATTED);
    }

    /**
     * Converts the given object to a json string. Any objects that
     * are not a valid json type (Maps (no null keys!), Lists and arrays,
     * numbers, booleans, Strings) will be represented as if they were
     * strings with their {@code toString()} value.
     *
     * @param object The object to convert
     * @param formatted Weather the json string should be formatted
     *                  with indents and newlines
     * @return The json string representing the object
     */
    public static String toString(Object object, boolean formatted) {
        StringWriter writer = new StringWriter();
        write(object, writer, formatted);
        return writer.toString();
    }

    /**
     * Converts the given object to a json string. Any objects that
     * are not a valid json type (Maps (no null keys!), Lists and arrays,
     * numbers, booleans, Strings) will be represented as if they were
     * strings with their {@code toString()} value.
     *
     * @param object The object to convert
     * @param formatted Weather the json string should be formatted
     *                  with indents and newlines
     * @return The json string representing the object
     */
    public static String toString(Object object, boolean formatted, JsonSerializer serializer) {
        return usingSerializer(serializer, () -> toString(object, formatted));
    }

    /**
     * Returns a string starting and ending with double quotes and
     * with the object's toString value inside. Any special characters
     * like double quotes, newlines, tabs and similar will be appropriately
     * escaped.
     * <p>Note that this method will always use the unformatted
     * toString representation of potential json structures, independent
     * of what was set using {@link #setDefaultFormatted(boolean)}.
     *
     * @param object The object to convert to a json string value
     * @return The object as an escaped json string value
     */
    public static String escape(Object object) {
        return escape(object, false);
    }

    /**
     * Returns a string starting and ending with double quotes and
     * with the object's toString value inside. Any special characters
     * like double quotes, newlines, tabs and similar will be appropriately
     * escaped.
     *
     * @param object The object to convert to a json string value
     * @param formatted Whether to format potential json structures with
     *                  newlines and indents (they will still be escaped
     *                  afterwards)
     * @return The object as an escaped json string value
     */
    public static String escape(Object object, boolean formatted) {
        object = serialize(object);
        if(object == null) return "\"null\"";
        StringWriter writer = new StringWriter();
        if(!(object instanceof List<?> || object instanceof Map<?,?> || object instanceof Number || object instanceof Boolean || object.getClass().isArray())) // Don't escape strings twice
            printStringFor(new PrintWriter(writer), object.toString());
        else printStringFor(new PrintWriter(writer), toString(object, formatted));
        return writer.toString();
    }


    // ---------- parse and load ----------


    /**
     * Parses the given json formatted string into a json structure.
     *
     * @param jsonString The json formatted string to parse
     * @return The parsed structure
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonElement parse(String jsonString) throws JsonParseException {
        return parse(new StringReader(jsonString));
    }

    /**
     * Parses the given json formatted input into a json element.
     *
     * @param input The input to parse
     * @return The parsed json element
     * @throws JsonParseException If the string does not follow json syntax
     * @throws UncheckedIOException If the stream causes an {@link IOException}
     */
    public static JsonElement parse(InputStream input) {
        return parse(new InputStreamReader(input));
    }

    /**
     * Parses the given json formatted input into a json element.
     *
     * @param reader The reader to parse from
     * @return The parsed json element
     * @throws JsonParseException If the string does not follow json syntax
     * @throws UncheckedIOException If the reader causes an {@link IOException}
     */
    public static JsonElement parse(Reader reader) throws JsonParseException, UncheckedIOException {
        try(JsonParser parser = new JsonParser(reader)) {
            return parser.next();
        }
    }


    /**
     * Parses the given json formatted file into a json element.
     *
     * @param file The file to parse from
     * @return The parsed json element
     * @throws JsonParseException If the string does not follow json syntax
     * @throws UncheckedIOException If an {@link IOException} occurs while
     *                              reading the file
     */
    public static JsonElement load(String file) throws JsonParseException {
        return load(Path.of(file));
    }

    /**
     * Parses the given json formatted file into a json element.
     *
     * @param file The file to parse from
     * @return The parsed json element
     * @throws JsonParseException If the string does not follow json syntax
     * @throws UncheckedIOException If an {@link IOException} occurs while
     *                              reading the file
     */
    public static JsonElement load(File file) throws JsonParseException {
        try {
            return parse(new FileReader(file));
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Parses the given json formatted file into a json element.
     *
     * @param file The file to parse from
     * @return The parsed json element
     * @throws JsonParseException If the string does not follow json syntax
     * @throws UncheckedIOException If an {@link IOException} occurs while
     *                              reading the file
     */
    public static JsonElement load(Path file) throws JsonParseException {
        try {
            return parse(Files.newInputStream(file));
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    // ---------- getParser() methods ----------


    /**
     * Returns a {@link JsonParser} over the given string.
     *
     * @param jsonString The string for the parser to parse
     * @return A JsonParser over the given string
     */
    public static JsonParser getParser(String jsonString) {
        return new JsonParser(new StringReader(jsonString));
    }

    /**
     * Returns a {@link JsonParser} over the given file.
     *
     * @param file The file for the parser to parse
     * @return A JsonParser over the given string
     * @throws UncheckedIOException If an {@link IOException} occurs while
     *                              opening the file
     */
    public static JsonParser getParser(File file) {
        try {
            return new JsonParser(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns a {@link JsonParser} over the given file.
     *
     * @param file The file for the parser to parse
     * @return A JsonParser over the given string
     * @throws UncheckedIOException If an {@link IOException} occurs while
     *                              opening the file
     */
    public static JsonParser getParser(Path file) {
        try {
            return new JsonParser(Files.newBufferedReader(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns a {@link JsonParser} over the given input stream.
     *
     * @param input The input stream for the parser to parse
     * @return A JsonParser over the given input stream
     */
    public static JsonParser getParser(InputStream input) {
        return new JsonParser(new InputStreamReader(input));
    }

    /**
     * Returns a {@link JsonParser} over the given reader.
     *
     * @param reader The reader for the parser to parse
     * @return A JsonParser over the given string
     */
    public static JsonParser getParser(Reader reader) {
        return new JsonParser(reader);
    }


    // ---------- Writing ----------


    /**
     * Converts the given value into a json string and stores it
     * in the specified file. If the file exists it will be cleared before,
     * otherwise a new file will be created.
     *
     * @param value The json element to store
     * @param file The file to store the structure in
     * @throws UncheckedIOException If an {@link IOException} occurs
     * @see #toString(Object)
     */
    public static void store(Object value, File file) {
        store(value, file, DEFAULT_FORMATTED);
    }

    /**
     * Converts the given value into a json string and stores it
     * in the specified file. If the file exists it will be cleared before,
     * otherwise a new file will be created.
     *
     * @param value The json element to store
     * @param file The file to store the structure in
     * @param formatted Whether the output should be formatted with
     *                  newlines and indents
     * @throws UncheckedIOException If an {@link IOException} occurs
     * @see #toString(Object)
     */
    public static void store(Object value, File file, boolean formatted) {
        try(PrintWriter p = new PrintWriter(file)) {
            write(value, p, formatted);
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Converts the given value into a json string and stores it
     * in the specified file. If the file exists it will be cleared before,
     * otherwise a new file will be created.
     *
     * @param value The json element to store
     * @param file The file to store the structure in
     * @throws UncheckedIOException If an {@link IOException} occurs
     * @see #toString(Object)
     */
    public static void store(Object value, Path file) {
        store(value, file, DEFAULT_FORMATTED);
    }

    /**
     * Converts the given value into a json string and stores it
     * in the specified file. If the file exists it will be cleared before,
     * otherwise a new file will be created.
     *
     * @param value The json element to store
     * @param file The file to store the structure in
     * @param formatted Whether the output should be formatted with
     *                  newlines and indents
     * @throws UncheckedIOException If an {@link IOException} occurs
     * @see #toString(Object)
     */
    public static void store(Object value, Path file, boolean formatted) {
        try(OutputStream p = Files.newOutputStream(file)) {
            write(value, p, formatted);
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes the given value as json into the given output stream.
     *
     * @param value The value to write
     * @param out The stream to write into
     */
    public static void write(Object value, OutputStream out) {
        write(value, out, DEFAULT_FORMATTED);
    }

    /**
     * Writes the given value as json into the given output stream.
     *
     * @param value The value to write
     * @param out The stream to write into
     * @param formatted Whether the output should be formatted with
     *                  newlines and indents
     */
    public static void write(Object value, OutputStream out, boolean formatted) {
        write(value, new PrintWriter(out), formatted);
    }

    /**
     * Writes the given value as json into the given writer.
     *
     * @param value The value to write
     * @param writer The writer to write into
     */
    public static void write(Object value, Writer writer) {
        write(value, writer, DEFAULT_FORMATTED);
    }

    /**
     * Writes the given value as json into the given writer.
     *
     * @param value The value to write
     * @param writer The writer to write into
     * @param formatted Whether the output should be formatted with
     *                  newlines and indents
     */
    public static void write(Object value, Writer writer, boolean formatted) {
        PrintWriter printWriter = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);
        printStringFor(printWriter, serialize(value), new HashSet<>(), formatted, 0);
        printWriter.flush();
    }

    /**
     * Writes the given value as json into the given writer.
     *
     * @param value The value to write
     * @param writer The writer to write into
     * @param formatted Whether the output should be formatted with
     *                  newlines and indents
     */
    public static void write(Object value, Writer writer, boolean formatted, JsonSerializer serializer) {
        usingSerializer(serializer, () -> write(value, writer, formatted));
    }


    // ---------- Configuration ----------


    /**
     * Sets the indent size used when generating a formatted json string.
     * Must not be negative.
     *
     * @param spaceCount The number of spaces used to create an indent
     */
    public static void setIndent(int spaceCount) {
        if(spaceCount < 0) throw new IllegalArgumentException("Indent space count must be positive");
        INDENT = repeat(" ", spaceCount);
    }

    /**
     * Returns the indent size used when generating a formatted json string.
     *
     * @return The number of spaces used to create an indent
     */
    public static int getIntent() {
        return INDENT.length();
    }

    /**
     * Sets weather generated json strings should be formatted if not
     * specified.
     *
     * @param formatted Weather to format json strings if not specified
     */
    public static void setDefaultFormatted(boolean formatted) {
        DEFAULT_FORMATTED = formatted;
    }

    /**
     * Returns whether generated json strings are currently formatted if not
     * specified.
     *
     * @return Whether to format json strings if not specified
     */
    public static boolean isDefaultFormatted() {
        return DEFAULT_FORMATTED;
    }

    /**
     * Registers an external json serializer for a specific class; a function that turns
     * objects of a given type into a json structure. The function may return another object
     * to be serialized recursively, however, make sure not to cause any "serialization loops".
     * The serializer may be invoked with an instance of a subclass as well. This option is only
     * intended to add serialization support for classes that cannot be modified, i.e. because
     * they are part of a library.
     * <p>Some standard java classes are added by default and cannot be overridden. Those
     * serializers cannot be registered and already have a deserializer counterpart registered
     * as well. The same is true for record classes (if the runtime java version supports them).</p>
     * <p>You cannot register a serializer for the {@link Object} class. Furthermore, arrays and
     * classes implementing {@link JsonSerializable} cannot be overridden (each of them already
     * has a serializer defined). Enums are automatically serialized, but you may want to override
     * their serializer in the unlikely case of a mutable enum instance. Serializers for interface
     * types are supported, note however that collections and maps are automatically serialized
     * (although specific properties of the implementation, like a sorting comparator or similar,
     * will not be serialized. In that case an external serializer may be helpful if the class cannot
     * implement JsonSerializable directly).</p>
     *
     * @param type The type to add a serializer for
     * @param toJson The serialization function
     */
    public static <T> void registerExternalSerializer(Class<T> type, Function<? super T, ?> toJson) {
        JsonSerialization.registerSerializer(type, toJson);
    }

    /**
     * Registers a json deserializer for (and only for) the specified type. The function will be
     * used to deserialize raw json data into the specified type when using the {@link JsonElement#as(Class)}
     * functions.
     * <p>Classes can also use annotations to specify deserialization, this method serves as an option
     * to add deserialization to classes that cannot be edited, and to be able to avoid the need of
     * reflection in case the target runtime does not support it.</p>
     * <p>The deserializer for a class should, if possible, be registered when the specified class
     * gets initialized, for example like this:</p>
     * <pre>
     * public class Foo {
     *     static {
     *         Json.registerDeserializer(Foo.class, json -> ...);
     *     }
     *     // Rest of class...
     * }
     * </pre>
     * When the type gets deserialized, the class will be automatically initialized, if not already done,
     * ensuring that the deserializer is registered in time.
     * <p>Some standard java classes are added by default, and some of them cannot be overridden.
     * Also, enums and records (if the runtime java version supports them) are deserialized automatically,
     * although this behaviour can be overridden. Collection classes and Maps <b>cannot</b> be deserialized
     * automatically, because this process does not work with generics (due to type erasure the generic
     * types cannot be specified). However, there are special methods available in {@link JsonElement}
     * (asList(), asMap(),...) to deserialize to these common types</p>
     *
     * @param type The type to be deserialized to by the deserializer
     * @param fromJson The function deserializing json into the specified type
     */
    public static <T> void registerDeserializer(Class<T> type, Function<JsonElement, ? extends T> fromJson) {
        JsonSerialization.registerDeserializer(type, fromJson);
    }

    /**
     * Converts the given object to an appropriate raw json type.
     *
     * @param object The object to convert
     * @return A json value representing the input
     * @throws IllegalJsonTypeException If the type cannot be
     *                                  converted to a json value
     */
    public static Object serialize(Object object) {
        return JsonSerialization.serialize(object);
    }

    /**
     * Converts the given object to an appropriate raw json type.
     *
     * @param object The object to convert
     * @param serializer The serializer to use to serialize the data
     * @return A json value representing the input
     * @throws IllegalJsonTypeException If the type cannot be
     *                                  converted to a json value
     */
    public static Object serialize(Object object, JsonSerializer serializer) {
        return usingSerializer(serializer, () -> serialize(object));
    }

    /**
     * Runs the given code while using the specified serializer for any serialization
     * performed within that code.
     *
     * @param serializer The serializer to use
     * @param code The code to run with the serializer set
     * @return The return value from the code to run
     */
    public static <T> T usingSerializer(JsonSerializer serializer, Supplier<T> code) {
        return JsonSerialization.usingSerializer(serializer, code);
    }

    /**
     * Runs the given code while using the specified serializer for any serialization
     * performed within that code.
     *
     * @param serializer The serializer to use
     * @param code The code to run with the serializer set
     */
    public static void usingSerializer(JsonSerializer serializer, Runnable code) {
        usingSerializer(serializer, () -> { code.run(); return null; });
    }

    /**
     * Deserialized the given json data into an object of the specified type. In general
     * {@link JsonElement#as(Class)} should be preferred over using this method directly.
     *
     * @param type The type to deserialize to
     * @param json The json data to deserialize, can be a json element, a raw json type
     *             or data that first needs to be serialized to json
     * @return The given json data deserialized as the specified type
     * @throws IllegalJsonTypeException If the input is not valid json data
     * @throws IllegalArgumentException If no deserializer is known to deserialize the specified type
     */
    public static <T> T deserialize(Class<T> type, Object json) {
        return JsonElement.wrap(json).as(type);
    }


    // ---------- Internals ----------


    /**
     * Writes the json string representation of the given object into the specified
     * {@link PrintWriter}.
     *
     * @param out The writer to write into
     * @param o The object to write as json string
     * @param blacklist Objects that would be nested
     * @param formatted Whether the string should be formatted
     * @param level The indent level, ignored if {@code formatted} is {@code false}
     */
    static void printStringFor(PrintWriter out, Object o, Set<Object> blacklist, boolean formatted, int level) {
        if(o == null) out.print("null");
        else if(o instanceof Map<?,?>) {
            Map<?,?> m = (Map<?,?>) o;
            if(m.isEmpty()) out.print("{}");

            else {
                out.print('{');
                blacklist.add(m);

                int i = m.size();
                for (Map.Entry<?, ?> member : m.entrySet()) {
                    if(blacklist.contains(member.getValue()))
                        throw new NestedJsonException();
                    if(formatted) {
                        out.println();
                        out.print(repeat(INDENT, level + 1));
                    }
                    Json.printStringFor(out, Objects.toString(Objects.requireNonNull(member.getKey(), "Json objects don't permit 'null' as key")));
                    out.print(':');
                    if(formatted) out.print(' ');
                    Json.printStringFor(out, member.getValue(), blacklist, formatted, level + 1);
                    if(--i != 0) out.print(',');
                }

                if(formatted) {
                    out.println();
                    out.print(repeat(INDENT, level));
                }
                out.print('}');

                blacklist.remove(m); // Allow same instances 'next to each other'
            }
        }
        else if(o instanceof List<?>) {
            List<?> l = (List<?>) o;
            if(l.isEmpty()) out.print("[]");
            else {

                blacklist.add(l);
                out.print('[');

                boolean formatLocal = formatted && l.size() != 1;
                int i = l.size();
                for(Object e : l) {
                    if(blacklist.contains(e))
                        throw new NestedJsonException();
                    if(formatLocal) {
                        out.println();
                        out.print(repeat(INDENT, level + 1));
                    }
                    Json.printStringFor(out, e, blacklist, formatted, level + (formatLocal ? 1 : 0));
                    if(--i != 0) out.print(',');
                }

                if(formatLocal) {
                    out.println();
                    out.print(repeat(INDENT, level));
                }
                out.print(']');

                blacklist.remove(l); // Allow same instances 'next to each other'
            }
        }
        else if(o instanceof Number || o instanceof Boolean) out.print(o);
        else printStringFor(out, o.toString());
    }

    /**
     * Writes the given string escaped into the specified {@link PrintWriter}.
     *
     * @param out The writer to write into
     * @param s The string to write as json
     */
    static void printStringFor(PrintWriter out, String s) {
        out.print('"');
        // Escape control characters
        s = s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("\"", "\\\"");
        // Escape characters that are not in the json charset
        for(int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if(CHARSET.canEncode(s.charAt(i))) out.print(c);
            else {
                String code = Integer.toHexString(c);
                out.print("\\u");
                out.print(repeat("0", 4 - code.length()));
                out.print(code);
            }
        }
        out.print('"');
    }

    /**
     * Parses the path from the given path string.
     *
     * @param path The path as string
     * @return The path, numbers are not parsed
     */
    static Object[] parsePath(String path) {
        return (path.startsWith("[") ? path.substring(1) : path).replace("]", "").split("[\\[.]");
    }

    private static final Pattern PATH_FIELD_PATTERN = Pattern.compile("[a-zA-Z_$][a-zA-Z_$0-9]*");

    static String pathToString(String prefix, Object[] path) {
        StringBuilder str = new StringBuilder(prefix);
        for(Object o : path) {
            if(o instanceof Number)
                str.append('[').append(o).append(']');
            else if(!(o instanceof CharSequence) || !PATH_FIELD_PATTERN.matcher((CharSequence) o).matches())
                str.append('[').append(escape(o)).append(']');
            else {
                if(str.length() != 0 && str.charAt(str.length() - 1) == ']')
                    str.append('.');
                str.append((CharSequence) o);
            }
        }
        return str.toString();
    }

    static String appendToPath(String path, Object o) {
        if(o instanceof Number)
            return path+"["+o+"]";
        if(!(o instanceof CharSequence) || !PATH_FIELD_PATTERN.matcher((CharSequence) o).matches())
            return path+"["+escape(o)+"]";
        else return path + (path.isEmpty() ? "" : ".") + o;
    }

    static String prependToPath(String path, Object o) {
        if(path == null)
            path = "";
        if(o instanceof Number)
            return "["+o+"]"+path;
        if(!(o instanceof CharSequence) || !PATH_FIELD_PATTERN.matcher((CharSequence) o).matches())
            return "["+escape(o)+"]"+path;
        else return o + (path.isEmpty() || path.charAt(0) == '[' ? "" : ".") + path;
    }

    static String joinPaths(String a, String b) {
        if(a == null) a = "";
        if(b == null) b = "";
        return a + (a.isEmpty() || b.isEmpty() || b.startsWith("[") ? "" : ".") + b;
    }

    static String repeat(String str, int times) {
        StringBuilder out = new StringBuilder();
        //noinspection StringRepeatCanBeUsed
        for(int i=0; i<times; i++)
            out.append(str);
        return out.toString();
    }
}
