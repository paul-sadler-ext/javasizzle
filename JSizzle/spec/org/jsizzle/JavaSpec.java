package org.jsizzle;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static java.util.Collections.singleton;
import static org.jsizzle.ValueObjects.contains;
import static org.jsizzle.ValueObjects.only;
import static org.jsizzle.ValueObjects.transform;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
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
    
    enum TypeFlag { CLASS, ENUMERATION, INTERFACE, ANNOTATION }
    
    enum TypeScope { TOP, MEMBER }
    
    enum DefaultSuperTypeName implements TypeName { OBJECT, ENUM, NONE }
    
    class Type
    {
        @Include Modifiers modifiers;
        TypeName name;
        TypeFlag flag;
        TypeScope scope;
        Set<Variable> fields;
        Set<Constructor> constructors;
        Set<Method> methods;
        Set<Type> memberTypes;
        TypeName superType;
        Set<TypeName> interfaces;
        
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
        
        @Invariant boolean memberTypesAreMemberScope()
        {
            return all(transform(memberTypes, getScope), equalTo(TypeScope.MEMBER));
        }
        
        @Invariant boolean modifiersAllowed()
        {
            switch (flag)
            {
            case CLASS:
                return only(EnumSet.of(Modifier.FINAL, Modifier.STATIC)).apply(otherModifiers);
            default:
                return otherModifiers.isEmpty();
            }
        }
        
        @Invariant boolean interfaceHasNoState()
        {
            switch (flag)
            {
            case INTERFACE:
            case ANNOTATION:
                return all(transform(fields, Variable.getOtherModifiers), contains(Modifier.STATIC));
            default:
                return true;
            }
        }
        
        @Invariant boolean interfaceCannotBeConstructed()
        {
            switch (flag)
            {
            case INTERFACE:
            case ANNOTATION:
                return constructors.isEmpty();
            default:
                return true;
            }
        }
        
        @Invariant boolean interfaceHasNoImplementation()
        {
            switch (flag)
            {
            case INTERFACE:
            case ANNOTATION:
                return all(transform(methods, Method.getStatements), equalTo(Collections.<Statement>emptyList()));
            default:
                return true;
            }
        }
        
        @Invariant boolean hasAppropriateSuperType()
        {
            switch (flag)
            {
            case ANNOTATION:
            case INTERFACE:
                return superType == DefaultSuperTypeName.NONE;
            case ENUMERATION:
                return superType == DefaultSuperTypeName.ENUM;
            default:
                return superType != DefaultSuperTypeName.NONE;
            }
        }
    }
    
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

    interface Statement {}

    interface Expression {}
    
    class If implements Statement
    {
        Expression condition;
        Statement conditional;
    }
    
    class VariableAccess implements Expression
    {
        Name variableName;
    }
    
    class DoubleEquals implements Expression
    {
        Expression left, right;
    }
    
    class Throw implements Statement
    {
        Expression throwable;
    }
    
    class New implements Expression, Statement
    {
        TypeName typeName;
        List<Expression> arguments;
    }
    
    class Literal implements Expression {}
    
    class FieldAccess implements Expression
    {
        Name fieldName;
    }
    
    class Assignment implements Expression, Statement
    {
        Expression assigned;
        Expression value;
    }
    
    class MethodCall implements Expression, Statement
    {
        Name methodName;
        List<Expression> arguments;
    }
}
