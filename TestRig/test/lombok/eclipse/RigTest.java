package lombok.eclipse;

import static junit.framework.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jdt.core.compiler.batch.BatchCompiler;
import org.junit.Test;


public class RigTest
{
    @Test
    public void testCompile()
    {
        final StringWriter errWriter = new StringWriter();
        BatchCompiler.compile(System.getProperty("testrig.path") + " -d none -1.6 -Xemacs",
                              new PrintWriter(System.out),
                              new PrintWriter(errWriter),
                              null);
        assertTrue(errWriter.toString(), errWriter.getBuffer().length() == 0);
    }
}
