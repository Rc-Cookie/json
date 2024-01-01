package de.rccookie.json;

public class IllegalJsonTypeException extends IllegalArgumentException {

    public IllegalJsonTypeException(Class<?> type) {
        super("'"+type+"' cannot be converted to Json; it does not implement JsonSerializable, is not an enum or a concrete class and no matching external serializer was registered");
    }
}
