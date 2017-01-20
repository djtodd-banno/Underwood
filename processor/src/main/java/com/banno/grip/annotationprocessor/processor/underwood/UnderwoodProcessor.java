package com.banno.grip.annotationprocessor.processor.underwood;


import com.banno.annotations.Underwood;
import com.google.auto.service.AutoService;
import com.google.common.collect.Sets;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public final class UnderwoodProcessor extends AbstractProcessor {

    private Messager mMessager;
    private UnderwoodCodeGenerator mGenerator;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        mMessager = processingEnv.getMessager();
        mGenerator = new UnderwoodCodeGenerator(processingEnv.getFiler());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Sets.newHashSet(Underwood.class.getCanonicalName(),
                               Underwood.Field.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        UnderwoodSupplier.handle(env.getElementsAnnotatedWith(Underwood.class))
                         .onError(error())
                         .map(generateCode());

        return true;
    }

    private UnderwoodSupplier.UnderwoodConsumer generateCode() {
        return new UnderwoodSupplier.UnderwoodConsumer() {
            @Override
            public void consume(UnderwoodAnnotatedClass annotatedClass) {
                mGenerator.onError(error())
                          .generateCode(annotatedClass);
            }
        };
    }

    private ErrorHandler error() {
        return new ErrorHandler() {
            @Override
            public void onError(Element e, String msg, Object... args) {
                mMessager.printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format(msg, args),
                        e);
            }
        };
    }

    interface ErrorHandler {
        void onError(Element e, String msg, Object... args);
    }
}
