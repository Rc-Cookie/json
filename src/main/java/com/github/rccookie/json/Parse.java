package com.github.rccookie.json;

import java.util.List;

/**
 * Utility class for parsing json-style strings into different formats.
 */
public final class Parse {

    private Parse() {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the given string into a map. The string has to be in json
     * syntax and representing a json object, but the outer brackets may
     * be omitted and single quotes with be threatened as double quotes,
     * for convenience.
     *
     * @param string The string to parse
     * @return The parsed map
     */
    public static JsonObject map(String string) {
        if(string == null) return null;
        string = string.stripLeading().replace('\'', '"');
        if(!string.startsWith("null") && !string.startsWith("{"))
            string = "{" + string + "}";
        return Json.parse(string).asObject();
    }

    /**
     * Parses the given string into a list. The string has to be in json
     * syntax and representing a json array, but the outer brackets may
     * be omitted and single quotes with be threatened as double quotes,
     * for convenience.
     *
     * @param string The string to parse
     * @return The parsed list
     */
    public static JsonArray list(String string) {
        if(string == null) return null;
        string = string.stripLeading().replace('\'', '"');
        if(!string.startsWith("["))
            string = "[" + string + "]";
        return Json.parse(string).asArray();
    }

    /**
     * Parses the given string into an array. The string has to be in json
     * syntax and representing a json array, but the outer brackets may
     * be omitted and single quotes with be threatened as double quotes,
     * for convenience.
     *
     * @param string The string to parse
     * @return The parsed array
     */
    public static Object[] array(String string) {
        List<Object> list = list(string);
        return list == null ? null : list.toArray();
    }

    /**
     * Parses the given string into a json element. The string has to be
     * in json syntax and representing a json array or a json object, but
     * single quotes with be threatened as double quotes, for convenience.
     *
     * @param string The string to parse
     * @return The parsed element
     */
    public static JsonElement json(String string) {
        if(string == null) return null;
        string = string.stripLeading().replace('\'', '"');
        return Json.parse(string);
    }
}
