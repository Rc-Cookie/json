package com.github.rccookie.json.newer;

public class JsonPathSyntaxException extends RuntimeException {

    JsonPathSyntaxException(String message) {
        super(message);
    }

    JsonPathSyntaxException(Throwable cause) {
        super(cause);
    }
}
