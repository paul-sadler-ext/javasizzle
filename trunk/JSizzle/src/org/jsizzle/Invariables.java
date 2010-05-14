package org.jsizzle;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import org.jcurry.AsFunction;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Invariables
{
    @AsFunction
    public static Invariable asInvariable(final Object from)
    {
        if (from instanceof Invariable)
        {
            return (Invariable)from;
        }
        final Iterable<?> subData;
        if (from instanceof Iterable<?>)
        {
            subData = (Iterable<?>)from;
        }
        else if (from != null && from.getClass().isArray() && !from.getClass().getComponentType().isPrimitive())
        {
            subData = asList((Object[])from);
        }
        else if (from instanceof Map<?, ?>)
        {
            subData = ((Map<?, ?>)from).entrySet();
        }
        else if (from instanceof Map.Entry<?, ?>)
        {
            subData = asList(((Map.Entry<?, ?>)from).getKey(), ((Map.Entry<?, ?>)from).getValue());
        }
        else
        {
            return null;
        }
        final Iterable<Invariable> subInvariables = filter(transform(subData, asInvariable), notNull());
        return isEmpty(subInvariables) ? null : and(subInvariables);
    }

    @AsFunction
    public static Invariable and(Iterable<? extends Invariable> invariables)
    {
        return new CompositeInvariable(invariables)
        {
            @Override
            public boolean invariant()
            {
                return all(components, invariant);
            }        
        };
    }
    
    @AsFunction
    public static Invariable or(Iterable<? extends Invariable> invariables)
    {
        return new CompositeInvariable(invariables)
        {
            @Override
            public boolean invariant()
            {
                return any(components, invariant);
            }        
        };
    }
    
    @AsFunction
    public static Invariable not(final Invariable invariable)
    {
        return new Invariable()
        {
            @Override
            public boolean invariant()
            {
                return !invariable.invariant();
            }

            @Override
            public Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations()
            {
                return invariant() ? noViolations : singleton(immutableEntry(this, singleton("failedInverseInvariant")));
            }
        };
    }
    
    public static final Iterable<? extends Entry<? extends Invariable, Set<String>>> noViolations = emptySet();
}
