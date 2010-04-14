/**
 * 
 */
package org.jsizzle;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;
import static org.jsizzle.ValueObjects.uniques;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.common.base.Function;

public class CompositeInvariable implements Invariable
{
    private final Iterable<Invariable> components;
    private final Iterable<Entry<? extends Invariable, Set<String>>> violations;

    public CompositeInvariable(Iterable<Invariable> components)
    {
        this.components = components;
        this.violations = uniques(concat(transform(filter(components, not(application)), getViolations)),
            new Function<Entry<? extends Invariable, Set<String>>, Invariable>()
        {
            @Override
            public Invariable apply(Entry<? extends Invariable, Set<String>> from)
            {
                return from.getKey();
            }
        });
    }

    @Override
    public boolean apply(Void input)
    {
        return and(components).apply(null);
    }

    @Override
    public Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations()
    {
        return violations;
    }

    protected static final Function<Object, Invariable> asInvariable = new Function<Object, Invariable>()
    {
        @Override
        public Invariable apply(Object from)
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
                subData = concat(((Map<?, ?>)from).keySet(), ((Map<?, ?>)from).values());
            }
            else
            {
                return null;
            }
            final Iterable<Invariable> subInvariables = filter(transform(subData, this), notNull());
            return isEmpty(subInvariables) ? null : new CompositeInvariable(subInvariables);
        }
    };
}