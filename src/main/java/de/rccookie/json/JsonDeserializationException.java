package de.rccookie.json;

public class JsonDeserializationException extends RuntimeException {

    private final JsonElement json;
    private final Class<?> targetClass;

    public JsonDeserializationException(JsonElement json, Class<?> targetClass, Throwable cause) {
        super("Failed to deserialize json '"+(json.isArray()?"[...]":json.isObject()?"{...}":Json.toString(json, false))+"' to "+targetClass, cause);
        this.json = json;
        this.targetClass = targetClass;
    }

    public JsonElement json() {
        return json;
    }

    public final Class<?> targetClass() {
        return targetClass;
    }
}
