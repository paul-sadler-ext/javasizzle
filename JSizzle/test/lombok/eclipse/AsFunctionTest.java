package lombok.eclipse;

import static junit.framework.Assert.assertEquals;
import lombok.core.PrintAST;

import org.jsizzle.AsFunction;
import org.junit.Test;

@PrintAST
public class AsFunctionTest
{
    @AsFunction
    int field = 1;
    
    @AsFunction
    private int noArgInstance()
    {
        return 1;
    }
    
    @AsFunction
    private int oneArgInstance(int n)
    {
        return n;
    }
    
    @AsFunction
    private int twoArgInstance(int x, int y)
    {
        return x + y;
    }
    
    @AsFunction
    private int threeArgInstance(int x, int y, int z)
    {
        return x + y + z;
    }
    
    @AsFunction
    private static int oneArgStatic(int x)
    {
        return x;
    }
    
    @AsFunction
    private static int twoArgStatic(int x, int y)
    {
        return x + y;
    }
    
    @AsFunction
    private static int threeArgStatic(int x, int y, int z)
    {
        return x + y + z;
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

    private static class ThreeArgConstructor
    {
        public int n;
        
        @AsFunction
        private ThreeArgConstructor(int x, int y, int z)
        {
            this.n = x + y + z;
        }
    }
    
    @Test
    public void testField()
    {
        assertEquals(1, getField.apply(this).intValue());
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
    public void testTwoArgInstance()
    {
        assertEquals(3, twoArgInstance.apply(1).apply(2).intValue());
    }
    
    @Test
    public void testThreeArgInstance()
    {
        assertEquals(6, threeArgInstance.apply(1).apply(2).apply(3).intValue());
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
    public void testThreeArgConstructor()
    {
        assertEquals(6, ThreeArgConstructor.threeArgConstructor.apply(1).apply(2).apply(3).n);
    }
    
    @Test
    public void testTwoArgStatic()
    {
        assertEquals(3, twoArgStatic.apply(1).apply(2).intValue());
    }
    
    @Test
    public void testThreeArgStatic()
    {
        assertEquals(6, threeArgStatic.apply(1).apply(2).apply(3).intValue());
    }
}
