package org.jsizzle;

import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.Iterables.all;
import static org.jsizzle.Delta.deltas;
import static org.jcurry.ValueObjects.transform;

import org.jsizzle.JavaSpec.DefaultSuperTypeName;
import org.jsizzle.JavaSpec.Modifier;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.JavaSpec.TypeFlag;
import org.jsizzle.JavaSpec.TypeName;
import org.jsizzle.JavaSpec.TypeScope;
import org.jsizzle.JavaSpec.Visibility;

@Schema
public class SchemaSpec
{
    Delta<Type> type;
    
    @Invariant boolean annotationsCannotBeSchemas()
    {
        return type.before.flag != TypeFlag.ANNOTATION;
    }
    
    @Invariant boolean schemasArePublic()
    {
        return type.after.visibility == Visibility.PUBLIC;
    }
    
    @Invariant boolean schemaClassesAreFinal()
    {
        return type.before.flag != TypeFlag.CLASS
                || type.after.otherModifiers.contains(Modifier.FINAL);
    }
    
    @Invariant boolean memberSchemaClassesAreStatic()
    {
        return type.before.flag != TypeFlag.CLASS
                || type.before.scope != TypeScope.MEMBER
                || type.after.otherModifiers.contains(Modifier.STATIC);
    }
    
    @Invariant boolean beforeClassesMustExtendObject()
    {
        return type.before.flag != TypeFlag.CLASS
                || (type.before.superType == DefaultSuperTypeName.OBJECT);
    }
    
    enum JSizzleTypeName implements TypeName { BINDING, INCLUDE, INVARIANT };
    
    @Invariant boolean schemaClassesExtendBinding()
    {
        return type.before.flag != TypeFlag.CLASS
                || (type.after.superType == JSizzleTypeName.BINDING);
    }
    
    @Invariant boolean subTypesAreSchemas()
    {
        return transform(type.after.memberTypes, Type.getName).containsAll(transform(type.before.memberTypes, Type.getName))
            && all(deltas(type.before.memberTypes, type.after.memberTypes, Type.getName), compose(invariant, schemaSpec));
    }
}
