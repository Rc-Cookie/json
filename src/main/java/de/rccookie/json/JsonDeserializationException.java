package de.rccookie.json;

import java.lang.reflect.Type;

public class JsonDeserializationException extends RuntimeException {

    public JsonDeserializationException() { }

    public JsonDeserializationException(String message) {
        super(message);
    }

    public JsonDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonDeserializationException(Throwable cause) {
        super(cause);
    }

    public JsonDeserializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    protected static String typeString(Type type) {
        return type instanceof Class<?> ? ((Class<?>) type).getSimpleName() : type.getTypeName();
    }
}
