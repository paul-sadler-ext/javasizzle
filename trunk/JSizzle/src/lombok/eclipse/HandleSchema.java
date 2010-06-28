package lombok.eclipse;

import static java.util.Arrays.asList;
import static java.util.Collections.disjoint;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.toQualifiedName;
import static lombok.eclipse.HandleAsFunction.generateFunction;
import static lombok.eclipse.Source.source;
import static lombok.eclipse.ast.AstUtilities.findLocalType;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AccessLevel;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.eclipse.ast.JavaSpecMapping;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;
import lombok.eclipse.handlers.HandleEqualsAndHashCode;
import lombok.eclipse.handlers.HandleGetter;
import lombok.eclipse.handlers.HandleToString;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
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
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
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
import org.jsizzle.Delta;
import org.jsizzle.Include;
import org.jsizzle.Initialise;
import org.jsizzle.Invariant;
import org.jsizzle.JavaSpec.Type;
import org.jsizzle.Schema;
import org.jsizzle.SchemaField;
import org.jsizzle.SchemaSpec;

public class HandleSchema implements EclipseAnnotationHandler<Schema>
{
    private static final char[][] ORG_JSIZZLE_BINDING = fromQualifiedName("org.jsizzle.Binding");
    private static final char[][] ORG_JSIZZLE_SCHEMA = fromQualifiedName("org.jsizzle.Schema");
    private static final char[][] ORG_JSIZZLE_SCHEMAFIELD = fromQualifiedName("org.jsizzle.SchemaField");
    private static final char[][] INCLUSION_DIRECT = fromQualifiedName("org.jsizzle.Binding.Inclusion.DIRECT");
    private static final char[][] INCLUSION_INCLUDED = fromQualifiedName("org.jsizzle.Binding.Inclusion.INCLUDED");
    private static final char[][] INCLUSION_EXPANDED = fromQualifiedName("org.jsizzle.Binding.Inclusion.EXPANDED");
    private static final char[] IDENTITY_NAME = "identity".toCharArray();
    private static final List<Argument> noArgs = emptyList();

    private static final boolean instrument = Boolean.valueOf(System.getProperty("org.jsizzle.instrument"));

    @Override
    public boolean handle(AnnotationValues<Schema> annotation,
                          Annotation source,
                          EclipseNode annotationNode)
    {
        final Instrumentation instrumentation = instrument ? new Instrumentation(annotationNode.up()) : null;
        try
        {
            return makeSchemaClass(annotationNode.up(), annotationNode, source(source));
        }
        finally
        {
            if (instrumentation != null)
            {
                try
                {
                    instrumentation.after();
                }
                catch (IllegalStateException e)
                {
                    annotationNode.addError(e.getMessage());
                }
            }
        }
    }
    
    private boolean makeSchemaClass(final EclipseNode typeNode,
                                    final EclipseNode errorNode,
                                    final Source source)
    {
        final TypeDeclaration type = (typeNode.get() instanceof TypeDeclaration) ? (TypeDeclaration)typeNode.get() : null;
        final CompilationUnitDeclaration compilationUnit = (CompilationUnitDeclaration)typeNode.top().get();
        
        // Entirely reject null types and annotations
        if (type == null || (type.modifiers & AccAnnotation) != 0)
        {
            errorNode.addError("This cannot be a Schema");
            return true;
        }
        
        // Make class public
        type.modifiers |= AccPublic;
        
        // If not marked as a Schema (may be implied), do so
        if (findAnnotation(typeNode, Schema.class) == null)
        {
            final QualifiedTypeReference schemaType = source.generated(new QualifiedTypeReference(ORG_JSIZZLE_SCHEMA, source.p(3)));
            injectAnnotation(typeNode, source.generated(new MarkerAnnotation(schemaType, source.pS)));
        }

        
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
                            // Check for initialised member fields
                            if (field.initialization != null)
                            {
                                child.addError("Schema fields may not be initialised this way. Use an @Initialise method instead.");
                                break;
                            }
                                
                            final EclipseNode includeAnnNode = findAnnotation(child, Include.class);
                            if (includeAnnNode != null)
                            {
                                final TypeDeclaration includedType = findLocalType(compilationUnit, type, field.type.getTypeName());
                                final EclipseNode includedTypeNode = typeNode.getNodeFor(includedType);
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
                                consBuilder.addIncludedField(new EmbellishedSchemaField(child, errorNode, source), includeCons);
                                
                                for (AbstractVariableDeclaration variableToInclude : variablesToInclude(includedTypeNode, includeCons))
                                {
                                    if (fieldExists(new String(variableToInclude.name), typeNode) == MemberExistsResult.NOT_EXISTS)
                                    {
                                        // NOTE: Must set source positions on copied type, because for some reason Eclipse
                                        // doesn't like argument types with source positions from a different scope.
                                        final FieldDeclaration expandedField = injectSchemaField(typeNode,
                                                         variableToInclude.name,
                                                         source.copyType(variableToInclude.type, true),
                                                         AccPublic,
                                                         source);
                                        consBuilder.addExpandedField(
                                            new EmbellishedSchemaField(typeNode.getNodeFor(expandedField), child, source), field);
                                    }
                                }
                            }
                            else
                            {
                                consBuilder.addDirectField(new EmbellishedSchemaField(child, errorNode, source));
                            }
                        }
                    }
                    else if (child.getKind() == Kind.METHOD)
                    {
                        if (child.get() instanceof ConstructorDeclaration)
                        {
                            if ((child.get().bits & ASTNode.IsDefaultConstructor) == 0)
                                child.addWarning("Schema classes should not have constructors.");
                        }
                        else if (child.get() instanceof MethodDeclaration)
                        {
                            final MethodDeclaration method = (MethodDeclaration)child.get();
                            // Invariant methods are marked with @Invariant
                            final EclipseNode invariantAnnNode = findAnnotation(child, Invariant.class);
                            if (invariantAnnNode != null)
                            {
                                if (Arrays.equals(method.returnType.getLastToken(), TypeConstants.BOOLEAN)
                                    && (method.arguments == null || method.arguments.length == 0))
                                {
                                    // Make invariant method private final
                                    method.modifiers |= (AccPrivate | AccFinal);
                                    consBuilder.addInvariant((MethodDeclaration)method);
                                }
                                else
                                {
                                    child.addError("Invariant method must have no arguments and return a boolean.");
                                }
                            }
                            else
                            {
                                // Initialiser methods are marked with @Initialise
                                final EclipseNode initialiseAnnNode = findAnnotation(child, Initialise.class);
                                if (initialiseAnnNode != null)
                                {
                                    if (method.arguments != null && method.arguments.length > 0)
                                    {
                                        child.addError("Initialiser cannot have arguments.");
                                    }
                                    else
                                    {
                                        // If the field to be initialised does not exist, create it.
                                        if (findField(typeNode, child.getName()) == null)
                                        {
                                            final FieldDeclaration impliedField = injectSchemaField(typeNode,
                                                              method.selector,
                                                              source.copyType(method.returnType, true),
                                                              AccPublic,
                                                              source);
                                            consBuilder.addDirectField(
                                                new EmbellishedSchemaField(typeNode.getNodeFor(impliedField), child, source));
                                        }
                                        // Make initialiser method private final
                                        method.modifiers |= (AccPrivate | AccFinal);
                                        consBuilder.addInitialiser(child.getName(), method);
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
            }
            
            // If no fields, create an Object field for identity
            if (!consBuilder.hasFields())
                consBuilder.addDirectField(injectIdentityField(typeNode, source));
            
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
    
    private class EmbellishedSchemaField
    {
        public final FieldDeclaration decl;
        public final char[] accessorName;
        
        public EmbellishedSchemaField(final EclipseNode fieldNode,
                                      final EclipseNode errorNode,
                                      final Source source)
        {
            this.decl = (FieldDeclaration)fieldNode.get();
            // Generate the field access function
            // NOTE: Do not use the field node as source, due to use of retrieveEndOfElementTypeNamePosition
            // in org.eclipse.jdt.core.dom.ASTConverter.convertType
            this.accessorName = generateFunction(fieldNode, AccessLevel.PUBLIC, errorNode, source);
            // Generate a Getter for the field (useful for auto-implementing interfaces)
            new HandleGetter().generateGetterForField(fieldNode, source.node);
            // Mark the field as a schema field
            final QualifiedTypeReference schemaFieldType = source.generated(new QualifiedTypeReference(ORG_JSIZZLE_SCHEMAFIELD, source.p(3)));
            injectAnnotation(fieldNode, source.generated(new MarkerAnnotation(schemaFieldType, source.pS)));
        }
    }

    private static List<AbstractVariableDeclaration> variablesToInclude(EclipseNode includedTypeNode,
                                                                        ConstructorDeclaration includeCons)
    {
        // Return the constructor arguments, then any additional fields
        final List<AbstractVariableDeclaration> variables = new ArrayList<AbstractVariableDeclaration>(
                includeCons.arguments != null ? asList(includeCons.arguments) : noArgs);
        
        children: for (EclipseNode child : includedTypeNode.down())
        {
            if (child.getKind() == Kind.FIELD
                    && findAnnotation(child, SchemaField.class) != null
                    && findAnnotation(child, Include.class) == null)
            {
                for (AbstractVariableDeclaration variable : variables)
                {
                    if (Arrays.equals(variable.name, child.getName().toCharArray()))
                        continue children;
                }
                variables.add((FieldDeclaration)child.get());
            }
        }
        return variables;
    }

    private static EclipseNode findField(final EclipseNode typeNode, final String name)
    {
        for (EclipseNode fieldNode : typeNode.down())
        {
            if (fieldNode.getKind() == Kind.FIELD && fieldNode.getName().equals(name))
                return fieldNode;
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
    
    /**
     * Inserts an annotation into an existing node. The node must represent an {@code
     * AbstractVariableDeclaration} or a {@code TypeDeclaration}.
     */
    private static void injectAnnotation(EclipseNode node, Annotation annotation)
    {
        final Annotation[] oldArray = node.getKind() == Kind.TYPE ?
                ((TypeDeclaration)node.get()).annotations : ((AbstractVariableDeclaration)node.get()).annotations;
        
        final Annotation[] newArray;
        if (oldArray == null)
        {
            newArray = new Annotation[1];
            newArray[0] = annotation;
        }
        else
        {
            newArray = new Annotation[oldArray.length + 1];
            System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
            newArray[oldArray.length] = annotation;
        }
        if (node.getKind() == Kind.TYPE)
            ((TypeDeclaration)node.get()).annotations = newArray;
        else
            ((AbstractVariableDeclaration)node.get()).annotations = newArray;
            
        node.add(annotation, Kind.ANNOTATION).recursiveSetHandled();
    }

    private class ConstructorBuilder
    {
        final EclipseNode type;
        final Source source;
        final Map<String, LocalDeclaration> initialisers = new HashMap<String, LocalDeclaration>();
        final List<FieldAssignment> fieldAssignments = new ArrayList<FieldAssignment>();
        final List<Statement> otherStatements = new ArrayList<Statement>();
        
        private class FieldAssignment
        {
            public final char[] fieldName;
            public final Statement statement;
            public final Map<String, Argument> requiredArguments = new LinkedHashMap<String, Argument>();
            
            public FieldAssignment(FieldDeclaration field)
            {
                this(field.name, createNameReference(field.name), singletonList(createArgument(field)));
            }
            
            public FieldAssignment(char[] fieldName, Expression value, Collection<Argument> requiredArguments)
            {
                this.fieldName = fieldName;
                final FieldReference thisX = createFieldReference(fieldName);
                this.statement = source.generated(new Assignment(thisX, value, source.pE));

                for (Argument argument : requiredArguments)
                    this.requiredArguments.put(new String(argument.name), argument);
            }
        }
        
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
            
            final List<Statement> stmts = new ArrayList<Statement>();
            final Map<String, Argument> args = new LinkedHashMap<String, Argument>();
            final Set<String> alreadyInitialised = new HashSet<String>();
            for (FieldAssignment assignment : fieldAssignments)
            {
                // If any argument is initialised, include the initialiser statement and remove it
                final Map<String, Argument> requiredArguments = new LinkedHashMap<String, Argument>(assignment.requiredArguments);
                if (!disjoint(initialisers.keySet(), requiredArguments.keySet()))
                {
                    // Remove any arguments that have an initialiser
                    final Set<String> initialised = new HashSet<String>(requiredArguments.keySet());
                    initialised.retainAll(initialisers.keySet());
                    requiredArguments.keySet().removeAll(initialised);
                    
                    // Include any initialisation statement that hasn't already been included
                    initialised.removeAll(alreadyInitialised);
                    for (String name : initialised)
                        stmts.add(initialisers.get(name));
                    alreadyInitialised.addAll(initialised);
                }
                args.putAll(requiredArguments);
                stmts.add(assignment.statement);
            }
            stmts.addAll(otherStatements);
            
            constructor.statements = stmts.isEmpty() ? null : stmts.toArray(new Statement[stmts.size()]);
            constructor.arguments = args.isEmpty() ? null : args.values().toArray(new Argument[args.size()]);
            return constructor;
        }

        public void addDirectField(final EmbellishedSchemaField field)
        {
            addDirectField(field.decl);
            addFieldAccessor(field.accessorName, INCLUSION_DIRECT);
        }

        public void addDirectField(final FieldDeclaration field)
        {
            fieldAssignments.add(new FieldAssignment(field));
            
            final Statement nullCheck = generateNullCheck(field, source.node);
            if (nullCheck != null)
                otherStatements.add(nullCheck);
        }

        public void addIncludedField(final EmbellishedSchemaField field,
                                     final ConstructorDeclaration includeCons)
        {
            final AllocationExpression constructed = source.generated(new AllocationExpression());
            constructed.type = source.copyType(field.decl.type, false);
            final List<Argument> requiredArguments = new ArrayList<Argument>();
            if (includeCons.arguments == null)
            {
                constructed.arguments = null;
            }
            else
            {
                constructed.arguments = new Expression[includeCons.arguments.length];
                for (int i = 0; i < includeCons.arguments.length; i++)
                {
                    if (findFieldAssignment(includeCons.arguments[i].name) > -1)
                    {
                        // If the field already exists (hiding), then use its value.
                        constructed.arguments[i] = createFieldReference(includeCons.arguments[i].name);
                    }
                    else
                    {
                        // Otherwise demand an argument.
                        constructed.arguments[i] = createNameReference(includeCons.arguments[i].name);
                        requiredArguments.add(createArgument(includeCons.arguments[i]));
                    }
                }
            }
            fieldAssignments.add(new FieldAssignment(field.decl.name, constructed, requiredArguments));
            addFieldAccessor(field.accessorName, INCLUSION_INCLUDED);
        }

        public void addExpandedField(EmbellishedSchemaField field,
                                     FieldDeclaration includedField)
        {
            final int indexIncluded = findFieldAssignment(includedField.name);
            if (fieldAssignments.get(indexIncluded).requiredArguments.containsKey(new String(field.decl.name)))
            {
                // Expanded fields that are in the included field's constructor are bumped up.
                // This is because they may be referenced by an initialiser for another field.
                fieldAssignments.add(indexIncluded, new FieldAssignment(field.decl));
            }
            else
            {
                // Expanded fields that are not in the included field's constructor must be initialised
                // from the constructed field's unexpanded field. If these are referenced by an initialiser
                // for another field, this can lead to unavoidable NPEs.
                final FieldReference copyFrom = source.generated(new FieldReference(
                    (new String(includedField.name) + "." + new String(field.decl.name)).toCharArray(), source.p));
                copyFrom.receiver = createFieldReference(includedField.name);
                copyFrom.token = field.decl.name;
                fieldAssignments.add(new FieldAssignment(field.decl.name, copyFrom, noArgs));
            }
            addFieldAccessor(field.accessorName, INCLUSION_EXPANDED);
        }

        public void addInitialiser(final String fieldName, final MethodDeclaration method)
        {
            final LocalDeclaration declaration = source.generated(new LocalDeclaration(fieldName.toCharArray(), source.pS, source.pE));
            declaration.type = source.copyType(method.returnType, false);
            declaration.modifiers |= AccFinal;
            declaration.initialization = createThisCall(new String(method.selector));
            initialisers.put(fieldName, declaration);
        }
        
        public void addInvariant(final MethodDeclaration method)
        {
            final Expression callInvariant = createThisCall(new String(method.selector));
            final UnaryExpression notInvariant = source.generated(new UnaryExpression(callInvariant, OperatorIds.NOT));
            otherStatements.add(source.generated(new IfStatement(notInvariant, createAddViolation(method), source.pS, source.pE)));
        }
        
        public boolean hasFields()
        {
            return !fieldAssignments.isEmpty();
        }
        
        private int findFieldAssignment(char[] fieldName)
        {
            for (int i = 0; i < fieldAssignments.size(); i++)
            {
                if (Arrays.equals(fieldName, fieldAssignments.get(i).fieldName))
                    return i;
            }
            return -1;
        }

        private void addFieldAccessor(final char[] accessorName, final char[][] inclusion)
        {
            if (accessorName != null)
                otherStatements.add(createAddAccessor(accessorName, inclusion));
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

        private Argument createArgument(final AbstractVariableDeclaration var)
        {
            return source.generated(new Argument(var.name,
                                                 source.p,
                                                 source.copyType(var.type, false),
                                                 AccFinal));
        }
    
        private FieldReference createFieldReference(final char[] name)
        {
            final FieldReference thisX = source.generated(new FieldReference(
                ("this." + new String(name)).toCharArray(), source.p));
            thisX.receiver = source.generated(new ThisReference(source.pS, source.pE));
            thisX.token = name;
            return thisX;
        }

        private SingleNameReference createNameReference(final char[] name)
        {
            return source.generated(new SingleNameReference(name, source.p));
        }
    }
    
    private class Instrumentation
    {
        private final TypeDeclaration type;
        private final JavaSpecMapping javaSpecMapping;
        private final Type specTypeBefore;
        
        public Instrumentation(EclipseNode typeNode)
        {
            this.type = (typeNode.get() instanceof TypeDeclaration) ? (TypeDeclaration)typeNode.get() : null;
            this.javaSpecMapping = new JavaSpecMapping(typeNode);
            this.specTypeBefore = javaSpecMapping.specType(type);
        }
        
        public void after() throws IllegalStateException
        {
            final Type specTypeAfter = javaSpecMapping.specType(type);
            new SchemaSpec(new Delta<Type>(specTypeBefore, specTypeAfter), javaSpecMapping.typeForName).checkInvariant();
        }
    }
}
