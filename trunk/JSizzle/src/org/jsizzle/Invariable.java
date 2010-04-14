package org.jsizzle;

import java.util.Set;
import java.util.Map.Entry;

import com.google.common.base.Predicate;

public interface Invariable extends Predicate<Void>
{
    @AsFunction
    Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations();
    
    final Predicate<Invariable> application = new Predicate<Invariable>()
    {
        @Override
        public boolean apply(Invariable input)
        {
            return input.apply(null);
        }
    };
}
