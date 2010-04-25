package org.jsizzle;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.jsizzle.InvariablesTest.bad;
import static org.jsizzle.ValueObjects.list;

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
    public void testIdentity()
    {
        assertTrue(withDatum.invariant());
        assertTrue(isEmpty(withDatum.getViolations()));
        assertEquals(singletonList("Fred"), withDatum.getData());
        withDatum.checkInvariant();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testInvariantViolation()
    {
        assertFalse(withInvariantViolation.invariant());
        assertEquals(singletonList(immutableEntry(withInvariantViolation,
                                                  singleton("invariantViolation"))),
                     list(withInvariantViolation.getViolations()));
        assertTrue(withInvariantViolation.getData().isEmpty());
        withInvariantViolation.checkInvariant();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testDatumViolation()
    {
        assertFalse(withDatumViolation.invariant());
        assertEquals(singletonList(immutableEntry(bad,
                                                  singleton("bad"))),
                     list(withDatumViolation.getViolations()));
        assertEquals(singletonList(bad), withDatumViolation.getData());
        withDatumViolation.checkInvariant();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testSubBindingViolation()
    {
        assertFalse(withSubBindingViolation.invariant());
        assertEquals(singletonList(immutableEntry(withInvariantViolation,
                                                  singleton("invariantViolation"))),
                     list(withSubBindingViolation.getViolations()));
        assertEquals(singletonList(withInvariantViolation), withSubBindingViolation.getData());
        withSubBindingViolation.checkInvariant();
    }
    
    public static final Binding empty = new Binding() {};
    public static final Binding withDatum = new Binding()
    {
        {
            addDatum("Fred");
        }
    };
    public static final Binding withInvariantViolation = new Binding()
    {
        {
            addViolation("invariantViolation");
        }
    };
    public static final Binding withDatumViolation = new Binding()
    {
        {
            addDatum(bad);
        }
    };
    public static final Binding withSubBindingViolation = new Binding()
    {
        {
            addDatum(withInvariantViolation);
        }
    };
}
