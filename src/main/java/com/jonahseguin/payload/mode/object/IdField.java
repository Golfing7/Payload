package com.jonahseguin.payload.mode.object;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to identify a class' ID field name.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface IdField {
    String value();
}
