package org.jsizzle;

import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.singleton;
import static org.jcurry.ValueObjects.flip;
import static org.jcurry.ValueObjects.transform;
import static org.jsizzle.Delta.deltas;

import org.jsizzle.JavaSpec.DefaultSuperTypeName;
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
 * This specification models the transformation of the static syntax tree of a 
 * schema class from the source Java. The dynamic behaviour of an instance of
 * the schema class is modelled in {@link BindingSpec}.
 */
@Schema
public class MakeSchema
{
    Delta<Type> type;
    Function<TypeName, Type> typeResolution;
    
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
        return transform(type.after.memberTypes, Type.getName).equals(transform(type.before.memberTypes, Type.getName))
            && all(deltas(type.before.memberTypes, type.after.memberTypes, Type.getName), compose(invariant, flip(makeSchema).apply(typeResolution)));
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
        
        @Invariant boolean onlyVisibilityAndOtherModifiersChanged()
        {
            return method.unchangedExcept(Method.getVisibility, Method.getOtherModifiers);
        }
        
        @Invariant boolean becomesPrivateFinal()
        {
            return method.after.visibility.equals(Visibility.PRIVATE)
                    && method.after.otherModifiers.equals(union(method.before.otherModifiers, singleton(Modifier.FINAL)));
        }
        
        @Invariant boolean invariantMustReturnBoolean()
        {
            return method.before.returnType.equals(PrimitiveName.BOOLEAN);
        }
        
        @Invariant boolean invariantMustHaveNoArguments()
        {
            return method.before.arguments.isEmpty();
        }
    }
    
    class MakeUtilityMethod
    {
        Delta<Method> method;
        
        @Invariant boolean utilityMethodNotInvariant()
        {
            return !method.before.annotations.contains(JSizzleTypeName.INVARIANT);
        }
        
        @Invariant boolean onlyVisibilityAndOtherModifiersChanged()
        {
            return method.unchangedExcept(Method.getVisibility, Method.getOtherModifiers);
        }
        
        @Invariant boolean becomesPublicFinal()
        {
            return method.after.visibility.equals(Visibility.PUBLIC)
                    && method.after.otherModifiers.equals(union(method.before.otherModifiers, singleton(Modifier.FINAL)));
        }
    }
    
    @Disjoint
    class MakeSchemaMethod
    {
        @Include MakeInvariantMethod makeInvariantMethod;
        @Include MakeUtilityMethod makeUtilityMethod;
    }
    
    @Invariant boolean methodsBecomeSchemaMethods()
    {
        return transform(type.after.methods, Method.getSignature).equals(transform(type.before.methods, Method.getSignature))
            && all(deltas(type.before.methods, type.after.methods, Method.getSignature), compose(invariant, MakeSchemaMethod.makeSchemaMethod));
    }
    
    class MakeSchemaField
    {
        Delta<Variable> field;
        
        @Invariant boolean onlyVisibilityAndOtherModifiersChanged()
        {
            return field.unchangedExcept(Variable.getVisibility, Variable.getOtherModifiers);
        }
        
        @Invariant boolean becomesPublicFinal()
        {
            return field.after.visibility.equals(Visibility.PUBLIC)
                    && field.after.otherModifiers.equals(union(field.before.otherModifiers, singleton(Modifier.FINAL)));
        }
    }
    
    @Invariant boolean fieldsBecomeSchemaFields()
    {
        
        
        return transform(type.after.fields, Variable.getName).containsAll(transform(type.before.fields, Variable.getName))
            && all(deltas(type.before.fields, type.after.fields, Variable.getName), compose(invariant, MakeSchemaField.makeSchemaField));
    }
}
