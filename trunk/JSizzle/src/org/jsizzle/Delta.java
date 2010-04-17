package org.jsizzle;

import static com.google.common.collect.Iterables.all;
import static java.util.Arrays.asList;

import java.util.Iterator;

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
}
