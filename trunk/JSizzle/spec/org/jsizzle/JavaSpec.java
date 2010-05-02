package org.jsizzle;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Maps.transformValues;
import static java.util.Collections.singleton;
import static org.jsizzle.ValueObjects.domainRestrict;
import static org.jsizzle.ValueObjects.only;
import static org.jsizzle.ValueObjects.transform;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

@Schema
class JavaSpec
{
    class Name {}
    
    class TypeName {}
    
    enum Visibility { DEFAULT, PRIVATE, PROTECTED, PUBLIC }
    
    enum Modifier { FINAL, STATIC, VOLATILE, TRANSIENT }
    
    class Modifiers
    {
        Visibility visibility;
        Set<Modifier> otherModifiers;
        Set<TypeName> annotations;
    }
    
    class Type
    {
        @Include Modifiers modifiers;
        TypeName name;
        Set<Variable> fields;
        Set<Constructor> constructors;
        Set<Method> methods;
        Set<Type> memberTypes;
        Set<TypeName> superTypes;
        
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
            return transform(memberTypes, Type.getName).size() == memberTypes.size();
        }
    }
    
    class Environment
    {
        Map<Name, Variable> variables;
        
        @Invariant boolean environmentVariablesHaveConsistentName()
        {
            return domainRestrict(identity(), variables.keySet()).equals(transformValues(variables, Variable.getName));
        }
    }
    
    interface Statement
    {
    }
    
    class Procedure
    {
        List<Variable> arguments;
        List<Statement> statements;

        @Invariant boolean argumentsHaveAllowedModifiers()
        {
            return all(Lists.transform(arguments, Variable.getVisibility), equalTo(Visibility.DEFAULT))
                && all(Lists.transform(arguments, Variable.getOtherModifiers), only(singleton(Modifier.FINAL)));
        }
    }
    
    class Constructor
    {
        @Include Procedure procedure;
    }
    
    class Signature
    {
        Name name;
        List<TypeName> argumentTypes;
        TypeName returnType;
    }

    class Method
    {
        @Include Modifiers modifiers;
        @Include Signature signature;
        @Include Procedure procedure;
        
        @Invariant boolean argumentsMatchSignature()
        {
            return argumentTypes.equals(Lists.transform(arguments, Variable.getTypeName));
        }
    }

    class Variable
    {
        @Include Modifiers modifiers;
        Name name;
        TypeName typeName;
    }
}
