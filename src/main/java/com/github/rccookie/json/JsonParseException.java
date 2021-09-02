package com.github.rccookie.json;

/**
 * Thrown to indicate that a string that should be json formatted does
 * not follow json syntax.
 */
public class JsonParseException extends RuntimeException {

    JsonParseException(String message, JsonStringBuilder json) {
        super(message + " (" + json.getPosition() + ')');
    }

    JsonParseException(Object expected, Object found, JsonStringBuilder json) {
        this("Expected '" + expected + "', found '" + found + "'", json);
    }
}
