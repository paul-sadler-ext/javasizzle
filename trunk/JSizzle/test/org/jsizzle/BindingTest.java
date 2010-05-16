package org.jsizzle;

import static com.google.common.base.Functions.compose;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.jcurry.ValueObjects.list;
import static org.jsizzle.InvariablesTest.bad;
import junit.framework.AssertionFailedError;

import org.jcurry.AsFunction;
import org.junit.Test;

public class BindingTest
{
    @Test
    public void testEmpty()
    {
        assertTrue(empty.invariant());
        assertTrue(isEmpty(empty.getViolations()));
        assertTrue(empty.getData().isEmpty());
        empty.checkInvariant();
    }
    
    @Test
    public void testWithDatum()
    {
        assertTrue(withDatum.invariant());
        assertTrue(isEmpty(withDatum.getViolations()));
        assertEquals(singletonList("Fred"), withDatum.getData());
        withDatum.checkInvariant();
    }
    
    @Test(expected = AssertionFailedError.class)
    public void testInvariantViolation()
    {
        assertFalse(withInvariantViolation.invariant());
        assertEquals(singletonList(immutableEntry(withInvariantViolation,
                                                  singleton("invariantViolation"))),
                     list(withInvariantViolation.getViolations()));
        assertTrue(withInvariantViolation.getData().isEmpty());
        withInvariantViolation.checkInvariant();
    }
    
    @Test(expected = AssertionFailedError.class)
    public void testDatumViolation()
    {
        assertFalse(withDatumViolation.invariant());
        assertEquals(singletonList(immutableEntry(bad,
                                                  singleton("bad"))),
                     list(withDatumViolation.getViolations()));
        assertEquals(singletonList(bad), withDatumViolation.getData());
        withDatumViolation.checkInvariant();
    }
    
    @Test(expected = AssertionFailedError.class)
    public void testSubBindingViolation()
    {
        assertFalse(withSubBindingViolation.invariant());
        assertEquals(singletonList(immutableEntry(withInvariantViolation,
                                                  singleton("invariantViolation"))),
                     list(withSubBindingViolation.getViolations()));
        assertEquals(singletonList(withInvariantViolation), withSubBindingViolation.getData());
        withSubBindingViolation.checkInvariant();
    }
    
    @Test
    public void testWithInclusion()
    {
        assertTrue(withInclusion.invariant());
        assertTrue(isEmpty(withInclusion.getViolations()));
        assertEquals(singletonList("Fred"), withInclusion.getData());
        withDatum.checkInvariant();
    }
    
    @Test(expected = AssertionFailedError.class)
    public void testViolatingInclusion()
    {
        assertFalse(withViolatingInclusion.invariant());
        assertEquals(singletonList(immutableEntry(withViolatingInclusion,
                                                  singleton("invariantViolation"))),
                     list(withViolatingInclusion.getViolations()));
        assertEquals(singletonList(withInvariantViolation), withViolatingInclusion.getData());
        withViolatingInclusion.checkInvariant();
    }
    
    public static final MockBinding empty = new MockBinding() {};
    
    public static final class WithDatum extends Binding<WithDatum>
    {
        @AsFunction
        private String datum = "Fred";
        
        {
            addAccessor(getDatum, Inclusion.DIRECT);
        }
    }
    public static final WithDatum withDatum = new WithDatum();
    
    public static final MockBinding withInvariantViolation = new MockBinding()
    {
        {
            addViolation("invariantViolation");
        }
    };
    
    public static final class WithDatumViolation extends Binding<WithDatumViolation>
    {
        @AsFunction
        private Invariable datum = bad;
        
        {
            addAccessor(getDatum, Inclusion.DIRECT);
        }
    };
    public static final WithDatumViolation withDatumViolation = new WithDatumViolation();
    
    public static final class WithSubBindingViolation extends Binding<WithSubBindingViolation>
    {
        @AsFunction
        private MockBinding datum = withInvariantViolation;
        
        {
            addAccessor(getDatum, Inclusion.DIRECT);
        }
    };
    public static final WithSubBindingViolation withSubBindingViolation = new WithSubBindingViolation();
    
    public static final class WithInclusion extends Binding<WithInclusion>
    {
        @AsFunction
        private WithDatum included = withDatum;
        
        {
            addAccessor(compose(WithDatum.getDatum, getIncluded), Inclusion.EXPANDED);
            addAccessor(getIncluded, Inclusion.INCLUDED);
        }
    };
    public static final WithInclusion withInclusion = new WithInclusion();
    
    public static final class WithViolatingInclusion extends Binding<WithViolatingInclusion>
    {
        @AsFunction
        private WithSubBindingViolation included = withSubBindingViolation;
        
        {
            addAccessor(compose(WithSubBindingViolation.getDatum, getIncluded), Inclusion.EXPANDED);
            addAccessor(getIncluded, Inclusion.INCLUDED);
        }
    };
    public static final WithViolatingInclusion withViolatingInclusion = new WithViolatingInclusion();
}
