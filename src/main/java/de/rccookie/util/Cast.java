package de.rccookie.util;

import java.lang.reflect.Array;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for casting between types, allowing casting between
 * boxed types and primitive types as between primitive types, including
 * arrays of them.
 */
public final class Cast {

    private Cast() { }

    /**
     * Returns <code>true</code> if and only if the given object can be converted
     * to the specified type using {@link Cast#to(Object, Class)}. Passing <code>null</code>
     * as the value to test for casting is allowed and will succeed iff the target
     * type is not a primitive type.
     *
     * <p>This method returns the same result as calling
     * <code>Cast.isPossible(o != null ? o.getClass() : null, type)</code>.</p>
     *
     * @param o The object to test being converted
     * @param type The type to test whether the given object can be converted to
     * @return <code>true</code> if {@link Cast#to(Object, Class)} would succeed for
     *         identical parameters, <code>false</code> if it would throw a
     *         {@link ClassCastException} or a {@link NullPointerException}.
     * @see Cast#to(Object, Class)
     */
    public static boolean isPossible(@Nullable Object o, @NotNull Class<?> type) {
        return Cast.isPossible(o != null ? o.getClass() : null, type);
    }

    /**
     * Returns <code>true</code> if and only if objects of type <code>from</code>
     * can be converted to objects of type <code>to</code> using {@link Cast#to(Object, Class)}.
     * Passing <code>null</code> as source type corresponds to casting the value
     * <code>null</code> to the specified target type (which is possible unless
     * the target type is a primitive type). The target type may never be <code>null</code>.
     *
     * @param from The source type. May be <code>null</code>, representing a cast
     *             of the value <code>null</code>.
     * @param to The target type to test whether a cast to is possible. This cannot
     *           be <code>null</code>.
     * @return Whether objects of type <code>from</code> can be converted to objects
     *         of type <code>to</code> using {@link Cast#to(Object, Class)}.
     * @see Cast#to(Object, Class)
     */
    public static boolean isPossible(@Nullable Class<?> from, @NotNull Class<?> to) {
        Objects.requireNonNull(to);
        if(from == null)
            return !to.isPrimitive();
        if(to.isAssignableFrom(from))
            return true;
        if(to.isPrimitive()) {
            if(to == void.class)
                return from == Void.class;
            if(to == boolean.class)
                return from == Boolean.class;
            if(from.isPrimitive())
                return from != void.class && from != boolean.class;
            return Number.class.isAssignableFrom(from) || from == Character.class;
        }
        if(from == void.class || from == Void.class)
            return to == Void.class;
        if(from == boolean.class || from == Boolean.class)
            return to == Boolean.class;
        if(from.isPrimitive() ||
                from == Byte.class || from == Short.class || from == Integer.class || from == Long.class ||
                from == Float.class || from == Double.class ||
                from == Character.class) {
            return to == Byte.class || to == Short.class || to == Integer.class || to == Long.class ||
                    to == Float.class || to == Double.class ||
                    to == Character.class;
        }

        if(from.isArray() && to.isArray())
            return isPossible(from.getComponentType(), to.getComponentType());
        return false;
    }

    /**
     * Casts the given value to the specified type, properly casting between
     * boxed and primitive types as regular casting between primitive types.
     * <p>
     * Special cases:
     * <ul>
     *     <li>If the value is <code>null</code> and the target type is primitive,
     *     a {@link NullPointerException} will be thrown.</li>
     *     <li>If the target type is a primitive type, the value will be cast
     *     to {@link Number} and converted to the specified type (including narrowing
     *     conversions, like <code>long</code> to <code>int</code>). Finally, the
     *     primitive value will be boxed again with the corresponding wrapper type
     *     (as this method must return a value of type {@link Object}).</li>
     *     <li>If the target type is a wrapper class of a primitive type (like {@link Integer})
     *     and the value is non-null, the result is identical to casting the value
     *     to the corresponding primitive type. This means that casts between two wrapper
     *     types are possible iff the corresponding primitive types can be converted.</li>
     *     <li>If the target type is an array type whose content type is a superclass
     *     of the content type of the value array (e.g. casting an instance of
     *     <code>String[]</code> to the type <code>Object[]</code>), the same instance will
     *     be returned (as objects <code>String[]</code> can be assigned to variables of
     *     type <code>Object[]</code>)</li>
     *     <li>If the target type is an array type whose content type is not a superclass
     *     or identical to the value array's content type, but the components can be cast
     *     to the target component type, a new array will be allocated and each element
     *     will be cast to the specified array type's content type. This includes the case
     *     of nested (multidimensional) arrays. If the arrays are not convertible, no array
     *     will be allocated. Specifically, casting an array of only <code>null</code> values
     *     to another array type whose content types are normally not convertible, is not
     *     possible and will fail.</li>
     * </ul>
     * </p>
     *
     * @param o The object to cast
     * @param type The type to cast <code>o</code> to
     * @return <code>o</code> cast to the given type. If the target type is non-primitive
     *         and not an array type with a (deep) primitive content type, the result will
     *         be <code>o</code> (the same instance). Otherwise, the returned instance may
     *         still be the same instance (e.g. casting <code>Integer.valueOf(0)</code> to
     *         type <code>int.class</code> may return the same <code>Integer</code> instance),
     *         but might be a different instance (e.g. when converting to an array of a primitive
     *         component type).
     * @throws ClassCastException If the object could not be cast to the given type
     * @throws NullPointerException <code>o</code> is <code>null</code> and should
     *                              be cast to a primitive type, or if <code>o</code>
     *                              is an array containing <code>null</code> and
     *                              should be cast to an array containing primitives.
     *                              Also thrown if <code>type</code> is <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> T to(Object o, @NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        if(type.isPrimitive())
            return (T) castPrimitive(o, type);
        if(o == null)
            return null;
        if(type.isArray())
            return castArray(o, type);
        if(type == Byte.class)
            return (T) castPrimitive(o, byte.class);
        if(type == Short.class)
            return (T) castPrimitive(o, short.class);
        if(type == Integer.class)
            return (T) castPrimitive(o, int.class);
        if(type == Long.class)
            return (T) castPrimitive(o, long.class);
        if(type == Float.class)
            return (T) castPrimitive(o, float.class);
        if(type == Double.class)
            return (T) castPrimitive(o, double.class);
        if(type == Character.class)
            return (T) castPrimitive(o, char.class);
        if(type == Boolean.class)
            return (T) castPrimitive(o, boolean.class);
        if(type == Void.class)
            return (T) castPrimitive(o, void.class);
        return type.cast(o);
    }

    @SuppressWarnings("RedundantCast")
    private static Object castPrimitive(Object o, Class<?> primitiveType) {
        if(o == null)
            throw new NullPointerException("Cannot cast null to primitive type "+primitiveType.getName());
        if(primitiveType == byte.class)
            return o instanceof Character ? (byte) (char) o : ((Number) o).byteValue();
        if(primitiveType == short.class)
            return o instanceof Character ? (short) (char) o : ((Number) o).shortValue();
        if(primitiveType == int.class)
            return o instanceof Character ? (int) (char) o : ((Number) o).intValue();
        if(primitiveType == long.class)
            return o instanceof Character ? (long) (char) o : ((Number) o).longValue();
        if(primitiveType == float.class)
            return o instanceof Character ? (float) (char) o : ((Number) o).floatValue();
        if(primitiveType == double.class)
            return o instanceof Character ? (double) (char) o : ((Number) o).doubleValue();
        if(primitiveType == char.class)
            return o instanceof Number ? (char) ((Number) o).intValue() : (char) o;
        if(primitiveType == boolean.class)
            return (Boolean) o;
        if(primitiveType == void.class)
            //noinspection DataFlowIssue
            return (Void) o;
        return primitiveType.cast(o);
    }

    @SuppressWarnings("unchecked")
    private static <T> T castArray(Object o, Class<T> arrayType) {
        // isInstance() also returns true for widening array types, e.g. Object[].class.isInstance(new String[0])
        if(o == null || arrayType.isInstance(o))
            return arrayType.cast(o);

        Class<?> componentType = arrayType.getComponentType();
        if(!o.getClass().isArray() || !isPossible(o.getClass().getComponentType(), componentType)) {
            // Force exceptions
            arrayType.cast(o);
        }

        int len = Array.getLength(o);
        T arr = (T) Array.newInstance(componentType, len);
        for(int i=0; i<len; i++)
            Array.set(arr, i, Cast.to(Array.get(o, i), componentType));
        return arr;
    }
}
