package org.jsizzle;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.elementsEqual;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.indexOf;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.jcurry.ValueObjects.contains;
import static org.jcurry.ValueObjects.containsInOrder;
import static org.jcurry.ValueObjects.flip;
import static org.jcurry.ValueObjects.list;
import static org.jcurry.ValueObjects.toSet;
import static org.jcurry.ValueObjects.transform;
import static org.jcurry.ValueObjects.uniques;
import static org.jsizzle.Delta.deltas;

import java.util.List;
import java.util.Set;

import org.jsizzle.JavaSpec.Constructor;
import org.jsizzle.JavaSpec.JavaLangTypeName;
import org.jsizzle.JavaSpec.MetaType;
import org.jsizzle.JavaSpec.Method;
import org.jsizzle.JavaSpec.Modifier;
import org.jsizzle.JavaSpec.Name;
import org.jsizzle.JavaSpec.PrimitiveName;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.JavaSpec.TypeName;
import org.jsizzle.JavaSpec.TypeScope;
import org.jsizzle.JavaSpec.Variable;
import org.jsizzle.JavaSpec.Visibility;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * This specification models the abstract syntax tree (AST) of a
 * schema class. It is effectively an operation schema for a
 * {@link Type}, because it includes the AST both before and after the
 * injection of boilerplate (such as modifiers, fields and
 * constructors), using a <code>Delta&ltType&gt</code>.
 * <p>
 * Also included is a function to resolve <code>Type</code>s from
 * <code>TypeName</code>s. This is required to model the injected
 * fields when using <code>@Include</code>. This function should
 * represent the post-transformation types.
 */
@Schema class SchemaSpec
{
    Delta<Type> type;
    Function<TypeName, Type> typeResolution;

    /**
     * This invariant encodes the expected mutations to
     * <code>Type</code> information during transformation of a schema
     * class by <font face="Cooper Black">JSizzle</font>.
     */
    @SuppressWarnings("unchecked")
    @Invariant boolean expectedChanges()
    {
        return type.unchangedExcept(Type.getAnnotations,
                                    Type.getConstructors,
                                    Type.getFields,
                                    Type.getMemberTypes,
                                    Type.getMethods,
                                    Type.getOtherModifiers,
                                    Type.getVisibility,
                                    Type.getSuperType);
    }

    /**
     * A schema class is not allowed to extend any class other than
     * <code>java.lang.Object</code>.
     */
    @Invariant boolean beforeClassesMustExtendObject()
    {
        return type.before.superType == JavaLangTypeName.NONE
                || type.before.superType == JavaLangTypeName.OBJECT;
    }

    /**
     * A schema class may not have any explicit constructors. An
     * operation schema should be used to model initialisation of
     * model state.
     */
    @Invariant boolean noBeforeConstructorsAllowed()
    {
        return isEmpty(type.before.constructors);
    }

    /**
     * Annotations are not supported as schema classes.
     */
    @Invariant boolean annotationsCannotBeSchemas()
    {
        return type.before.metaType != MetaType.ANNOTATION;
    }

    /**
     * A schema type is marked with the <code>@Schema</code>
     * annotation. Note that this invariant expresses a post-condition
     * on the transformation. Schema types need not be marked prior
     * to conversion if they are a member type of a marked class.
     */
    @Invariant boolean markedAsASchema()
    {
        return type.after.annotations.equals(
                 union(type.before.annotations,
                       singleton(JSizzleTypeName.SCHEMA)));
    }

    /**
     * All schema types are marked as <i>public</i> after
     * transformation.
     */
    @Invariant boolean schemasArePublic()
    {
        return type.after.visibility == Visibility.PUBLIC;
    }

    /**
     * All schema types are marked as <i>final</i> after
     * transformation.
     */
    @Invariant boolean schemasAreFinal()
    {
        return type.after.metaType != MetaType.CLASS
            || type.after.otherModifiers.contains(Modifier.FINAL);
    }

    /**
     * All member schemas are marked as <i>static</i> after
     * transformation.
     */
    @Invariant boolean memberSchemasAreStatic()
    {
        return type.before.metaType != MetaType.CLASS
            || type.before.scope != TypeScope.MEMBER
            || type.after.otherModifiers.contains(Modifier.STATIC);
    }

    /**
     * The <code>JSizzleTypeName</code> enumeration defines the type
     * names that are built into <font
     * face="Cooper Black">JSizzle</font>.
     */
    enum JSizzleTypeName implements TypeName
    {
        SCHEMA, BINDING, INCLUDE, INVARIANT, INITIALISE, SCHEMAFIELD
    };

    /**
     * All schema classes extend the utility base class
     * <code>org.jsizzle.Binding</code> after transformation.
     */
    @Invariant boolean schemasExtendBinding()
    {
        return type.after.metaType != MetaType.CLASS
                || (type.after.superType == JSizzleTypeName.BINDING);
    }

    /**
     * All member types defined in the schema type pre-transformation
     * are retained post-transformation, in the same list order.
     */
    @Invariant boolean allMemberTypesRetained()
    {
        return containsInOrder(transform(type.after.memberTypes,
                                         Type.getName),
                               transform(type.before.memberTypes,
                                         Type.getName));
    }

    /**
     * The <code>memberSchemas</code> initialised field defines the
     * set of transformation operations on member types.
     * <p>
     * First, the member types before and after transformation are
     * matched using their names and collected into a set of
     * <code>Delta&ltType&gt</code>s. Then <code>SchemaSpec</code>
     * objects are created from these using the curried constructor
     * function, re-using the current type resolution map.
     * <p>
     * Note that this field is not used in any invariant, but its
     * existence asserts that all member types that are in both input
     * and output classes are transformed according to
     * <code>SchemaSpec</code>. Since all input types are necessarily
     * in the output due to the invariant
     * <code>allMemberTypesRetained</code>, this means that all
     * member types of the input class are transformed (but not all
     * member types of the output are necessarily derived from input
     * member types).
     */
    @Initialise Set<SchemaSpec> memberSchemas()
    {
        return transform(deltas(toSet(type.before.memberTypes),
                                toSet(type.after.memberTypes),
                                Type.getName),
                         flip(schemaSpec).apply(typeResolution));
    }

    /**
     * A <code>SchemaConstructor</code> specifies the utility
     * boilerplate constructor that is generated by the
     * transformation.
     */
    interface SchemaConstructor {}

    /**
     * One possibility for a schema constructor is that no boilerplate
     * constructor is generated. This is the case if the schema type
     * is not a class (it is an interface or an enumeration).
     */
    enum SchemaConstructors implements SchemaConstructor
        { NO_CONSTRUCTOR }

    /**
     * The standard identity field, injected when there are no schema
     * fields (that is, when the schema represents an abstract type),
     * is defined to have the given identifier.
     */
    static Name IDENTITY = new Name("identity");

    /**
     * A <code>NameAndType</code> is a utility schema used to encode
     * variable names and types independently of their modifiers.
     */
    class NameAndType
    {
        Name name;
        TypeName typeName;
    }

    /**
     * A <code>InitSchemaClassConstructor</code> represents the
     * creation of a boilerplate schema constructor in the compiled
     * schema class. This is encoded as an initialisation schema.
     * <p>
     * The arguments for the constructor are given as input to the
     * initialisation using an iterable sequence of
     * <code>NameAndType</code>s.
     */
    class InitSchemaClassConstructor implements SchemaConstructor
    {
        Prime<Constructor> constructor;
        Iterable<NameAndType> arguments;
        
        /**
         * The identity parameter always has the standard name and is
         * of type <code>java.lang.Object</code>.
         */
        static NameAndType IDENTITY_PARAMETER =
            new NameAndType(IDENTITY, JavaLangTypeName.OBJECT);

        /**
         * A generated schema class constructor is <i>public</i>.
         */
        @Invariant boolean isPublic()
        {
            return constructor.after.visibility == Visibility.PUBLIC;
        }

        /**
         * A generated schema class constructor has no modifiers
         * besides <i>public</i>.
         */
        @Invariant boolean hasNoOtherModifiers()
        {
            return constructor.after.otherModifiers.isEmpty();
        }

        /**
         * A generated schema class constructor has the argument names
         * and types given in the input to this initialisation schema,
         * or if that is empty, then a single identity parameter.
         */
        @Invariant boolean hasGivenArgumentsOrIdentity()
        {
            return elementsEqual(transform(constructor.after.arguments,
                                           variableNameAndType),
                                 isEmpty(arguments) ?
                                     singleton(IDENTITY_PARAMETER) :
                                     arguments);
        }
    } // End of InitSchemaClassConstructor schema

    /**
     * A schema class has only one constructor.
     */
    @Invariant boolean onlyOneConstructor()
    {
        return type.after.metaType != MetaType.CLASS ||
            size(type.after.constructors) == 1;
    }

    /**
     * The <code>schemaConstructor</code> initialised field asserts
     * that the generated constructor of a schema class is generated
     * according to the <code>InitSchemaClassConstructor</code>
     * initialisation schema (or there is no constructor, if this is
     * an interface or enumeration type).
     * <p>
     * To do this, it uses the
     * <code>getSchemaClassConstructor()</code> and
     * <code>getConstructorArgs()</code> utility functions.
     */
    @Initialise SchemaConstructor schemaConstructor()
    {
        switch (type.after.metaType)
        {
        case CLASS:
            return new InitSchemaClassConstructor(
                       new Prime<Constructor>(
                           getSchemaClassConstructor(type.after)),
                       getConstructorArgs());
        default:
            return SchemaConstructors.NO_CONSTRUCTOR;
        }
    }

    /**
     * An <code>InvariantMethod</code> defines the operation to
     * transform a method marked with the <code>@Invariant</code>
     * annotation in a schema class.
     */
    class InvariantMethod
    {
        Delta<Method> method;

        /**
         * Invariant methods must not be marked as <i>static</i>.
         */
        @Invariant boolean isNotStatic()
        {
            return !method.before.otherModifiers
                        .contains(Modifier.STATIC);
        }

        /**
         * Invariant methods must be annotated as such.
         */
        @Invariant boolean isAnnotated()
        {
            return method.before.annotations
                        .contains(JSizzleTypeName.INVARIANT);
        }

        /**
         * After transformation, an invariant method is marked as
         * <i>private</i> and <i>final</i>.
         */
        @Invariant boolean isPrivateFinal()
        {
            return method.after.visibility.equals(Visibility.PRIVATE)
                && method.after.otherModifiers.equals(
                       singleton(Modifier.FINAL));
        }

        /**
         * An invariant method must return a <code>boolean</code>.
         */
        @Invariant boolean returnsBoolean()
        {
            return method.before.returnType.equals(
                       PrimitiveName.BOOLEAN);
        }

        /**
         * An invariant method may not have any arguments. (It is
         * called by the generated schema constructor.)
         */
        @Invariant boolean hasNoArguments()
        {
            return method.before.arguments.isEmpty();
        }
    } // End of InvariantMethod schema

    /**
     * An <code>InitialiseMethod</code> defines the operation to
     * transform a method marked with the <code>@Initialise</code>
     * annotation in a schema class.
     */
    class InitialiseMethod
    {
        Delta<Method> method;

        /**
         * Initialise methods must not be marked as <i>static</i>.
         */
        @Invariant boolean isNotStatic()
        {
            return !method.before.otherModifiers
                        .contains(Modifier.STATIC);
        }

        /**
         * Initialise methods must be annotated as such.
         */
        @Invariant boolean isAnnotated()
        {
            return method.before.annotations
                        .contains(JSizzleTypeName.INITIALISE);
        }

        /**
         * After transformation, an initialise method is marked as
         * <i>private</i> and <i>final</i>.
         */
        @Invariant boolean isPrivateFinal()
        {
            return method.after.visibility.equals(Visibility.PRIVATE)
                && method.after.otherModifiers.equals(
                       singleton(Modifier.FINAL));
        }

        /**
         * An initialise method may not have any arguments. (It is
         * called by the generated schema constructor.)
         */
        @Invariant boolean hasNoArguments()
        {
            return method.before.arguments.isEmpty();
        }
    } // End of InitialiseMethod schema

    /**
     * A <code>UtilityMethod</code> defines the operation to transform
     * a method that is not marked with the <code>@Initialise</code>
     * or <code>@Invariant</code> annotations in a schema class.
     */
    class UtilityMethod
    {
        Delta<Method> method;

        /**
         * A utility method is not marked as an invariant or an
         * initialisation.
         */
        @Invariant boolean notInvariantOrInitialise()
        {
            return !method.before.annotations.contains(
                        JSizzleTypeName.INVARIANT)
                && !method.before.annotations.contains(
                        JSizzleTypeName.INITIALISE);
        }

        /**
         * After transformation, a utility method is marked as
         * <i>public</i> and <i>final</i>.
         */
        @Invariant boolean isPublicFinal()
        {
            return method.after.visibility.equals(Visibility.PUBLIC)
                && method.after.otherModifiers.contains(Modifier.FINAL);
        }
    }

    /**
     * A schema method is transformed either a according to the
     * utility, invariant or initialise specifications.
     * <p>
     * The <code>@Disjoint</code> annotation indicates that when
     * evaluating whether the binding violates an invariant, a logical
     * OR is applied to the schema field invariants instead of an
     * implicit logical AND. Note that any invariants applied to this
     * schema itself must still evaluate to <i>true</i> regardless of
     * the other fields. Note also that 'expanded' fields injected
     * from any included fields are not considered when evaluating
     * invariants (because they are always checked in the included
     * field).
     */
    @Disjoint class SchemaMethod
    {
        @Include UtilityMethod utility;
        @Include InvariantMethod invariant;
        @Include InitialiseMethod initialise;

        /**
         * Only modifiers are changed during method transformations.
         */
        @Invariant boolean onlyModifiersChanged()
        {
            return method.unchangedExcept(Method.getOtherModifiers,
                                          Method.getVisibility);
        }
    }

    /**
     * All methods in the input class are always retained with the
     * same signature in the output schema class, in the same list
     * order.
     */
    @Invariant boolean allMethodSignaturesRetained()
    {
        return containsInOrder(transform(type.after.methods,
                                         Method.getSignature),
                               transform(type.before.methods,
                                         Method.getSignature));
    }

    /**
     * The <code>memberSchemaMethods</code> initialised field asserts
     * that the methods of a schema class are transformed according to
     * the <code>SchemaMethod</code> operation schema.
     * <p>
     * First, the methods in the input and output classes are matched
     * up according to their signatures, and a list of
     * <code>Delta&ltMethod&gt</code> is generated, from which
     * <code>SchemaMethod</code>s are constructed using the
     * constructor function.
     * <p>
     * Note that this field is not used in any invariant, but its
     * existence asserts that all methods that are in both input and
     * output classes are transformed according to
     * <code>SchemaMethod</code>. Since all input methods are
     * necessarily in the output due to the invariant
     * <code>allMethodSignaturesRetained</code>, this means that all
     * methods of the input class are transformed (but not all methods
     * of the output are necessarily derived from input methods).
     */
    @Initialise Set<SchemaMethod> memberSchemaMethods()
    {
        switch (type.before.metaType)
        {
        case CLASS:
            return transform(deltas(toSet(type.before.methods),
                                    toSet(type.after.methods),
                                    Method.getSignature),
                             schemaMethod);
        default:
            return emptySet();
        }
    }

    /**
     * All schema class fields are transformed according to this
     * operation schema.
     */
    class SchemaField
    {
        Delta<Variable> field;

        /**
         * Only the modifiers of a field are changed in transformation.
         */
        @Invariant boolean onlyModifiersChanged()
        {
            return field.unchangedExcept(Variable.getOtherModifiers,
                                         Variable.getVisibility,
                                         Variable.getAnnotations);
        }

        /**
         * All fields are made <i>public</i> and <i>final</i>.
         */
        @Invariant boolean isPublicFinal()
        {
            return field.after.visibility.equals(Visibility.PUBLIC)
                && field.after.otherModifiers.contains(Modifier.FINAL);
        }

        /**
         * All non-<i>static</i> fields are marked after
         * transformation with the <code>@SchemaField</code>
         * annotation.
         */
        @Invariant boolean hasSchemaFieldAnnotation()
        {
            return field.before.otherModifiers
                       .contains(Modifier.STATIC)
                || field.after.annotations.equals(
                       union(field.before.annotations,
                             singleton(JSizzleTypeName.SCHEMAFIELD)));
        }
    } // End of SchemaField schema

    /**
     * If the input is a class, then the output must contain all of
     * the expected schema fields.
     * <p>
     * Note that this specification is abstract with respect to any
     * additional fields that are injected in the output class for
     * utility purposes.
     */
    @Invariant boolean fieldsContainExpectedFields()
    {
        return type.before.metaType != MetaType.CLASS
                || transform(type.after.fields,
                             variableNameAndType)
                       .containsAll(getExpectedFields());
    }

    /**
     * The <code>schemaFields</code> initialised field asserts that
     * the fields of a schema class are transformed according to the
     * <code>SchemaField</code> operation schema.
     * <p>
     * First, the fields in the input and output classes are matched
     * up according to their names, and a list of
     * <code>Delta&ltField&gt</code> is generated, from which
     * <code>SchemaFields</code>s are constructed using the
     * constructor function.
     */
    @Initialise Set<SchemaField> schemaFields()
    {
        switch (type.before.metaType)
        {
        case CLASS:
            return transform(deltas(toSet(type.before.fields),
                                    toSet(type.after.fields),
                                    Variable.getName),
                             schemaField);
        default:
            return emptySet();
        }
    }

    /**
     * All fields marked with <code>@Include</code> must have types
     * that are available in the given <code>typeResolution</code>
     * function, otherwise the processing of included fields cannot be
     * modelled in this specification.
     * <p>
     * First, the list of fields is filtered to those that are marked
     * as included. This filtered list is transformed into a list of
     * <code>TypeAndName</code>s. All members of this list must have a
     * non-<code>null</code> mapping in the
     * <code>typeResolution</code> function.
     */
    @Invariant boolean includedFieldsMustBeResolved()
    {
        return all(transform(filter(type.before.fields,
                                    compose(contains(
                                             JSizzleTypeName.INCLUDE),
                                            Variable.getAnnotations)),
                             Variable.getTypeName),
                   compose(notNull(), typeResolution));
    }

    /**
     * <code>getConstructorArgs()</code> is a helper method that gets
     * all direct and expanded fields in the expected order for the
     * constructor. All included and initialised fields (including
     * expanded fields initialised in the included schema) are
     * excluded.
     * <p>
     * The expected order is defined as the list concatenation of all
     * expected constructor arguments for each field of the input
     * class, as returned by the
     * <code>constructorArgsForField()</code> helper, de-duplicated in
     * the order in which they first appear, and excluding any fields
     * that are initialised locally in this schema.
     */
    Iterable<NameAndType> getConstructorArgs()
    {
        return filter(uniques(concat(transform(type.before.fields,
                                            constructorArgsForField.apply(this))),
                              NameAndType.getName),
                      not(fieldInitialisedLocally.apply(this)));
    }

    /**
     * <code>getExpectedFields()</code> is a helper method that gets
     * all direct, expanded and initialised fields in the expected
     * order.
     * <p>
     * Direct fields are those that are members of the input class.
     * Expanded fields are those that are added to the output class
     * from the fields of a direct field marked <code>@Include</code>.
     * Initialised fields are those that are included by virtue of the
     * existence of an initialise method (note that an initialise
     * method can initialise any direct or expanded field, or create a
     * new field if none of the fields match the method name).
     */
    Set<NameAndType> getExpectedFields()
    {
        final List<NameAndType> directFields =
            transform(type.before.fields, variableNameAndType);
        
        final Iterable<NameAndType> expandedFields =
            concat(transform(type.before.fields,
                             injectedFieldsForField.apply(this)));
        
        final Iterable<NameAndType> initialisedFields =
            concat(transform(type.before.methods,
                             injectedFieldsForMethod.apply(this)));
        
        return toSet(uniques(concat(directFields,
                                    expandedFields,
                                    initialisedFields),
                             NameAndType.getName));
    }

    /**
     * <code>constructorArgsForField(Variable)</code> is a utility
     * function that obtains the expected arguments for a field in the
     * input class.
     * <p>
     * For <i>static</i> fields, nothing is returned. For fields not
     * marked <code>@Include</code>, the name and type of the field
     * itself is returned. For included fields, all arguments to the
     * schema class constructor of the resolved field type are
     * returned, <i>except</i> those that conflict by name with an
     * existing field, and those that are initialised by any previous
     * inclusion.
     * <p>
     * Note that arguments from an inclusion that are initialised by a
     * later inclusion will appear as a constructor argument. The
     * corresponding expanded field will be initialised from the
     * argument, possibly leading to a conflict between the expanded
     * field and the corresponding field of the later included field
     * object. This is a symptom of the ordering of fields and of
     * constructor body statements, and the need for a distinction
     * between initialisations and invariants; which are both due to
     * <font face="Cooper Black">JSizzle</font> being based on Java.
     * Resolving this issue could be a subject for future work.
     * 
     * @param field a field in the input class
     * @return the arguments required to fully initialise the given
     *         field
     */
    Iterable<NameAndType> constructorArgsForField(Variable field)
    {
        if (field.otherModifiers.contains(Modifier.STATIC))
        {
            return emptySet();
        }
        else if (field.annotations.contains(JSizzleTypeName.INCLUDE))
        {
            final Constructor incTypeConstructor =
                getSchemaClassConstructor(
                    typeResolution.apply(field.typeName));
            
            final Predicate<NameAndType> conflictExisting =
                compose(in(transform(type.before.fields,
                                     Variable.getName)),
                        NameAndType.getName);

            /* Note that fieldInitialisedByInclusion is a
             * two-parameter function returning a boolean. It is
             * curried and partially applied here to generate a
             * predicate. */
            final Predicate<NameAndType> initByPreviousInclusion =
                or(transform(limit(type.before.fields,
                                   indexOf(type.before.fields,
                                           equalTo(field))),
                             fieldInitialisedByInclusion.apply(this)));
            
            return filter(transform(incTypeConstructor.getArguments(),
                                    variableNameAndType),
                          and(not(conflictExisting),
                              not(initByPreviousInclusion)));
        }
        else
        {
            return singleton(variableNameAndType(field));
        }
    }

    /**
     * The <code>injectedFieldsForField(Variable)</code> utility
     * function obtains an ordered sequence of expanded field names
     * and types for a given input field. If the input field is not
     * marked with <code>@Include</code>, then no new fields are
     * returned. If it is, then all direct and expanded schema fields
     * (identified by the <code>@SchemaField</code> annotation) from
     * the resolved schema type of the field are returned, excluding
     * any fields marked <code>@Include</code>.
     * <p>
     * This formally defines the set of 'expanded' fields for a given
     * 'included' field.
     * 
     * @param field any field of the input class.
     * @return a sequence of fields to inject into the output class.
     */
    Iterable<NameAndType> injectedFieldsForField(Variable field)
    {
        if (field.annotations.contains(JSizzleTypeName.INCLUDE))
        {
            final List<Variable> typeFields =
                typeResolution.apply(field.typeName).fields;
            
            return transform(filter(typeFields,
                compose(and(contains(JSizzleTypeName.SCHEMAFIELD),
                            not(contains(JSizzleTypeName.INCLUDE))),
                        Variable.getAnnotations)),
                             variableNameAndType);
        }
        else
        {
            return emptySet();
        }
    }

    /**
     * The <code>injectedFieldsForMethod(Method)</code> utility
     * returns a sequence of fields to be in injected into the output
     * schema class, given an input method.
     * <p>
     * If the method is marked with <code>@Initialise</code> then a
     * single field with the same name as the method and of its return
     * type is returned. Otherwise, an empty sequence is returned.
     * <p>
     * Note that this utility returns a field for a method that may be
     * initialising an existing field; it is the responsibility of the
     * caller to handle this scenario and not inject a field that
     * already exists.
     * 
     * @param method a method of the input class
     * @return a sequence of fields to inject in the output class
     */
    Iterable<NameAndType> injectedFieldsForMethod(Method method)
    {
        if (method.annotations.contains(JSizzleTypeName.INITIALISE))
        {
            return singleton(new NameAndType(method.name,
                                             method.returnType));
        }
        else
        {
            return emptySet();
        }
    }

    /**
     * The <code>fieldInitialisedLocally(NameAndType)</code> utility
     * determines if the given field is initialised by an initialiser
     * method in the input class.
     * 
     * @param field an existing or putative schema field
     */
    boolean fieldInitialisedLocally(NameAndType field)
    {
        return contains(transform(filter(type.before.methods,
                     compose(contains(JSizzleTypeName.INITIALISE),
                             Method.getAnnotations)),
                                  methodNameAndType),
                        field);
    }

    /**
     * The
     * <code>fieldInitialisedByInclusion(Variable, NameAndType)</code>
     * utility determines if the given field is initialised in the
     * given included field's schema type.
     * <p>
     * This operates by checking whether the given field is one of the
     * expanded fields for the included schema but is not found in its
     * constructor (and therefore must be initialised in the schema,
     * one way or another).
     * 
     * @param included an included field
     * @param field a schema field
     */
    boolean fieldInitialisedByInclusion(Variable included,
                                        NameAndType field)
    {
        return contains(filter(injectedFieldsForField(included),
                               not(in(list(
                                constructorArgsForField(included))))),
                        field);
    }

    /**
     * The <code>variableNameAndType(Variable)</code> utility returns
     * a <code>NameAndType</code> with the name and type of the given
     * <code>Variable</code>.
     */
    static NameAndType variableNameAndType(Variable variable)
    {
        return new NameAndType(variable.name, variable.typeName);
    }

    /**
     * The <code>variableNameAndType(Method)</code> utility returns a
     * <code>NameAndType</code> with the name and return type of the
     * given <code>Method</code>.
     */
    static NameAndType methodNameAndType(Method method)
    {
        return new NameAndType(method.name, method.returnType);
    }

    /**
     * The <code>getSchemaClassConstructor(Type)</code> utility
     * returns the one and only constructor for the given schema type.
     * Note that the given type is assumed to be a schema.
     */
    static Constructor getSchemaClassConstructor(Type type)
    {
        return type.constructors.iterator().next();
    }
} // End of SchemaSpec schema
