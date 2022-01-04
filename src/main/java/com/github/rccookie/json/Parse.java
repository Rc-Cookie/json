package com.github.rccookie.json;

import java.util.List;
import java.util.Map;

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
    public static Map<String, Object> map(String string) {
        if(string == null) return null;
        string = string.stripLeading().replace('\'', '"');
        if(!string.startsWith("null") && !string.startsWith("{"))
            string = "{" + string + "}";
        return Json.parseObject(string);
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
    public static List<Object> list(String string) {
        if(string == null) return null;
        string = string.stripLeading().replace('\'', '"');
        if(!string.startsWith("["))
            string = "[" + string + "]";
        return Json.parseArray(string);
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
}
