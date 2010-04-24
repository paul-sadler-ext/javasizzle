package org.jsizzle;

import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Collections.singleton;
import static org.jsizzle.Invariables.noViolations;

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
        return invariant() ? noViolations : singleton(immutableEntry(this, singleton("dataChanged")));
    }

    @Override
    public boolean invariant()
    {
        return before.getData().equals(after.getData());
    }
}
