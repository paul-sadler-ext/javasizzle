package org.jsizzle;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableList;
import static org.jsizzle.CompositeInvariable.asInvariable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

public abstract class Binding implements Invariable
{
    private final List<Object> data = new ArrayList<Object>();
    private final Set<String> violations = new LinkedHashSet<String>();
    private final Set<Invariable> invariables = new LinkedHashSet<Invariable>();
    private final Invariable composition = new CompositeInvariable(invariables);

    protected void addDatum(Object datum)
    {
        data.add(datum);
        final Invariable invariable = asInvariable.apply(datum);
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
    public boolean apply(Void input)
    {
        final boolean disjoint = getClass().getAnnotation(Disjoint.class) != null;
        return violations.isEmpty() && (disjoint ? or(invariables) : and(invariables)).apply(null);
    }

    public void checkInvariant() throws IllegalStateException
    {
        if (!apply(null))
            throw new IllegalStateException(getViolations().toString());
    }
    
    public List<?> getData()
    {
        return unmodifiableList(data);
    }
}
