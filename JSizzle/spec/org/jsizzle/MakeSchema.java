package org.jsizzle;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.singleton;
import static org.jcurry.ValueObjects.contains;
import static org.jcurry.ValueObjects.transform;
import static org.jsizzle.Delta.deltas;

import org.jsizzle.JavaSpec.DefaultSuperTypeName;
import org.jsizzle.JavaSpec.MetaType;
import org.jsizzle.JavaSpec.Method;
import org.jsizzle.JavaSpec.Modifier;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.JavaSpec.TypeName;
import org.jsizzle.JavaSpec.TypeScope;
import org.jsizzle.JavaSpec.Visibility;

@Schema
public class MakeSchema
{
    Delta<Type> type;
    
    @Invariant boolean annotationsCannotBeSchemas()
    {
        return type.before.metaType != MetaType.ANNOTATION;
    }
    
    @Invariant boolean schemasArePublic()
    {
        return type.after.visibility == Visibility.PUBLIC;
    }
    
    @Invariant boolean schemaClassesAreFinal()
    {
        return type.before.metaType != MetaType.CLASS
                || type.after.otherModifiers.contains(Modifier.FINAL);
    }
    
    @Invariant boolean memberSchemaClassesAreStatic()
    {
        return type.before.metaType != MetaType.CLASS
                || type.before.scope != TypeScope.MEMBER
                || type.after.otherModifiers.contains(Modifier.STATIC);
    }
    
    @Invariant boolean beforeClassesMustExtendObject()
    {
        return type.before.metaType != MetaType.CLASS
                || (type.before.superType == DefaultSuperTypeName.OBJECT);
    }
    
    enum JSizzleTypeName implements TypeName { BINDING, INCLUDE, INVARIANT };
    
    @Invariant boolean schemaClassesExtendBinding()
    {
        return type.before.metaType != MetaType.CLASS
                || (type.after.superType == JSizzleTypeName.BINDING);
    }
    
    @Invariant boolean memberTypesBecomeSchemas()
    {
        return transform(type.after.memberTypes, Type.getName).containsAll(transform(type.before.memberTypes, Type.getName))
            && all(deltas(type.before.memberTypes, type.after.memberTypes, Type.getName), compose(invariant, makeSchema));
    }
    
    @Invariant boolean noConstructorsAllowed()
    {
        return type.before.constructors.isEmpty();
    }
    
    class MakeInvariantMethod
    {
        Delta<Method> method;
        
        @Invariant boolean invariantMethodAnnotated()
        {
            return method.before.annotations.contains(JSizzleTypeName.INVARIANT);
        }
        
        @Invariant boolean signatureArgumentsAndStatementsUnchanged()
        {
            return method.unchanged(Method.getSignature)
                && method.unchanged(Method.getArguments)
                && method.unchanged(Method.getStatements);
        }
        
        @Invariant boolean becomesPrivateFinal()
        {
            return method.after.visibility.equals(Visibility.PRIVATE)
                    && method.after.otherModifiers.equals(union(method.before.otherModifiers, singleton(Modifier.FINAL)));
        }
    }
    
    @Invariant boolean utilityMethodsBecomePublicFinal()
    {
        return all(filter(type.after.methods, compose(not(contains(JSizzleTypeName.INVARIANT)), Method.getAnnotations)),
                   and(compose(equalTo(Visibility.PUBLIC), Method.getVisibility),
                       compose(contains(Modifier.FINAL), Method.getOtherModifiers)));
    }
}
