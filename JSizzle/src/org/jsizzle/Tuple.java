package org.jsizzle;

import static java.util.Arrays.asList;

import java.util.AbstractList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public interface Tuple extends List<Object>
{
    @Data(staticConstructor = "of")
    @EqualsAndHashCode(callSuper=false)
    public static class Tuple2<T1, T2> extends AbstractList<Object> implements Tuple
    {
        public final T1 v1;
        public final T2 v2;
        
        @Override
        public int size()
        {
            return 2;
        }
        
        @Override
        public Object get(int i)
        {
            return i == 0 ? v1 : i == 1 ? v2 : null;
        }
    }

    @Data(staticConstructor = "of")
    @EqualsAndHashCode(callSuper=false)
    public static class Tuple3<T1, T2, T3> extends AbstractList<Object> implements Tuple
    {
        public final T1 v1;
        public final T2 v2;
        public final T3 v3;
        
        @Override
        public int size()
        {
            return 3;
        }
        
        @Override
        public Object get(int i)
        {
            return i == 0 ? v1 : i == 1 ? v2 : i == 2 ? v3 : null;
        }
    }

    @Data(staticConstructor = "of")
    @EqualsAndHashCode(callSuper=false)
    public static class Tuple4<T1, T2, T3, T4> extends AbstractList<Object> implements Tuple
    {
        public final T1 v1;
        public final T2 v2;
        public final T3 v3;
        public final T4 v4;
        
        @Override
        public int size()
        {
            return 4;
        }
        
        @Override
        public Object get(int i)
        {
            return i == 0 ? v1 : i == 1 ? v2 : i == 2 ? v3 : i == 3 ? v4 : null;
        }
    }

    @Data(staticConstructor = "of")
    @EqualsAndHashCode(callSuper=false)
    public static class Tuple5<T1, T2, T3, T4, T5> extends AbstractList<Object> implements Tuple
    {
        public final T1 v1;
        public final T2 v2;
        public final T3 v3;
        public final T4 v4;
        public final T5 v5;
        
        @Override
        public int size()
        {
            return 5;
        }
        
        @Override
        public Object get(int i)
        {
            return i == 0 ? v1 : i == 1 ? v2 : i == 2 ? v3 : i == 3 ? v4 : i == 4 ? v5 : null;
        }
    }
    
    @ToString
    @EqualsAndHashCode(callSuper=false)
    public static class TupleX extends AbstractList<Object> implements Tuple
    {
        public final List<Object> vs;
        
        public TupleX(Object... vs)
        {
            this.vs = asList(vs);
        }
        
        public static TupleX of(Object... vs)
        {
            return new TupleX(vs);
        }
        
        @Override
        public int size()
        {
            return vs.size();
        }
        
        @Override
        public Object get(int i)
        {
            return vs.get(i);
        }
    }
    
    public static abstract class Predicate2<T1, T2> implements Predicate<Tuple2<T1, T2>>
    {
        @Override
        public boolean apply(Tuple2<T1, T2> input) { return apply(input.v1, input.v2); }
        public abstract boolean apply(T1 v1, T2 v2);
    }
    
    public static abstract class Predicate3<T1, T2, T3> implements Predicate<Tuple3<T1, T2, T3>>
    {
        @Override
        public boolean apply(Tuple3<T1, T2, T3> input) { return apply(input.v1, input.v2, input.v3); }
        public abstract boolean apply(T1 v1, T2 v2, T3 v3);
    }
    
    public static abstract class Predicate4<T1, T2, T3, T4> implements Predicate<Tuple4<T1, T2, T3, T4>>
    {
        @Override
        public boolean apply(Tuple4<T1, T2, T3, T4> input) { return apply(input.v1, input.v2, input.v3, input.v4); }
        public abstract boolean apply(T1 v1, T2 v2, T3 v3, T4 v4);
    }
    
    public static abstract class Predicate5<T1, T2, T3, T4, T5> implements Predicate<Tuple5<T1, T2, T3, T4, T5>>
    {
        @Override
        public boolean apply(Tuple5<T1, T2, T3, T4, T5> input) { return apply(input.v1, input.v2, input.v3, input.v4, input.v5); }
        public abstract boolean apply(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5);
    }
    
    public static abstract class Function2<F1, F2, T> implements Function<Tuple2<F1, F2>, T>
    {
        @Override
        public T apply(Tuple2<F1, F2> input) { return apply(input.v1, input.v2); }
        public abstract T apply(F1 v1, F2 v2);
    }
    
    public static abstract class Function3<F1, F2, F3, T> implements Function<Tuple3<F1, F2, F3>, T>
    {
        @Override
        public T apply(Tuple3<F1, F2, F3> input) { return apply(input.v1, input.v2, input.v3); }
        public abstract T apply(F1 v1, F2 v2, F3 v3);
    }
    
    public static abstract class Function4<F1, F2, F3, F4, T> implements Function<Tuple4<F1, F2, F3, F4>, T>
    {
        @Override
        public T apply(Tuple4<F1, F2, F3, F4> input) { return apply(input.v1, input.v2, input.v3, input.v4); }
        public abstract T apply(F1 v1, F2 v2, F3 v3, F4 v4);
    }
    
    public static abstract class Function5<F1, F2, F3, F4, F5, T> implements Function<Tuple5<F1, F2, F3, F4, F5>, T>
    {
        @Override
        public T apply(Tuple5<F1, F2, F3, F4, F5> input) { return apply(input.v1, input.v2, input.v3, input.v4, input.v5); }
        public abstract T apply(F1 v1, F2 v2, F3 v3, F4 v4, F5 v5);
    }
}