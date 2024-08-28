package de.rccookie.json;

public abstract class JsonSerializer {

    public static final JsonSerializer DEFAULT = new JsonSerializer() { };

    public Object serialize(Object data) {
        return JsonSerialization.doSerialize(data);
    }
}
