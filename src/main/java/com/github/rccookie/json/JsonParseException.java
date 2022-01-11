package com.github.rccookie.json;

import java.util.NoSuchElementException;

/**
 * Thrown to indicate that a string that should be json formatted does
 * not follow json syntax.
 */
public class JsonParseException extends NoSuchElementException { // To conform Iterator.next() exception type in JsonParser

    JsonParseException(String message, JsonReader json) {
        super(message + " (at " + json.getPosition() + ')');
    }

    JsonParseException(Object expected, Object found, JsonReader json) {
        this("Expected '" + expected + "', found '" + found + "'", json);
    }
}
