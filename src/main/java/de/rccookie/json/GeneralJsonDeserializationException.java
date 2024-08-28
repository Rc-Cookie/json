package de.rccookie.json;

import java.lang.reflect.Type;

public class GeneralJsonDeserializationException extends JsonDeserializationException {

    private final JsonElement json;
    private final Type targetType;

    public GeneralJsonDeserializationException(JsonElement json, Type targetType, Throwable cause) {
        super("Failed to deserialize json '" + (json.isArray()?"[...]":json.isObject()?"{...}":Json.toString(json, false)) + "' to " + (targetType instanceof Class ? ((Class<?>) targetType).getSimpleName() : targetType.getTypeName()) + (cause != null && cause.getMessage() != null ? ": "+cause.getMessage() : ""), cause);
        this.json = json;
        this.targetType = targetType;
    }

    public JsonElement json() {
        return json;
    }

    public final Type targetType() {
        return targetType;
    }
}
