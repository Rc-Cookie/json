package com.github.rccookie.json.newer;

import com.github.rccookie.json.JsonArray;
import com.github.rccookie.json.JsonObject;
import com.github.rccookie.json.JsonSerializable;

import static com.github.rccookie.json.newer.JsonUtils.validateValue;

public abstract class JsonValue implements Iterable<JsonValue>, JsonSerializable {

    public abstract <T> T as(Class<T> type);

    public abstract JsonObject asObject();

    public abstract JsonArray asArray();

    public abstract String asString();

    public abstract Number asNumber();

    public abstract long asLong();

    public abstract int asInt();

    public abstract double asDouble();

    public abstract float asFloat();





    public static JsonValue of(Object o) {
        validateValue(o);
        return null;
    }
}
