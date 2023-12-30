package de.rccookie.json;

public class IllegalJsonDeserializerException extends RuntimeException {

    public IllegalJsonDeserializerException() {
    }

    public IllegalJsonDeserializerException(String message) {
        super(message);
    }

    public IllegalJsonDeserializerException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalJsonDeserializerException(Throwable cause) {
        super(cause);
    }

    public IllegalJsonDeserializerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
