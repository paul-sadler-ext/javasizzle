/**
 * 
 */
package org.jsizzle;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static org.jcurry.ValueObjects.uniques;

import java.util.Set;
import java.util.Map.Entry;

import com.google.common.base.Function;

public abstract class CompositeInvariable implements Invariable
{
    protected final Iterable<? extends Invariable> components;
    protected final Iterable<Entry<? extends Invariable, Set<String>>> violations;

    public CompositeInvariable(Iterable<? extends Invariable> components)
    {
        this.components = components;
        this.violations = uniques(concat(transform(filter(components, not(invariant)), getViolations)),
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
    public Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations()
    {
        return violations;
    }
}