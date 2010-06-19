package org.jsizzle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Does not need to be set explicitly
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface SchemaField
{

}
