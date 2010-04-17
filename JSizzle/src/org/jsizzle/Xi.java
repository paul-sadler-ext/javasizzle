package org.jsizzle;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.Set;
import java.util.Map.Entry;

public class Xi<T extends Binding> extends Delta<T> implements Invariable
{
    public Xi(T before, T after)
    {
        super(before, after);
    }

    @Override
    public Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations()
    {
        return apply(null) ? singletonMap(this, singleton("Data changed")).entrySet()
                           : Collections.<Entry<Invariable, Set<String>>>emptySet();
    }

    @Override
    public boolean apply(Void input)
    {
        return before.getData().equals(after.getData());
    }
}
