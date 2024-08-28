package de.rccookie.json;

import java.lang.reflect.Type;

public interface TypeBuilder extends Type {


    default TypeBuilder array() {
        return array(this);
    }

    default TypeBuilder array(int depth) {
        return array(this, depth);
    }

    default TypeBuilder inner(Class<?> innerRawType, Type... typeParameters) {
        return inner(innerRawType, this, typeParameters);
    }


    static TypeBuilder generic(Class<?> rawType, Type... typeParameters) {
        if(typeParameters.length == 0)
            throw new IllegalArgumentException("Generic type parameter required");
        return new ParameterizedTypeImpl(rawType, typeParameters, null);
    }

    static TypeBuilder inner(Class<?> innerRawType, Type outerType, Type... typeParameters) {
        if(typeParameters.length == 0 && (outerType == null || outerType instanceof Class<?>))
            throw new IllegalArgumentException("Generic type parameter required on inner or outer class");
        return new ParameterizedTypeImpl(innerRawType, typeParameters, outerType);
    }

    static TypeBuilder array(Type componentType) {
        return new GenericArrayTypeImpl(componentType);
    }

    static TypeBuilder array(Type componentType, int depth) {
        if(depth < 0)
            throw new NegativeArraySizeException(depth+"");
        if(depth == 0)
            throw new IllegalArgumentException("Array depth is 0");
        for(int i=0; i<depth; i++)
            componentType = array(componentType);
        return (TypeBuilder) componentType;
    }
}
