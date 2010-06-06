package lombok.eclipse.ast;

import static com.google.common.base.Functions.compose;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static lombok.eclipse.Eclipse.toQualifiedName;
import static lombok.eclipse.ast.AstUtilities.findLocalType;
import static lombok.eclipse.ast.AstUtilities.fullyQualifiedName;
import static org.eclipse.jdt.internal.compiler.ast.ASTNode.IsMemberType;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccAnnotation;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccEnum;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccFinal;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccInterface;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccPrivate;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccProtected;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccPublic;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccStatic;
import static org.jcurry.ValueObjects.toSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.jcurry.AsFunction;
import org.jsizzle.Include;
import org.jsizzle.Invariant;
import org.jsizzle.JavaSpec.Constructor;
import org.jsizzle.JavaSpec.JavaLangTypeName;
import org.jsizzle.JavaSpec.MetaType;
import org.jsizzle.JavaSpec.Method;
import org.jsizzle.JavaSpec.Modifier;
import org.jsizzle.JavaSpec.Name;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.JavaSpec.TypeName;
import org.jsizzle.JavaSpec.TypeScope;
import org.jsizzle.JavaSpec.Variable;
import org.jsizzle.JavaSpec.Visibility;
import org.jsizzle.SchemaSpec.JSizzleTypeName;

import com.google.common.base.Function;
import com.sun.xml.internal.org.jvnet.staxex.NamespaceContextEx.Binding;

public abstract class JavaSpecMapping
{
    private static final Map<String, TypeName> fixedTypeNames = newHashMap();
    static
    {
        fixedTypeNames.put(Binding.class.getName(), JSizzleTypeName.BINDING);
        fixedTypeNames.put(Include.class.getName(), JSizzleTypeName.INCLUDE);
        fixedTypeNames.put(Invariant.class.getName(), JSizzleTypeName.INVARIANT);
        fixedTypeNames.put(Object.class.getName(), JavaLangTypeName.OBJECT);
        fixedTypeNames.put(Enum.class.getName(), JavaLangTypeName.ENUM);
        fixedTypeNames.put(null, JavaLangTypeName.NONE);
    }
    private final Map<String, TypeName> typeNames = newHashMap(fixedTypeNames);
    private final CompilationUnitDeclaration compilationUnit;
    
    public JavaSpecMapping(CompilationUnitDeclaration compilationUnit)
    {
        this.compilationUnit = compilationUnit;
    }

    @AsFunction
    public Type specType(TypeDeclaration type)
    {
        return new Type(specVisibility(type.modifiers),
                        specOtherModifiers(type.modifiers),
                        specSet(type.annotations, specAnnotation.apply(type)),
                        specTypeName(fullyQualifiedName(type, compilationUnit)),
                        specMetaType(type.modifiers),
                        specTypeScope(type.bits),
                        specSet(type.fields, specVariable.apply(type)),
                        specSet(type.methods, specConstructor.apply(type), ConstructorDeclaration.class),
                        specSet(type.methods, specMethod.apply(type), MethodDeclaration.class),
                        specSet(type.memberTypes, specType),
                        specTypeName(type.enclosingType, type.superclass.getTypeName()),
                        specSet(type.superInterfaces, specReference.apply(type.enclosingType)));
    }
    
    @AsFunction
    public Method specMethod(TypeDeclaration scope, MethodDeclaration method)
    {
        return new Method(specVisibility(method.modifiers),
                          specOtherModifiers(method.modifiers),
                          specSet(method.annotations, specAnnotation.apply(scope)),
                          specList(method.arguments, specVariable.apply(scope)),
                          new Name(new String(method.selector)),
                          specList(method.arguments, compose(Variable.getTypeName, specVariable.apply(scope))),
                          specTypeName(scope, method.returnType.getTypeName()));
    }
    
    @AsFunction
    public Constructor specConstructor(TypeDeclaration scope, ConstructorDeclaration constructor)
    {
        return new Constructor(specVisibility(constructor.modifiers),
                               specOtherModifiers(constructor.modifiers),
                               specSet(constructor.annotations, specAnnotation.apply(scope)),
                               specList(constructor.arguments, specVariable.apply(scope)));
    }
    
    @AsFunction
    public Variable specVariable(TypeDeclaration scope, AbstractVariableDeclaration variable)
    {
        return new Variable(specVisibility(variable.modifiers),
                            specOtherModifiers(variable.modifiers),
                            specSet(variable.annotations, specAnnotation.apply(scope)),
                            new Name(new String(variable.name)),
                            specTypeName(scope, variable.type.getTypeName()));
    }

    public static TypeScope specTypeScope(int bits)
    {
        return (bits & IsMemberType) != 0 ? TypeScope.MEMBER : TypeScope.TOP;
    }
    
    public <A, S> Set<S> specSet(A[] astns, Function<A, S> specA)
    {
        final Set<S> noSpecs = emptySet();
        return astns == null ? noSpecs : toSet(transform(asList(astns), specA));
    }
    
    public <A, D extends A, S> Set<S> specSet(A[] astns, Function<D, S> specA, Class<D> typeD)
    {
        final Set<S> noSpecs = emptySet();
        return astns == null ? noSpecs : toSet(transform(filter(asList(astns), typeD), specA));
    }
    
    public <A, S> List<S> specList(A[] astns, Function<A, S> specA)
    {
        final List<S> noSpecs = emptyList();
        return astns == null ? noSpecs : transform(asList(astns), specA);
    }
    
    @AsFunction
    public TypeName specAnnotation(TypeDeclaration scope, Annotation annotation)
    {
        return specReference(scope, annotation.type);
    }
    
    @AsFunction
    public TypeName specReference(TypeDeclaration scope, TypeReference implemented)
    {
        return specTypeName(scope, implemented.getTypeName());
    }
    
    public TypeName specTypeName(TypeDeclaration scope, final char[][] localName)
    {
        final TypeDeclaration localType = findLocalType(compilationUnit, scope, localName);
        return specTypeName(localType == null ? localName : fullyQualifiedName(localType, compilationUnit));
    }
    
    public TypeName specTypeName(final char[][] qualifiedName)
    {
        TypeName typeName = typeNames.get(toQualifiedName(qualifiedName));
        if (typeName == null)
            typeNames.put(toQualifiedName(qualifiedName), typeName = new TypeName() {});
        return typeName;
    }

    private static Set<Modifier> specOtherModifiers(final int modifiers)
    {
        final Set<Modifier> otherModifiers = newHashSet();
        if ((modifiers & AccFinal) != 0)
            otherModifiers.add(Modifier.FINAL);
        if ((modifiers & AccStatic) != 0)
            otherModifiers.add(Modifier.STATIC);
        return otherModifiers;
    }

    private static MetaType specMetaType(final int modifiers)
    {
        if ((modifiers & AccAnnotation) != 0)
            return MetaType.ANNOTATION;
        else if ((modifiers & AccEnum) != 0)
            return MetaType.ENUMERATION;
        else if ((modifiers & AccInterface) != 0)
            return MetaType.INTERFACE;
        else
            return MetaType.CLASS;
    }

    private static Visibility specVisibility(final int modifiers)
    {
        if ((modifiers & AccPublic) != 0)
            return Visibility.PUBLIC;
        else if ((modifiers & AccProtected) != 0)
            return Visibility.PROTECTED;
        else if ((modifiers & AccPrivate) != 0)
            return Visibility.PRIVATE;
        else
            return Visibility.PUBLIC;
    }
}
