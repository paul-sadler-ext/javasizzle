package demo;

import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.EMPTY_SET;

import org.junit.Test;


public class AnimateMatrix
{
    @Test public void animate()
    {
        @SuppressWarnings("unchecked")
        Matrix m = new Matrix(EMPTY_SET, EMPTY_MAP);
        m.checkInvariant();
    }
}
