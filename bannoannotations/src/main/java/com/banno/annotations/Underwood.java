package com.banno.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) @Retention(RetentionPolicy.CLASS)
public @interface Underwood {

    @Target(ElementType.FIELD) @Retention(RetentionPolicy.CLASS)
    public @interface Field{

    }

}
