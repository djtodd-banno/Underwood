package com.banno.grip.annotationprocessor.processor.underwood;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;

final class UnderwoodCodeGenerator {

    private static final String SUFIX = "$UnderwoodAdapter";
    private static final String PACKAGE_NAME = "com.banno.underwood";

    private Filer mFiler;
    private UnderwoodProcessor.ErrorHandler mErrorHandler;

    UnderwoodCodeGenerator(Filer filer) {
        mFiler = filer;
    }

    UnderwoodCodeGenerator onError(UnderwoodProcessor.ErrorHandler handler) {
        mErrorHandler = handler;
        return this;
    }

    void generateCode(UnderwoodAnnotatedClass annotatedClass) {

        final MethodSpec read = generateReadMethod(annotatedClass);

        final MethodSpec write = generateWriteMethod(annotatedClass);


        final TypeSpec typeAdapter =
                classBuilder(annotatedClass.getSimpleTypeName() + SUFIX)
                        .superclass(ParameterizedTypeName.get(ClassName.get(TypeAdapter.class),
                                                              annotatedClass.getType()))
                        .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                        .addMethod(read)
                        .addMethod(write)
                        .build();

        JavaFile file = JavaFile.builder(PACKAGE_NAME, typeAdapter).build();

        try {
            file.writeTo(mFiler);
        } catch (IOException e) {
            error(annotatedClass.getTypeElement(), e.getMessage());
            e.printStackTrace();
        }

    }

    private MethodSpec generateWriteMethod(UnderwoodAnnotatedClass annotatedClass) {
        return methodBuilder("write")
                .addAnnotation(Override.class)
                .addParameter(JsonWriter.class, "out", Modifier.FINAL)
                .addParameter(annotatedClass.getType(),
                              annotatedClass.getSimpleTypeName()
                                            .toLowerCase(),
                              Modifier.FINAL)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .addStatement("out.beginObject()")
                .addCode(addPrimitives(annotatedClass))
                .addCode(addNestedObjects(annotatedClass))
                .addStatement("out.endObject()")
                .returns(TypeName.VOID)
                .build();
    }

    private CodeBlock addNestedObjects(UnderwoodAnnotatedClass annotatedClass) {
        final CodeBlock.Builder builder = CodeBlock.builder();

        for (UnderwoodAnnotatedClass.FieldHolder holder : annotatedClass.getObjectFields()) {
            final String[] split = holder.type.toString().split("\\.");
            final String adapterType = split[split.length - 1] + SUFIX;
            final String adapterName = adapterType.toLowerCase();

            builder.addStatement("$L $L = new $L()",
                                 adapterType,
                                 adapterName,
                                 adapterType)
                   .addStatement("$L.write($L,$L.$L)",
                                 adapterName,
                                 "out",
                                 annotatedClass.getSimpleTypeName().toLowerCase(),
                                 holder.name);
        }

        return builder.build();
    }

    private CodeBlock addPrimitives(UnderwoodAnnotatedClass annotatedClass) {
        final CodeBlock.Builder builder = CodeBlock.builder();

        for (UnderwoodAnnotatedClass.FieldHolder holder : annotatedClass.getPrimitiveFields()) {
            builder.addStatement("out.name(\"$L\").value($L.$L)", holder.name,
                                 annotatedClass.getSimpleTypeName().toLowerCase(),
                                 holder.name);
        }

        return builder.build();
    }

    private MethodSpec generateReadMethod(UnderwoodAnnotatedClass annotatedClass) {
        final TypeName type = annotatedClass.getType();
        final String simpleName = annotatedClass.getSimpleTypeName();

        return methodBuilder("read")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(JsonReader.class, "reader", Modifier.FINAL)
                .addException(IOException.class)
                .addStatement("$L $L = new $L()", simpleName, simpleName.toLowerCase(), simpleName)
                .addStatement("reader.beginObject()")
                .addCode(createCaseStatement(annotatedClass, simpleName.toLowerCase()))
                .addCode(handleInnerObjects(annotatedClass, simpleName.toLowerCase()))
                .addStatement("reader.endObject()")
                .addStatement("return " + simpleName.toLowerCase())
                .returns(type)
                .build();
    }

    private CodeBlock handleInnerObjects(UnderwoodAnnotatedClass annotatedClass, String pojo) {
        final CodeBlock.Builder builder = CodeBlock.builder();

        for (UnderwoodAnnotatedClass.FieldHolder holder : annotatedClass.getObjectFields()) {
            final String[] split = holder.type.toString().split("\\.");
            final String adapterType = split[split.length - 1] + SUFIX;
            final String adapterName = adapterType.toLowerCase();

            builder.addStatement("$L $L = new $L()",
                                 adapterType,
                                 adapterName,
                                 adapterType)
                   .addStatement("$L.$L = $L.read(reader)",
                                 pojo,
                                 holder.name,
                                 adapterName);
        }

        return builder.build();
    }

    private String createCaseStatement(UnderwoodAnnotatedClass annotatedClass, String pojo) {
        String caseStatement = "while (reader.hasNext()) {\n"
                + "    switch (reader.nextName()) {\n";

        for (UnderwoodAnnotatedClass.FieldHolder holder : annotatedClass.getPrimitiveFields()) {
            caseStatement = String.format(caseStatement
                                                  + "        case \"%s\":\n"
                                                  + "            %s.%s = reader.next%s();\n"
                                                  + "            break;\n",
                                          holder.name,
                                          pojo,
                                          holder.name,
                                          getType(holder));
        }

        return caseStatement
                + "    }\n"
                + "}\n";
    }

    private String getType(UnderwoodAnnotatedClass.FieldHolder holder) {

        if (holder.type.equals(TypeName.BOOLEAN) || holder.type.equals(TypeName.BOOLEAN.box())) {
            return "Boolean";
        } else if (holder.type.equals(TypeName.INT) || holder.type.equals(TypeName.INT.box())
                || holder.type.equals(TypeName.BYTE) || holder.type.equals(TypeName.BYTE.box())
                || holder.type.equals(TypeName.SHORT) || holder.type.equals(TypeName.SHORT.box())) {
            return "Integer";
        } else if (holder.type.equals(TypeName.LONG) || holder.type.equals(TypeName.LONG.box())) {
            return "Long";
        } else if (holder.type.equals(TypeName.DOUBLE) || holder.type.equals(
                TypeName.DOUBLE.box())) {
            return "Double";
        }

        return "String";
    }

    private void error(Element element, String msg) {
        if (mErrorHandler != null) {
            mErrorHandler.onError(element, msg);
        }
    }
}
