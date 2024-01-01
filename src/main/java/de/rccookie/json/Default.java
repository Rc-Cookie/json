package de.rccookie.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a default value for the annotated constructor parameter, overriding the
 * standard default value of <code>null</code> when the specified parameter is not present
 * in the json structure being deserialized.
 * <p>Each parameter will be treated as json string and deserialized into the expected
 * parameter type using its standard deserializer. This especially includes <code>null</code>,
 * numbers, strings (need to be enquoted within the string, or set {@link #string()} to
 * <code>true</code>), (nested) array, boolean values, enum constants (can but don't need
 * to be enquoted), most common collection classes, and also all other types which support
 * json deserialization.</p>
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
}
