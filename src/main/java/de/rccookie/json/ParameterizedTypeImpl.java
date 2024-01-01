package de.rccookie.json;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

final class ParameterizedTypeImpl implements ParameterizedType, TypeBuilder {

    private final Type[] actualTypeArguments;
    private final Class<?> rawType;
    private final Type ownerType;

    ParameterizedTypeImpl(Class<?> rawType,
                          Type[] actualTypeArguments,
                          Type ownerType) {
        this.actualTypeArguments = Objects.requireNonNull(actualTypeArguments);
        this.rawType = Objects.requireNonNull(rawType);
        this.ownerType = (ownerType != null) ? ownerType : rawType.getDeclaringClass();
        validateConstructorArguments();
    }

    private void validateConstructorArguments() {
        TypeVariable<?>[] formals = rawType.getTypeParameters();
        // check correct arity of actual type args
        if(formals.length != actualTypeArguments.length) {
            throw new MalformedParameterizedTypeException(String.format("Mismatch of count of " +
                                                                        "formal and actual type " +
                                                                        "arguments in constructor " +
                                                                        "of %s: %d formal argument(s) " +
                                                                        "%d actual argument(s)",
                    rawType.getName(),
                    formals.length,
                    actualTypeArguments.length));
        }
    }


    @Override
    public Type[] getActualTypeArguments() {
        return actualTypeArguments.clone();
    }

    @Override
    public Class<?> getRawType() {
        return rawType;
    }


    @Override
    public Type getOwnerType() {
        return ownerType;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof ParameterizedType) {
            // Check that information is equivalent
            ParameterizedType that = (ParameterizedType) o;

            if(this == that)
                return true;

            Type thatOwner = that.getOwnerType();
            Type thatRawType = that.getRawType();

            return Objects.equals(ownerType, thatOwner) &&
                   Objects.equals(rawType, thatRawType) &&
                   Arrays.equals(actualTypeArguments, // avoid clone
                           that.getActualTypeArguments());
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(actualTypeArguments) ^
               Objects.hashCode(ownerType) ^
               Objects.hashCode(rawType);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        if(ownerType != null) {
            sb.append(ownerType.getTypeName());

            sb.append("$");

            if(ownerType instanceof ParameterizedTypeImpl) {
                // Find simple name of nested type by removing the
                // shared prefix with owner.
                sb.append(rawType.getName().replace(((ParameterizedTypeImpl) ownerType).rawType.getName() + "$", ""));
            } else
                sb.append(rawType.getSimpleName());
        } else
            sb.append(rawType.getName());

        if(actualTypeArguments != null) {
            StringJoiner sj = new StringJoiner(", ", "<", ">");
            sj.setEmptyValue("");
            for(Type t : actualTypeArguments) {
                sj.add(t.getTypeName());
            }
            sb.append(sj);
        }

        return sb.toString();
    }
}
