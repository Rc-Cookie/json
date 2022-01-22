package com.github.rccookie.json;

public class IllegalJsonTypeException extends IllegalArgumentException {

    public IllegalJsonTypeException(Object o) {
        super(o.getClass().getName() + " is not a valid json type");
    }
}
