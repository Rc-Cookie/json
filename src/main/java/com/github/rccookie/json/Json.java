package com.github.rccookie.json;

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
import java.lang.reflect.Array;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 * <li>{@code [<key or index>]}
 * <li>{@code .<key or index>}
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
        object = extractJson(object);
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
     * @throws UncheckedIOException If an {@link IOException} occurres while
     *                              reading the file
     */
    public static JsonElement load(String file) throws JsonParseException {
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
     * @throws UncheckedIOException If an {@link IOException} occurres while
     *                              reading the file
     */
    public static JsonElement load(File file) throws JsonParseException {
        try {
            return parse(new FileReader(file));
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
     * @throws UncheckedIOException If an {@link IOException} occurres while
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
     * @throws UncheckedIOException If an {@link IOException} occurres
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
     * @throws UncheckedIOException If an {@link IOException} occurres
     * @see #toString(Object)
     */
    public static void store(Object value, File file, boolean formatted) {
        try(PrintWriter p = new PrintWriter(file)) {
            write(value, p);
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
        printStringFor(writer instanceof PrintWriter ? ((PrintWriter) writer) : new PrintWriter(writer), extractJson(value), new HashSet<>(), formatted, 0);
    }


    // ---------- Preferences ----------


    /**
     * Sets the indent size used when generating a formatted json string.
     * Must not be negative.
     *
     * @param spaceCount The number of spaces used to create an indent
     */
    public static void setIndent(int spaceCount) {
        if(spaceCount < 0) throw new IllegalArgumentException("Indent space count must be positive");
        INDENT = " ".repeat(spaceCount);
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
                        out.print(INDENT.repeat(level + 1));
                    }
                    Json.printStringFor(out, Objects.toString(Objects.requireNonNull(member.getKey(), "Json objects don't permit 'null' as key")));
                    out.print(':');
                    if(formatted) out.print(' ');
                    Json.printStringFor(out, member.getValue(), blacklist, formatted, level + 1);
                    if(--i != 0) out.print(',');
                }

                if(formatted) {
                    out.println();
                    out.print(INDENT.repeat(level));
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

                int i = l.size();
                for (Object e : l) {
                    if(blacklist.contains(e))
                        throw new NestedJsonException();
                    if(formatted) {
                        out.println();
                        out.print(INDENT.repeat(level + 1));
                    }
                    Json.printStringFor(out, e, blacklist, formatted, level + 1);
                    if(--i != 0) out.print(',');
                }

                if(formatted) {
                    out.println();
                    out.print(INDENT.repeat(level));
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
                out.print("0".repeat(4 - code.length()));
                out.print(code);
            }
        }
        out.print('"');
    }

    /**
     * Converts the given object to an appropriate json type.
     *
     * @param object The object to convert
     * @return A json value representing the input
     * @throws IllegalJsonTypeException If the type cannot be
     *                                  converted to a json value
     */
    public static Object extractJson(Object object) {
        while(true) {
            if(object instanceof JsonSerializable)
                object = ((JsonSerializable) object).toJson();
            else if(object != null && !(object instanceof JsonStructure)) {
                if(object.getClass().isArray()) {
                    JsonArray array = new JsonArray();
                    int size = Array.getLength(object);
                    for(int i=0; i<size; i++)
                        array.add(Array.get(object, i));
                    return array; // Definitely valid
                }
                else if(object instanceof List)
                    return new JsonArray((List<?>) object);
                else if(object instanceof Map)
                    return new JsonObject((Map<?,?>) object);
                else break;
            }
            else break;
        }
        return validateJsonType(object);
    }

    /**
     * Validates that the given object is of a valid json type.
     *
     * @param o The object to check
     * @return The input
     * @throws IllegalJsonTypeException If the object is not of a valid
     *                                  json type
     */
    static Object validateJsonType(Object o) throws IllegalJsonTypeException {
        if(!(
            o == null ||
            o instanceof Number ||
            o instanceof String ||
            o instanceof Boolean ||
            o instanceof JsonStructure ||
            o instanceof JsonSerializable
        )) throw new IllegalJsonTypeException(o);
        return o;
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
}
