package org.jsizzle;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.all;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public class Type<T> extends AbstractSet<T>
{
    private static final Map<Class<?>, Type<?>> types = new HashMap<Class<?>, Type<?>>();
    private final Class<T> typeClass;
    
    @SuppressWarnings("unchecked")
    public static <T> Type<T> type(Class<T> typeClass)
    {
        Type<T> type = (Type<T>)types.get(typeClass);
        if (type == null)
            types.put(typeClass, type = new Type(typeClass));
        return type;
    }
    
    private Type(Class<T> typeClass)
    {
        this.typeClass = typeClass;
    }

    @Override
    public Iterator<T> iterator()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o)
    {
        return typeClass.isAssignableFrom(o.getClass());
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return all(c, instanceOf(typeClass));
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }
}
