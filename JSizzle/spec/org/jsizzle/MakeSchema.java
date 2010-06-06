package org.jsizzle;

import static org.jcurry.ValueObjects.transform;
import static org.jsizzle.SchemaSpec.namesAndTypes;
import static org.jsizzle.SchemaSpec.schemaSpec;

import org.jsizzle.JavaSpec.JavaLangTypeName;
import org.jsizzle.JavaSpec.MetaType;
import org.jsizzle.JavaSpec.Method;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.JavaSpec.TypeName;

import com.google.common.base.Function;

/**
 * This specification models the transformation of the static syntax tree of a 
 * schema class from the source Java.
 */
@Schema
public class MakeSchema
{
    Delta<Type> type;
    Function<TypeName, Type> typeResolution;
    
    @SuppressWarnings("unchecked")
    @Invariant boolean expectedChanges()
    {
        return type.unchangedExcept(Type.getConstructors,
                                    Type.getFields,
                                    Type.getMemberTypes,
                                    Type.getMethods,
                                    Type.getOtherModifiers,
                                    Type.getVisibility,
                                    Type.getSuperType);
    }
    
    @Invariant boolean afterIsASchema()
    {
        return schemaSpec.apply(type.after).apply(typeResolution).invariant();
    }
    
    @Invariant boolean beforeClassesMustExtendObject()
    {
        return type.before.metaType != MetaType.CLASS
                || (type.before.superType == JavaLangTypeName.OBJECT);
    }
    
    @Invariant boolean noBeforeConstructorsAllowed()
    {
        return type.before.constructors.isEmpty();
    }
    
    @Invariant boolean memberTypeNamesUnchanged()
    {
        return transform(type.after.memberTypes, Type.getName).equals(transform(type.before.memberTypes, Type.getName));
    }
    
    @Invariant boolean methodSignaturesUnchanged()
    {
        return transform(type.after.methods, Method.getSignature).equals(transform(type.before.methods, Method.getSignature));
    }
    
    @Invariant boolean fieldNamesAndTypesUnchanged()
    {
        return namesAndTypes(type.after.fields).equals(namesAndTypes(type.before.fields));
    }
}
