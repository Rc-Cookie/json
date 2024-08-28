package de.rccookie.json;

import java.lang.reflect.Type;

public class MissingFieldException extends JsonDeserializationException {
    public MissingFieldException(Type type, String name) {
        super("Missing "+typeString(type)+" value '"+name+"'"); // TODO: Resolve type parameter names
    }
}
