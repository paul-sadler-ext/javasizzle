package lombok.eclipse.handlers;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;

import lombok.AccessLevel;
import lombok.eclipse.EclipseNode;

public class LombokUtil
{
    public static void generateEqualsAndHashCode(EclipseNode typeNode, EclipseNode errorNode)
    {
        new HandleEqualsAndHashCode().generateEqualsAndHashCodeForType(typeNode, errorNode);
    }
    
    public static void generateToStringForType(EclipseNode typeNode, EclipseNode errorNode)
    {
        new HandleToString().generateToStringForType(typeNode, errorNode);
    }
    
    public static void generateGetterForField(EclipseNode fieldNode, ASTNode pos, AccessLevel level, boolean lazy)
    {
        new HandleGetter().generateGetterForField(fieldNode, pos, level, lazy);
    }
}
