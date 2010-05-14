/**
 * 
 */
package lombok.eclipse;

import java.util.Arrays;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public class Source
{
    public final ASTNode node;
    public final long p;
    public final int pS, pE;
    
    public static Source source(ASTNode sourceNode)
    {
        return new Source(sourceNode);
    }
    
    public <T extends ASTNode> T generated(T newNode)
    {
        newNode.sourceStart = pS;
        newNode.sourceEnd = pE;
        if (newNode instanceof Expression)
            ((Expression)newNode).statementEnd = pE;
        Eclipse.setGeneratedBy(newNode, node);

        if (newNode instanceof ParameterizedQualifiedTypeReference)
        {
            final ParameterizedQualifiedTypeReference pqtr = (ParameterizedQualifiedTypeReference)newNode;
            if (pqtr.typeArguments != null)
            {
                for (TypeReference[] qar : pqtr.typeArguments)
                {
                    if (qar != null)
                    {
                        for (TypeReference tr : qar)
                            generated(tr);
                    }
                }
            }
        }
        else if (newNode instanceof ParameterizedSingleTypeReference)
        {
            final ParameterizedSingleTypeReference pstr = (ParameterizedSingleTypeReference)newNode;
            if (pstr.typeArguments != null)
            {
                for (TypeReference tr : pstr.typeArguments)
                    generated(tr);
            }
        }
        
        return newNode;
    }
    
    public long[] p(int length)
    {
        long[] ps = new long[length];
        Arrays.fill(ps, p);
        return ps;
    }
    
    public TypeReference copyType(TypeReference type, boolean setSourcePos)
    {
        final TypeReference copied = Eclipse.copyType(type, node);
        return setSourcePos ? generated(copied) : copied;
    }
    
    @Override
    public String toString()
    {
        return "Source [node=" + node + ", p=" + p + ", pE=" + pE + ", pS="
                + pS + "]";
    }

    private Source(ASTNode sourceNode)
    {
        this.node = sourceNode;
        this.pS = sourceNode.sourceStart;
        this.pE = sourceNode.sourceEnd;
        this.p = (long)pS << 32 | pE;
    }
}