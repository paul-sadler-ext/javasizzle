package org.jsizzle;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.elementsEqual;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.jcurry.ValueObjects.contains;
import static org.jcurry.ValueObjects.containsInOrder;
import static org.jcurry.ValueObjects.flip;
import static org.jcurry.ValueObjects.toSet;
import static org.jcurry.ValueObjects.transform;
import static org.jcurry.ValueObjects.uniques;
import static org.jsizzle.Delta.deltas;

import java.util.Collections;
import java.util.Set;

import org.jsizzle.JavaSpec.Constructor;
import org.jsizzle.JavaSpec.Field;
import org.jsizzle.JavaSpec.JavaLangTypeName;
import org.jsizzle.JavaSpec.Member;
import org.jsizzle.JavaSpec.MemberType;
import org.jsizzle.JavaSpec.MetaType;
import org.jsizzle.JavaSpec.Method;
import org.jsizzle.JavaSpec.Modifier;
import org.jsizzle.JavaSpec.Name;
import org.jsizzle.JavaSpec.PrimitiveName;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.JavaSpec.TypeName;
import org.jsizzle.JavaSpec.Variable;
import org.jsizzle.JavaSpec.Visibility;

import com.google.common.base.Function;
import com.google.common.base.Predicates;

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
    boolean member;
    
    @SuppressWarnings("unchecked")
    @Invariant boolean expectedChanges()
    {
        return type.unchangedExcept(Type.getAnnotations,
        							Type.getConstructors,
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
        return isEmpty(type.before.constructors);
    }
    
    @Invariant boolean annotationsCannotBeSchemas()
    {
        return type.before.metaType != MetaType.ANNOTATION;
    }
    
    @Invariant boolean markedAsASchema()
    {
        return type.after.annotations.equals(union(type.before.annotations, singleton(JSizzleTypeName.SCHEMA)));
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
        return transform(deltas(toSet(transform(type.before.memberTypes, MemberType.getType)),
        						toSet(transform(type.after.memberTypes, MemberType.getType)), Type.getName),
        				 flip(flip(schemaSpec).apply(typeResolution)).apply(true));
    }

    @Invariant boolean memberSchemasAreStatic()
    {
        return type.after.metaType != MetaType.CLASS || !member
                || type.after.otherModifiers.contains(Modifier.STATIC);
    }
    
    enum JSizzleTypeName implements TypeName { SCHEMA, BINDING, INCLUDE, INVARIANT, INITIALISE, SCHEMAFIELD };
    
    @Invariant boolean schemasExtendBinding()
    {
        return type.after.metaType != MetaType.CLASS
                || (type.after.superType == JSizzleTypeName.BINDING);
    }
    
    @Invariant boolean allMemberTypesRetained()
    {
    	return containsInOrder(transform(type.after.memberTypes, MemberType.getName), transform(type.before.memberTypes, MemberType.getName));
    }
    
    interface SchemaConstructor {}
    
    enum SchemaConstructors implements SchemaConstructor { NO_CONSTRUCTOR }
    
    enum JSizzleName implements Name { IDENTITY }
    
    class InitSchemaClassConstructor implements SchemaConstructor
    {
        Prime<Constructor> constructor;
        Iterable<NameAndType> arguments;
        
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
        
        @Invariant boolean hasGivenArgumentsOrIdentity()
        {
            return elementsEqual(transform(constructor.after.arguments, variableNameAndType), isEmpty(arguments) ?
            		singleton(new NameAndType(JSizzleName.IDENTITY, JavaLangTypeName.OBJECT)) : arguments);
        }
    }
    
    @Invariant boolean onlyOneConstructor()
    {
        return type.after.metaType != MetaType.CLASS || size(type.after.constructors) == 1;
    }
    
    @Initialise SchemaConstructor schemaConstructor()
    {
        if (type.after.metaType == MetaType.CLASS)
        {
            return new InitSchemaClassConstructor(new Prime<Constructor>(getSchemaClassConstructor(type.after)),
            		transform(getFields(true), fieldNameAndType));
        }
        else
        {
            return SchemaConstructors.NO_CONSTRUCTOR;
        }
    }
    
    class NameAndType
    {
    	Name name;
    	TypeName typeName;
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
    
    class InitialiseMethod
    {
        Delta<Method> method;
        
        @Invariant boolean isNotStatic()
        {
            return !method.before.otherModifiers.contains(Modifier.STATIC);
        }
        
        @Invariant boolean isAnnotated()
        {
            return method.before.annotations.contains(JSizzleTypeName.INITIALISE);
        }
        
        @Invariant boolean isPrivateFinal()
        {
            return method.after.visibility.equals(Visibility.PRIVATE)
                    && method.after.otherModifiers.equals(union(method.before.otherModifiers, singleton(Modifier.FINAL)));
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
            return !method.before.annotations.contains(JSizzleTypeName.INVARIANT)
            	&& !method.before.annotations.contains(JSizzleTypeName.INITIALISE);
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
        @Include InitialiseMethod initialise;
        
        @Invariant boolean onlyModifiersChanged()
        {
            return method.unchangedExcept(Method.getOtherModifiers, Method.getVisibility);
        }
    }
    
    @Invariant boolean allMethodSignaturesRetained()
    {
        return containsInOrder(transform(type.before.methods, Method.getSignature), transform(type.after.methods, Method.getSignature));
    }

    @Initialise Set<SchemaMethod> memberSchemaMethods()
    {
        return transform(deltas(toSet(type.before.methods),
        						toSet(type.after.methods), Method.getSignature),
        				  SchemaMethod.schemaMethod);
    }

    class SchemaField
    {
        Delta<Field> field;
        
        @Invariant boolean onlyModifiersChanged()
        {
            return field.unchangedExcept(Field.getOtherModifiers, Field.getVisibility, Field.getAnnotations);
        }
        
        @Invariant boolean isPublicFinal()
        {
            return field.after.visibility.equals(Visibility.PUBLIC)
                    && field.after.otherModifiers.equals(union(field.before.otherModifiers, singleton(Modifier.FINAL)));
        }
        
        @Invariant boolean hasSchemaFieldAnnotation()
        {
            return field.after.annotations.equals(union(field.before.annotations, singleton(JSizzleTypeName.SCHEMAFIELD)));
        }
    }
    
    @Invariant boolean includedFieldsMustBeResolved()
    {
        return all(transform(getIncludedFields(), Field.getTypeName), compose(notNull(), typeResolution));
    }
    
    Set<Field> getIncludedFields()
    {
        return toSet(filter(type.before.fields, compose(contains(JSizzleTypeName.INCLUDE), Field.getAnnotations)));
    }

	/**
	 * Helper method that gets all direct, included and expanded fields in the
	 * expected order. If {@code excludeInitialised} is {@code true} then all
	 * initialised fields (including expanded fields initialised in the included
	 * schema) are excluded.
	 */
    Iterable<Field> getFields(final boolean excludeInitialised)
    {
		return filter(uniques(concat(type.before.fields, getInjectedFields(excludeInitialised)), Field.getName),
				excludeInitialised ? not(compose(equalTo(true), fieldInitialised)) : Predicates.<Field>alwaysTrue());
    }
    
    Iterable<Field> getInjectedFields(final boolean excludeInitialised)
    {
    	return concat(transform(type.before.members, injectedFieldsForMember.apply(excludeInitialised)));
    }
    
    Iterable<Field> injectedFieldsForMember(boolean excludeInitialised, Member member)
    {
    	if (member instanceof Field && ((Field)member).annotations.contains(JSizzleTypeName.INCLUDE))
    	{
			// If excluding initialised fields, get fields from the included constructor, because
			// this will already have taken into account fields initialised in upstream inclusions.
			final Type fieldType = typeResolution.apply(((Field)member).typeName);
			return excludeInitialised ? transform(getSchemaClassConstructor(fieldType).getArguments(), fieldFromVariable) : 
					  filter(fieldType.fields, compose(and(contains(JSizzleTypeName.SCHEMAFIELD),
	  			              not(contains(JSizzleTypeName.INCLUDE))), Field.getAnnotations));
		}
    	else if (member instanceof Method && ((Method)member).annotations.contains(JSizzleTypeName.INITIALISE))
    	{
			return singleton(new Field(Visibility.PUBLIC,
									   singleton(Modifier.FINAL),
									   Collections.<TypeName>emptySet(), ((Method)member).name,
									   ((Method)member).returnType));
    	}
    	else
    	{
    		return emptySet();
    	}
    }
    
    boolean fieldInitialised(Field field)
    {
    	return contains(transform(filter(type.before.methods, compose(contains(JSizzleTypeName.INITIALISE), Method.getAnnotations)),
    			methodNameAndType), fieldNameAndType(field));
    }
    
    static NameAndType variableNameAndType(Variable variable)
    {
    	return new NameAndType(variable.name, variable.typeName);
    }
    
    static NameAndType fieldNameAndType(Field field)
    {
    	return variableNameAndType(field.variable);
    }
    
    static NameAndType methodNameAndType(Method method)
    {
    	return new NameAndType(method.name, method.returnType);
    }
    
    static Field fieldFromVariable(Variable variable)
    {
    	return new Field(Visibility.PUBLIC, variable.otherModifiers, variable.annotations, variable.name, variable.typeName);
    }
    
    static Constructor getSchemaClassConstructor(Type type)
    {
        return type.constructors.iterator().next();
    }
    
    @Invariant boolean fieldsContainExpectedFields()
    {
        return containsInOrder(transform(getFields(false), fieldNameAndType), transform(type.after.fields, fieldNameAndType));
    }
    
    @Initialise Set<SchemaField> schemaFields()
    {
        return transform(deltas(toSet(type.before.fields), toSet(type.after.fields), Field.getName), SchemaField.schemaField);
    }
}
