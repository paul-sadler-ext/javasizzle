package lombok.eclipse;

import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Source.source;
import static lombok.eclipse.handlers.EclipseHandlerUtil.fieldExists;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectField;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectMethod;
import static lombok.eclipse.handlers.EclipseHandlerUtil.toEclipseModifier;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccFinal;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccPublic;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccStatic;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccVisibilityMASK;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.handlers.TransformationsUtil;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
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
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.jsizzle.AsFunction;

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
        private final TypeReference fromType, toType;
        private final Expression application;
        private int modifiers;

        public FunctionBuilder(final EclipseNode target,
                               final AccessLevel access,
                               final Source source)
        {
            this.source = source;
            this.typeNode = target.up();
            
            // Check that we are on a method or field
            if (target == null || !(target.getKind() == Kind.METHOD || target.getKind() == Kind.FIELD)
                    || !(target.get() instanceof MethodDeclaration || target.get() instanceof FieldDeclaration))
            {
                throw new IllegalArgumentException("@AsFunction is legal only on methods and fields.");
            }
            
            final TypeReference enclosingType =
                source.generated(new SingleTypeReference(target.up().getName().toCharArray(), source.p));
            final SingleNameReference fromReference = source.generated(new SingleNameReference("from".toCharArray(), source.p));
    
            if (target.getKind() == Kind.METHOD)
            {
                functionName = target.getName();
                final MethodDeclaration method = (MethodDeclaration)target.get();
                toType = createBoxedTypeReference(method.returnType);
                modifiers = method.modifiers;
    
                final Expression receiver, argument;
                /*
                 * Supported method signatures:
                 *  no-arg instance -> static function
                 *  one-arg instance -> instance function
                 *  one-arg static -> static function
                 */
                if ((modifiers & AccStatic) != 0)
                {
                    if (method.arguments != null && method.arguments.length == 1)
                    {
                        fromType = createBoxedTypeReference(method.arguments[0].type);
                        receiver = source.generated(new SingleNameReference(target.up().getName().toCharArray(), source.p));
                        argument = fromReference;
                    }
                    else
                    {
                        // Static method must have one parameter
                        throw new IllegalArgumentException("@AsFunction can only support one-argument static methods.");
                    }
                }
                else
                {
                    // Instance method can have zero or one parameter
                    if (method.arguments == null || method.arguments.length == 0)
                    {
                        fromType = enclosingType;
                        receiver = fromReference;
                        argument = null;
                        modifiers |= AccStatic;
                    }
                    else if (method.arguments.length == 1)
                    {
                        fromType = createBoxedTypeReference(method.arguments[0].type);
                        receiver = source.generated(new QualifiedThisReference(enclosingType, source.pS, source.pE));
                        argument = fromReference;
                    }
                    else
                    {
                        throw new IllegalArgumentException("@AsFunction can only support one- or no-argument instance methods.");
                    }
                }
                application = createMethodCall(receiver, method, argument);
            }
            else
            {
                final FieldDeclaration field = (FieldDeclaration)target.get();
                if ((field.modifiers & AccStatic) != 0)
                    throw new IllegalArgumentException("@AsFunction can only support instance fields.");
                
                modifiers = field.modifiers | AccStatic;
                fromType = enclosingType;
                toType = createBoxedTypeReference(field.type);
                boolean isBoolean = Arrays.equals(field.type.getLastToken(), "boolean".toCharArray()) && toType.dimensions() == 0;
                functionName = TransformationsUtil.toGetterName(target.getName(), isBoolean);
                final FieldReference fieldRef = source.generated(new FieldReference(field.name, source.p));
                fieldRef.receiver = fromReference;
                fieldRef.token = field.name;
                application = fieldRef;
            }
            if (access != AccessLevel.NONE)
                modifiers = (modifiers & ~AccVisibilityMASK) | toEclipseModifier(access);
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
            
            final FieldDeclaration functionField =
                source.generated(new FieldDeclaration(functionName.toCharArray(), source.pS, source.pE));
            functionField.declarationSourceStart = functionField.sourceStart;
            functionField.declarationEnd = functionField.declarationSourceEnd = functionField.sourceEnd;
            functionField.modifiers = AccFinal | modifiers;
            functionField.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
            functionField.type = createFunctionInterfaceType();

            final TypeDeclaration functionType = source.generated(new TypeDeclaration(compilationResult));
            functionType.modifiers |= AccFinal | modifiers;
            functionType.name = ("$" + functionName).toCharArray();
            functionType.superInterfaces = new TypeReference[] {source.copyType(functionField.type, false)};
            functionType.bodyStart = functionType.declarationSourceStart = functionType.sourceStart;
            functionType.bodyEnd = functionType.declarationSourceEnd = functionType.sourceEnd;
            
            final MethodDeclaration applyMethod = source.generated(new MethodDeclaration(compilationResult));
            applyMethod.modifiers |= AccPublic;
            applyMethod.returnType = source.copyType(toType, false);
            applyMethod.selector = "apply".toCharArray();
            applyMethod.arguments = new Argument[] {createFromArg()};
            applyMethod.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
            applyMethod.bodyStart = applyMethod.declarationSourceStart = applyMethod.sourceStart;
            applyMethod.bodyEnd = applyMethod.declarationSourceEnd = applyMethod.sourceEnd;
            
            final Statement returnStatement = source.generated(new ReturnStatement(application, source.pS, source.pE));
            applyMethod.statements = new Statement[] { returnStatement };
            final ConstructorDeclaration defCon = source.generated(functionType.createDefaultConstructor(false, false));
            defCon.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
            functionType.methods = new AbstractMethodDeclaration[] {defCon, applyMethod};
            
            final AllocationExpression allocateFunction = source.generated(new AllocationExpression());
            allocateFunction.type = source.generated(new SingleTypeReference(functionType.name, source.p));
            functionField.initialization = allocateFunction;
            
            injectType(functionType);
            injectField(typeNode, functionField);
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

        private TypeReference createFunctionInterfaceType()
        {
            final char[][] tokens = fromQualifiedName("com.google.common.base.Function");
            final TypeReference[][] typeArguments = new TypeReference[tokens.length][];
            typeArguments[tokens.length - 1] = new TypeReference[] {fromType, toType};
            final TypeReference functionInterface = source.generated(
                new ParameterizedQualifiedTypeReference(tokens, typeArguments, 0, source.p(tokens.length)));
            return functionInterface;
        }
    
        private MessageSend createMethodCall(final Expression receiver,
                                             final MethodDeclaration method,
                                             final Expression argument)
        {
            final MessageSend methodCall = source.generated(new MessageSend());
            methodCall.receiver = receiver;
            methodCall.selector = method.selector;
            methodCall.arguments = argument == null ? null : new Expression[] {argument};
            methodCall.statementEnd = methodCall.sourceEnd;
            return methodCall;
        }
    
        private Argument createFromArg()
        {
            return source.generated(new Argument("from".toCharArray(),
                                                 source.p,
                                                 source.copyType(fromType, false),
                                                 AccFinal));
        }
        
        /**
         * Inserts a member type into an existing type. The type must represent a {@code TypeDeclaration}.
         */
        public void injectType(final TypeDeclaration memberType)
        {
            final TypeDeclaration parent = (TypeDeclaration) typeNode.get();
            if (parent.memberTypes == null)
            {
                parent.memberTypes = new TypeDeclaration[1];
                parent.memberTypes[0] = memberType;
            }
            else
            {
                TypeDeclaration[] newArray = new TypeDeclaration[parent.memberTypes.length + 1];
                System.arraycopy(parent.memberTypes, 0, newArray, 0, parent.memberTypes.length);
                newArray[parent.memberTypes.length] = memberType;
                parent.memberTypes = newArray;
            }
            typeNode.add(memberType, Kind.TYPE).recursiveSetHandled();
        }
    }
}
