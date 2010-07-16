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

/**
 * This specification models the static syntax tree of a schema class. The model
 * includes the pre-schema AST, so that added members and fields can taken into
 * account.
 */
@Schema public class SchemaSpec
{
    Delta<Type> type;
    Function<TypeName, Type> typeResolution;

    @SuppressWarnings("unchecked") @Invariant boolean expectedChanges()
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

    @Invariant boolean beforeClassesMustExtendObject()
    {
        return type.before.superType == JavaLangTypeName.NONE
                || type.before.superType == JavaLangTypeName.OBJECT;
    }

    @Invariant boolean noBeforeConstructorsAllowed()
    {
        return isEmpty(type.before.constructors);
    }

    @Invariant boolean annotationsCannotBeSchemas()
    {
        return type.before.metaType != MetaType.ANNOTATION;
    }

    @Invariant boolean markedAsASchema()
    {
        return type.after.annotations.equals(union(type.before.annotations,
                                                   singleton(JSizzleTypeName.SCHEMA)));
    }

    @Invariant boolean schemasArePublic()
    {
        return type.after.visibility == Visibility.PUBLIC;
    }

    @Invariant boolean schemasAreFinal()
    {
        return type.after.metaType != MetaType.CLASS
                || type.after.otherModifiers.contains(Modifier.FINAL);
    }

    @Initialise Set<SchemaSpec> memberSchemas()
    {
        return transform(deltas(toSet(type.before.memberTypes),
                                toSet(type.after.memberTypes),
                                Type.getName),
                         flip(schemaSpec).apply(typeResolution));
    }

    @Invariant boolean memberSchemasAreStatic()
    {
        return type.before.metaType != MetaType.CLASS || type.before.scope != TypeScope.MEMBER
                || type.after.otherModifiers.contains(Modifier.STATIC);
    }

    enum JSizzleTypeName implements TypeName
    {
        SCHEMA, BINDING, INCLUDE, INVARIANT, INITIALISE, SCHEMAFIELD
    };

    @Invariant boolean schemasExtendBinding()
    {
        return type.after.metaType != MetaType.CLASS
                || (type.after.superType == JSizzleTypeName.BINDING);
    }

    @Invariant boolean allMemberTypesRetained()
    {
        return containsInOrder(transform(type.after.memberTypes, Type.getName),
                               transform(type.before.memberTypes, Type.getName));
    }

    interface SchemaConstructor
    {
    }

    enum SchemaConstructors implements SchemaConstructor { NO_CONSTRUCTOR }

    enum JSizzleName implements Name { IDENTITY }

    class InitSchemaClassConstructor implements SchemaConstructor
    {
        Prime<Constructor> constructor;
        Iterable<NameAndType> arguments;
        static final NameAndType identity = new NameAndType(JSizzleName.IDENTITY,
                                                            JavaLangTypeName.OBJECT);

        @Invariant boolean isPublic()
        {
            return constructor.after.visibility == Visibility.PUBLIC;
        }

        @Invariant boolean hasNoAnnotations()
        {
            return constructor.after.annotations.isEmpty();
        }

        @Invariant boolean hasNoOtherModifiers()
        {
            return constructor.after.otherModifiers.isEmpty();
        }

        @Invariant boolean hasGivenArgumentsOrIdentity()
        {
            final boolean b = elementsEqual(transform(constructor.after.arguments,
                                                      variableNameAndType),
                                            isEmpty(arguments) ? singleton(identity) : arguments);
            return b;
        }
    }

    @Invariant boolean onlyOneConstructor()
    {
        return type.after.metaType != MetaType.CLASS || size(type.after.constructors) == 1;
    }

    @Initialise SchemaConstructor schemaConstructor()
    {
        switch (type.after.metaType)
        {
        case CLASS:
            return new InitSchemaClassConstructor(new Prime<Constructor>(getSchemaClassConstructor(type.after)),
                                                  getConstructorArgs());
        default:
            return SchemaConstructors.NO_CONSTRUCTOR;
        }
    }

    class NameAndType
    {
        Name name;
        TypeName typeName;
    }

    class InvariantMethod
    {
        Delta<Method> method;

        @Invariant boolean isNotStatic()
        {
            return !method.before.otherModifiers.contains(Modifier.STATIC);
        }

        @Invariant boolean isAnnotated()
        {
            return method.before.annotations.contains(JSizzleTypeName.INVARIANT);
        }

        @Invariant boolean isPrivateFinal()
        {
            return method.after.visibility.equals(Visibility.PRIVATE)
                    && method.after.otherModifiers.equals(union(method.before.otherModifiers,
                                                                singleton(Modifier.FINAL)));
        }

        @Invariant boolean returnsBoolean()
        {
            return method.before.returnType.equals(PrimitiveName.BOOLEAN);
        }

        @Invariant boolean hasNoArguments()
        {
            return method.before.arguments.isEmpty();
        }
    }

    class InitialiseMethod
    {
        Delta<Method> method;

        @Invariant boolean isNotStatic()
        {
            return !method.before.otherModifiers.contains(Modifier.STATIC);
        }

        @Invariant boolean isAnnotated()
        {
            return method.before.annotations.contains(JSizzleTypeName.INITIALISE);
        }

        @Invariant boolean isPrivateFinal()
        {
            return method.after.visibility.equals(Visibility.PRIVATE)
                    && method.after.otherModifiers.equals(union(method.before.otherModifiers,
                                                                singleton(Modifier.FINAL)));
        }

        @Invariant boolean hasNoArguments()
        {
            return method.before.arguments.isEmpty();
        }
    }

    class UtilityMethod
    {
        Delta<Method> method;

        @Invariant boolean notInvariantOrInitialise()
        {
            return !method.before.annotations.contains(JSizzleTypeName.INVARIANT)
                    && !method.before.annotations.contains(JSizzleTypeName.INITIALISE);
        }

        @Invariant boolean isPublicFinal()
        {
            return method.after.visibility.equals(Visibility.PUBLIC)
                    && method.after.otherModifiers.equals(union(method.before.otherModifiers,
                                                                singleton(Modifier.FINAL)));
        }
    }

    @Disjoint class SchemaMethod
    {
        @Include UtilityMethod utility;
        @Include InvariantMethod invariant;
        @Include InitialiseMethod initialise;

        @Invariant boolean onlyModifiersChanged()
        {
            return method.unchangedExcept(Method.getOtherModifiers, Method.getVisibility);
        }
    }

    @Invariant boolean allMethodSignaturesRetained()
    {
        return containsInOrder(transform(type.after.methods, Method.getSignature),
                               transform(type.before.methods, Method.getSignature));
    }

    @Initialise Set<SchemaMethod> memberSchemaMethods()
    {
        switch (type.before.metaType)
        {
        case CLASS:
            return transform(deltas(toSet(type.before.methods),
                                    toSet(type.after.methods),
                                    Method.getSignature),
                             SchemaMethod.schemaMethod);
        default:
            return emptySet();
        }
    }

    class SchemaField
    {
        Delta<Variable> field;

        @Invariant boolean onlyModifiersChanged()
        {
            return field.unchangedExcept(Variable.getOtherModifiers,
                                         Variable.getVisibility,
                                         Variable.getAnnotations);
        }

        @Invariant boolean isPublicFinal()
        {
            return field.after.visibility.equals(Visibility.PUBLIC)
                    && field.after.otherModifiers.equals(union(field.before.otherModifiers,
                                                               singleton(Modifier.FINAL)));
        }

        @Invariant boolean hasSchemaFieldAnnotation()
        {
            return field.before.otherModifiers.contains(Modifier.STATIC)
                    || field.after.annotations.equals(union(field.before.annotations, singleton(JSizzleTypeName.SCHEMAFIELD)));
        }
    }

    @Invariant boolean includedFieldsMustBeResolved()
    {
        return all(transform(filter(type.before.fields,
                                    compose(contains(JSizzleTypeName.INCLUDE),
                                            Variable.getAnnotations)),
                             Variable.getTypeName),
                   compose(notNull(), typeResolution));
    }

    /**
     * Helper method that gets all direct, included and expanded fields in the
     * expected order for the constructor. All included and initialised fields
     * (including expanded fields initialised in the included schema) are
     * excluded.
     */
    Iterable<NameAndType> getConstructorArgs()
    {
        return filter(uniques(concat(transform(type.before.fields, constructorArgsForField)),
                              NameAndType.getName),
                      not(fieldInitialisedLocally));
    }

    /**
     * Helper method that gets all direct, included and expanded fields in the
     * expected order.
     */
    Set<NameAndType> getExpectedFields()
    {
        return toSet(uniques(concat(transform(type.before.fields, variableNameAndType),
                                    concat(transform(type.before.fields, injectedFieldsForField)),
                                    concat(transform(type.before.methods, injectedFieldsForMethod))),
                             NameAndType.getName));
    }

    Iterable<NameAndType> constructorArgsForField(Variable field)
    {
        if (field.otherModifiers.contains(Modifier.STATIC))
        {
            return emptySet();
        }
        else if (field.annotations.contains(JSizzleTypeName.INCLUDE))
        {
            return filter(transform(getSchemaClassConstructor(typeResolution.apply(field.typeName)).getArguments(),
                                    variableNameAndType),
                          and(compose(not(in(transform(type.before.fields, Variable.getName))), NameAndType.getName),
                              not(or(transform(limit(type.before.fields, indexOf(type.before.fields, equalTo(field))),
                                               fieldInitialisedByInclusion)))));
        }
        else
        {
            return singleton(variableNameAndType(field));
        }
    }

    Iterable<NameAndType> injectedFieldsForField(Variable field)
    {
        if (field.annotations.contains(JSizzleTypeName.INCLUDE))
        {
            return transform(filter(typeResolution.apply(field.typeName).fields,
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

    Iterable<NameAndType> injectedFieldsForMethod(Method method)
    {
        if (method.annotations.contains(JSizzleTypeName.INITIALISE))
        {
            return singleton(new NameAndType(method.name, method.returnType));
        }
        else
        {
            return emptySet();
        }
    }

    boolean fieldInitialisedLocally(NameAndType field)
    {
        return contains(transform(filter(type.before.methods,
                                         compose(contains(JSizzleTypeName.INITIALISE),
                                                 Method.getAnnotations)),
                                  methodNameAndType),
                        field);
    }
    
    boolean fieldInitialisedByInclusion(Variable included, NameAndType field)
    {
        return contains(filter(injectedFieldsForField(included), not(in(list(constructorArgsForField(included))))), field);
    }

    static NameAndType variableNameAndType(Variable variable)
    {
        return new NameAndType(variable.name, variable.typeName);
    }

    static NameAndType methodNameAndType(Method method)
    {
        return new NameAndType(method.name, method.returnType);
    }

    static Constructor getSchemaClassConstructor(Type type)
    {
        return type.constructors.iterator().next();
    }

    @Invariant boolean fieldsContainExpectedFields()
    {
        return type.before.metaType != MetaType.CLASS
                || transform(type.after.fields, variableNameAndType).containsAll(getExpectedFields());
    }

    @Initialise Set<SchemaField> schemaFields()
    {
        switch (type.before.metaType)
        {
        case CLASS:
            return transform(deltas(toSet(type.before.fields),
                                    toSet(type.after.fields),
                                    Variable.getName),
                             SchemaField.schemaField);
        default:
            return emptySet();
        }
    }
}
