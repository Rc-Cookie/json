package com.github.rccookie.json;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Set;

/**
 * Utility class to parse from and to json. Json formatting supports single line
 * and multiline comments, and trailing commas.
 */
public final class Json {

    private Json() {
        throw new UnsupportedOperationException();
    }



    static String INDENT = "    ";

    static boolean DEFAULT_FORMATTED = true;



    /**
     * Converts the given json structure to a json string.
     *
     * @param jsonStructure The json structure to convert
     * @return The json string representing the json structure
     */
    public static String toString(JsonStructure jsonStructure) {
        return toString(jsonStructure, DEFAULT_FORMATTED);
    }

    /**
     * Converts the given json structure to a json string.
     *
     * @param jsonStructure The json structure to convert
     * @param formatted Weather the json string should be formatted
     *                  with indents and newlines
     * @return The json string representing the json structure
     */
    public static String toString(JsonStructure jsonStructure, boolean formatted) {
        return jsonStructure == null ? "null" : jsonStructure.toString(formatted);
    }



    /**
     * Parses the given json formatted string into a json object. Throws a
     * {@link ClassCastException} if the json string describes a json array.
     *
     * @param jsonString The json formatted string to parse
     * @return The parsed object
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonObject parseObjectString(String jsonString) throws JsonParseException {
        return parseString(jsonString).asObject();
    }

    /**
     * Parses the given json formatted string into a json array. Throws a
     * {@link ClassCastException} if the json string describes a json object.
     *
     * @param jsonString The json formatted string to parse
     * @return The parsed array
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonArray parseArrayString(String jsonString) throws JsonParseException {
        return parseString(jsonString).asArray();
    }

    /**
     * Parses the given json formatted string into a json structure.
     *
     * @param jsonString The json formatted string to parse
     * @return The parsed structure
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonElement parseString(String jsonString) throws JsonParseException {
        return parse(new StringReader(jsonString));
    }



    /**
     * Parses the given json formatted file into a json object. If an
     * {@link IOException} occurres an empty json element will be returned.
     * Throws a {@link ClassCastException} if the json file describes a
     * json array.
     *
     * @param file The file to parse from
     * @return The parsed json object
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonObject parseObject(String file) throws JsonParseException {
        return parse(file).asObject();
    }

    /**
     * Parses the given json formatted file into a json array. If an
     * {@link IOException} occurres an empty json element will be returned.
     * Throws a {@link ClassCastException} if the json file describes a
     * json object.
     *
     * @param file The file to parse from
     * @return The parsed json array
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonArray parseArray(String file) throws JsonParseException {
        return parse(file).asArray();
    }

    /**
     * Parses the given json formatted file into a json element. If an
     * {@link IOException} occurres an empty json element will be returned.
     *
     * @param file The file to parse from
     * @return The parsed json element
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonElement parse(String file) throws JsonParseException {
        try {
            return parse(new FileReader(file));
        } catch(IOException e) {
            e.printStackTrace();
            return EmptyJsonElement.INSTANCE;
        }
    }


    /**
     * Parses the given json formatted file into a json object. If an
     * {@link IOException} occurres an empty json element will be returned.
     * Throws a {@link ClassCastException} if the json file describes a
     * json array.
     *
     * @param file The file to parse from
     * @return The parsed json object
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonObject parseObject(File file) throws JsonParseException {
        return parse(file).asObject();
    }

    /**
     * Parses the given json formatted file into a json array. If an
     * {@link IOException} occurres an empty json element will be returned.
     * Throws a {@link ClassCastException} if the json file describes a
     * json object.
     *
     * @param file The file to parse from
     * @return The parsed json array
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonArray parseArray(File file) throws JsonParseException {
        return parse(file).asArray();
    }

    /**
     * Parses the given json formatted file into a json element. If an
     * {@link IOException} occurres an empty json element will be returned.
     *
     * @param file The file to parse from
     * @return The parsed json element
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonElement parse(File file) throws JsonParseException {
        try {
            return parse(new FileReader(file));
        } catch(IOException e) {
            e.printStackTrace();
            return EmptyJsonElement.INSTANCE;
        }
    }


    /**
     * Parses the given json formatted input into a json object. If an
     * {@link IOException} occurres an empty json element will be returned.
     * Throws a {@link ClassCastException} if the json file describes a
     * json array.
     *
     * @param reader The file to parse from
     * @return The parsed json object
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonObject parseObject(Reader reader) throws JsonParseException {
        return parse(reader).asObject();
    }

    /**
     * Parses the given json formatted input into a json array. If an
     * {@link IOException} occurres an empty json element will be returned.
     * Throws a {@link ClassCastException} if the json file describes a
     * json object.
     *
     * @param reader The file to parse from
     * @return The parsed json array
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonArray parseArray(Reader reader) throws JsonParseException {
        return parse(reader).asArray();
    }

    /**
     * Parses the given json formatted input into a json element. If an
     * {@link IOException} occurres an empty json element will be returned.
     *
     * @param reader The reader to parse from
     * @return The parsed json element
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonElement parse(Reader reader) throws JsonParseException {
        try {
            JsonParser parser = new JsonParser(reader);
            JsonElement result = new FullJsonElement(parser.parseNextStructure());
            parser.close();
            return result;
        } catch(UncheckedIOException e) {
            e.getCause().printStackTrace();
            return EmptyJsonElement.INSTANCE;
        }
    }



    /**
     * Converts the given json structure into a json string and stores it
     * in the specified file. If the file exists it will be cleared before,
     * otherwise a new file will be created.
     *
     * @param jsonStructure The json element to store
     * @param file The file to store the structure in
     * @return Weather the storing was successful
     */
    public static boolean store(JsonStructure jsonStructure, File file) {
        try(PrintWriter p = new PrintWriter(file)) {
            p.println(toString(jsonStructure));
            return true;
        } catch(IOException e) {
            return false;
        }
    }


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





    static String stringFor(Object o, Set<Object> blacklist, boolean formatted, int level) {
        if(o == null) return "null";
        if(o instanceof JsonObject)
            return ((JsonObject)o).toString(blacklist, formatted, level);
        if(o instanceof JsonArray)
            return ((JsonArray)o).toString(blacklist, formatted, level);
        if(o instanceof Number || o instanceof Boolean) return o.toString();
        return stringFor(o.toString());
    }

    static String stringFor(String s) {
        return '"' + (s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("\"", "\\\"")) + '"';
    }

    static Object[] parsePath(String path) {
        return path.replace("]", "").split("[\\[.]");
    }
}
