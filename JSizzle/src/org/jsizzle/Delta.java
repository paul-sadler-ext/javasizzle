package org.jsizzle;

import static com.google.common.base.Functions.forMap;
import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.google.common.collect.Sets.difference;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.jcurry.ValueObjects.domainRestrict;
import static org.jcurry.ValueObjects.toSet;
import static org.jcurry.ValueObjects.transform;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import lombok.Data;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

@Data
public class Delta<T extends Binding<T>> implements Iterable<T>
{
    public final T before;
    public final T after;
    
    public boolean unchangedExcept(Function<T, ?> accessor)
    {
        return unchangedExcept(singleton(accessor));
    }
    
    @SuppressWarnings("unchecked")
    public boolean unchangedExcept(Function<T, ?> accessor1, Function<T, ?> accessor2)
    {
        return unchangedExcept(asList(accessor1, accessor2));
    }
    
    @SuppressWarnings("unchecked")
    public boolean unchangedExcept(Function<T, ?> accessor1, Function<T, ?> accessor2, Function<T, ?> accessor3)
    {
        return unchangedExcept(asList(accessor1, accessor2, accessor3));
    }
    
    @SuppressWarnings("unchecked")
    public boolean unchangedExcept(Function<T, ?> accessor1, Function<T, ?> accessor2, Function<T, ?> accessor3, Function<T, ?> accessor4)
    {
        return unchangedExcept(asList(accessor1, accessor2, accessor3, accessor4));
    }
    
    public boolean unchangedExcept(Function<T, ?>... accessors)
    {
        return unchangedExcept(asList(accessors));
    }
    
    public boolean unchangedExcept(Iterable<? extends Function<T, ?>> accessors)
    {
        return all(difference(before.getDataAccessors(), toSet(accessors)), new Predicate<Function<T, ?>>()
        {
            @Override
            public boolean apply(Function<T, ?> accessor)
            {
                return accessor.apply(before).equals(accessor.apply(after));
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator()
    {
        return Iterators.forArray(before, after);
    }

    /**
     * Returns a set of {@link Delta}s that relate the bindings in the
     * <code>before</code> parameter to the bindings in the
     * <code>after</code> parameter, using the given function to
     * establish binding identity.
     * <p>
     * Only bindings that are present (according to the identity
     * function) in both parameters will be returned.
     */
    public static <T extends Binding<T>, U> Set<Delta<T>> deltas(Set<? extends T> befores,
                                                                 Set<? extends T> afters,
                                                                 Function<? super T, U> uniqueness)
            throws IllegalArgumentException
    {
        final Map<? extends T, U> beforeToKey = domainRestrict(uniqueness, befores);
        final Map<U, ? extends T> keyToAfter = uniqueIndex(afters, uniqueness);
        final Map<? extends T, ? extends T> deltas = transformValues(filterValues(beforeToKey, in(keyToAfter.keySet())), forMap(keyToAfter));
        return transform(deltas.entrySet(), new Function<Entry<? extends T, ? extends T>, Delta<T>>()
        {
            @Override
            public Delta<T> apply(Entry<? extends T, ? extends T> from)
            {
                return new Delta<T>(from.getKey(), from.getValue());
            }
        });
    }
}