package lombok.eclipse;

import static junit.framework.Assert.assertEquals;
import lombok.core.PrintAST;

import org.jsizzle.AsFunction;
import org.junit.Test;

import com.google.common.base.Function;

@PrintAST
public class AsFunctionTest
{
    @AsFunction
    private int noArgInstance()
    {
        return 1;
    }
    
    @AsFunction
    private int oneArgInstance(int n)
    {
        return 1;
    }
    
    @AsFunction
    private int oneArgStatic(int x)
    {
        return 1;
    }

    private static class OneArgConstructor
    {
        public int x;
        
        @AsFunction
        private OneArgConstructor(int x)
        {
            this.x = x;
        }
    }
    
    @Test
    public void testNoArgInstance()
    {
        assertEquals(1, noArgInstance.apply(this).intValue());
    }
    
    @Test
    public void testOneArgInstance()
    {
        assertEquals(1, oneArgInstance.apply(1).intValue());
    }
    
    @Test
    public void testOneArgStatic()
    {
        assertEquals(1, oneArgStatic.apply(1).intValue());
    }
    
    @Test
    public void testOneArgConstructor()
    {
        assertEquals(1, OneArgConstructor.oneArgConstructor.apply(1).x);
    }
    
    @Test
    public void testThreeArgStatic()
    {
        assertEquals(6, add.apply(1).apply(2).apply(3).intValue());
    }

    static Function<Integer, Function<Integer, Function<Integer, Integer>>> add = new add();

    static final class add implements Function<Integer, Function<Integer, Function<Integer, Integer>>>
    {
        @Override
        public Function<Integer, Function<Integer, Integer>> apply(final Integer x)
        {
            return new $addY(x);
        }
    }

    static final class $addY implements Function<Integer, Function<Integer, Integer>>
    {
        private final Integer x;

        private $addY(Integer x)
        {
            this.x = x;
        }

        @Override
        public Function<Integer, Integer> apply(final Integer y)
        {
            return new $addZ(x, y);
        }
    }

    static final class $addZ implements Function<Integer, Integer>
    {
        private final Integer x;
        private final Integer y;

        private $addZ(Integer x, Integer y)
        {
            this.x = x;
            this.y = y;
        }

        @Override
        public Integer apply(final Integer z)
        {
            return add(x, y, z);
        }
    }

    static int add(int x, int y, int z)
    {
        return x + y + z;
    }
}
