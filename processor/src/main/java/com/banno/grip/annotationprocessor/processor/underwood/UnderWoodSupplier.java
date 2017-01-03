package com.banno.grip.annotationprocessor.processor.underwood;


import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

final class UnderwoodSupplier {

    private Set<? extends Element> mAnnotatedElements;
    private UnderwoodProcessor.ErrorHandler mErrorHandler;

    private UnderwoodSupplier(Set<? extends Element> elements) {
        mAnnotatedElements = elements;
    }

    static UnderwoodSupplier handle(Set<? extends Element> elements) {
        return new UnderwoodSupplier(elements);
    }

    UnderwoodSupplier onError(UnderwoodProcessor.ErrorHandler handler) {
        mErrorHandler = handler;
        return this;
    }

    private void error(Element e, String msg, Object... args) {
        if (mErrorHandler != null) {
            mErrorHandler.onError(e, msg, args);
        }
    }

    void flatMap(UnderwoodConsumer consumer) {
        for (Element element : mAnnotatedElements) {
            if (validate(element)) {
                consumer.consume(new UnderwoodAnnotatedClass(element));
            }
        }
    }

    private boolean validate(Element element) {
        if (!isCorrectKind(element)) {
            error(element, "@Underwood can only be used to annotate classes");
            return false;
        } else if (!isPublic(element)) {
            error(element, "Classes annotated with @Underwood need to be public.");
            return false;
        } else if (isAbstract(element)) {
            error(element, "Classes annotated with @Underwood must not be abstract");
            return false;
        } else if (!containtsEmptyConstructor(element)) {
            error(element,
                  "Classes annotated with @Underwood must provide an public empty default "
                          + "constructor");
            return false;
        } else {
            return validateFields(element);
        }


    }

    private boolean validateFields(Element element) {
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD
                    && enclosedElement.getModifiers().contains(Modifier.PUBLIC)
                    && !enclosedElement.getModifiers().contains(Modifier.FINAL)) {


                if (!(UnderwoodUtils.isPrimitiveOrString(enclosedElement)
                        || UnderwoodUtils.isAnnotatedWithUnderwood(enclosedElement))) {
                    error(enclosedElement, "Invalid Field type. All public non final fields must"
                            + " either be primitives, boxed primitives, String, or a Type that is "
                            + "annotated with @Underwood");
                    return false;
                }

            }
        }
        return true;
    }

    private boolean containtsEmptyConstructor(Element element) {
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructorElement = (ExecutableElement) enclosed;
                if (constructorElement.getParameters().size() == 0
                        && constructorElement.getModifiers()
                                             .contains(Modifier.PUBLIC)) {
                    // Found an empty constructor
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAbstract(Element element) {
        return element.getModifiers().contains(Modifier.ABSTRACT);
    }

    private boolean isPublic(Element element) {
        return element.getModifiers().contains(Modifier.PUBLIC);
    }

    private boolean isCorrectKind(Element element) {
        return element.getKind() == ElementKind.CLASS;
    }

    interface UnderwoodConsumer {

        void consume(UnderwoodAnnotatedClass annotatedClass);
    }
}
