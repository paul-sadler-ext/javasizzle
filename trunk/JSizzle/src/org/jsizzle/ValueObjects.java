package org.jsizzle;

import static com.google.common.collect.Maps.filterKeys;
import static com.google.common.collect.Sets.union;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    
    public static <K, V> Map<K, V> override(final Map<K, V> left, final Map<K, V> right)
    {
        return new AbstractMap<K, V>()
        {
            @Override
            public Set<Map.Entry<K, V>> entrySet()
            {
                return union(right.entrySet(), filterKeys(left, new Predicate<K>()
                {
                    @Override
                    public boolean apply(K key)
                    {
                        return !right.containsKey(key);
                    }
                }).entrySet());
            }

            @Override
            public boolean containsKey(Object key)
            {
                return left.containsKey(key) || right.containsKey(key);
            }

            @Override
            public V get(Object key)
            {
                return right.containsKey(key) ? right.get(key) : left.get(key);
            }

            @Override
            public boolean isEmpty()
            {
                return right.isEmpty() && left.isEmpty();
            }

            @Override
            public Set<K> keySet()
            {
                return union(left.keySet(), right.keySet());
            }
        };
    }
    
    public static <T> List<T> list(final Iterable<T> iterable)
    {
        return new AbstractList<T>()
        {
            @Override
            public T get(int position)
            {
                return Iterables.get(iterable, position);
            }

            @Override
            public int size()
            {
                return Iterables.size(iterable);
            }

            @Override
            public Iterator<T> iterator()
            {
                return iterable.iterator();
            }
        };
    }
}