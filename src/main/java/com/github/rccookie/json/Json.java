package com.github.rccookie.json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.IdentityHashMap;
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
        return stringFor(new StringBuilder(), object, Collections.newSetFromMap(new IdentityHashMap<>()), formatted, 0).toString();
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
        if(object == null) return "\"null\"";
        if(!(object instanceof List<?> || object instanceof Map<?,?> || object instanceof Number || object instanceof Boolean || object.getClass().isArray())) // Don't escape strings twice
            return stringFor(new StringBuilder(), object.toString()).toString();
        return stringFor(new StringBuilder(), toString(object, formatted)).toString();
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


    // ---------- Storing ----------


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
        try(PrintWriter p = new PrintWriter(file)) {
            p.println(toString(value));
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
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
     * {@link StringBuilder}.
     *
     * @param out The string builder to write into
     * @param o The object to write as json string
     * @param blacklist Objects that would be nested
     * @param formatted Whether the string should be formatted
     * @param level The indent level, ignored if {@code formatted} is {@code false}
     * @return {@code out}
     */
    static StringBuilder stringFor(StringBuilder out, Object o, Set<Object> blacklist, boolean formatted, int level) {
        if(o == null) return out.append("null");
        if(o instanceof Map<?,?>) {
            Map<?,?> m = (Map<?,?>) o;
            if(m.isEmpty()) return out.append("{}");

            out.append('{');
            blacklist.add(m);

            for (Map.Entry<?,?> member : m.entrySet()) {
                if(blacklist.contains(member.getValue()))
                    throw new NestedJsonException();
                if(formatted) out.append('\n').append(INDENT.repeat(level + 1));
                Json.stringFor(out, Objects.toString(Objects.requireNonNull(member.getKey(), "Json objects don't permit 'null' as key"))).append(':');
                if(formatted) out.append(' ');
                Json.stringFor(out, member.getValue(), blacklist, formatted, level + 1).append(',');
            }

            out.deleteCharAt(out.length() - 1);
            if(formatted) out.append('\n').append(INDENT.repeat(level));

            blacklist.remove(m); // Allow same instances 'next to each other'
            return out.append('}');
        }
        if(o instanceof List<?>) {
            List<?> l = (List<?>) o;
            if(l.isEmpty()) return out.append("[]");

            blacklist.add(l);
            out.append('[');
            for(Object e : l) {
                if(blacklist.contains(e))
                    throw new NestedJsonException();
                if(formatted) out.append('\n').append(INDENT.repeat(level + 1));
                Json.stringFor(out, e, blacklist, formatted, level + 1).append(',');
            }

            out.deleteCharAt(out.length() - 1);
            if(formatted) out.append('\n').append(INDENT.repeat(level));

            blacklist.remove(l); // Allow same instances 'next to each other'
            return out.append(']');
        }
        if(o.getClass().isArray()) {
            int l = Array.getLength(o);
            if(l == 0) return out.append("[]");

            blacklist.add(o);
            out.append('[');
            for(int i=0; i<l; i++) {
                Object e = Array.get(o, i);
                if(blacklist.contains(e))
                    throw new NestedJsonException();
                if(formatted) out.append('\n').append(INDENT.repeat(level + 1));
                Json.stringFor(out, e, blacklist, formatted, level + 1).append(',');
            }

            out.deleteCharAt(out.length() - 1);
            if(formatted) out.append('\n').append(INDENT.repeat(level));

            blacklist.remove(l); // Allow same instances 'next to each other'
            return out.append(']');
        }
        if(o instanceof Number || o instanceof Boolean) return out.append(o);
        return stringFor(out, o.toString());
    }

    /**
     * Writes the given string escaped into the specified {@link StringBuilder}.
     *
     * @param out The string builder to write into
     * @param s The string to write as json
     * @return {@code out}
     */
    static StringBuilder stringFor(StringBuilder out, String s) {
        out.append('"');
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
            if(CHARSET.canEncode(s.charAt(i))) out.append(c);
            else {
                String code = Integer.toHexString(c);
                out.append("\\u").append("0".repeat(4 - code.length())).append(code);
            }
        }
        return out.append('"');
    }

    /**
     * Parses the path from the given path string.
     *
     * @param path The path as string
     * @return The path, numbers are not parsed
     */
    static Object[] parsePath(String path) {
        if(path.startsWith("[")) path = path.substring(1);
        return path.replace("]", "").split("[\\[.]");
    }
}
