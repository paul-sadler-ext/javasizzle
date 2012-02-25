package demo;

import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import java.util.Set;

import org.junit.Test;

import demo.Matrix.Category;

public class AnimateMatrix
{
    @SuppressWarnings("unchecked") @Test public void createMatrix()
    {
        Set<Category> categories =
            singleton(new Matrix.Category(new Matrix.Name("Fred"),
                                          singletonList(new Matrix.Name("Fred1"))));
        Matrix matrix = new Matrix(categories, EMPTY_MAP);
        matrix.checkInvariant();
    }
}
