package de.rccookie.json;

public class MissingParameterException extends RuntimeException {
    public MissingParameterException(Class<?> type, String name) {
        throw new NullPointerException("Missing "+type.getSimpleName()+" value "+name);
    }
}
