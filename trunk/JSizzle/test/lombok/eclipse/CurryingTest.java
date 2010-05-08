package lombok.eclipse;

import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;

import org.jsizzle.AsFunction;
import org.junit.Test;

public class CurryingTest
{
    @AsFunction
    private String concat(String prefix, int suffix)
    {
        return prefix + suffix;
    }
    
    @Test
    public void testTransformListWithPartialApplication()
    {
        assertEquals(asList("Fred1", "Fred2"), transform(asList(1, 2), concat.apply("Fred")));
    }
}
