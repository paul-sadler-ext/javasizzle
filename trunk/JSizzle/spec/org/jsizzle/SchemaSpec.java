package org.jsizzle;

import org.jsizzle.JavaSpec.Name;
import org.jsizzle.JavaSpec.Statement;
import org.jsizzle.JavaSpec.Environment;

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
    }
}
