package org.jsizzle;

import static com.google.common.collect.Maps.uniqueIndex;
import static org.jsizzle.ValueObjects.transform;

import org.jsizzle.JavaSpec.Environment;
import org.jsizzle.JavaSpec.Name;
import org.jsizzle.JavaSpec.PrimitiveName;
import org.jsizzle.JavaSpec.Statement;
import org.jsizzle.JavaSpec.Variable;

@Schema
public class SchemaSpec
{
    class IfNameIsNullThrowNpe implements Statement
    {
        Environment env;
        Name name;
        
        @Invariant boolean nameIsInEnvironment()
        {
            return env.variables.containsKey(name);
        }
        
        @Invariant boolean mustBeObjectType()
        {
            return !(env.variables.get(name).typeName instanceof PrimitiveName);
        }
    }

    class AssignToMember implements Statement
    {
        Environment env;
        Name name;
        Name fieldName;
        
        @Invariant boolean nameIsInEnvironment()
        {
            return env.variables.containsKey(name);
        }
        
        @Invariant boolean fieldIsInEnvironment()
        {
            return transform(env.type.fields, Variable.getName).contains(fieldName);
        }
        
        @Invariant boolean nameAndFieldHaveSameType()
        {
            return uniqueIndex(env.type.fields, Variable.getName).get(fieldName).typeName.equals(env.variables.get(name).typeName);
        }
    }
}
