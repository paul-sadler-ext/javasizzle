package org.jsizzle;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Maps.transformValues;
import static java.util.Collections.singleton;
import static org.jsizzle.ValueObjects.domainRestrict;
import static org.jsizzle.ValueObjects.only;
import static org.jsizzle.ValueObjects.transform;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

@Schema
class JavaSpec
{
    class Name {}
    
    interface TypeName {}
    
    enum PrimitiveName implements TypeName { VOID, BYTE, SHORT, INTEGER, LONG, CHAR, BOOLEAN, DOUBLE, FLOAT }
    
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
        
        @Invariant boolean modifiersAllowed()
        {
            return only(EnumSet.of(Modifier.FINAL, Modifier.STATIC)).apply(otherModifiers);
        }
    }
    
    class Environment
    {
        Map<Name, Variable> variables;
        Type type;
        
        @Invariant boolean environmentVariablesHaveConsistentName()
        {
            return domainRestrict(identity(), variables.keySet()).equals(transformValues(variables, Variable.getName));
        }
        
        @Invariant boolean allFieldsAvailable()
        {
            return variables.keySet().containsAll(transform(type.fields, Variable.getName));
        }
    }
    
    interface Statement {}
    
    class Procedure
    {
        @Include Modifiers modifiers;
        List<Variable> arguments;
        List<Statement> statements;

        @Invariant boolean argumentsHaveAllowedModifiers()
        {
            return all(Lists.transform(arguments, Variable.getVisibility), equalTo(Visibility.DEFAULT))
                && all(Lists.transform(arguments, Variable.getOtherModifiers), only(singleton(Modifier.FINAL)));
        }
        
        @Invariant boolean modifiersAllowed()
        {
            return only(EnumSet.of(Modifier.FINAL, Modifier.STATIC)).apply(otherModifiers);
        }
    }
    
    class Constructor
    {
        @Include Procedure procedure;
        
        @Invariant boolean noModifiersAllowed()
        {
            return otherModifiers.isEmpty();
        }
    }
    
    class Signature
    {
        Name name;
        List<TypeName> argumentTypes;
        TypeName returnType;
    }

    class Method
    {
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
