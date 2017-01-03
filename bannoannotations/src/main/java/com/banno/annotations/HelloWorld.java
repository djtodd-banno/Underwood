package com.banno.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) @Retention(RetentionPolicy.CLASS)
public @interface HelloWorld {

    /**
     * The name of the factory
     */
    Class type();

    /**
     * The identifier for determining which item should be instantiated
     */
    String id();

}