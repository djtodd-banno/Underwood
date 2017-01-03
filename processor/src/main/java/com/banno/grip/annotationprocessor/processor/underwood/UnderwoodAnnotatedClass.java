package com.banno.grip.annotationprocessor.processor.underwood;

import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

final class UnderwoodAnnotatedClass {

    private TypeElement mTypeElement;
    private String mQualifiedName;
    private String mSimpleTypeName;
    private List<FieldHolder> mPrimitiveFields;
    private List<FieldHolder> mObjectFields;


    UnderwoodAnnotatedClass(Element element) {
        mTypeElement = (TypeElement) element;
        mSimpleTypeName = mTypeElement.getSimpleName().toString();
        mQualifiedName = mTypeElement.getQualifiedName().toString();
        mPrimitiveFields = new ArrayList<>();
        mObjectFields = new ArrayList<>();

        final List<? extends Element> enclosedElements = mTypeElement.getEnclosedElements();
        for (Element enclosedElement : enclosedElements) {
            if (enclosedElement.getKind() == ElementKind.FIELD
                    && enclosedElement.getModifiers().contains(Modifier.PUBLIC)
                    && !enclosedElement.getModifiers().contains(Modifier.FINAL)) {

                final FieldHolder field = new FieldHolder(enclosedElement.toString(),
                                                          TypeName.get(enclosedElement.asType()));

                if (UnderwoodUtils.isPrimitiveOrString(enclosedElement)) {
                    mPrimitiveFields.add(field);
                } else if(UnderwoodUtils.isAnnotatedWithUnderwood(enclosedElement)) {
                    mObjectFields.add(field);
                }

            }
        }

    }

    TypeElement getTypeElement() {
        return mTypeElement;
    }


    String getSimpleTypeName() {
        return mSimpleTypeName;
    }

    String getmQualifiedName() {
        return mQualifiedName;
    }

    TypeName getType() {
        return TypeName.get(mTypeElement.asType());
    }

    List<FieldHolder> getPrimitiveFields() {
        return mPrimitiveFields;
    }

    List<FieldHolder> getObjectFields() {
        return mObjectFields;
    }

    class FieldHolder {

        final String name;
        final TypeName type;

        FieldHolder(String name, TypeName type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return type.toString() + " " + name;
        }
    }
}
