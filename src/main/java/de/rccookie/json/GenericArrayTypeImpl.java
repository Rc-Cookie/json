package de.rccookie.json;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;

final class GenericArrayTypeImpl implements GenericArrayType, TypeBuilder {
    private final Type genericComponentType;

    GenericArrayTypeImpl(Type ct) {
        genericComponentType = Objects.requireNonNull(ct);
    }

    @Override
    public Type getGenericComponentType() {
        return genericComponentType; // return cached component type
    }

    public String toString() {
        return getGenericComponentType().getTypeName() + "[]";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GenericArrayType && Objects.equals(genericComponentType, ((GenericArrayType) o).getGenericComponentType());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(genericComponentType);
    }
}
