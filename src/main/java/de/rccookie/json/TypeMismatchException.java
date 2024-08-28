package de.rccookie.json;

import java.lang.reflect.Type;

import org.jetbrains.annotations.Nullable;

public class TypeMismatchException extends JsonDeserializationException {

    public TypeMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeMismatchException(String message) {
        this(message, (Throwable) null);
    }

    public TypeMismatchException(String fieldName, String message, Throwable cause) {
        this((fieldName != null && !fieldName.isEmpty() ? "Wrong type for '"+fieldName+"': " : "")+message, cause);
    }

    public TypeMismatchException(String fieldName, String message) {
        this(fieldName, message, null);
    }

    public TypeMismatchException(String fieldName, Type required, Type found, @Nullable Throwable cause) {
        this(fieldName, "Cannot convert "+typeString(found)+" to "+typeString(required), cause);
    }

    public TypeMismatchException(String fieldName, Type required, Type found) {
        this(fieldName, required, found, null);
    }

    public TypeMismatchException(Type required, Type found, @Nullable Throwable cause) {
        this("Cannot convert "+typeString(found)+" to "+typeString(required), cause);
    }

    public TypeMismatchException(Type required, Type found) {
        this(required, found, null);
    }
}
