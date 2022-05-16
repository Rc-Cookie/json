package com.github.rccookie.json.newer;

import com.github.rccookie.json.JsonSerializable;

final class JsonUtils {

    private JsonUtils() {
        throw new UnsupportedOperationException();
    }



    static String validateKey(String key) {
        if(key == null)
            throw new IllegalArgumentException("Json does not allow null keys");
        return key;
    }

    static <T> T validateValue(T value) {
        if(!(
                value == null ||
                        value instanceof JsonSerializable ||
                        value instanceof String ||
                        value instanceof Number ||
                        value instanceof Boolean
        )) throw new IllegalArgumentException(value.getClass() + " is not a valid json type");
        return value;
    }
}
