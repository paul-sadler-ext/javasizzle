package org.jsizzle;

import static com.google.common.base.Functions.forMap;
import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.util.Arrays.asList;
import static org.jsizzle.ValueObjects.domainRestrict;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import lombok.Data;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

@Data
public class Delta<T> implements Iterable<T>
{
    public final T before;
    public final T after;
    
    public boolean unchanged(Function<T, ?>... functions)
    {
        return all(asList(functions), new Predicate<Function<T, ?>>()
        {
            @Override
            public boolean apply(Function<T, ?> function)
            {
                return function.apply(before).equals(function.apply(after));
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator()
    {
        return Iterators.forArray(before, after);
    }
    
    public static <T, U> Set<Delta<T>> deltas(Set<? extends T> befores,
                                              Set<? extends T> afters,
                                              Function<T, U> uniqueness)
            throws IllegalArgumentException
    {
        final Map<? extends T, U> beforeToKey = domainRestrict(uniqueness, befores);
        final Map<U, ? extends T> keyToAfter = uniqueIndex(afters, uniqueness);
        final Map<? extends T, ? extends T> deltas = transformValues(filterValues(beforeToKey, in(keyToAfter.keySet())), forMap(keyToAfter));
        return ValueObjects.transform(deltas.entrySet(), new Function<Entry<? extends T, ? extends T>, Delta<T>>()
        {
            @Override
            public Delta<T> apply(Entry<? extends T, ? extends T> from)
            {
                return new Delta<T>(from.getKey(), from.getValue());
            }
        });
    }
}