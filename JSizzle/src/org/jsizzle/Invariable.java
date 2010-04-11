package org.jsizzle;

import java.util.Set;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public interface Invariable extends Predicate<Void>
{
    Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations();
    
    // TODO: Should be generated by AsFunction but blows up Eclipse
    final static Function<Invariable, Iterable<? extends Entry<? extends Invariable, Set<String>>>> getViolations =
        new Function<Invariable, Iterable<? extends Entry<? extends Invariable, Set<String>>>>()
    {
        @Override
        public Iterable<? extends Entry<? extends Invariable, Set<String>>> apply(Invariable from)
        {
            return from.getViolations();
        }
    };
    
    final Predicate<Invariable> application = new Predicate<Invariable>()
    {
        @Override
        public boolean apply(Invariable input)
        {
            return input.apply(null);
        }
    };
}
