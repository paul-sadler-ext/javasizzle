package lombok.eclipse.ast;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.toArray;
import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

public class AstUtilities
{
    public static char[][] fullyQualifiedName(TypeDeclaration type,
                                              CompilationUnitDeclaration compilationUnit)
    {
        final LinkedList<char[]> qName = new LinkedList<char[]>();
        for (TypeDeclaration up = type; up != null; up = up.enclosingType)
            qName.addFirst(up.name);
        return toArray(concat(asList(compilationUnit.currentPackage.getImportName()), qName), char[].class);
    }

    public static TypeDeclaration findLocalType(CompilationUnitDeclaration compilationUnit, TypeDeclaration scope, char[][] typeName)
    {
        final TypeDeclaration found =
            findTypeDown(compilationUnit, scope == null ? compilationUnit.types : scope.memberTypes, asList(typeName));
        return found != null ? found : (scope == null ? null : findLocalType(compilationUnit, scope.enclosingType, typeName));
    }

    private static TypeDeclaration findTypeDown(CompilationUnitDeclaration compilationUnit, TypeDeclaration[] types, List<char[]> typeName)
    {
        if (!typeName.isEmpty() && types != null)
        {
            for (TypeDeclaration type : types)
            {
                if (Arrays.equals(type.name, typeName.get(0)))
                {
                    return typeName.size() == 1 ? type : findTypeDown(compilationUnit,
                                                                      type.memberTypes,
                                                                      typeName.subList(1, typeName.size()));
                }
            }
        }
        return null;
    }
}
