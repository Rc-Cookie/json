package de.rccookie.json;

import java.lang.reflect.Type;

public class MissingFieldException extends RuntimeException {
    public MissingFieldException(Type type, String name) {
        throw new NullPointerException("Missing "+type.getTypeName()+" value "+name); // TODO: Resolve type parameter names
    }
}
