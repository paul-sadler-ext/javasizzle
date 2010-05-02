package org.jsizzle;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableList;
import static org.jsizzle.Invariables.and;
import static org.jsizzle.Invariables.asInvariable;
import static org.jsizzle.Invariables.or;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.Assert;

import com.google.common.base.Function;

public abstract class Binding implements Invariable
{
    private final List<Object> data = new ArrayList<Object>();
    private final Set<String> violations = new LinkedHashSet<String>();
    private final Set<Invariable> invariables = new LinkedHashSet<Invariable>();
    private final Invariable composition = and(invariables);

    protected void addDatum(Object datum)
    {
        data.add(datum);
        final Invariable invariable = asInvariable(datum);
        if (invariable != null)
            invariables.add(invariable);
    }
    
    protected void addViolation(String violation)
    {
        violations.add(violation);
    }
    
    @Override
    public Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations()
    {
        return violations.isEmpty()
            ? composition.getViolations()
            : concat(singleton(immutableEntry(this, violations)), composition.getViolations());
    }
    
    @Override
    public boolean invariant()
    {
        final boolean disjoint = getClass().getAnnotation(Disjoint.class) != null;
        return violations.isEmpty() && (disjoint ? or(invariables) : and(invariables)).invariant();
    }

    public void checkInvariant() throws IllegalStateException
    {
        if (!invariant())
        {
            Assert.fail(transform(getViolations(), new Function<Entry<? extends Invariable, Set<String>>, String>()
            {
                @Override
                public String apply(Entry<? extends Invariable, Set<String>> from)
                {
                    return from.getValue() + " failed in " + from.getKey().getClass().getSimpleName();
                }
            }).toString());
        }
    }
    
    public List<?> getData()
    {
        return unmodifiableList(data);
    }
}
