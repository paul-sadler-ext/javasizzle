package lombok.eclipse;

import static java.util.Arrays.asList;
import static lombok.eclipse.Eclipse.copyAnnotations;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.toQualifiedName;
import static lombok.eclipse.HandleAsFunction.generateFunction;
import static lombok.eclipse.Source.source;
import static lombok.eclipse.handlers.EclipseHandlerUtil.fieldExists;
import static lombok.eclipse.handlers.EclipseHandlerUtil.generateNullCheck;
import static lombok.eclipse.handlers.EclipseHandlerUtil.getExistingLombokConstructor;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectField;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectMethod;
import static lombok.eclipse.handlers.EclipseHandlerUtil.toEclipseModifier;
import static org.eclipse.jdt.internal.compiler.ast.ASTNode.IsMemberType;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccAnnotation;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccEnum;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccFinal;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccInterface;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccPrivate;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccPublic;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.AccessLevel;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.eclipse.handlers.HandleEqualsAndHashCode;
import lombok.eclipse.handlers.HandleGetter;
import lombok.eclipse.handlers.HandleToString;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.jsizzle.Include;
import org.jsizzle.Invariant;
import org.jsizzle.Schema;

public class HandleSchema implements EclipseAnnotationHandler<Schema>
{
    private static final char[][] ORG_JSIZZLE_BINDING = fromQualifiedName("org.jsizzle.Binding");
    private static final char[][] INCLUSION_DIRECT = fromQualifiedName("org.jsizzle.Binding.Inclusion.DIRECT");
    private static final char[][] INCLUSION_INCLUDED = fromQualifiedName("org.jsizzle.Binding.Inclusion.INCLUDED");
    private static final char[][] INCLUSION_EXPANDED = fromQualifiedName("org.jsizzle.Binding.Inclusion.EXPANDED");
    private static final char[] IDENTITY_NAME = "identity".toCharArray();
    
    @SuppressWarnings("unused")
    private static final boolean instrument = Boolean.valueOf(System.getProperty("org.jsizzle.instrument"));

    @Override
    public boolean handle(AnnotationValues<Schema> annotation,
                          Annotation source,
                          EclipseNode annotationNode)
    {
        return makeSchemaClass(annotationNode.up(), annotationNode, source(source));
    }
    
    private boolean makeSchemaClass(final EclipseNode typeNode,
                                    final EclipseNode errorNode,
                                    final Source source)
    {
        final TypeDeclaration type = (typeNode.get() instanceof TypeDeclaration) ? (TypeDeclaration)typeNode.get() : null;
        
        // Entirely reject null types and annotations
        if (type == null || (type.modifiers & AccAnnotation) != 0)
        {
            errorNode.addError("This cannot be a Schema");
            return true;
        }
        
        // Make class public
        type.modifiers |= AccPublic;
        
        // For enumerations and interfaces, we don't do anything more
        if ((type.modifiers & (AccInterface | AccEnum)) == 0)
        {
            // Make class static final
            type.modifiers |= AccFinal;
            if ((type.bits & IsMemberType) != 0)
                type.modifiers |= AccStatic;
            
            // Extend org.jsizzle.Binding<ThisType>
            if (type.superclass == null)
            {
                final TypeReference[][] typeArguments = new TypeReference[ORG_JSIZZLE_BINDING.length][];
                typeArguments[ORG_JSIZZLE_BINDING.length - 1] =
                    new TypeReference[] {source.generated(new SingleTypeReference(type.name, source.p))};
                
                type.superclass = source.generated(new ParameterizedQualifiedTypeReference(
                    ORG_JSIZZLE_BINDING, typeArguments, 0, source.p(3)));
            }
            else
            {
                typeNode.addError("Schema classes may not use inheritance. Include instead.");
                return true;
            }
            
            final ConstructorBuilder consBuilder = new ConstructorBuilder(typeNode, source);
            for (EclipseNode child : typeNode.down())
            {
                if (!child.isHandled())
                {
                    if (child.getKind() == Kind.FIELD)
                    {
                        final FieldDeclaration field = (FieldDeclaration)child.get();
                        
                        // Make fields public and final
                        field.modifiers |= (AccFinal | AccPublic);
                        
                        // Otherwise, leave statics alone (uninitialised statics will error due to final)
                        if ((field.modifiers & AccStatic) == 0)
                        {
                            // Generate the field access function
                            // NOTE: Do not use the child as source, due to use of retrieveEndOfElementTypeNamePosition
                            // in org.eclipse.jdt.core.dom.ASTConverter.convertType
                            final char[] fieldAccessorName = generateFunction(child, AccessLevel.PUBLIC, child, source);
                            new HandleGetter().generateGetterForField(child, source.node);
                            
                            // If the field is initialised, leave it alone
                            if (field.initialization == null)
                            {
                                final EclipseNode includeAnnNode = findAnnotation(child, Include.class);
                                if (includeAnnNode != null)
                                {
                                    final EclipseNode includedTypeNode = findType(typeNode, asList(field.type.getTypeName()));
                                    if (includedTypeNode == null)
                                    {
                                        includeAnnNode.addError("Local type " + toQualifiedName(field.type.getTypeName()) + " not found (may not be local).");
                                        break;
                                    }
                                    final EclipseNode includeConsNode = getExistingLombokConstructor(includedTypeNode);
                                    if (includeConsNode == null)
                                    {
                                        includeAnnNode.addError("Cannot Include " + toQualifiedName(field.type.getTypeName()) + " (may not be a Schema).");
                                        break;
                                    }
                                    final ConstructorDeclaration includeCons = (ConstructorDeclaration)includeConsNode.get();
                                    if (includeCons.arguments != null)
                                    {
                                        for (Argument arg : includeCons.arguments)
                                        {
                                            if (fieldExists(new String(arg.name), typeNode) == MemberExistsResult.NOT_EXISTS)
                                            {
                                                // NOTE: Must set source positions on copied type, because for some reason Eclipse
                                                // doesn't like argument types with source positions from a different scope.
                                                final FieldDeclaration includedField = injectSchemaField(typeNode,
                                                                  arg.name,
                                                                  source.copyType(arg.type, true),
                                                                  AccPublic,
                                                                  source);
                                                final EclipseNode fieldNode = typeNode.getNodeFor(includedField);
                                                final char[] expandedAccessorName = generateFunction(fieldNode, AccessLevel.PUBLIC, child, source);
                                                new HandleGetter().generateGetterForField(fieldNode, source.node);
                                                consBuilder.addAssignedField(includedField, expandedAccessorName, INCLUSION_EXPANDED);
                                            }
                                        }
                                    }
                                    consBuilder.addConstructedField(field, fieldAccessorName, INCLUSION_INCLUDED, includeCons);
                                }
                                else
                                {
                                    consBuilder.addAssignedField(field, fieldAccessorName, INCLUSION_DIRECT);
                                }
                            }
                        }
                    }
                    else if (child.getKind() == Kind.METHOD)
                    {
                        final AbstractMethodDeclaration method = (AbstractMethodDeclaration)child.get();
                        if (method instanceof ConstructorDeclaration)
                        {
                            if ((method.bits & ASTNode.IsDefaultConstructor) == 0)
                                typeNode.getNodeFor(method).addWarning("Schema classes should not have constructors.");
                        }
                        else if (method instanceof MethodDeclaration)
                        {
                            final EclipseNode invariantAnnNode = findAnnotation(child, Invariant.class);
                            if (invariantAnnNode != null)
                            {
                                if (Arrays.equals(((MethodDeclaration)method).returnType.getLastToken(), TypeConstants.BOOLEAN)
                                    && (method.arguments == null || method.arguments.length == 0))
                                {
                                    // Make invariant method private final
                                    method.modifiers |= (AccPrivate | AccFinal);
                                    consBuilder.addInvariant(method);
                                }
                                else
                                {
                                    typeNode.getNodeFor(method).addError("Invariant method must have no arguments and return a boolean.");
                                }
                            }
                            else
                            {
                                // Make utility method public final
                                method.modifiers |= (AccPublic | AccFinal);
                                // Generate the method access function
                                generateFunction(child, AccessLevel.PUBLIC, child, source);
                            }
                        }
                    }
                }
            }
            
            // If no fields, create an Object field for identity
            if (!consBuilder.hasArgs())
                consBuilder.addAssignedField(injectIdentityField(typeNode, source), null, null);
            
            // Inject the constructor and construction function
            final ConstructorDeclaration constructor = consBuilder.build();
            injectMethod(typeNode, constructor);
            generateFunction(typeNode.getNodeFor(constructor), AccessLevel.PUBLIC, errorNode, source);
            
            // Create toString, equals and hashCode
            new HandleToString().generateToStringForType(typeNode, errorNode);
            new HandleEqualsAndHashCode().generateMethods(typeNode, errorNode, null, null, false, true);
        }
        
        for (EclipseNode subTypeNode : typeNode.down())
        {
            if (subTypeNode.getKind() == Kind.TYPE && !subTypeNode.isHandled())
                makeSchemaClass(subTypeNode, subTypeNode, source(subTypeNode.get()));
        }

        return true;
    }

    private static EclipseNode findType(EclipseNode scope, List<char[]> typeName)
    {
        final EclipseNode found = findTypeInScope(scope, typeName);
        return found != null ? found : (scope.up() == null ? null : findType(scope.up(), typeName));
    }

    private static EclipseNode findTypeInScope(EclipseNode scope, List<char[]> typeName)
    {
        if (!typeName.isEmpty())
        {
            for (EclipseNode child : scope.down())
            {
                if (child.getKind() == Kind.TYPE)
                {
                    if (child.getName().equals(new String(typeName.get(0))))
                        return typeName.size() == 1 ? child : findType(child, typeName.subList(1, typeName.size()));
                }
            }
        }
        return null;
    }

    private static EclipseNode findAnnotation(final EclipseNode node, final Class<? extends java.lang.annotation.Annotation> annClass)
    {
        for (EclipseNode annNode : node.down())
        {
            if (annNode.getKind() == Kind.ANNOTATION)
            {
                if (Eclipse.annotationTypeMatches(annClass, annNode))
                    return annNode;
            }
        }
        return null;
    }

    private static FieldDeclaration injectIdentityField(final EclipseNode typeNode,
                                                        final Source source)
    {
        final TypeReference type = new QualifiedTypeReference(TypeConstants.JAVA_LANG_OBJECT, source.p(3));
        return injectSchemaField(typeNode, IDENTITY_NAME, type, AccPrivate, source);
    }

    private static FieldDeclaration injectSchemaField(final EclipseNode schemaNode,
                                                      final char[] name,
                                                      final TypeReference type,
                                                      final int visibility,
                                                      final Source source)
    {
        final FieldDeclaration field = source
                .generated(new FieldDeclaration(name, source.pS, source.pE));
        field.declarationSourceStart = field.sourceStart;
        field.declarationEnd = field.declarationSourceEnd = field.sourceEnd;
        field.modifiers = AccFinal | visibility;
        field.type = type;
        injectField(schemaNode, field);
        return field;
    }
    
    private class ConstructorBuilder
    {
        final EclipseNode type;
        final Source source;
        final List<Argument> args = new ArrayList<Argument>();
        final List<Statement> stmts = new ArrayList<Statement>();
        
        public ConstructorBuilder(EclipseNode type, Source source)
        {
            this.type = type;
            this.source = source;
        }

        public ConstructorDeclaration build()
        {
            final ConstructorDeclaration constructor = source.generated(new ConstructorDeclaration(
                ((CompilationUnitDeclaration)type.top().get()).compilationResult));
    
            constructor.modifiers = toEclipseModifier(AccessLevel.PUBLIC);
            constructor.annotations = null;
            constructor.selector = ((TypeDeclaration)type.get()).name;
            constructor.constructorCall = source.generated(new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper));
            constructor.thrownExceptions = null;
            constructor.typeParameters = null;
            constructor.bits |= Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
            constructor.bodyStart = constructor.declarationSourceStart = constructor.sourceStart;
            constructor.bodyEnd = constructor.declarationSourceEnd = constructor.sourceEnd;
            constructor.statements = stmts.isEmpty() ? null : stmts.toArray(new Statement[stmts.size()]);
            constructor.arguments = args.isEmpty() ? null : args.toArray(new Argument[args.size()]);
            return constructor;
        }

        public void addAssignedField(final FieldDeclaration field, final char[] accessorName, final char[][] inclusion)
        {
            stmts.add(createFieldAssignment(field, createArgReference(field.name)));
            args.add(createArgument(field));
            final Statement nullCheck = generateNullCheck(field, source.node);
            if (nullCheck != null)
                stmts.add(nullCheck);
            if (accessorName != null)
                stmts.add(createAddAccessor(accessorName, inclusion));
        }

        public void addConstructedField(final FieldDeclaration field,
                                        final char[] accessorName,
                                        final char[][] inclusion,
                                        final ConstructorDeclaration includeCons)
        {
            final AllocationExpression constructed = source.generated(new AllocationExpression());
            constructed.type = source.copyType(field.type, false);
            if (includeCons.arguments == null)
            {
                constructed.arguments = null;
            }
            else
            {
                constructed.arguments = new Expression[includeCons.arguments.length];
                for (int i = 0; i < includeCons.arguments.length; i++)
                    constructed.arguments[i] = createArgReference(includeCons.arguments[i].name);
            }
            stmts.add(createFieldAssignment(field, constructed));

            stmts.add(createAddAccessor(accessorName, inclusion));
        }

        public void addInvariant(final AbstractMethodDeclaration method)
        {
            final Expression callInvariant = createThisCall(new String(method.selector));
            final UnaryExpression notInvariant = source.generated(new UnaryExpression(callInvariant, OperatorIds.NOT));
            stmts.add(source.generated(new IfStatement(notInvariant, createAddViolation(method), source.pS, source.pE)));
        }
        
        public boolean hasArgs()
        {
            return !args.isEmpty();
        }

        private Statement createAddViolation(final AbstractMethodDeclaration method)
        {
            return createThisCall("addViolation", new StringLiteral(method.selector, source.pS, source.pE, 0));
        }

        private Statement createAddAccessor(char[] accessorName, char[][] inclusion)
        {
            final Expression accessorReference = source.generated(new SingleNameReference(accessorName, source.p));
            final Expression inclusionReference = source.generated(new QualifiedNameReference(inclusion,
                                                                                              source.p(inclusion.length),
                                                                                              source.pS,
                                                                                              source.pE));
            return createThisCall("addAccessor", accessorReference, inclusionReference);
        }

        private Expression createThisCall(String methodName, Expression... arguments)
        {
            final MessageSend methodCall = source.generated(new MessageSend());
            methodCall.receiver = source.generated(new ThisReference(source.pS, source.pE));
            methodCall.selector = methodName.toCharArray();
            methodCall.arguments = arguments.length == 0 ? null : arguments;
            return methodCall;
        }

        private Argument createArgument(final FieldDeclaration field)
        {
            final Argument argument = source.generated(new Argument(field.name,
                                                                    source.p,
                                                                    source.copyType(field.type, false),
                                                                    AccFinal));
            argument.annotations = copyAnnotations(field.annotations, source.node);
            return argument;
        }
    
        private Assignment createFieldAssignment(final FieldDeclaration field, final Expression value)
        {
            final FieldReference thisX = createFieldReference(field.name);
            return source.generated(new Assignment(thisX, value, source.pE));
        }

        private FieldReference createFieldReference(final char[] name)
        {
            final FieldReference thisX = source.generated(new FieldReference(
                ("this." + new String(name)).toCharArray(), source.p));
            thisX.receiver = source.generated(new ThisReference(source.pS, source.pE));
            thisX.token = name;
            return thisX;
        }

        private SingleNameReference createArgReference(final char[] name)
        {
            return source.generated(new SingleNameReference(name, source.p));
        }
    }
}
