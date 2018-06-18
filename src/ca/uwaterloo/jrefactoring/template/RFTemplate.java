package ca.uwaterloo.jrefactoring.template;

import ca.uwaterloo.jrefactoring.utility.ContextUtil;
import ca.uwaterloo.jrefactoring.utility.Transformer;
import org.eclipse.jdt.core.dom.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RFTemplate {

    private static final String TYPE_NAME_PREFIX = "T";
    private static final String CLAZZ_NAME_PREFIX = "clazz";
    private static final String CLASS_NAME = "Class";
    private static final String NEW_INSTANCE_METHOD_NAME = "newInstance";
    private static final String GET_DECLARED_CONSTRUCTOR_METHOD_NAME = "getDeclaredConstructor";

    private AST ast;
    //private GenericManager genericManager;
    private MethodDeclaration templateMethod;
    private Map<TypePair, String> typeMap;
    private Map<String, String> clazzInstanceMap;
    private int clazzCount;
    private int typeCount;

    public RFTemplate(AST ast) {
        this.ast = ast;
        typeMap = new HashMap<>();
        clazzInstanceMap = new HashMap<>();
        clazzCount = 1;
        typeCount = 1;
        initTemplate();
    }

    public AST getAst() {
        return ast;
    }

    private void initTemplate() {
        templateMethod = ast.newMethodDeclaration();
        Modifier privateModifier = ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
        templateMethod.modifiers().add(privateModifier);
        templateMethod.setBody(ast.newBlock());
        templateMethod.setName(ast.newSimpleName("template1"));
    }

    public void addStatement(Statement statement) {
        templateMethod.getBody().statements().add(statement);
    }

    private void addGenericType(String genericTypeName) {
        TypeParameter typeParameter = ast.newTypeParameter();
        typeParameter.setName(ast.newSimpleName(genericTypeName));
        templateMethod.typeParameters().add(typeParameter);
    }

    public void setModifier(Modifier.ModifierKeyword modifier) {
        templateMethod.modifiers().add(ast.newModifier(modifier));
    }

    private void addClazzInParameter(String genericTypeName) {
        Type genericType = ast.newSimpleType(ast.newSimpleName(genericTypeName));
        Type classType = ast.newSimpleType(ast.newSimpleName(CLASS_NAME));
        ParameterizedType classTypeWithGenericType = ast.newParameterizedType(classType);
        classTypeWithGenericType.typeArguments().add(genericType);

        SingleVariableDeclaration clazz= ast.newSingleVariableDeclaration();
        SimpleName clazzName = ast.newSimpleName(resolveGenericType(genericTypeName));
        clazz.setType(classTypeWithGenericType);
        clazz.setName(clazzName);
        templateMethod.parameters().add(clazz);
    }

    public boolean containsTypePair(TypePair typePair) {
        return typeMap.containsKey(typePair);
    }

    public String getGenericTypeName(TypePair typePair) {
        return typeMap.get(typePair);
    }

    public String resolveTypePair(TypePair typePair) {
        if (!typeMap.containsKey(typePair)) {
            String typeName = TYPE_NAME_PREFIX + typeCount++;
            typeMap.put(typePair, typeName);
            addGenericType(typeName);
        }
        return typeMap.get(typePair);
    }

    public boolean containsGenericType(String type) {
        return clazzInstanceMap.containsKey(type);
    }

    public String getClazz(String type) {
        return clazzInstanceMap.get(type);
    }

    public String resolveGenericType(String genericType) {
        if (!clazzInstanceMap.containsKey(genericType)) {
            String clazzName = CLAZZ_NAME_PREFIX + clazzCount++;
            clazzInstanceMap.put(genericType, clazzName);
            addClazzInParameter(genericType);
        }
        return clazzInstanceMap.get(genericType);
    }

    public void createClazzInstance(Expression expr, String clazzName) {
        ASTNode contextNode = ContextUtil.getContextNode(expr);
        if (contextNode.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {

            // get VariableDeclarationFragment node
            VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) contextNode.getParent();

            // replace initializer
            MethodInvocation newInstanceMethodInvocation = ast.newMethodInvocation();
            MethodInvocation getDeclaredConstructorMethodInvocation = ast.newMethodInvocation();
            newInstanceMethodInvocation.setExpression(getDeclaredConstructorMethodInvocation);
            newInstanceMethodInvocation.setName(ast.newSimpleName(NEW_INSTANCE_METHOD_NAME));
            getDeclaredConstructorMethodInvocation.setName(ast.newSimpleName(GET_DECLARED_CONSTRUCTOR_METHOD_NAME));
            getDeclaredConstructorMethodInvocation.setExpression(ast.newSimpleName(clazzName));
            variableDeclarationFragment.setInitializer(newInstanceMethodInvocation);

            // add method parameters
            ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) contextNode;
            List<Expression> arguments = classInstanceCreation.arguments();
            for (Expression argument: arguments) {
                TypeLiteral typeLiteral = ast.newTypeLiteral();
                Type type = Transformer.typeFromBinding(ast, argument.resolveTypeBinding());
                typeLiteral.setType(type);
                getDeclaredConstructorMethodInvocation.arguments().add(typeLiteral);
                Expression newArg = (Expression) ASTNode.copySubtree(ast, argument);
                newInstanceMethodInvocation.arguments().add(newArg);
            }

        } else {
            throw new IllegalStateException("unexpected context node: " + contextNode + "[Type: " + contextNode.getNodeType()
                    + "] for expression [" + expr + "]");
        }
    }

    @Override
    public String toString() {
        return templateMethod.toString();
    }

}
