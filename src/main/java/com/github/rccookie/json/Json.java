package com.github.rccookie.json;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Set;

/**
 * Utility class to parse from and to json. Json formatting supports single line
 * and multiline comments, and trailing commas.
 */
public final class Json {

    private Json() {
        throw new UnsupportedOperationException();
    }



    public static final String INDENT = "    ";



    /**
     * Converts the given json structure to a json string.
     *
     * @param jsonStructure The json structure to convert
     * @return The json string representing the json structure
     */
    public static String toString(JsonStructure jsonStructure) {
        return jsonStructure == null ? "null" : jsonStructure.toString();
    }



    /**
     * Parses the given json formatted string into a json object.
     *
     * @param jsonString The json formatted string to parse
     * @return The parsed object
     * @throws JsonParseException If the string does not follow json syntax
     *                            or describes an array rather than an object
     */
    public static JsonObject parseObject(String jsonString) throws JsonParseException {
        try {
            return parseNextObject(new NicerStringBuilder(jsonString));
        } catch(IndexOutOfBoundsException e) {
            throw new JsonParseException("Reached end of file while parsing");
        }
    }

    /**
     * Parses the given json formatted string into a json array.
     *
     * @param jsonString The json formatted string to parse
     * @return The parsed array
     * @throws JsonParseException If the string does not follow json syntax
     *                            or describes an object rather than an array
     */
    public static JsonArray parseArray(String jsonString) throws JsonParseException {
        try {
            return parseNextArray(new NicerStringBuilder(jsonString));
        } catch(IndexOutOfBoundsException e) {
            throw new JsonParseException("Reached end of file while parsing");
        }
    }

    /**
     * Parses the given json formatted string into a json structure.
     *
     * @param jsonString The json formatted string to parse
     * @return The parsed structure
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonElement parse(String jsonString) throws JsonParseException {
        return new JsonElement.EmptyJsonElement(removeComment(new NicerStringBuilder(jsonString.strip())).startsWith("{") ? parseObject(jsonString) : parseArray(jsonString));
    }



    /**
     * Parses the given json formatted file into a json object. If an
     * {@link IOException} occurres {@code null} will be returned.
     *
     * @param file The file to parse from
     * @return The parsed object
     * @throws JsonParseException If the string does not follow json syntax
     *                            or describes an array rather than an object
     */
    public static JsonObject loadObject(File file) throws JsonParseException {
        try {
            return parseObject(Files.readString(file.toPath()));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parses the given json formatted file into a json array. If an
     * {@link IOException} occurres {@code null} will be returned.
     *
     * @param file The file to parse from
     * @return The parsed array
     * @throws JsonParseException If the string does not follow json syntax
     *                            or describes an object rather than an array
     */
    public static JsonArray loadArray(File file) throws JsonParseException {
        try {
            return parseArray(Files.readString(file.toPath()));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parses the given json formatted file into a json structure. If an
     * {@link IOException} occurres {@code null} will be returned.
     *
     * @param file The file to parse from
     * @return The parsed structure
     * @throws JsonParseException If the string does not follow json syntax
     */
    public static JsonElement load(File file) throws JsonParseException {
        try {
            return parse(Files.readString(file.toPath()));
        } catch (IOException e) {
            return null;
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



    private static JsonObject parseNextObject(NicerStringBuilder json) {
        removeComment(json);
        if(json.startsWith("null")) {
            json.delete(4);
            return null;
        }

        char unexpected = json.getAndDeleteFirst();
        if(unexpected != '{') throw new JsonParseException("'{' expected, '" + unexpected + "' found");

        JsonObject object = new JsonObject();
        if(removeComment(json.stripLeading()).startsWith('}')) {
            json.deleteFirst();
            return object;
        }

        do {
            String key = parseNextString(json);
            removeComment(json.stripLeading());
            if((unexpected = json.getAndDeleteFirst()) != ':')
                throw new JsonParseException(" ': <value>' expected, '" + unexpected + "' found");
            Object value = parseNextValue(removeComment(json.stripLeading()));
            if(object.put(key, value) != null)
                throw new JsonParseException("Duplicate key '" + key + "'");

            if (removeComment(json.stripLeading()).startsWith(','))
                json.deleteFirst().stripLeading();
        } while(noEndOfLine(removeComment(json).first()));

        if((unexpected = removeComment(json).getAndDeleteFirst()) != '}')
            throw new JsonParseException("'}' expected, '" + unexpected + "' found");
        return object;
    }

    private static JsonArray parseNextArray(NicerStringBuilder json) {
        removeComment(json);
        if(json.startsWith("null")) {
            json.delete(4);
            return null;
        }

        char unexpected = json.getAndDeleteFirst();
        if(unexpected != '[') throw new JsonParseException("'[' expected, '" + unexpected + "' found (" + json + ")");

        JsonArray array = new JsonArray();
        if(removeComment(json.stripLeading()).startsWith(']')) {
            json.deleteFirst();
            return array;
        }

        do {
            array.add(parseNextValue(json));

            if (removeComment(json.stripLeading()).startsWith(','))
                json.deleteFirst().stripLeading();
        } while(noEndOfLine(removeComment(json).first()));

        if((unexpected = removeComment(json).getAndDeleteFirst()) != ']')
            throw new JsonParseException("']' expected, '" + unexpected + "' found");
        return array;
    }

    private static Object parseNextValue(NicerStringBuilder json) {
        if(removeComment(json).startsWith("null")) {
            json.delete(4);
            return null;
        }

        if(json.startsWith("true")) {
            json.delete(4);
            return true;
        }
        if(json.startsWith("false")) {
            json.delete(5);
            return false;
        }

        if(json.startsWith('{')) return parseNextObject(json);
        if(json.startsWith('[')) return parseNextArray(json);
        if(json.startsWith('"')) return parseNextString(json);

        StringBuilder numberString = new StringBuilder();
        while(noEndOfLine(json.first()))
            numberString.append(json.getAndDeleteFirst());

        if(numberString.length() == 0)
            throw new JsonParseException("Expected value, found end of line");
        String num = numberString.toString();

        try {
            return Integer.parseInt(num);
        } catch(NumberFormatException ignored) { }
        try {
            return Long.parseLong(num);
        } catch(NumberFormatException ignored) { }
        try {
            return Double.parseDouble(num);
        } catch(NumberFormatException ignored) { }
        try {
            return Float.parseFloat(num);
        } catch(NumberFormatException ignored) { }
        try {
            return Short.parseShort(num);
        } catch(NumberFormatException ignored) { }
        try {
            return Byte.parseByte(num);
        } catch(NumberFormatException ignored) { }

        throw new JsonParseException("Unexpected non-string value '" + num + "'");
    }

    // 'null' is not parsed with this method!
    private static String parseNextString(NicerStringBuilder json) {
        char unexpected = json.getAndDeleteFirst();
        if(unexpected != '"') throw new JsonParseException("'\"' expected, '" + unexpected + "' found");

        StringBuilder string = new StringBuilder();
        while(!json.startsWith('"')) {
            char c = json.getAndDeleteFirst();
            if(c == '\\') {
                c = json.getAndDeleteFirst();
                if(c == 't') c = '\t';
                else if(c == 'b') c = '\b';
                else if(c == 'n') c = '\n';
                else if(c == 'r') c = '\r';
                else if(c == 'f') c = '\f';
                else if(c != '\\' && c != '"') throw new JsonParseException("Unknown character: '\\" + c + "'");
            }
            string.append(c);
        }

        if((unexpected = json.getAndDeleteFirst()) != '"') throw new JsonParseException("'\"' expected, '" + unexpected + "' found");
        return string.toString();
    }

    private static NicerStringBuilder removeComment(NicerStringBuilder json) {
        if(json.startsWith("//")) {
            int index = json.indexOf('\n');
            if(index == -1) throw new JsonParseException("Reached end of line during comment");
            json.delete(index + 1).stripLeading();
            return removeComment(json);
        }
        if(json.startsWith("/*")) {
            int index = json.indexOf("*/");
            if(index == -1) throw new JsonParseException("Reached end of line during comment");
            json.delete(index + 2).stripLeading();
            return removeComment(json);
        }
        return json;
    }

    private static boolean noEndOfLine(char c) {
        return c != ',' && c != ']' && c != '}' && c != '/'; // '/' -> Comment
    }



    static String stringFor(Object o, Set<Object> blacklist, int level) {
        if(o == null) return "null";
        if(o instanceof JsonObject)
            return ((JsonObject)o).toString(blacklist, level);
        if(o instanceof JsonArray)
            return ((JsonArray)o).toString(blacklist, level);
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
}
