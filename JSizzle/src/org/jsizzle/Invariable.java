package org.jsizzle;

import java.util.Set;
import java.util.Map.Entry;

import org.jcurry.AsFunction;

import com.google.common.base.Predicate;

public interface Invariable
{
    /**
     * Returns all violations of this invariable and its contained invariables.
     * Note that the returned iterable may not be empty even if this invariable
     * does not violate its invariant, because its contained invariables may be
     * disjoint.
     */
    @AsFunction
    Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations();

    /**
     * Returns whether this invariable violates its invariant. Note this method
     * is not made available as a function because a predicate is more useful.
     * 
     * @see #invariant
     */
    boolean invariant();
    
    static final Predicate<Invariable> invariant = new Predicate<Invariable>()
    {
        @Override
        public boolean apply(Invariable invariable)
        {
            return invariable.invariant();
        }
    };
}
