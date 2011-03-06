package org.jsizzle;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Iterables.transform;
import static java.util.Collections.singleton;
import static org.jcurry.ValueObjects.contains;
import static org.jcurry.ValueObjects.only;
import static org.jcurry.ValueObjects.uniques;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

/**
 * The <code>JavaSpec</code> specification represents a subset of the
 * Java language specification, as required to set up the constructs
 * that will be used in specifying <font
 * face="Cooper Black">JSizzle</font> behaviour in {@link SchemaSpec}.
 * <p>
 * Some of the invariants used in this specification are redundant
 * because <font face="Cooper Black">JSizzle</font> code is legal Java
 * both before and after processing, so they are actually checked by
 * the compiler. These invariants exist here only for illustration.
 */
@Schema class JavaSpec
{
    /**
     * A <code>Name</code> is an abstract Java variable identifier.
     */
    class Name
    {
        String identifier;
    }

    /**
     * A <code>TypeName</code> is an abstract name of a Java type.
     * Type names are modelled using the following three constructs.
     */
    interface TypeName {}

    /**
     * A <code>PrimitiveName</code> is a type name for a Java
     * primitive type.
     */
    enum PrimitiveName implements TypeName
        { VOID, BYTE, SHORT, INT, LONG, CHAR, BOOLEAN, DOUBLE, FLOAT }

    /**
     * A <code>JavaLangTypeName</code> is a type name for the types
     * that have special meaning in the Java language.
     * <code>NONE</code> represents the absence of a type name, for
     * example the supertype of an interface.
     */
    enum JavaLangTypeName implements TypeName { OBJECT, ENUM, NONE }

    /**
     * A <code>QualifiedTypeName</code> is a catch-all for all other
     * type names.
     */
    class QualifiedTypeName implements TypeName {}

    /**
     * A <code>Visibility</code> is a modifier on a class or class
     * member that specifies its visibility to other code modules.
     */
    enum Visibility { DEFAULT, PRIVATE, PROTECTED, PUBLIC }

    /**
     * A <code>Modifier</code> is a modifier on a class or class
     * member that specifies some language-specific meaning. We choose
     * not to model Java modifiers that have no effect on <font
     * face="Cooper Black">JSizzle</font> such as <i>abstract,
     * transient</i> and <i>volatile</i>.
     */
    enum Modifier { FINAL, STATIC }

    /**
     * <code>Modifiers</code> is a utility schema containing a
     * <code>Visibility</code>, a set of <code>Modifiers</code> and a
     * set of annotations. It is used in the specification of
     * <code>Types</code> and <code>Procedures</code>.
     */
    class Modifiers
    {
        Visibility visibility;
        Set<Modifier> otherModifiers;
        Set<TypeName> annotations;
    }

    /**
     * A <code>MetaType</code> represents the type of a type. Types in
     * Java can be classes, enumerations, interfaces or annotations.
     */
    enum MetaType { CLASS, ENUMERATION, INTERFACE, ANNOTATION }

    /**
     * The scope of a type within a Java file can be top-level or a
     * member of another type.
     */
    enum TypeScope { TOP, MEMBER }

    /**
     * A <code>Type</code> is a class, interface, enumeration or
     * annotation with its corresponding members. It has:
     * <ul>
     * <li>A set of modifiers (visibility, plus <i>static</i> or
     * <i>final</i>, plus annotations)</li>
     * <li>A scope, name and super-type</li>
     * <li>A set of implemented (or extended) interfaces</li>
     * <li>Lists of fields, member types, constructors and methods</li>
     * </ul>
     */
    class Type
    {
        @Include Modifiers modifiers;
        TypeName name;
        MetaType metaType;
        TypeScope scope;
        TypeName superType;
        Set<TypeName> interfaces;
        List<Variable> fields;
        List<Type> memberTypes;
        List<Constructor> constructors;
        List<Method> methods;

        /**
         * All fields in a <code>Type</code> must have unique names.
         * (The number of unique field names, obtained by transforming
         * the <code>fields</code> list with the accessor function
         * <code>Variable.getName</code>, must be the same as the
         * number of fields.)
         */
        @Invariant boolean fieldNamesUnique()
        {
            return size(uniques(fields, Variable.getName)) ==
                   size(fields);
        }

        /**
         * All methods in a <code>Type</code> must have unique
         * signatures. (The number of unique method signatures,
         * obtained by transforming the <code>methods</code> list with
         * the accessor function <code>Method.getSignature</code>,
         * must be the same as the number of methods.)
         */
        @Invariant boolean methodSignaturesUnique()
        {
            return size(uniques(methods, Method.getSignature)) ==
                   size(methods);
        }

        /**
         * All member types in a <code>Type</code> must have unique
         * names. (The number of member type names, obtained by
         * transforming the <code>memberTypes</code> list with the
         * accessor function <code>Type.getName</code>, must be the
         * same as the number of member types.)
         */
        @Invariant boolean memberTypeNamesUnique()
        {
            return size(uniques(memberTypes, Type.getName)) ==
                   size(memberTypes);
        }

        /**
         * Classes can be final and/or static. Neither of these
         * modifiers can be used for enumerations, interfaces or
         * annotations.
         */
        @Invariant boolean modifiersAllowed()
        {
            switch (metaType)
            {
            case CLASS:
                return true;
            default:
                return otherModifiers.isEmpty();
            }
        }

        /**
         * An interface or an annotation cannot have instance fields.
         * (For an interface or annotation, the modifiers of all
         * fields must contain <i>static</i>.)
         */
        @Invariant boolean interfaceHasNoState()
        {
            switch (metaType)
            {
            case INTERFACE:
            case ANNOTATION:
                return all(transform(fields,
                                     Variable.getOtherModifiers),
                           contains(Modifier.STATIC));
            default:
                return true;
            }
        }

        /**
         * An interface or an annotation cannot specify a constructor.
         */
        @Invariant boolean interfaceCannotBeConstructed()
        {
            switch (metaType)
            {
            case INTERFACE:
            case ANNOTATION:
                return isEmpty(constructors);
            default:
                return true;
            }
        }

        /**
         * An annotation or interface cannot specify a super-type.
         * Note that the compiler will additionally disallow other
         * cases, for example classes with interface or annotation
         * super-types. It is not possible to specify these
         * constraints in the absence of a resolution from type names
         * to types. Since this invariant is redundant with the
         * compiler anyway, we choose not to model the more complex
         * invariant.
         */
        @Invariant boolean hasAppropriateSuperType()
        {
            switch (metaType)
            {
            case ANNOTATION:
            case INTERFACE:
                return superType == JavaLangTypeName.NONE;
            default:
                return true;
            }
        }
    } // End of Type schema

    /**
     * A <code>Procedure</code> represents a method or constructor
     * body, with its modifiers and arguments. The modifiers are
     * <i>included</i> for convenience. Note that a method body can be
     * empty if the method is abstract, as in an interface.
     */
    class Procedure
    {
        @Include Modifiers modifiers;
        List<Variable> arguments;

        /**
         * All arguments of a <code>Procedure</code> must have unique
         * names. (The number of unique argument names, obtained by
         * transforming the <code>arguments</code> list with the
         * accessor function <code>Variable.getName</code>, must be
         * the same as the number of arguments.)
         */
        @Invariant boolean argumentNamesUnique()
        {
            return size(uniques(arguments, Variable.getName)) ==
                   arguments.size();
        }

        /**
         * Visibility must not be specified on arguments, and the only
         * modifier allowed is <i>final</i>. (All visibilities for
         * arguments, obtained by applying the accessor function
         * Variable.getVisibility, must have the value
         * <code>Visibility.DEFAULT</code>, and all other modifiers
         * for arguments, similarly obtained, must only contain the
         * modifier <i>final</i>.)
         */
        @Invariant boolean argumentsHaveAllowedModifiers()
        {
            return all(Lists.transform(arguments,
                                       Variable.getVisibility),
                       equalTo(Visibility.DEFAULT))
                && all(Lists.transform(arguments,
                                       Variable.getOtherModifiers),
                       only(singleton(Modifier.FINAL)));
        }
    } // End of Procedure schema

    /**
     * A <code>Constructor</code> contains a procedure, but has no
     * signature.
     */
    class Constructor
    {
        @Include Procedure procedure;

        /**
         * A <code>Constructor</code> cannot be <i>static</i> or
         * <i>final</i>.
         */
        @Invariant boolean noModifiersAllowed()
        {
            return otherModifiers.isEmpty();
        }
    } // End of Constructor schema

    /**
     * A method <code>Signature</code> contains the method's name, its
     * list of argument types and its return type. The items taken
     * together must be unique for every method of a type.
     */
    class Signature
    {
        Name name;
        List<TypeName> argumentTypes;
        TypeName returnType;
    }

    /**
     * A <code>Method</code> contains a signature and a procedure.
     */
    class Method
    {
        @Include Procedure procedure;
        @Include Signature signature;

        /**
         * The arguments of the method procedure must have the same
         * types, in the same order, as its signature.
         */
        @Invariant boolean argumentsMatchSignature()
        {
            return argumentTypes.equals(
                Lists.transform(arguments, Variable.getTypeName));
        }
    } // End of Method schema

    /**
     * A <code>Variable</code> is used to model a local variable such
     * as a method argument, and a type field. It has a set of
     * modifiers, a name and a type name.
     */
    class Variable
    {
        @Include Modifiers modifiers;
        Name name;
        TypeName typeName;
    }
} // End of JavaSpec schema
