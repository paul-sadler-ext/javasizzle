package org.jsizzle;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.filter;
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
    
    interface Member {}
    
    class Type
    {
        @Include Modifiers modifiers;
        TypeName name;
        MetaType metaType;
        TypeName superType;
        Set<TypeName> interfaces;
        List<Member> members;
        
        @Initialise Iterable<Field> fields()
        {
        	return filter(members, Field.class);
        }
        
        @Initialise Iterable<Method> methods()
        {
        	return filter(members, Method.class);
        }
        
        @Initialise Iterable<MemberType> memberTypes()
        {
        	return filter(members, MemberType.class);
        }
        
        @Initialise Iterable<Constructor> constructors()
        {
        	return filter(members, Constructor.class);
        }
        
        @Invariant boolean fieldNamesUnique()
        {
            return size(uniques(fields, Field.getName)) == size(fields);
        }
        
        @Invariant boolean methodSignaturesUnique()
        {
            return size(uniques(methods, Method.getSignature)) == size(methods);
        }
        
        @Invariant boolean memberTypeNamesUnique()
        {
            return size(uniques(memberTypes, MemberType.getName)) == size(memberTypes);
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
                return all(transform(fields, Field.getOtherModifiers), contains(Modifier.STATIC));
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
    
    class MemberType implements Member
    {
    	@Include Type type;
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
    
    class Constructor implements Member
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

    class Method implements Member
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
    
    class Field implements Member
    {
    	@Include Variable variable;
    }
}
