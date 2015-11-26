package org.javameta.apt;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.List;

/**
 * 
 */
public class MetacodeUtils {

    public static String getMetacodeOf(Elements elementsUtils, String masterCanonicalName) {
        TypeElement typeElement = elementsUtils.getTypeElement(masterCanonicalName);
        Object packageName = elementsUtils.getPackageOf(typeElement).getQualifiedName().toString();
        return packageName + "." + masterCanonicalName.replace(packageName + ".", "")
                .replaceAll("\\.", "_") + MetacodeProcessor.METACODE_CLASS_POSTFIX;
    }

    public static TypeElement getSourceTypeElement(Element element) {
        Element sourceElement = element;
        while (sourceElement.getEnclosingElement() != null && sourceElement.getEnclosingElement().getKind().isClass()) {
            sourceElement = sourceElement.getEnclosingElement();
        }
        return (TypeElement) sourceElement;
    }

    public static TypeElement typeOf(Element element) {
        return element.getKind().isClass() || element.getKind().isInterface()
                ? (TypeElement) element : (TypeElement) element.getEnclosingElement();
    }

    public static String extractClassName(Runnable invoke) {
        try {
            invoke.run();
            throw new IllegalArgumentException("invoke doesn't throws MirroredTypesException");

        } catch (MirroredTypesException e) {
            return e.getTypeMirrors().get(0).toString();
        }
    }

    public static List<String> extractClassesNames(Runnable invoke) {
        try {
            invoke.run();
            throw new IllegalArgumentException("invoke doesn't throws MirroredTypesException");

        } catch (MirroredTypesException e) {
            return Lists.transform(e.getTypeMirrors(), new Function<TypeMirror, String>() {
                @Override
                public String apply(TypeMirror input) {
                    return input.toString();
                }
            });
        }
    }
}