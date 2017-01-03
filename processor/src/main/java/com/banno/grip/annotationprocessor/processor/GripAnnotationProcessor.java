package com.banno.grip.annotationprocessor.processor;

import com.banno.annotations.AdaptableType;
import com.banno.annotations.FutureFeature;
import com.banno.annotations.HelloWorld;
import com.banno.annotations.Underwood;
import com.banno.grip.annotationprocessor.processor.helloworld.HelloWorldAnnotatedClass;
import com.banno.grip.annotationprocessor.processor.helloworld.WorldGroupedClasses;
import com.banno.grip.annotationprocessor.processor.underwood.UnderWoodSupplier;
import com.banno.grip.annotationprocessor.processor.underwood.UnderwoodAnnotatedClass;
import com.banno.grip.annotationprocessor.processor.underwood.UnderwoodCodeGenerator;
import com.banno.grip.annotationprocessor.processor.underwood.UnderwoodProcessor;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

//@AutoService(Processor.class)
public class GripAnnotationProcessor extends AbstractProcessor {


    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private Map<String, WorldGroupedClasses> helloWorldClasses = new LinkedHashMap<>();
    private UnderwoodCodeGenerator mGenerator;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        typeUtils = env.getTypeUtils();
        elementUtils = env.getElementUtils();
        filer = env.getFiler();
        messager = env.getMessager();
        mGenerator = new UnderwoodCodeGenerator(filer);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new HashSet<>();

        annotations.add(HelloWorld.class.getCanonicalName());
        annotations.add(AdaptableType.class.getCanonicalName());
        annotations.add(FutureFeature.class.getCanonicalName());
        annotations.add(Underwood.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        processWorld(annotations, env);

        UnderWoodSupplier.handle(env.getElementsAnnotatedWith(Underwood.class))
                         .onError(error())
                         .flatMap(generateCode());


        return true;
    }

    private boolean processWorld(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        // Iterate over all @HelloWorld annotated elements
        for (Element element : env.getElementsAnnotatedWith(HelloWorld.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                error(element,
                      "Only classes can be annotated with @%s",
                      HelloWorld.class.getSimpleName());
                return true;
            }

            // We can cast it, because we know that it of ElementKind.CLASS
            TypeElement typeElement = (TypeElement) element;

            try {
                HelloWorldAnnotatedClass annotatedClass =
                        new HelloWorldAnnotatedClass(
                                typeElement); // throws IllegalArgumentException

                if (!isValidClass(annotatedClass)) {
                    return true; // Error message printed, exit processing
                }

                // Everything is fine, so try to add
                WorldGroupedClasses helloWorldClass =
                        helloWorldClasses.get(annotatedClass.getQualifiedHelloWorldGroupName());
                if (helloWorldClass == null) {
                    String qualifiedGroupName = annotatedClass.getQualifiedHelloWorldGroupName();
                    helloWorldClass = new WorldGroupedClasses(qualifiedGroupName);
                    helloWorldClasses.put(qualifiedGroupName, helloWorldClass);
                }

                // Throws IdAlreadyUsedException if id is conflicting with
                // another @HelloWorld annotated class with the same id
                helloWorldClass.add(annotatedClass);
            } catch (IllegalArgumentException e) {
                // @HelloWorld.id() is empty --> printing error message
                error(typeElement, e.getMessage());
                return true;
            } catch (WorldGroupedClasses.IdAlreadyUsedException e) {
                HelloWorldAnnotatedClass existing = e.getExisting();
                // Already existing
                error(element,
                      "Conflict: The class %s is annotated with @%s with id ='%s' but %s already "
                              + "uses the same id",
                      typeElement.getQualifiedName().toString(), HelloWorld.class.getSimpleName(),
                      existing.getTypeElement().getQualifiedName().toString());
                return true;
            }
            messager.printMessage(Diagnostic.Kind.WARNING, "BUT WHYYYYY", element);
        }

        try {
            for (WorldGroupedClasses helloWorldClass : helloWorldClasses.values()) {
                helloWorldClass.generateCode(elementUtils, filer);
            }

            helloWorldClasses.clear();

        } catch (IOException e) {
            error(null, e.getMessage());
        }


        return true;
    }

    private boolean isValidClass(HelloWorldAnnotatedClass item) {

        TypeElement classElement = item.getTypeElement();

        if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
            error(classElement, "The class %s is not public.",
                  classElement.getQualifiedName().toString());
            return false;
        }

        // Check if it's an abstract class
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            error(classElement,
                  "The class %s is abstract. You can't annotate abstract classes with @%",
                  classElement.getQualifiedName().toString(), HelloWorld.class.getSimpleName());
            return false;
        }

        // Check inheritance: Class must be childclass as specified in @HelloWorld.type();
        TypeElement superClassElement =
                elementUtils.getTypeElement(item.getQualifiedHelloWorldGroupName());
        if (superClassElement.getKind() == ElementKind.INTERFACE) {
            // Check interface implemented
            if (!classElement.getInterfaces().contains(superClassElement.asType())) {
                error(classElement,
                      "The class %s annotated with @%s must implement the interface %s",
                      classElement.getQualifiedName().toString(), HelloWorld.class.getSimpleName(),
                      item.getQualifiedHelloWorldGroupName());
                return false;
            }
        } else {
            // Check subclassing
            TypeElement currentClass = classElement;
            while (true) {
                TypeMirror superClassType = currentClass.getSuperclass();

                if (superClassType.getKind() == TypeKind.NONE) {
                    // Basis class (java.lang.Object) reached, so exit
                    error(classElement, "The class %s annotated with @%s must inherit from %s",
                          classElement.getQualifiedName().toString(),
                          HelloWorld.class.getSimpleName(),
                          item.getQualifiedHelloWorldGroupName());
                    return false;
                }

                if (superClassType.toString().equals(item.getQualifiedHelloWorldGroupName())) {
                    // Required super class found
                    break;
                }

                // Moving up in inheritance tree
                currentClass = (TypeElement) typeUtils.asElement(superClassType);
            }
        }

        // Check if an empty public constructor is given
        for (Element enclosed : classElement.getEnclosedElements()) {
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

        // No empty constructor found
        error(classElement, "The class %s must provide an public empty default constructor",
              classElement.getQualifiedName().toString());
        return false;
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }

    private UnderWoodSupplier.UnderwoodConsumer generateCode() {
        return new UnderWoodSupplier.UnderwoodConsumer() {
            @Override
            public void consume(UnderwoodAnnotatedClass annotatedClass) {
                mGenerator.onError(error())
                          .generateCode(annotatedClass);
            }
        };
    }

    private UnderwoodProcessor.ErrorHandler error() {
        return new UnderwoodProcessor.ErrorHandler() {
            @Override
            public void onError(Element e, String msg, Object... args) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format(msg, args),
                        e);
            }
        };

    }

}
