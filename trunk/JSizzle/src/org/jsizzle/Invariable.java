package org.jsizzle;

import java.util.Map.Entry;
import java.util.Set;

import org.jcurry.AsFunction;

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
     * Returns whether this invariable violates its invariant.
     */
    @AsFunction
    boolean invariant();
}
