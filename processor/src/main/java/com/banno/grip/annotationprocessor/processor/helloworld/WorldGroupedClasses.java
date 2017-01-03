package com.banno.grip.annotationprocessor.processor.helloworld;

import com.squareup.javawriter.JavaWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

public class WorldGroupedClasses {

    /**
     * Will be added to the name of the generated HelloWorld class
     */
    private static final String SUFFIX = "Hello";

    private String qualifiedClassName;

    private Map<String, HelloWorldAnnotatedClass> itemsMap = new LinkedHashMap<>();

    public WorldGroupedClasses(String qualifiedClassName) {
        this.qualifiedClassName = qualifiedClassName;
    }

    public void add(HelloWorldAnnotatedClass toInsert) throws IdAlreadyUsedException {

        HelloWorldAnnotatedClass existing = itemsMap.get(toInsert.getId());
        if (existing != null) {
            throw new IdAlreadyUsedException(existing);
        }

        itemsMap.put(toInsert.getId(), toInsert);
    }

    public void generateCode(Elements elementUtils, Filer filer) throws IOException {
        TypeElement superClassName = elementUtils.getTypeElement(qualifiedClassName);
        String worldClassName = superClassName.getSimpleName() + SUFFIX;

        JavaFileObject jfo = filer.createSourceFile(qualifiedClassName + SUFFIX);
        Writer writer = jfo.openWriter();

        JavaWriter jw = new JavaWriter(writer);

        // Write package
        PackageElement pkg = elementUtils.getPackageOf(superClassName);
        if (!pkg.isUnnamed()) {
            jw.emitPackage(pkg.getQualifiedName().toString());
            jw.emitEmptyLine();
        } else {
            jw.emitPackage("");
        }

        jw.beginType(worldClassName, "class", EnumSet.of(Modifier.PUBLIC));
        jw.emitEmptyLine();
        jw.beginMethod(qualifiedClassName, "create", EnumSet.of(Modifier.PUBLIC), "String", "id");

        jw.beginControlFlow("if (id == null)");
        jw.emitStatement("throw new IllegalArgumentException(\"id is null!\")");
        jw.endControlFlow();

        for (HelloWorldAnnotatedClass item : itemsMap.values()) {
            jw.beginControlFlow("if (\"%s\".equals(id))", item.getId());
            jw.emitStatement("return new %s()", item.getTypeElement().getQualifiedName().toString());
            jw.endControlFlow();
            jw.emitEmptyLine();
        }

        jw.emitStatement("throw new IllegalArgumentException(\"Unknown id = \" + id)");
        jw.endMethod();

        jw.endType();

        jw.close();
    }

    public class IdAlreadyUsedException extends Exception {

        private HelloWorldAnnotatedClass existing;

        IdAlreadyUsedException(HelloWorldAnnotatedClass existing) {
            this.existing = existing;
        }

        public HelloWorldAnnotatedClass getExisting() {
            return existing;
        }
    }
}
