package org.jcurry;

import static com.google.common.base.Functions.identity;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.jcurry.ValueObjects.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.base.Functions;

public class ValueObjectsTest
{
    @Test(expected = NullPointerException.class)
    public void testListNullIterable()
    {
        ValueObjects.list(null).size();
    }
    
    @Test
    public void testListEmptyIterable()
    {
        assertEquals(emptyList(), ValueObjects.list(emptyList()));
    }

    @Test
    public void testListList()
    {
        final List<String> flintstones = asList("Fred", "Barney");
        final List<String> listed = ValueObjects.list(flintstones);
        assertEquals(flintstones, listed);
    }
    
    @Test
    public void testListIsLive()
    {
        final List<String> flintstones = new ArrayList<String>(asList("Fred", "Barney"));
        final List<String> listed = ValueObjects.list(flintstones);
        flintstones.add("Wilma");
        assertEquals(flintstones, listed);
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testListIsUnmodifiable()
    {
        ValueObjects.list(new ArrayList<String>()).add("Wilma");
    }
    
    @Test
    public void testOverrideEmptyWithEmpty()
    {
        assertTrue(ValueObjects.override(emptyMap(), emptyMap()).isEmpty());
    }
    
    @Test
    public void testOverrideOneWithEmpty()
    {
        final Map<String, String> flintstones = singletonMap("Fred", "Flintstone");
        assertEquals(flintstones, ValueObjects.override(flintstones, Collections.<String, String>emptyMap()));
    }
    
    @Test
    public void testOverrideEmptyWithOne()
    {
        final Map<String, String> flintstones = singletonMap("Fred", "Flintstone");
        assertEquals(flintstones, ValueObjects.override(Collections.<String, String>emptyMap(), flintstones));
    }
    
    @Test
    public void testOverrideOneWithOneDifferentKey()
    {
        final Map<String, String> flintstones = singletonMap("Fred", "Flintstone");
        final Map<String, String> rubbles = singletonMap("Barney", "Rubble");
        final Map<String, String> hannaBarbera = new HashMap<String, String>(flintstones);
        hannaBarbera.putAll(rubbles);
        
        assertEquals(hannaBarbera, ValueObjects.override(flintstones, rubbles));
    }
    
    @Test
    public void testOverrideOneWithOneSameKey()
    {
        final Map<String, String> flintstones = singletonMap("Fred", "Flintstone");
        final Map<String, String> flintstones2 = singletonMap("Fred", "Rubble");
        
        assertEquals(flintstones2, ValueObjects.override(flintstones, flintstones2));
    }
    
    @Test
    public void testOverrideGetsNewKeysLive()
    {
        final Map<String, String> flintstones = singletonMap("Fred", "Flintstone");
        final Map<String, String> flintstones2 = new HashMap<String, String>(singletonMap("Fred", "Rubble"));
        final Map<String, String> override = ValueObjects.override(flintstones, flintstones2);
        flintstones2.put("Wilma", "Flintstone");
        
        assertEquals(flintstones2, override);
    }
    
    @Test
    public void testOverrideStillOverridesLive()
    {
        final Map<String, String> flintstones = new HashMap<String, String>(singletonMap("Fred", "Flintstone"));
        final Map<String, String> flintstones2 = singletonMap("Fred", "Rubble");
        final Map<String, String> override = ValueObjects.override(flintstones, flintstones2);
        flintstones.put("Fred", "Bedrock");
        
        assertEquals(flintstones2, override);
    }
    
    @Test
    public void testUniquesEmpty()
    {
        assertTrue(isEmpty(ValueObjects.uniques(emptyList(), identity())));
    }
    
    @Test
    public void testUniquesOne()
    {
        assertEquals(asList("Fred"), list(ValueObjects.uniques(asList("Fred"), identity())));
    }
    
    @Test
    public void testUniquesTwoDifferent()
    {
        assertEquals(asList("Fred", "Barney"), list(ValueObjects.uniques(asList("Fred", "Barney"), identity())));
    }
    
    @Test
    public void testUniquesTwoSame()
    {
        assertEquals(asList("Fred"), list(ValueObjects.uniques(asList("Fred", "Fred"), identity())));
    }
    
    @Test
    public void testUniquesMixedBag()
    {
        assertEquals(asList("Fred", "Barney", "Wilma"),
                     list(ValueObjects.uniques(asList("Fred", "Fred", "Barney", "Wilma", "Barney"), identity())));
    }
    
    @Test
    public void testUniquesRunTwice()
    {
        final Iterable<String> uniques = ValueObjects.uniques(asList("Fred", "Fred", "Barney", "Wilma", "Barney"), identity());
        assertEquals(asList("Fred", "Barney", "Wilma"), list(uniques));
        assertEquals(asList("Fred", "Barney", "Wilma"), list(uniques));
    }
    
    @Test
    public void testUniquesWithUniquenessFunction()
    {
        final Map<String, Integer> uniqueness = new HashMap<String, Integer>();
        uniqueness.put("Fred", 1);
        uniqueness.put("Barney", 1);
        uniqueness.put("Wilma", 2);
        assertEquals(asList("Fred", "Wilma"),
                     list(ValueObjects.uniques(asList("Fred", "Barney", "Wilma"), Functions.forMap(uniqueness))));
    }
    
    @Test
    public void testToSetEmpty()
    {
        assertEquals(emptySet(), ValueObjects.toSet(emptyList()));
    }
    
    @Test
    public void testToSetMixedBag()
    {
        assertEquals(new HashSet<String>(asList("Fred", "Barney", "Wilma")),
                     ValueObjects.toSet(asList("Fred", "Fred", "Barney", "Wilma", "Barney")));
    }
    
    @Test
    public void testToSetIsLive()
    {
        final List<String> flintstones = new ArrayList<String>();
        final Set<String> set = ValueObjects.toSet(flintstones);
        flintstones.addAll(asList("Fred", "Fred"));
        assertEquals(singleton("Fred"), set);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testBigUnionEmpty()
    {
        assertEquals(emptySet(), ValueObjects.bigUnion(asList(emptySet(), emptySet())));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBigUnionDisjoint()
    {
        assertEquals(new HashSet<String>(asList("Fred", "Barney")),
                     ValueObjects.bigUnion(asList(singleton("Fred"), singleton("Barney"))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBigUnionIntersecting()
    {
        assertEquals(singleton("Fred"),
                     ValueObjects.bigUnion(asList(singleton("Fred"), singleton("Fred"))));
    }
    
    @Test
    public void testTransformEmptySetNoOp()
    {
        assertEquals(emptySet(), ValueObjects.transform(emptySet(), identity()));
    }
    
    @Test
    public void testTransformSingletonNoOp()
    {
        assertEquals(singleton("Fred"), ValueObjects.transform(singleton("Fred"), identity()));
    }
    
    @Test
    public void testTransformSingleton()
    {
        assertEquals(singleton("Barney"), ValueObjects.transform(singleton("Fred"), Functions.forMap(singletonMap("Fred", "Barney"))));
    }
    
    @Test
    public void testTransformIsLive()
    {
        final Set<String> flintstones = new HashSet<String>(singleton("Fred"));
        final Set<Object> transformed = ValueObjects.transform(flintstones, identity());
        flintstones.add("Barney");
        assertEquals(flintstones, transformed);
    }
    
    @Test
    public void testEmptyContainsOnlyEmpty()
    {
        assertTrue(ValueObjects.only(emptySet()).apply(emptySet()));
    }
    
    @Test
    public void testEmptyContainsOnlySome()
    {
        assertTrue(ValueObjects.only(singleton("Fred")).apply(emptySet()));
    }
    
    @Test
    public void testSomeContainsOnlySame()
    {
        assertTrue(ValueObjects.only(singleton("Fred")).apply(singleton("Fred")));
    }
    
    @Test
    public void testSomeNotContainsOnlyDifferent()
    {
        assertFalse(ValueObjects.only(singleton("Fred")).apply(singleton("Barney")));
    }
    
    @Test
    public void testDomainRestrictToNothing()
    {
        final Map<Object, Object> restricted = ValueObjects.domainRestrict(identity(), emptySet());
        assertTrue(restricted.isEmpty());
        assertEquals(0, restricted.size());
        assertTrue(restricted.keySet().isEmpty());
        assertTrue(restricted.entrySet().isEmpty());
        assertTrue(restricted.values().isEmpty());
    }
    
    @Test
    public void testDomainRestrictToSingleton()
    {
        final Map<String, Object> restricted = ValueObjects.domainRestrict(identity(), singleton("Fred"));
        assertFalse(restricted.isEmpty());
        assertEquals(1, restricted.size());
        assertEquals(singleton("Fred"), restricted.keySet());
        assertEquals(singleton(immutableEntry("Fred", "Fred")), restricted.entrySet());
        assertEquals(singleton("Fred"), newHashSet(restricted.values()));
    }
}
