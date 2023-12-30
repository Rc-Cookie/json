package de.rccookie.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotating a parameter of a constructor with this annotation indicates that
 * an exception should be thrown if the deserialized json does not contain a <b>non-null</b> value
 * for the parameter. By default, non-primitive parameters will be set to <code>null</code>
 * if not specified in the json. Primitive parameters are always required, except when
 * annotated with {@link Default}. A parameter may not be annotated with both
 * {@link Required} and {@link Default}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Required { }
