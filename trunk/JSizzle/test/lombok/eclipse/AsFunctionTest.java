package lombok.eclipse;

import static junit.framework.Assert.assertEquals;
import lombok.core.PrintAST;

import org.jsizzle.AsFunction;
import org.junit.Test;

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
}
