package com.banno.grip.annotationprocessor.processor.underwood;

import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;

class UnderwoodUtils {

    static boolean isPrimitiveOrString(Element element) {
        final TypeName type = TypeName.get(element.asType());

        return type.isPrimitive()
                || type.isBoxedPrimitive()
                || type.equals(TypeName.get(String.class));

    }

    static boolean isAnnotatedWithUnderwood(Element enclosedElement) {
        //TODO: find a way to tell if this element's class is annotated with Underwood
        return true;
    }
}
