package org.jsizzle;

import static com.google.common.base.Functions.compose;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.filter;
import static com.google.common.collect.Sets.union;
import static org.jcurry.ValueObjects.contains;
import static org.jcurry.ValueObjects.flip;
import static org.jcurry.ValueObjects.toSet;
import static org.jcurry.ValueObjects.transform;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsizzle.JavaSpec.Constructor;
import org.jsizzle.JavaSpec.MetaType;
import org.jsizzle.JavaSpec.Method;
import org.jsizzle.JavaSpec.Modifier;
import org.jsizzle.JavaSpec.PrimitiveName;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.JavaSpec.TypeName;
import org.jsizzle.JavaSpec.TypeScope;
import org.jsizzle.JavaSpec.Variable;
import org.jsizzle.JavaSpec.Visibility;

import com.google.common.base.Function;

/**
 * This specification models the static syntax tree of a schema class.
 * Dynamic behaviour of a schemas are modelled in {@link BindingSpec}.
 */
@Schema
public class SchemaSpec
{
    Type type;
    Function<TypeName, Type> typeResolution;

    @Invariant boolean annotationsCannotBeSchemas()
    {
        return type.metaType != MetaType.ANNOTATION;
    }
    
    @Invariant boolean schemasArePublic()
    {
        return type.visibility == Visibility.PUBLIC;
    }
    
    @Invariant boolean schemaClassesAreFinal()
    {
        return type.metaType != MetaType.CLASS
                || type.otherModifiers.contains(Modifier.FINAL);
    }
    
    @Invariant boolean memberSchemaClassesAreStatic()
    {
        return type.metaType != MetaType.CLASS
                || type.scope != TypeScope.MEMBER
                || type.otherModifiers.contains(Modifier.STATIC);
    }
    
    enum JSizzleTypeName implements TypeName { BINDING, INCLUDE, INVARIANT };
    
    @Invariant boolean schemaClassesExtendBinding()
    {
        return type.metaType != MetaType.CLASS
                || (type.superType == JSizzleTypeName.BINDING);
    }
    
    @Invariant boolean memberTypesAreSchemas()
    {
        return all(type.memberTypes, compose(invariant, flip(schemaSpec).apply(typeResolution)));
    }
    
    @Invariant boolean schemaConstructorHasUnincludedAndExpandedFieldsAsParameters()
    {
        return type.constructors.size() == 1
                && getSchemaConstructor(type).visibility == Visibility.PUBLIC
                && getSchemaConstructor(type).annotations.isEmpty()
                && namesAndTypes(getSchemaConstructor(type).arguments).equals(
                    namesAndTypes(union(getExpandedFields(), difference(type.fields, getIncludedFields()))))
                && getSchemaConstructor(type).otherModifiers.isEmpty();
    }
    
    static Map<JavaSpec.Name, TypeName> namesAndTypes(Iterable<Variable> variables)
    {
        return transformValues(uniqueIndex(variables, Variable.getName), Variable.getTypeName);
    }
    
    class InvariantMethod
    {
        Method method;
        
        @Invariant boolean isAnnotated()
        {
            return method.annotations.contains(JSizzleTypeName.INVARIANT);
        }
        
        @Invariant boolean isPrivateFinal()
        {
            return method.visibility.equals(Visibility.PRIVATE)
                    && method.otherModifiers.contains(Modifier.FINAL);
        }
        
        @Invariant boolean returnsBoolean()
        {
            return method.returnType.equals(PrimitiveName.BOOLEAN);
        }
        
        @Invariant boolean hasNoArguments()
        {
            return method.arguments.isEmpty();
        }
    }
    
    class UtilityMethod
    {
        Method method;
        
        @Invariant boolean notInvariant()
        {
            return !method.annotations.contains(JSizzleTypeName.INVARIANT);
        }
        
        @Invariant boolean isPublicFinal()
        {
            return method.visibility.equals(Visibility.PUBLIC)
                    && method.otherModifiers.contains(Modifier.FINAL);
        }
    }
    
    @Invariant boolean methodsAreInvariantsOrUtilities()
    {
        return all(type.methods, or(compose(invariant, InvariantMethod.invariantMethod),
                                    compose(invariant, UtilityMethod.utilityMethod)));
    }
    
    class SchemaField
    {
        Variable field;
        
        @Invariant boolean isPublicFinal()
        {
            return field.visibility.equals(Visibility.PUBLIC)
                    && field.otherModifiers.contains(Modifier.FINAL);
        }
    }
    
    @Invariant boolean includedFieldsMustBeResolved()
    {
        return all(transform(getIncludedFields(), Variable.getTypeName), compose(notNull(), typeResolution));
    }

    Set<Variable> getIncludedFields()
    {
        return filter(type.fields, compose(contains(JSizzleTypeName.INCLUDE), Variable.getAnnotations));
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
    
    @Invariant boolean fieldsAreSchemaFields()
    {
        return all(type.fields, compose(invariant, SchemaField.schemaField));
    }
    
    @Invariant boolean fieldsAreIncludedAndExpandedFields()
    {
        return namesAndTypes(type.fields).equals(namesAndTypes(union(getIncludedFields(), getExpandedFields())));
    }
}
