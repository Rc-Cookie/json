package de.rccookie.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specifies a default value for the annotated constructor parameter, overriding the
 * standard default value of <code>null</code> when the specified parameter is not present
 * in the json structure being deserialized.
 * <p>Each parameter will be treated as json string and deserialized into the expected
 * parameter type using its standard deserializer. This especially includes <code>null</code>,
 * numbers, strings (need to be enquoted within the string, or set {@link #string()} to
 * <code>true</code>), (nested) array, boolean values, enum constants (can but don't need
 * to be enquoted), and also all types which support json deserialization. The types
 * {@link Collection}, {@link List}, {@link Set} and {@link Map} can also be deserialized,
 * but require the {@link #valueType()} parameter to be set the the value type of the
 * collection. For maps, the key type can optionally set using the {@link #keyType()}
 * parameter. The program has no way to validate that these types actually match the
 * expected generic type, so it is the task of the programmer to ensure these are correct.
 * Also, a class may not supply a default value for a parameter of the type itself.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Default {

    /**
     * The default value for the parameter.
     */
    String value();

    /**
     * If this is set to <code>true</code>, the value of {@link #value()} will be used
     * literally as string value. Otherwise, it will be parsed from json.
     *
     * @return Whether to treat {@link #value()} as literal string, <code>false</code> by default
     */
    boolean string() default false;

    /**
     * The type to deserialize the keys into for a {@link Map} type deserialization,
     * defaults to {@link String}.
     */
    Class<?> keyType() default String.class;

    /**
     * The type to deserialize the value of the collection or map into when the parameter
     * type is of such a type.
     */
    Class<?> valueType() default void.class;
}
