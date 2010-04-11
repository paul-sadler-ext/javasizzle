package org.jsizzle;

import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Sets.union;
import static org.jsizzle.ValueObjects.bigUnion;
import static org.jsizzle.ValueObjects.transform;

import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

@Schema
class JavaSpec
{
    class Name {}
    
    enum Visibility { PRIVATE, PACKAGE, PROTECTED, PUBLIC }
    
    enum Modifier { FINAL, STATIC, VOLATILE, TRANSIENT }
    
    class Modifiers
    {
        Visibility visibility;
        Set<Modifier> otherModifiers;
    }
    
    class Type
    {
        @Include Modifiers modifiers;
        Name id;
        Set<Variable> fields;
        Set<Method> methods;
        Set<Type> memberTypes;
        Set<Type> superTypes;
        
        Set<Method> getAllMethods()
        {
            return union(methods, bigUnion(transform(superTypes, Type.getAllMethods)));
        }
        
        @Invariant boolean fieldNamesUnique()
        {
            return transform(fields, Variable.getName).size() == fields.size();
        }
        
        @Invariant boolean methodSignaturesUnique()
        {
            return transform(methods, Method.getSignature).size() == methods.size();
        }
        
        @Invariant boolean memberTypeNamesUnique()
        {
            return transform(memberTypes, Type.getId).size() == memberTypes.size();
        }
        
        @Invariant boolean cannotReduceMethodVisibility()
        {
            return all(methods, new Predicate<Method>()
            {
                public boolean apply(final Method method)
                {
                    return all(bigUnion(transform(superTypes, Type.getAllMethods)), new Predicate<Method>()
                    {
                        public boolean apply(final Method superMethod)
                        {
                            return !method.signature.equals(superMethod.signature) ||
                                method.visibility.ordinal() >= superMethod.visibility.ordinal();
                        }
                    });
                }
            });
        }
    }
    
    class Signature
    {
        Name name;
        List<Name> argumentTypes;
        Name returnType;
    }

    class Method
    {
        @Include Modifiers modifiers;
        @Include Signature signature;
        List<Variable> arguments;
        
        @Invariant boolean argumentsMatchSignature()
        {
            return argumentTypes.equals(Lists.transform(arguments, Variable.getTypeName));
        }
    }

    class Variable
    {
        @Include Modifiers modifiers;
        Name name;
        Name typeName;
    }
}
