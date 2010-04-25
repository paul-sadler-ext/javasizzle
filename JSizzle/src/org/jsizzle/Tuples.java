package org.jsizzle;

import com.google.common.base.Predicate;

import lombok.Data;

public class Tuples
{
    @Data(staticConstructor="of") public static class Tuple2<T1, T2> { T1 v1; T2 v2; }
    @Data(staticConstructor="of") public static class Tuple3<T1, T2, T3> { T1 v1; T2 v2; T3 v3; }
    @Data(staticConstructor="of") public static class Tuple4<T1, T2, T3, T4> { T1 v1; T2 v2; T3 v3; T4 v4; }
    @Data(staticConstructor="of") public static class Tuple5<T1, T2, T3, T4, T5> { T1 v1; T2 v2; T3 v3; T4 v4; T5 v5; }
    
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
}