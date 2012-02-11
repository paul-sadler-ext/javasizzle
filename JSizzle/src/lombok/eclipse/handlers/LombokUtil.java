package lombok.eclipse.handlers;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;

import lombok.AccessLevel;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.EclipseHandlerUtil.FieldAccess;

public class LombokUtil
{
    public static void generateEqualsAndHashCode(EclipseNode typeNode, EclipseNode errorNode)
    {
        // Have to force the generation not to call super(), so can't use generateEqualsAndHashCodeForType
        new HandleEqualsAndHashCode().generateMethods(typeNode, errorNode, null, null, false, false, FieldAccess.GETTER);
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
