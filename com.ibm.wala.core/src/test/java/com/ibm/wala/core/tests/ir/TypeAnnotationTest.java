/*
 * Copyright (c) 2013,2016 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Hecker, KIT - adaptation to type annotations
 */
package com.ibm.wala.core.tests.ir;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ibm.wala.classLoader.FieldImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.ConstantElementValue;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrike.shrikeCT.TypeAnnotationsReader;
import com.ibm.wala.shrike.shrikeCT.TypeAnnotationsReader.TargetType;
import com.ibm.wala.shrike.shrikeCT.TypeAnnotationsReader.TypePathKind;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.types.annotations.TypeAnnotation;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("UnconstructableJUnitTestCase")
public class TypeAnnotationTest extends WalaTestCase {

  private final IClassHierarchy cha;

  protected TypeAnnotationTest(IClassHierarchy cha) {
    this.cha = cha;
  }

  public TypeAnnotationTest() throws ClassHierarchyException, IOException {
    this(AnnotationTest.makeCHA());
  }

  private final String typeAnnotatedClass1 = "Lannotations/TypeAnnotatedClass1";
  private final String typeAnnotatedClass2 = "Lannotations/TypeAnnotatedClass2";

  @Test
  public void testClassAnnotations5() throws Exception {
    TypeReference typeUnderTest =
        TypeReference.findOrCreate(ClassLoaderReference.Application, typeAnnotatedClass1);

    Collection<TypeAnnotation> expectedRuntimeInvisibleAnnotations = HashSetFactory.make();
    expectedRuntimeInvisibleAnnotations.add(
        TypeAnnotation.make(
            Annotation.make(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lannotations/TypeAnnotationTypeUse")),
            // TODO: currently, annotations will reference class loaders from which they were
            // loaded, even if the type
            // comes from, e.g., primordial (e.g.: Application instead of Primordial).
            // See {@link TypeAnnotation#fromString(ClassLoaderReference, String)}
            new TypeAnnotation.SuperTypeTarget(
                TypeReference.findOrCreate(ClassLoaderReference.Application, "Ljava/lang/Object")),
            TargetType.CLASS_EXTENDS));

    Collection<TypeAnnotation> expectedRuntimeVisibleAnnotations = HashSetFactory.make();

    testClassAnnotations(
        typeUnderTest, expectedRuntimeInvisibleAnnotations, expectedRuntimeVisibleAnnotations);
  }

  @Test
  public void testClassAnnotations5Foo() throws Exception {
    // TODO: the catchIIndex is obviously somewhat unstable wrt change in generated bytecode (and
    // changes in Decoder).
    // Just change it whenever a test starts to fail
    final int catchIIndex = 16;

    // TODO: the instanceOfIIndex is obviously somewhat unstable wrt change in generated bytecode
    // (and changes in Decoder).
    // This is even more so true since apparently, some compilers generate (for an instanceof test)
    // the bytecode index
    // not of the instanceof instruction (as required per spec:
    //   "The value of the offset item specifies the code array offset of either the instanceof
    // bytecode instruction
    //    corresponding to the instanceof expression, ..."
    // ), but instead of the preceding aload instruction.
    //
    // Just change it whenever a test starts to fail
    final int instanceOfIIndex = 6;

    TypeReference typeUnderTest =
        TypeReference.findOrCreate(ClassLoaderReference.Application, typeAnnotatedClass1);

    MethodReference methodRefUnderTest =
        MethodReference.findOrCreate(
            typeUnderTest, Selector.make("foo(ILjava/lang/Object;)Ljava/lang/Integer;"));

    Collection<TypeAnnotation> expectedRuntimeInvisibleAnnotations = HashSetFactory.make();

    expectedRuntimeInvisibleAnnotations.add(
        TypeAnnotation.make(
            Annotation.make(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lannotations/TypeAnnotationTypeUse")),
            new TypeAnnotation.LocalVarTarget(3, "x"),
            TargetType.LOCAL_VARIABLE));

    expectedRuntimeInvisibleAnnotations.add(
        TypeAnnotation.make(
            Annotation.make(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lannotations/TypeAnnotationTypeUse")),
            new TypeAnnotation.LocalVarTarget(4, "y"),
            TargetType.LOCAL_VARIABLE));

    // TODO: comment wrt. ClassLoaderReference in testClassAnnotations5() also applies here
    final TypeReference runtimeExceptionRef =
        TypeReference.findOrCreate(ClassLoaderReference.Application, "Ljava/lang/RuntimeException");
    expectedRuntimeInvisibleAnnotations.add(
        TypeAnnotation.make(
            Annotation.make(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lannotations/TypeAnnotationTypeUse")),
            new TypeAnnotation.CatchTarget(catchIIndex, runtimeExceptionRef),
            TargetType.EXCEPTION_PARAMETER));

    final Map<String, ElementValue> values = HashMapFactory.make();
    values.put("someKey", new ConstantElementValue("lul"));
    // TODO: comment wrt. ClassLoaderReference in testClassAnnotations5() also applies here
    expectedRuntimeInvisibleAnnotations.add(
        TypeAnnotation.make(
            Annotation.makeWithNamed(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lannotations/TypeAnnotationTypeUse"),
                values),
            new TypeAnnotation.OffsetTarget(instanceOfIIndex),
            TargetType.INSTANCEOF));

    expectedRuntimeInvisibleAnnotations.add(
        TypeAnnotation.make(
            Annotation.make(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lannotations/TypeAnnotationTypeUse")),
            new TypeAnnotation.EmptyTarget(),
            TargetType.METHOD_RETURN));

    expectedRuntimeInvisibleAnnotations.add(
        TypeAnnotation.make(
            Annotation.make(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lannotations/TypeAnnotationTypeUse")),
            new TypeAnnotation.FormalParameterTarget(0),
            TargetType.METHOD_FORMAL_PARAMETER));

    expectedRuntimeInvisibleAnnotations.add(
        TypeAnnotation.make(
            Annotation.make(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lannotations/TypeAnnotationTypeUse")),
            new TypeAnnotation.FormalParameterTarget(1),
            TargetType.METHOD_FORMAL_PARAMETER));

    Collection<TypeAnnotation> expectedRuntimeVisibleAnnotations = HashSetFactory.make();

    testMethodAnnotations(
        methodRefUnderTest, expectedRuntimeInvisibleAnnotations, expectedRuntimeVisibleAnnotations);
  }

  @Test
  public void testClassAnnotations5field() throws Exception {
    TypeReference typeUnderTest =
        TypeReference.findOrCreate(ClassLoaderReference.Application, typeAnnotatedClass1);

    Collection<TypeAnnotation> expectedAnnotations = HashSetFactory.make();
    expectedAnnotations.add(
        TypeAnnotation.make(
            Annotation.make(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lannotations/TypeAnnotationTypeUse")),
            TypeAnnotationsReader.TYPEPATH_EMPTY,
            new TypeAnnotation.EmptyTarget(),
            TargetType.FIELD));

    final List<Pair<TypePathKind, Integer>> path = new LinkedList<>();
    path.add(Pair.make(TypeAnnotationsReader.TypePathKind.TYPE_ARGUMENT, 0));
    path.add(Pair.make(TypeAnnotationsReader.TypePathKind.TYPE_ARGUMENT, 0));

    expectedAnnotations.add(
        TypeAnnotation.make(
            Annotation.make(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lannotations/TypeAnnotationTypeUse")),
            path,
            new TypeAnnotation.EmptyTarget(),
            TargetType.FIELD));

    testFieldAnnotations("field", typeUnderTest, expectedAnnotations);
  }

  private TypeAnnotation makeForAnnotations6(
      String annotation, List<Pair<TypePathKind, Integer>> path) {
    return TypeAnnotation.make(
        Annotation.make(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, typeAnnotatedClass2 + "$" + annotation)),
        path,
        new TypeAnnotation.EmptyTarget(),
        TargetType.FIELD);
  }

  @Test
  public void testClassAnnotations6field1() throws Exception {
    TypeReference typeUnderTest =
        TypeReference.findOrCreate(ClassLoaderReference.Application, typeAnnotatedClass2);

    Collection<TypeAnnotation> expectedAnnotations = HashSetFactory.make();
    {
      final List<Pair<TypePathKind, Integer>> pathA = new LinkedList<>();
      expectedAnnotations.add(makeForAnnotations6("A", pathA));
    }
    {
      final List<Pair<TypePathKind, Integer>> pathB = new LinkedList<>();
      pathB.add(Pair.make(TypeAnnotationsReader.TypePathKind.TYPE_ARGUMENT, 0));
      expectedAnnotations.add(makeForAnnotations6("B", pathB));
    }
    {
      final List<Pair<TypePathKind, Integer>> pathC = new LinkedList<>();
      pathC.add(Pair.make(TypeAnnotationsReader.TypePathKind.TYPE_ARGUMENT, 0));
      pathC.add(Pair.make(TypeAnnotationsReader.TypePathKind.WILDCARD_BOUND, 0));
      expectedAnnotations.add(makeForAnnotations6("C", pathC));
    }
    {
      final List<Pair<TypePathKind, Integer>> pathD = new LinkedList<>();
      pathD.add(Pair.make(TypeAnnotationsReader.TypePathKind.TYPE_ARGUMENT, 1));
      expectedAnnotations.add(makeForAnnotations6("D", pathD));
    }
    {
      final List<Pair<TypePathKind, Integer>> pathE = new LinkedList<>();
      pathE.add(Pair.make(TypeAnnotationsReader.TypePathKind.TYPE_ARGUMENT, 1));
      pathE.add(Pair.make(TypeAnnotationsReader.TypePathKind.TYPE_ARGUMENT, 0));
      expectedAnnotations.add(makeForAnnotations6("E", pathE));
    }
    testFieldAnnotations("field1", typeUnderTest, expectedAnnotations);
  }

  private void testClassAnnotations(
      TypeReference typeUnderTest,
      Collection<TypeAnnotation> expectedRuntimeInvisibleAnnotations,
      Collection<TypeAnnotation> expectedRuntimeVisibleAnnotations)
      throws InvalidClassFileException {
    IClass classUnderTest = cha.lookupClass(typeUnderTest);
    assertNotNull(typeUnderTest.toString() + " not found", classUnderTest);
    assertTrue(classUnderTest + " must be BytecodeClass", classUnderTest instanceof ShrikeClass);
    ShrikeClass bcClassUnderTest = (ShrikeClass) classUnderTest;

    Collection<TypeAnnotation> runtimeInvisibleAnnotations =
        bcClassUnderTest.getTypeAnnotations(true);
    AnnotationTest.assertEqualCollections(
        expectedRuntimeInvisibleAnnotations, runtimeInvisibleAnnotations);

    Collection<TypeAnnotation> runtimeVisibleAnnotations =
        bcClassUnderTest.getTypeAnnotations(false);
    AnnotationTest.assertEqualCollections(
        expectedRuntimeVisibleAnnotations, runtimeVisibleAnnotations);
  }

  private void testMethodAnnotations(
      MethodReference methodRefUnderTest,
      Collection<TypeAnnotation> expectedRuntimeInvisibleAnnotations,
      Collection<TypeAnnotation> expectedRuntimeVisibleAnnotations)
      throws InvalidClassFileException {
    IMethod methodUnderTest = cha.resolveMethod(methodRefUnderTest);
    assertNotNull(methodRefUnderTest.toString() + " not found", methodUnderTest);
    assertTrue(
        methodUnderTest + " must be ShrikeCTMethod", methodUnderTest instanceof ShrikeCTMethod);
    ShrikeCTMethod bcMethodUnderTest = (ShrikeCTMethod) methodUnderTest;

    Collection<TypeAnnotation> runtimeInvisibleAnnotations = HashSetFactory.make();
    runtimeInvisibleAnnotations.addAll(bcMethodUnderTest.getTypeAnnotationsAtCode(true));
    runtimeInvisibleAnnotations.addAll(bcMethodUnderTest.getTypeAnnotationsAtMethodInfo(true));
    AnnotationTest.assertEqualCollections(
        expectedRuntimeInvisibleAnnotations, runtimeInvisibleAnnotations);

    Collection<TypeAnnotation> runtimeVisibleAnnotations = HashSetFactory.make();
    runtimeVisibleAnnotations.addAll(bcMethodUnderTest.getTypeAnnotationsAtCode(false));
    runtimeVisibleAnnotations.addAll(bcMethodUnderTest.getTypeAnnotationsAtMethodInfo(false));
    AnnotationTest.assertEqualCollections(
        expectedRuntimeVisibleAnnotations, runtimeVisibleAnnotations);
  }

  private void testFieldAnnotations(
      String fieldNameStr,
      TypeReference typeUnderTest,
      Collection<TypeAnnotation> expectedAnnotations) {
    IClass classUnderTest = cha.lookupClass(typeUnderTest);
    assertNotNull(typeUnderTest.toString() + " not found", classUnderTest);
    assertTrue(classUnderTest + " must be BytecodeClass", classUnderTest instanceof ShrikeClass);
    ShrikeClass bcClassUnderTest = (ShrikeClass) classUnderTest;

    final Atom fieldName = Atom.findOrCreateUnicodeAtom(fieldNameStr);
    FieldImpl field = (FieldImpl) bcClassUnderTest.getField(fieldName);
    Collection<TypeAnnotation> annotations = field.getTypeAnnotations();
    AnnotationTest.assertEqualCollections(expectedAnnotations, annotations);
  }
}
