package org.jsizzle;

import static com.google.common.base.Functions.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.Sets.filter;
import static java.util.Collections.singleton;
import static junit.framework.Assert.fail;
import static org.jcurry.ValueObjects.list;
import static org.jcurry.ValueObjects.transform;
import static org.jsizzle.Invariables.and;
import static org.jsizzle.Invariables.asInvariable;
import static org.jsizzle.Invariables.or;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.AssertionFailedError;

import com.google.common.base.Function;

public abstract class Binding<T extends Binding<T>> implements Invariable
{
    public enum Inclusion {DIRECT, INCLUDED, EXPANDED}

    /**
     * All accessor functions to binding members, with an indication of how they
     * are included; should be populated by the subclass constructor.
     */
    private final Map<Function<T, ?>, Inclusion> accessors = new LinkedHashMap<Function<T, ?>, Inclusion>();
    
    /**
     * A function that applies accessors to this binding.
     */
    private final Function<Function<T, ?>, Object> applyAccessor = new Function<Function<T, ?>, Object>()
    {
        @Override
        @SuppressWarnings("unchecked")
        public Object apply(Function<T, ?> accessor)
        {
            return accessor.apply((T)Binding.this);
        }
    };

    /**
     * The set of invariables in this bindings data. First take the accessors to
     * unexpanded data (because expanded data would miss invariants), apply the
     * accessor to this binding, convert to an invariable, and exclude nulls
     * (data that is not an invariable).
     */
    private final Set<Invariable> invariables =
        filter(transform(filterValues(accessors, not(equalTo(Inclusion.EXPANDED))).keySet(), compose(asInvariable, applyAccessor)), notNull());

    /**
     * All violations of invariants of this binding, should be populated by the
     * subclass constructor.
     */
    private final Set<String> violations = new LinkedHashSet<String>();

    protected void addAccessor(Function<T, ?> accessor, Inclusion inclusion)
    {
        accessors.put(accessor, inclusion);
    }
    
    protected void addViolation(String violation)
    {
        violations.add(violation);
    }
    
    @Override
    public Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations()
    {
        return violations.isEmpty()
            ? and(invariables).getViolations()
            : concat(singleton(immutableEntry(this, violations)), and(invariables).getViolations());
    }
    
    @Override
    public boolean invariant()
    {
        final boolean disjoint = getClass().getAnnotation(Disjoint.class) != null;
        return violations.isEmpty() && (disjoint ? or(invariables) : and(invariables)).invariant();
    }

    public void checkInvariant() throws AssertionFailedError
    {
        if (!invariant())
        {
            fail(transform(getViolations(), new Function<Entry<? extends Invariable, Set<String>>, String>()
            {
                @Override
                public String apply(Entry<? extends Invariable, Set<String>> from)
                {
                    return from.getValue() + " failed in " + from.getKey().getClass().getSimpleName();
                }
            }).toString());
        }
    }

    public Set<Function<T, ?>> getDataAccessors()
    {
        return filterValues(accessors, not(equalTo(Inclusion.INCLUDED))).keySet();
    }
    
    public List<Object> getData()
    {
        return list(transform(getDataAccessors(), applyAccessor));
    }
}
