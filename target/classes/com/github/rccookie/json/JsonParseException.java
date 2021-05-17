package com.github.rccookie.json;

/**
 * Thrown to indicate that a string that should be json formatted does
 * not follow json syntax or does not resemble the expected object type
 * (json object / json array).
 */
public class JsonParseException extends RuntimeException {

    /**
     * Creates a new json parse exception with the given error message.
     *
     * @param message A message describing the reason for the exception
     */
    JsonParseException(String message) {
        super(message);
    }
}
