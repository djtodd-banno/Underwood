package com.banno.grip.annotationprocessor.processor.underwood;

import com.banno.annotations.Underwood;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

class UnderwoodUtils {

    static boolean isPrimitiveOrString(Element element) {
        final TypeName type = TypeName.get(element.asType());

        return type.isPrimitive()
                || type.isBoxedPrimitive()
                || type.equals(TypeName.get(String.class));
    }

    static boolean isAnnotatedWithUnderwood(Element element) {
        return element.getAnnotation(Underwood.Field.class) != null;
    }
}
