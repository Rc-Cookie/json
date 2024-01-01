package de.rccookie.json;

public class MissingFieldException extends RuntimeException {
    public MissingFieldException(Class<?> type, String name) {
        throw new NullPointerException("Missing "+type.getSimpleName()+" value "+name);
    }
}
