package org.jsizzle;

import static com.google.common.collect.Iterables.elementsEqual;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.jsizzle.Invariables.and;
import static org.jsizzle.Invariables.asInvariable;
import static org.jsizzle.Invariables.not;
import static org.jsizzle.Invariables.or;

import java.util.Collections;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.Test;


public class InvariablesTest
{
    @Test
    public void testNullAsInvariable()
    {
        assertNull(asInvariable(null));
    }
    
    @Test
    public void testObjectAsInvariable()
    {
        assertNull(asInvariable(new Object()));
    }
    
    @Test
    public void testInvariableAsInvariable()
    {
        assertTrue(asInvariable(good).invariant());
        assertTrue(isEmpty(asInvariable(good).getViolations()));
    }
    
    @Test
    public void testIterableAsInvariable()
    {
        assertTrue(asInvariable(singleton(good)).invariant());
        assertTrue(isEmpty(asInvariable(singleton(good)).getViolations()));
    }
    
    @Test
    public void testArrayAsInvariable()
    {
        assertTrue(asInvariable(new Object [] {good}).invariant());
        assertTrue(isEmpty(asInvariable(new Object [] {good}).getViolations()));
    }
    
    @Test
    public void testMapAsInvariable()
    {
        assertTrue(asInvariable(singletonMap(good, good)).invariant());
        assertTrue(isEmpty(asInvariable(singletonMap(good, good)).getViolations()));
    }
    
    @Test
    public void testAndEmpty()
    {
        assertTrue(and(Collections.<Invariable>emptySet()).invariant());
        assertTrue(isEmpty(and(Collections.<Invariable>emptySet()).getViolations()));
    }
    
    @Test
    public void testAndOne()
    {
        assertTrue(and(singleton(good)).invariant());
        assertTrue(isEmpty(and(singleton(good)).getViolations()));
    }
    
    @Test
    public void testAndTrueTrue()
    {
        assertTrue(and(asList(good, good)).invariant());
        assertTrue(isEmpty(and(asList(good, good)).getViolations()));
    }
    
    @Test
    public void testAndTrueFalse()
    {
        assertFalse(and(asList(good, bad)).invariant());
        assertTrue(elementsEqual(bad.getViolations(), and(asList(good, bad)).getViolations()));
    }
    
    @Test
    public void testAndFalseFalse()
    {
        assertFalse(and(asList(bad, bad)).invariant());
        assertTrue(elementsEqual(bad.getViolations(), and(asList(bad, bad)).getViolations()));
    }
    
    @Test
    public void testOrEmpty()
    {
        assertFalse(or(Collections.<Invariable>emptySet()).invariant());
        assertTrue(isEmpty(or(Collections.<Invariable>emptySet()).getViolations()));
    }
    
    @Test
    public void testOrOne()
    {
        assertTrue(or(singleton(good)).invariant());
        assertTrue(isEmpty(or(singleton(good)).getViolations()));
    }
    
    @Test
    public void testOrTrueTrue()
    {
        assertTrue(or(asList(good, good)).invariant());
        assertTrue(isEmpty(or(asList(good, good)).getViolations()));
    }
    
    @Test
    public void testOrTrueFalse()
    {
        assertTrue(or(asList(good, bad)).invariant());
        assertTrue(elementsEqual(bad.getViolations(), or(asList(good, bad)).getViolations()));
    }
    
    @Test
    public void testOrFalseFalse()
    {
        assertFalse(or(asList(bad, bad)).invariant());
        assertTrue(elementsEqual(bad.getViolations(), or(asList(bad, bad)).getViolations()));
    }
    
    @Test
    public void testNotFalse()
    {
        assertTrue(not(bad).invariant());
        assertTrue(isEmpty(not(bad).getViolations()));
    }
    
    @Test
    public void testNotTrue()
    {
        assertFalse(not(good).invariant());
        assertEquals(1, size(not(good).getViolations()));
    }
    
    private static final Invariable bad = new Invariable()
    {
        @Override
        public Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations()
        {
            return singleton(immutableEntry(this, singleton("bad")));
        }

        @Override
        public boolean invariant()
        {
            return false;
        }
    };
    
    private static final Invariable good = new Invariable()
    {
        @Override
        public Iterable<? extends Entry<? extends Invariable, Set<String>>> getViolations()
        {
            return emptySet();
        }

        @Override
        public boolean invariant()
        {
            return true;
        }
    };
}
