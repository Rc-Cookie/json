package de.rccookie.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated constructor should be used for deserializing
 * json values into the type of the constructors class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface JsonCtor {

    /**
     * Name of the fields in the json object.
     *
     * @return The names of the parameters
     */
    String[] value() default { };

    /**
     * Indices in the json array to use for the constructor parameters.
     *
     * @return The indices of the parameters
     */
    int[] indices() default { };

    /**
     * Overrides the used packaging type.
     *
     * @return The used packaging type
     */
    Type type() default Type.AUTO;
}
