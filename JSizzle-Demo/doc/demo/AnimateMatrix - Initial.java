package demo;

import java.util.Collections;

import org.junit.Test;

public class AnimateMatrix
{
    @SuppressWarnings("unchecked") @Test public void createMatrix()
    {
        Matrix matrix = new Matrix(Collections.EMPTY_SET,
                                   Collections.EMPTY_MAP);
        matrix.checkInvariant();
    }
}
