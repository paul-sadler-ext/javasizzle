package org.jsizzle;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Iterables.transform;
import static java.util.Collections.singleton;
import static org.jcurry.ValueObjects.contains;
import static org.jcurry.ValueObjects.only;
import static org.jcurry.ValueObjects.uniques;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

@Schema
class JavaSpec
{
    interface Name {}
    
    class Identifier implements Name {}
    
    interface TypeName {}
    
    enum PrimitiveName implements TypeName { VOID, BYTE, SHORT, INT, LONG, CHAR, BOOLEAN, DOUBLE, FLOAT }
    
    enum JavaLangTypeName implements TypeName { OBJECT, ENUM, NONE }
    
    class QualifiedTypeName implements TypeName {}
    
    enum Visibility { DEFAULT, PRIVATE, PROTECTED, PUBLIC }
    
    enum Modifier { FINAL, STATIC }
    
    class Modifiers
    {
        Visibility visibility;
        Set<Modifier> otherModifiers;
        Set<TypeName> annotations;
    }
    
    enum MetaType { CLASS, ENUMERATION, INTERFACE, ANNOTATION }
    
    enum TypeScope { TOP, MEMBER }
    
    class Type
    {
        @Include Modifiers modifiers;
        TypeName name;
        MetaType metaType;
        TypeScope scope;
        TypeName superType;
        Set<TypeName> interfaces;
        List<Variable> fields;
        List<Type> memberTypes;
        List<Constructor> constructors;
        List<Method> methods;
        
        @Invariant boolean fieldNamesUnique()
        {
            return size(uniques(fields, Variable.getName)) == size(fields);
        }
        
        @Invariant boolean methodSignaturesUnique()
        {
            return size(uniques(methods, Method.getSignature)) == size(methods);
        }
        
        @Invariant boolean memberTypeNamesUnique()
        {
            return size(uniques(memberTypes, Type.getName)) == size(memberTypes);
        }
        
        @Invariant boolean modifiersAllowed()
        {
            switch (metaType)
            {
            case CLASS:
                return only(EnumSet.of(Modifier.FINAL, Modifier.STATIC)).apply(otherModifiers);
            default:
                return otherModifiers.isEmpty();
            }
        }
        
        @Invariant boolean interfaceHasNoState()
        {
            switch (metaType)
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
            switch (metaType)
            {
            case INTERFACE:
            case ANNOTATION:
                return isEmpty(constructors);
            default:
                return true;
            }
        }
        
        @Invariant boolean hasAppropriateSuperType()
        {
            switch (metaType)
            {
            case ANNOTATION:
            case INTERFACE:
                return superType == JavaLangTypeName.NONE;
            default:
                return true;
            }
        }
    }
    
    class Procedure
    {
        @Include Modifiers modifiers;
        List<Variable> arguments;
        
        @Invariant boolean argumentNamesUnique()
        {
            return size(uniques(arguments, Variable.getName)) == arguments.size();
        }

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
        @Include Procedure procedure;
        @Include Signature signature;
        
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
