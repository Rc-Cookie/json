package com.github.rccookie.json;

/**
 * Thrown to indicate that a string that should be json formatted does
 * not follow json syntax.
 */
public class JsonParseException extends RuntimeException {

    JsonParseException(String message) {
        super(message);
    }

    JsonParseException(Object expected, Object found) {
        this("'" + expected + "', '" + found + "' found");
    }
}
