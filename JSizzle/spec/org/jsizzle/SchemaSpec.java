package org.jsizzle;

import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.Iterables.all;
import static org.jsizzle.Delta.deltas;
import static org.jsizzle.ValueObjects.transform;

import org.jsizzle.JavaSpec.DefaultSuperTypeName;
import org.jsizzle.JavaSpec.Modifier;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.JavaSpec.TypeFlag;
import org.jsizzle.JavaSpec.TypeName;
import org.jsizzle.JavaSpec.TypeScope;
import org.jsizzle.JavaSpec.Visibility;

import com.google.common.base.Function;

@Schema
public class SchemaSpec
{
    Delta<Type> type;
    
    static Function<Delta<Type>, SchemaSpec> schemaSpec = new Function<Delta<Type>, SchemaSpec>()
    {
        public SchemaSpec apply(Delta<Type> type)
        {
            return new SchemaSpec(type);
        }
    };
    
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
    
    @Invariant boolean schemaClassesMustExtendObject()
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
