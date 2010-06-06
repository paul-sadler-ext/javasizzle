package org.jsizzle;

import static org.jcurry.ValueObjects.transform;
import static org.jsizzle.SchemaSpec.namesAndTypes;

import org.jsizzle.JavaSpec.JavaLangTypeName;
import org.jsizzle.JavaSpec.Method;
import org.jsizzle.JavaSpec.Type;

/**
 * This specification models the transformation of the static syntax tree of a 
 * schema class from the source Java.
 */
@Schema
public class MakeSchema
{
    Type before;
    SchemaSpec after;
    
    @SuppressWarnings("unchecked")
    @Invariant boolean expectedChanges()
    {
        return new Delta<Type>(before, after.type)
                .unchangedExcept(Type.getConstructors,
                                 Type.getFields,
                                 Type.getMemberTypes,
                                 Type.getMethods,
                                 Type.getOtherModifiers,
                                 Type.getVisibility,
                                 Type.getSuperType);
    }
    
    @Invariant boolean beforeClassesMustExtendObject()
    {
        return before.superType == JavaLangTypeName.NONE
                || (before.superType == JavaLangTypeName.OBJECT);
    }
    
    @Invariant boolean noBeforeConstructorsAllowed()
    {
        return before.constructors.isEmpty();
    }
    
    @Invariant boolean memberTypeNamesUnchanged()
    {
        return transform(after.type.memberTypes, Type.getName).equals(transform(before.memberTypes, Type.getName));
    }
    
    @Invariant boolean methodSignaturesUnchanged()
    {
        return transform(after.type.methods, Method.getSignature).equals(transform(before.methods, Method.getSignature));
    }
    
    @Invariant boolean fieldNamesAndTypesUnchanged()
    {
        return namesAndTypes(after.type.fields).equals(namesAndTypes(before.fields));
    }
}
