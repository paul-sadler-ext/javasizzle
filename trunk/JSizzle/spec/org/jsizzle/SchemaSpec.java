package org.jsizzle;

import static com.google.common.base.Functions.compose;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.filter;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.singleton;
import static org.jcurry.ValueObjects.contains;
import static org.jcurry.ValueObjects.flip;
import static org.jcurry.ValueObjects.toSet;
import static org.jcurry.ValueObjects.transform;
import static org.jsizzle.Delta.deltas;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsizzle.JavaSpec.Constructor;
import org.jsizzle.JavaSpec.JavaLangTypeName;
import org.jsizzle.JavaSpec.MetaType;
import org.jsizzle.JavaSpec.Method;
import org.jsizzle.JavaSpec.Modifier;
import org.jsizzle.JavaSpec.PrimitiveName;
import org.jsizzle.JavaSpec.Name;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.JavaSpec.TypeName;
import org.jsizzle.JavaSpec.TypeScope;
import org.jsizzle.JavaSpec.Variable;
import org.jsizzle.JavaSpec.Visibility;

import com.google.common.base.Function;

/**
 * This specification models the static syntax tree of a schema class.
 * The model includes the pre-schema AST, so that added members and fields
 * can taken into account.
 */
@Schema
public class SchemaSpec
{
    Delta<Type> type;
    Function<TypeName, Type> typeResolution;
    
    @SuppressWarnings("unchecked")
    @Invariant boolean expectedChanges()
    {
        return type.unchangedExcept(Type.getConstructors,
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
        return type.before.constructors.isEmpty();
    }
    
    @Invariant boolean annotationsCannotBeSchemas()
    {
        return type.before.metaType != MetaType.ANNOTATION;
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
        return transform(deltas(type.before.memberTypes, type.after.memberTypes, Type.getName), flip(schemaSpec).apply(typeResolution));
    }

    @Invariant boolean memberSchemasAreStatic()
    {
        return type.after.metaType != MetaType.CLASS
                || type.after.scope != TypeScope.MEMBER
                || type.after.otherModifiers.contains(Modifier.STATIC);
    }
    
    enum JSizzleTypeName implements TypeName { BINDING, INCLUDE, INVARIANT };
    
    @Invariant boolean schemasExtendBinding()
    {
        return type.after.metaType != MetaType.CLASS
                || (type.after.superType == JSizzleTypeName.BINDING);
    }
    
    @Invariant boolean allMemberTypesRetained()
    {
        return transform(type.after.memberTypes, Type.getName).containsAll(transform(type.before.memberTypes, Type.getName));
    }
    
    class InitSchemaConstructor
    {
        Prime<Constructor> constructor;
        Map<Name, TypeName> arguments;
        
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
        
        @Invariant boolean hasGivenArguments()
        {
            return namesAndTypes(constructor.after.arguments).equals(arguments);
        }
    }
    
    @Invariant boolean schemaConstructorHasUnincludedAndExpandedFieldsAsParameters()
    {
        final Map<Name, TypeName> arguments = namesAndTypes(union(getExpandedFields(), difference(type.before.fields, getIncludedFields())));
        return type.after.constructors.size() == 1
                && new InitSchemaConstructor(new Prime<Constructor>(getSchemaConstructor(type.after)), arguments).invariant();
    }
    
    static Map<JavaSpec.Name, TypeName> namesAndTypes(Iterable<Variable> variables)
    {
        return transformValues(uniqueIndex(variables, Variable.getName), Variable.getTypeName);
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
                    && method.after.otherModifiers.equals(union(method.before.otherModifiers, singleton(Modifier.FINAL)));
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
    
    class UtilityMethod
    {
        Delta<Method> method;
        
        @Invariant boolean notInvariant()
        {
            return !method.before.annotations.contains(JSizzleTypeName.INVARIANT);
        }
        
        @Invariant boolean isPublicFinal()
        {
            return method.after.visibility.equals(Visibility.PUBLIC)
                    && method.after.otherModifiers.equals(union(method.before.otherModifiers, singleton(Modifier.FINAL)));
        }
    }
    
    @Disjoint
    class SchemaMethod
    {
        @Include UtilityMethod utility;
        @Include InvariantMethod invariant;
        
        @Invariant boolean onlyModifiersChanged()
        {
            return method.unchangedExcept(Method.getOtherModifiers, Method.getVisibility);
        }
    }
    
    @Invariant boolean allMethodSignaturesRetained()
    {
        return transform(type.after.methods, Method.getSignature).containsAll(transform(type.after.methods, Method.getSignature));
    }

    @Initialise Set<SchemaMethod> memberSchemaMethods()
    {
        return transform(deltas(type.before.methods, type.after.methods, Method.getSignature), SchemaMethod.schemaMethod);
    }

    class SchemaField
    {
        Delta<Variable> field;
        
        @Invariant boolean onlyModifiersChanged()
        {
            return field.unchangedExcept(Variable.getOtherModifiers, Variable.getVisibility);
        }
        
        @Invariant boolean isPublicFinal()
        {
            return field.after.visibility.equals(Visibility.PUBLIC)
                    && field.after.otherModifiers.equals(union(field.before.otherModifiers, singleton(Modifier.FINAL)));
        }
    }
    
    @Invariant boolean includedFieldsMustBeResolved()
    {
        return all(transform(getIncludedFields(), Variable.getTypeName), compose(notNull(), typeResolution));
    }

    Set<Variable> getIncludedFields()
    {
        return filter(type.before.fields, compose(contains(JSizzleTypeName.INCLUDE), Variable.getAnnotations));
    }
    
    Set<Variable> getExpandedFields()
    {
        final Function<Variable, List<Variable>> schemaConstructorArgsForTypeName =
            compose(Constructor.getArguments, compose(getSchemaConstructor, compose(typeResolution, Variable.getTypeName)));
        return toSet(concat(transform(getIncludedFields(), schemaConstructorArgsForTypeName)));
    }
    
    static Constructor getSchemaConstructor(Type type)
    {
        return type.constructors.iterator().next();
    }
    
    @Invariant boolean allFieldsAreRetained()
    {
        return namesAndTypes(type.after.fields).entrySet().containsAll(namesAndTypes(type.before.fields).entrySet());
    }
    
    @Invariant boolean fieldsIncludeIncludedAndExpandedFields()
    {
        return namesAndTypes(type.after.fields).entrySet().containsAll(namesAndTypes(union(getIncludedFields(), getExpandedFields())).entrySet());
    }
    
    @Initialise Set<SchemaField> schemaFields()
    {
        return transform(deltas(type.before.fields, type.after.fields, Variable.getName), SchemaField.schemaField);
    }
}
