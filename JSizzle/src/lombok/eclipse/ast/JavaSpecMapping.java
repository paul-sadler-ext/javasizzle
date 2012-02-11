package lombok.eclipse.ast;

import static com.google.common.base.Functions.compose;
import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.toQualifiedName;
import static lombok.eclipse.ast.AstUtilities.findLocalType;
import static lombok.eclipse.ast.AstUtilities.fullyQualifiedName;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccAnnotation;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccEnum;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccFinal;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccInterface;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccPrivate;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccProtected;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccPublic;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccStatic;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.core.TypeLibrary;
import lombok.core.TypeResolver;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.jcurry.AsFunction;
import org.jsizzle.Binding;
import org.jsizzle.Include;
import org.jsizzle.Initialise;
import org.jsizzle.Invariant;
import org.jsizzle.JavaSpec.Constructor;
import org.jsizzle.JavaSpec.JavaLangTypeName;
import org.jsizzle.JavaSpec.MetaType;
import org.jsizzle.JavaSpec.Method;
import org.jsizzle.JavaSpec.Modifier;
import org.jsizzle.JavaSpec.Name;
import org.jsizzle.JavaSpec.PrimitiveName;
import org.jsizzle.JavaSpec.QualifiedTypeName;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.JavaSpec.TypeName;
import org.jsizzle.JavaSpec.TypeScope;
import org.jsizzle.JavaSpec.Variable;
import org.jsizzle.JavaSpec.Visibility;
import org.jsizzle.Schema;
import org.jsizzle.SchemaField;
import org.jsizzle.SchemaSpec.JSizzleTypeName;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class JavaSpecMapping
{
    private static final TypeLibrary typeLibrary = new TypeLibrary();
    static
    {
        typeLibrary.addType(Schema.class.getName());
        typeLibrary.addType(Binding.class.getName());
        typeLibrary.addType(Include.class.getName());
        typeLibrary.addType(Invariant.class.getName());
        typeLibrary.addType(Initialise.class.getName());
        typeLibrary.addType(SchemaField.class.getName());
        typeLibrary.addType(Object.class.getName());
        typeLibrary.addType(Enum.class.getName());
    }
    
    private static final Map<String, TypeName> fixedTypeNames = newHashMap();
    static
    {
        fixedTypeNames.put(Schema.class.getName(), JSizzleTypeName.SCHEMA);
        fixedTypeNames.put(Binding.class.getName(), JSizzleTypeName.BINDING);
        fixedTypeNames.put(Include.class.getName(), JSizzleTypeName.INCLUDE);
        fixedTypeNames.put(Invariant.class.getName(), JSizzleTypeName.INVARIANT);
        fixedTypeNames.put(Initialise.class.getName(), JSizzleTypeName.INITIALISE);
        fixedTypeNames.put(SchemaField.class.getName(), JSizzleTypeName.SCHEMAFIELD);
        fixedTypeNames.put(Object.class.getName(), JavaLangTypeName.OBJECT);
        fixedTypeNames.put(Enum.class.getName(), JavaLangTypeName.ENUM);
        fixedTypeNames.put(null, JavaLangTypeName.NONE);
        fixedTypeNames.put("byte", PrimitiveName.BYTE);
        fixedTypeNames.put("short", PrimitiveName.SHORT);
        fixedTypeNames.put("int", PrimitiveName.INT);
        fixedTypeNames.put("long", PrimitiveName.LONG);
        fixedTypeNames.put("char", PrimitiveName.CHAR);
        fixedTypeNames.put("boolean", PrimitiveName.BOOLEAN);
        fixedTypeNames.put("double", PrimitiveName.DOUBLE);
        fixedTypeNames.put("float", PrimitiveName.FLOAT);
    }
    private final BiMap<String, TypeName> typeNames = HashBiMap.create(fixedTypeNames);
    private final CompilationUnitDeclaration compilationUnit;
    private final TypeResolver resolver;
	private static final Predicate<ConstructorDeclaration> nonDefaultConstructors = new Predicate<ConstructorDeclaration>()
	{
		@Override
		public boolean apply(ConstructorDeclaration constructor)
		{
			return !constructor.isDefaultConstructor();
		}
	};

    public JavaSpecMapping(EclipseNode start)
    {
        this.compilationUnit = (CompilationUnitDeclaration)start.top().get();
        this.resolver = new TypeResolver(start.getPackageDeclaration(), start.getImportStatements());
    }
    
    @AsFunction
    public Type typeForName(TypeName typeName)
    {
        final String qualifiedName = typeNames.inverse().get(typeName);
        final String packageName = toQualifiedName(compilationUnit.currentPackage.tokens);
        if (qualifiedName != null && qualifiedName.startsWith(packageName))
            return specType(compilationUnit.declarationOfType(fromQualifiedName(qualifiedName.substring(packageName.length() + 1))));
        
        return null;
    }

    @AsFunction
    public Type specType(TypeDeclaration type)
    {
		return new Type(specVisibility(type.modifiers),
                        specOtherModifiers(type.modifiers),
                        specSet(type.annotations, specAnnotation.apply(this).apply(type)),
                        specClassName(fullyQualifiedName(type, compilationUnit)),
                        specMetaType(type.modifiers),
                        specTypeScope(type),
                        specReference(type.enclosingType, type.superclass),
                        specSet(type.superInterfaces, specReference.apply(this).apply(type.enclosingType)),
                        specList(type.fields, specVariable.apply(this).apply(type)),
                        specList(type.memberTypes, specType.apply(this)),
                        specList(type.methods, specConstructor.apply(this).apply(type), ConstructorDeclaration.class, nonDefaultConstructors),
                        specList(type.methods, specMethod.apply(this).apply(type), MethodDeclaration.class));
    }
    
    @AsFunction
    private Method specMethod(TypeDeclaration scope, MethodDeclaration method)
    {
        char[] name = method.selector;
		return new Method(specVisibility(method.modifiers),
                          specOtherModifiers(method.modifiers),
                          specSet(method.annotations, specAnnotation.apply(this).apply(scope)),
                          specList(method.arguments, specVariable.apply(this).apply(scope)),
                          specName(name),
                          specList(method.arguments, compose(Variable.getTypeName, specVariable.apply(this).apply(scope))),
                          specReference(scope, method.returnType));
    }
    
    @AsFunction
    private Constructor specConstructor(TypeDeclaration scope, ConstructorDeclaration constructor)
    {
        return new Constructor(specVisibility(constructor.modifiers),
                               specOtherModifiers(constructor.modifiers),
                               specSet(constructor.annotations, specAnnotation.apply(this).apply(scope)),
                               specList(constructor.arguments, specVariable.apply(this).apply(scope)));
    }
    
    @AsFunction
    private Variable specVariable(TypeDeclaration scope, AbstractVariableDeclaration variable)
    {
        return new Variable(specVisibility(variable.modifiers),
                            specOtherModifiers(variable.modifiers),
                            specSet(variable.annotations, specAnnotation.apply(this).apply(scope)),
                            specName(variable.name),
                            specReference(scope, variable.type));
    }
    
    @AsFunction
    private TypeName specAnnotation(TypeDeclaration scope, Annotation annotation)
    {
        return specReference(scope, annotation.type);
    }
    
    @AsFunction
    private TypeName specReference(TypeDeclaration scope, TypeReference reference)
    {
        return specTypeName(scope, reference == null ? null : reference.getTypeName());
    }
    
    private TypeName specTypeName(TypeDeclaration scope, final char[][] localName)
    {
        if (localName == null)
        {
            return specClassName(null);
        }
        else
        {
            final TypeDeclaration localType = findLocalType(compilationUnit, scope, localName);
            if (localType != null)
            {
                return specClassName(fullyQualifiedName(localType, compilationUnit));
            }
            else
            {
                final Iterator<String> matches = resolver.findTypeMatches(null, typeLibrary, toQualifiedName(localName)).iterator();
                return specClassName(matches.hasNext() ? fromQualifiedName(matches.next()) : localName);
            }
        }
    }
    
    private TypeName specClassName(final char[][] qualifiedName)
    {
        final String key = qualifiedName == null ? null : toQualifiedName(qualifiedName);
        TypeName typeName = typeNames.get(key);
        if (typeName == null)
            typeNames.put(key, typeName = new QualifiedTypeName(key));

        return typeName;
    }

	private static Name specName(char[] identifier)
	{
		return new Name(new String(identifier));
	}
    
    private static <A> List<A> safeList(A[] astns)
    {
    	if (astns == null)
    	{
    		return emptyList();
    	}
    	else
    	{
    		return asList(astns);
    	}
    }
    
    private static <A, S> List<S> specList(A[] astns, Function<A, S> specA)
    {
    	return newArrayList(transform(safeList(astns), specA));
    }
    
    private static <A, B extends A, S> List<S> specList(A[] astns, Function<B, S> specB, Class<B> classB)
    {
        return specList(astns, specB, classB, alwaysTrue());
    }
    
    private static <A, B extends A, S> List<S> specList(A[] astns, Function<B, S> specB, Class<B> classB, Predicate<? super B> filter)
    {
        return newArrayList(transform(filter(filter(safeList(astns), classB), filter), specB));
    }

    private static <A, S> Set<S> specSet(A[] astns, Function<A, S> specA)
    {
        return newHashSet(transform(safeList(astns), specA));
    }
    
    private static TypeScope specTypeScope(TypeDeclaration type)
    {
    	return type.enclosingType == null ? TypeScope.TOP : TypeScope.MEMBER;
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
            return Visibility.DEFAULT;
    }
}
