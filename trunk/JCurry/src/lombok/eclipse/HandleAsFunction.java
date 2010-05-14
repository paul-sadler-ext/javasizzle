package lombok.eclipse;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static java.util.Arrays.asList;
import static lombok.core.handlers.TransformationsUtil.toGetterName;
import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Source.source;
import static lombok.eclipse.handlers.EclipseHandlerUtil.fieldExists;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectField;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectMethod;
import static lombok.eclipse.handlers.EclipseHandlerUtil.toEclipseModifier;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccFinal;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccPrivate;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccPublic;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccStatic;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccVisibilityMASK;

import java.util.Arrays;
import java.util.List;

import lombok.AccessLevel;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.jcurry.AsFunction;

public class HandleAsFunction implements EclipseAnnotationHandler<AsFunction>
{
    @Override
    public boolean handle(AnnotationValues<AsFunction> annotation,
                          Annotation source,
                          EclipseNode annotationNode)
    {
        generateFunction(annotationNode.up(), AccessLevel.NONE, annotationNode, source(source));
        return true;
    }

    public static void generateFunction(final EclipseNode target,
                                        final AccessLevel access,
                                        final EclipseNode errorNode,
                                        final Source source)
    {
        try
        {
            new FunctionBuilder(target, access, source).build();
        }
        catch (IllegalArgumentException e)
        {
            errorNode.addError(e.getMessage());
        }
    }
    
    private static class FunctionBuilder
    {
        private final Source source;
        private final EclipseNode typeNode;
        private final String functionName;
        private final FunctionTypeBuilder functionTypeBuilder;
        private int functionModifiers;

        public FunctionBuilder(final EclipseNode target,
                               final AccessLevel access,
                               final Source source)
        {
            this.source = source;
            this.typeNode = target.up();
            
            // Check that we are on a constructor, method or field
            if (target == null || !(target.getKind() == Kind.METHOD || target.getKind() == Kind.FIELD)
                    || !(target.get() instanceof MethodDeclaration
                            || target.get() instanceof ConstructorDeclaration
                            || target.get() instanceof FieldDeclaration))
            {
                throw new IllegalArgumentException("@AsFunction is legal only on constructors, methods and fields.");
            }
            
            final String enclosingTypeName = target.up().getName();
            final TypeReference enclosingType =
                source.generated(new SingleTypeReference(enclosingTypeName.toCharArray(), source.p));
    
            if (target.getKind() == Kind.METHOD)
            {
                final AbstractMethodDeclaration method = (AbstractMethodDeclaration)target.get();
                functionModifiers = method.modifiers;
    
                /*
                 * Supported method signatures:
                 *  one or more arg constructor -> static function (must have argument)
                 *  no-arg instance -> static function
                 *  one or more arg instance -> instance function
                 *  one or more arg static -> static function
                 */
                if (method instanceof ConstructorDeclaration)
                {
                    functionName = createConstructorFunctionName(enclosingTypeName);
                    functionTypeBuilder = new ConstructorFunctionTypeBuilder((ConstructorDeclaration)method, enclosingType);
                    functionModifiers |= AccStatic;
                }
                else
                {
                    functionName = target.getName();
                    if ((functionModifiers & AccStatic) != 0)
                    {
                        functionTypeBuilder = new MethodCallFunctionTypeBuilder((MethodDeclaration)method, enclosingType);
                    }
                    else
                    {
                        // Instance method can have zero or one parameter
                        if (method.arguments == null || method.arguments.length == 0)
                        {
                            functionTypeBuilder = new ThisCallFunctionTypeBuilder((MethodDeclaration)method, enclosingType);
                            functionModifiers |= AccStatic;
                        }
                        else
                        {
                            functionTypeBuilder = new MethodCallFunctionTypeBuilder((MethodDeclaration)method, enclosingType);
                        }
                    }
                }
            }
            else
            {
                final FieldDeclaration field = (FieldDeclaration)target.get();
                if ((field.modifiers & AccStatic) != 0)
                    throw new IllegalArgumentException("@AsFunction can only support instance fields.");
                
                functionModifiers = field.modifiers | AccStatic;
                boolean isBoolean = Arrays.equals(field.type.getLastToken(), "boolean".toCharArray()) && field.type.dimensions() == 0;
                functionName = toGetterName(target.getName(), isBoolean);
                functionTypeBuilder = new FieldAccessFunctionTypeBuilder(field, enclosingType);
            }
            if (access != AccessLevel.NONE)
                functionModifiers = (functionModifiers & ~AccVisibilityMASK) | toEclipseModifier(access);
        }
        
        public void build()
        {
            if (fieldExists(functionName, typeNode) != MemberExistsResult.NOT_EXISTS)
                throw new IllegalArgumentException("A field called " + functionName + " already exists");

            /*
             * Workaround for Eclipse weirdness: if we're adding a static field,
             * then Eclipse insists on initialising it to null UNLESS a static
             * field or initialiser already exists. So, here we add a static
             * initialiser if needed.
             */
            final CompilationResult compilationResult =
                ((CompilationUnitDeclaration)typeNode.top().get()).compilationResult;
            if (!typeHasClinit())
                injectMethod(typeNode, source.generated(new Clinit(compilationResult)));
            
            final TypeDeclaration functionType = functionTypeBuilder.build(compilationResult);
            
            final FieldDeclaration functionField =
                source.generated(new FieldDeclaration(functionName.toCharArray(), source.pS, source.pE));
            functionField.declarationSourceStart = functionField.sourceStart;
            functionField.declarationEnd = functionField.declarationSourceEnd = functionField.sourceEnd;
            functionField.modifiers = AccFinal | functionModifiers;
            functionField.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
            functionField.type = functionTypeBuilder.implementedType();
            
            final AllocationExpression allocateFunction = source.generated(new AllocationExpression());
            allocateFunction.type = source.generated(new SingleTypeReference(functionType.name, source.p));
            functionField.initialization = allocateFunction;
            
            injectType(typeNode, functionType);
            injectField(typeNode, functionField);
        }
        
        private abstract class FunctionTypeBuilder
        {
            protected abstract TypeReference fromType();
            protected abstract char[] fromName();
            protected abstract TypeReference toType();
            protected abstract Expression application();
            
            public TypeReference implementedType()
            {
                return createFunctionInterfaceType(fromType(), toType());
            }
            
            protected String functionTypeName()
            {
                return "$" + functionName + toUpperCase(fromName()[0]) + new String(fromName()).substring(1);
            }
            
            protected int modifiers()
            {
                return functionModifiers;
            }

            protected ConstructorDeclaration createConstructor(final TypeDeclaration functionType, CompilationResult compilationResult)
            {
                final ConstructorDeclaration defCon = source.generated(functionType.createDefaultConstructor(false, false));
                defCon.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
                return defCon;
            }

            public TypeDeclaration build(CompilationResult compilationResult)
            {
                final TypeDeclaration functionType = source.generated(new TypeDeclaration(compilationResult));
                functionType.modifiers |= AccFinal | modifiers();
                functionType.name = functionTypeName().toCharArray();
                functionType.superInterfaces = new TypeReference[] {implementedType()};
                functionType.bodyStart = functionType.declarationSourceStart = functionType.sourceStart;
                functionType.bodyEnd = functionType.declarationSourceEnd = functionType.sourceEnd;
                
                final MethodDeclaration applyMethod = source.generated(new MethodDeclaration(compilationResult));
                applyMethod.modifiers |= AccPublic;
                applyMethod.returnType = toType();
                applyMethod.selector = "apply".toCharArray();
                applyMethod.arguments = new Argument[] {createFromArg(fromName(), fromType())};
                applyMethod.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
                applyMethod.bodyStart = applyMethod.declarationSourceStart = applyMethod.sourceStart;
                applyMethod.bodyEnd = applyMethod.declarationSourceEnd = applyMethod.sourceEnd;
                final Statement returnStatement = source.generated(new ReturnStatement(application(), source.pS, source.pE));
                applyMethod.statements = new Statement[] { returnStatement };
                
                final ConstructorDeclaration constructor = createConstructor(functionType, compilationResult);
                functionType.methods = new AbstractMethodDeclaration[] {constructor, applyMethod};
                
                return functionType;
            }
        }
        
        private abstract class AbstractMultiArgumentFunctionTypeBuilder extends FunctionTypeBuilder
        {
            private final Argument argument;
            private final AbstractMultiArgumentFunctionTypeBuilder next;

            public AbstractMultiArgumentFunctionTypeBuilder(List<Argument> arguments)
            {
                if (arguments.isEmpty())
                    throw new IllegalArgumentException("No argument available for function application");
                
                this.argument = arguments.get(0);
                this.next = arguments.size() == 1 ? null :
                    new IntermediateArgumentFunctionTypeBuilder(this, arguments.subList(1, arguments.size()));
            }
            
            protected abstract TypeReference finalType();
            protected abstract Expression finalApplication();

            @Override
            protected Expression application()
            {
                return next == null ? finalApplication() :
                    createAllocation(source.generated(new SingleTypeReference(next.functionTypeName().toCharArray(), source.p)),
                                     callArguments(argument));
            }

            @Override
            protected TypeReference toType()
            {
                return next == null ? finalType() : next.implementedType();
            }

            @Override
            protected char[] fromName()
            {
                return argument.name;
            }

            @Override
            protected TypeReference fromType()
            {
                return createBoxedTypeReference(argument.type);
            }

            @Override
            public TypeDeclaration build(CompilationResult compilationResult)
            {
                final TypeDeclaration functionType = super.build(compilationResult);
                if (next != null)
                    injectType(functionType, next.build(compilationResult));

                return functionType;
            }
        }
        
        private class IntermediateArgumentFunctionTypeBuilder extends AbstractMultiArgumentFunctionTypeBuilder
        {
            private final AbstractMultiArgumentFunctionTypeBuilder prev;
            
            private IntermediateArgumentFunctionTypeBuilder(AbstractMultiArgumentFunctionTypeBuilder prev, List<Argument> arguments)
            {
                super(arguments);
                this.prev = prev;
            }

            @Override
            protected TypeReference finalType()
            {
                return prev.finalType();
            }
        
            @Override
            protected Expression finalApplication()
            {
                return prev.finalApplication();
            }

            @Override
            protected int modifiers()
            {
                // Intermediate function types need to access parent fields
                return super.modifiers() & ~AccStatic;
            }

            @Override
            public TypeDeclaration build(CompilationResult compilationResult)
            {
                final TypeDeclaration functionType = super.build(compilationResult);
                
                // Add previous argument as field
                final FieldDeclaration field = source.generated(new FieldDeclaration(prev.argument.name, source.pS, source.pE));
                field.declarationSourceStart = field.sourceStart;
                field.declarationEnd = field.declarationSourceEnd = field.sourceEnd;
                field.modifiers = AccFinal | AccPrivate;
                field.type = source.copyType(prev.argument.type, false);
                functionType.fields = new FieldDeclaration[] {field};

                return functionType;
            }

            @Override
            protected ConstructorDeclaration createConstructor(TypeDeclaration functionType, CompilationResult compilationResult)
            {
                final TypeReference fieldType = prev.argument.type;
                final char[] fieldName = prev.argument.name;

                // Create single-argument constructor assigning to the field
                final ConstructorDeclaration constructor = source.generated(new ConstructorDeclaration(compilationResult));
                constructor.modifiers = AccPublic;
                constructor.selector = functionType.name;
                constructor.constructorCall = source.generated(new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper));
                constructor.bits |= Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
                constructor.bodyStart = constructor.declarationSourceStart = constructor.sourceStart;
                constructor.bodyEnd = constructor.declarationSourceEnd = constructor.sourceEnd;
                
                // Constructor argument
                final Argument consArg = source.generated(new Argument(fieldName,
                                                                       source.p,
                                                                       source.copyType(fieldType, false),
                                                                       AccFinal));
                constructor.arguments = new Argument[] {consArg};

                // Constructor assignment statement
                final FieldReference thisX =
                    source.generated(new FieldReference(("this." + new String(fieldName)).toCharArray(), source.p));
                thisX.receiver = source.generated(new ThisReference(source.pS, source.pE));
                thisX.token = fieldName;
                final SingleNameReference argReference = source.generated(new SingleNameReference(fieldName, source.p));
                final Statement assignment = source.generated(new Assignment(thisX, argReference, source.pE));
                constructor.statements = new Statement[] {assignment};
                
                return constructor;
            }
        }

        private class ConstructorFunctionTypeBuilder extends AbstractMultiArgumentFunctionTypeBuilder
        {
            private final TypeReference type;
            private final ConstructorDeclaration constructor;
            
            public ConstructorFunctionTypeBuilder(ConstructorDeclaration constructor,
                                                  TypeReference type)
            {
                super(asList(constructor.arguments));
                this.type = type;
                this.constructor = constructor;
            }

            @Override
            protected Expression finalApplication()
            {
                return createAllocation(type, callArguments(constructor.arguments));
            }

            @Override
            protected TypeReference finalType()
            {
                return type;
            }
        }
        
        private class MethodCallFunctionTypeBuilder extends AbstractMultiArgumentFunctionTypeBuilder
        {
            private final TypeReference type;
            private final MethodDeclaration method;
            
            public MethodCallFunctionTypeBuilder(MethodDeclaration method,
                                                 TypeReference type)
            {
                super(asList(method.arguments));
                this.type = type;
                this.method = method;
            }

            @Override
            protected Expression finalApplication()
            {
                final Expression receiver = source.generated((functionModifiers & AccStatic) != 0 ? 
                        new SingleNameReference(type.getLastToken(), source.p) :
                        new QualifiedThisReference(type, source.pS, source.pE));
                return createMethodCall(receiver, method.selector, callArguments(method.arguments));
            }

            @Override
            protected TypeReference finalType()
            {
                return createBoxedTypeReference(method.returnType);
            }
        }
        
        private class ThisCallFunctionTypeBuilder extends FunctionTypeBuilder
        {
            private final TypeReference type;
            private final MethodDeclaration method;
            
            public ThisCallFunctionTypeBuilder(MethodDeclaration method, TypeReference type)
            {
                this.type = type;
                this.method = method;
            }

            @Override
            protected Expression application()
            {
                final Expression receiver = source.generated(new SingleNameReference(fromName(), source.p));
                return createMethodCall(receiver, method.selector, null);
            }

            @Override
            protected char[] fromName()
            {
                return "from".toCharArray();
            }

            @Override
            protected TypeReference fromType()
            {
                return type;
            }

            @Override
            protected TypeReference toType()
            {
                return createBoxedTypeReference(method.returnType);
            }
        }
        
        private class FieldAccessFunctionTypeBuilder extends FunctionTypeBuilder
        {
            private final TypeReference type;
            private final FieldDeclaration field;
            
            public FieldAccessFunctionTypeBuilder(FieldDeclaration field, TypeReference type)
            {
                this.type = type;
                this.field = field;
            }

            @Override
            protected Expression application()
            {
                final FieldReference fieldRef = source.generated(new FieldReference(field.name, source.p));
                fieldRef.receiver = source.generated(new SingleNameReference(fromName(), source.p));
                fieldRef.token = field.name;
                return fieldRef;
            }

            @Override
            protected char[] fromName()
            {
                return "from".toCharArray();
            }

            @Override
            protected TypeReference fromType()
            {
                return type;
            }

            @Override
            protected TypeReference toType()
            {
                return createBoxedTypeReference(field.type);
            }
        }
        
        private Expression[] callArguments(Argument... arguments)
        {
            final Expression[] callArguments = new Expression[arguments.length];
            for (int i = 0; i < arguments.length; i++)
                callArguments[i] = source.generated(new SingleNameReference(arguments[i].name, source.p));
            
            return callArguments;
        }

        private String createConstructorFunctionName(final String enclosingTypeName)
        {
            final char firstLetter = enclosingTypeName.charAt(0);
            return isUpperCase(firstLetter) ?
                    toLowerCase(firstLetter) + enclosingTypeName.substring(1) :
                        toGetterName(enclosingTypeName, false);
        }

        private Expression createAllocation(final TypeReference type,
                                            final Expression[] arguments)
        {
            final AllocationExpression constructed = source.generated(new AllocationExpression());
            constructed.type = source.copyType(type, false);
            constructed.arguments = arguments;
            return constructed;
        }

        private boolean typeHasClinit()
        {
            for (EclipseNode child : typeNode.down())
            {
                if (child.getKind() == Kind.METHOD && ((AbstractMethodDeclaration)child.get()).isClinit())
                    return true;
            }
            return false;
        }
        
        private static char[][] boxedTypeConstant(String token)
        {
            if (token.equals("byte"))
                return TypeConstants.JAVA_LANG_BYTE;
            if (token.equals("short"))
                return TypeConstants.JAVA_LANG_SHORT;
            if (token.equals("int"))
                return TypeConstants.JAVA_LANG_INTEGER;
            if (token.equals("long"))
                return TypeConstants.JAVA_LANG_LONG;
            if (token.equals("char"))
                return TypeConstants.JAVA_LANG_CHARACTER;
            if (token.equals("boolean"))
                return TypeConstants.JAVA_LANG_BOOLEAN;
            if (token.equals("double"))
                return TypeConstants.JAVA_LANG_DOUBLE;
            if (token.equals("float"))
                return TypeConstants.JAVA_LANG_FLOAT;
            return null;
        }
        
        private TypeReference createBoxedTypeReference(TypeReference rawType)
        {
            final char[][] boxedTypeConstant = boxedTypeConstant(new String(rawType.getLastToken()));
            return boxedTypeConstant != null ?
                    source.generated(new QualifiedTypeReference(boxedTypeConstant, source.p(3))) : source.copyType(rawType, false);
        }

        private TypeReference createFunctionInterfaceType(final TypeReference fromType,
                                                          final TypeReference toType)
        {
            final char[][] tokens = fromQualifiedName("com.google.common.base.Function");
            final TypeReference[][] typeArguments = new TypeReference[tokens.length][];
            typeArguments[tokens.length - 1] = new TypeReference[] {fromType, toType};
            final TypeReference functionInterface = source.generated(
                new ParameterizedQualifiedTypeReference(tokens, typeArguments, 0, source.p(tokens.length)));
            return functionInterface;
        }
    
        private MessageSend createMethodCall(final Expression receiver,
                                             final char[] methodName,
                                             final Expression[] arguments)
        {
            final MessageSend methodCall = source.generated(new MessageSend());
            methodCall.receiver = receiver;
            methodCall.selector = methodName;
            methodCall.arguments = arguments;
            methodCall.statementEnd = methodCall.sourceEnd;
            return methodCall;
        }
    
        private Argument createFromArg(final char[] name, final TypeReference fromType)
        {
            return source.generated(new Argument(name,
                                                 source.p,
                                                 source.copyType(fromType, false),
                                                 AccFinal));
        }
    }
    
    /**
     * Inserts a member type into an existing type. The type must represent a {@code TypeDeclaration}.
     */
    public static void injectType(EclipseNode typeNode, final TypeDeclaration memberType)
    {
        injectType((TypeDeclaration)typeNode.get(), memberType);
        typeNode.add(memberType, Kind.TYPE).recursiveSetHandled();
    }
    
    /**
     * Inserts a member type into an existing type.
     */
    public static void injectType(TypeDeclaration parentType, final TypeDeclaration memberType)
    {
        if (parentType.memberTypes == null)
        {
            parentType.memberTypes = new TypeDeclaration[1];
            parentType.memberTypes[0] = memberType;
        }
        else
        {
            TypeDeclaration[] newArray = new TypeDeclaration[parentType.memberTypes.length + 1];
            System.arraycopy(parentType.memberTypes, 0, newArray, 0, parentType.memberTypes.length);
            newArray[parentType.memberTypes.length] = memberType;
            parentType.memberTypes = newArray;
        }
    }
}
