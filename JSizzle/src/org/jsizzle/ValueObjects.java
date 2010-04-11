package org.jsizzle;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

public class ValueObjects
{
    public static <F, T> Set<T> transform(final Set<F> fromSet, final Function<? super F, ? extends T> function)
    {
        return toSet(Iterables.transform(fromSet, function));
    }
    
    public static <T> Set<T> bigUnion(final Iterable<? extends Set<? extends T>> inputs)
    {
        Set<T> union = ImmutableSet.of();
        for (Set<? extends T> input : inputs)
            union = Sets.union(union, input);
        return union;
    }
    
    public static <T> Set<T> toSet(final Iterable<T> input)
    {
        return new AbstractSet<T>()
        {
            final Iterable<T> uniques = uniques(input, Functions.identity());
            
            @Override
            public Iterator<T> iterator()
            {
                return uniques.iterator();
            }

            @Override
            public int size()
            {
                return Iterables.size(uniques);
            }

            @Override
            public boolean contains(Object o)
            {
                return Iterables.contains(input, o);
            }

            @Override
            public boolean isEmpty()
            {
                return Iterables.isEmpty(input);
            }
        };
    }
    
    public static <T, U> Iterable<T> uniques(final Iterable<T> input, final Function<? super T, U> uniqueness)
    {
        // Note that we must use a new "done" set for every iteration
        return new Iterable<T>()
        {
            @Override
            public Iterator<T> iterator()
            {
                return Iterators.filter(input.iterator(), new Predicate<T>()
                {
                    final Set<U> done = Sets.newHashSet();

                    @Override
                    public boolean apply(T input)
                    {
                        return done.add(uniqueness.apply(input));
                    }
                });
            }
        };
    }
}
