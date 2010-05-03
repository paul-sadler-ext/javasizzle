package org.jsizzle;

import static com.google.common.base.Functions.constant;
import static com.google.common.base.Functions.identity;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static junit.framework.Assert.assertEquals;
import static org.jsizzle.Delta.deltas;

import org.junit.Test;


public class DeltaTest
{
    @Test
    public void deltasEmptyEmptyIsEmpty()
    {
        assertEquals(emptySet(), deltas(emptySet(), emptySet(), identity()));
    }

    @Test
    public void deltasSingletonEmptyIsEmpty()
    {
        assertEquals(emptySet(), deltas(singleton("Fred"), emptySet(), identity()));
    }

    @Test
    public void deltasEmptySingletonIsEmpty()
    {
        assertEquals(emptySet(), deltas(emptySet(), singleton("Fred"), identity()));
    }

    @Test
    public void deltasNoMatchIsEmpty()
    {
        assertEquals(emptySet(), deltas(singleton("Fred"), singleton("Barney"), identity()));
    }

    @Test
    public void deltasSingletonSingletonMatchIsSingleton()
    {
        assertEquals(singleton(new Delta<String>("Fred", "Barney")), deltas(singleton("Fred"), singleton("Barney"), constant(1)));
    }
}
