package org.jsizzle;

import static com.google.common.base.Functions.constant;
import static com.google.common.base.Functions.identity;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static junit.framework.Assert.assertEquals;
import static org.jsizzle.Delta.deltas;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;


public class DeltaTest
{
    private static final Set<MockBinding> noBindings = Collections.emptySet();
    private static final MockBinding fred = new MockBinding();
    private static final MockBinding barney = new MockBinding();
    
    @Test
    public void deltasEmptyEmptyIsEmpty()
    {
        assertEquals(emptySet(), deltas(noBindings, noBindings, identity()));
    }

    @Test
    public void deltasSingletonEmptyIsEmpty()
    {
        assertEquals(emptySet(), deltas(singleton(fred), noBindings, identity()));
    }

    @Test
    public void deltasEmptySingletonIsEmpty()
    {
        assertEquals(emptySet(), deltas(noBindings, singleton(fred), identity()));
    }

    @Test
    public void deltasNoMatchIsEmpty()
    {
        assertEquals(emptySet(), deltas(singleton(fred), singleton(barney), identity()));
    }

    @Test
    public void deltasSingletonSingletonMatchIsSingleton()
    {
        assertEquals(singleton(new Delta<MockBinding>(fred, barney)), deltas(singleton(fred), singleton(barney), constant(1)));
    }
}
