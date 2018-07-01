package ca.uwaterloo.jrefactoring.template;

import ca.uwaterloo.jrefactoring.utility.ASTNodeUtil;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import ca.uwaterloo.jrefactoring.utility.RenameUtil;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

import java.util.*;

public class RFTemplate {

    private static Logger log = FileLogger.getLogger(RFTemplate.class);
    private static final String TYPE_NAME_PREFIX = "T";
    private static final String CLAZZ_NAME_PREFIX = "clazz";
    private static final String CLASS_NAME = "Class";
    private static final String EXCEPTION_NAME = "Exception";
    private static final String DEFAULT_TEMPLATE_NAME = "template1";
    private static final String DEFAULT_ADAPTER_TYPE_NAME = "Adapter1";
    private static final String DEFAULT_ADAPTER_VARIABLE_NAME = "adapter";
    private static final String DEFAULT_ADAPTER_METHOD_NAME = "action";
    private static final String[] DEFAULT_ADAPTER_IMPL_NAME_PAIR = new String[] {"adapter1Impl1", "adapter1Impl2"};

    private AST ast;
    private MethodDeclaration templateMethod;
    private TypeDeclaration adapter;
    private TypeDeclaration adapterImpl1;
    private TypeDeclaration adapterImpl2;
    private Map<TypePair, String> typeMap;
    private Map<String, TypePair> genericTypeMap;
    private Map<MethodInvocationPair, String> methodInvocationMap;
    private Map<String, String> clazzInstanceMap;
    private Map<String, String> nameMap1;
    private Map<String, String> nameMap2;
    private Map<String, Integer> parameterMap;
    private SingleVariableDeclaration adapterVariable;
    private Set<String> adapterTypes;
    private Map<ClassInstanceCreation, Type> instanceCreationTypeMap;
    private int clazzCount;
    private int typeCount;
    private int actionCount;
    private int variableCount;

    public RFTemplate(AST ast) {
        init(ast, DEFAULT_TEMPLATE_NAME, DEFAULT_ADAPTER_TYPE_NAME, DEFAULT_ADAPTER_IMPL_NAME_PAIR);
    }

    public RFTemplate(AST ast, String templateName, String adapterName, String[] adapterImplNamePair) {
        assert adapterImplNamePair.length == 2;
        init(ast, templateName, adapterName, adapterImplNamePair);
    }

    private void init(AST ast, String templateName, String adapterName, String[] adapterImplNamePair) {
        this.ast = ast;
        this.typeMap = new HashMap<>();
        this.genericTypeMap = new HashMap<>();
        this.methodInvocationMap = new HashMap<>();
        this.clazzInstanceMap = new HashMap<>();
        this.nameMap1 = new HashMap<>();
        this.nameMap2 = new HashMap<>();
        this.parameterMap = new HashMap<>();
        this.adapterTypes = new HashSet<>();
        this.instanceCreationTypeMap = new HashMap<>();
        this.clazzCount = 1;
        this.typeCount = 1;
        this.actionCount = 1;
        this.variableCount = 1;
        initTemplate(templateName);
        initAdapter(adapterName);
        initAdapterImpl(adapterImplNamePair[0], adapterImplNamePair[1]);
    }

    private void initTemplate(String templateName) {
        templateMethod = ast.newMethodDeclaration();
        Modifier privateModifier = ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
        templateMethod.modifiers().add(privateModifier);
        templateMethod.setBody(ast.newBlock());
        templateMethod.setName(ast.newSimpleName(templateName));
    }

    private void initAdapter(String adapterName) {
        // init Adapter interface
        adapter = ast.newTypeDeclaration();
        Modifier publicModifier = ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
        adapter.modifiers().add(publicModifier);
        adapter.setInterface(true);
        adapter.setName(ast.newSimpleName(adapterName));

        // init adapter variable
        this.adapterVariable = ast.newSingleVariableDeclaration();
        Type type = ast.newSimpleType((SimpleName) ASTNode.copySubtree(ast, adapter.getName()));
        adapterVariable.setType(type);
        adapterVariable.setName(ast.newSimpleName(DEFAULT_ADAPTER_VARIABLE_NAME));

    }

    private void initAdapterImpl(String adapterImplName1, String adapterImplName2) {
        // init Adapter impl class
        adapterImpl1 = ast.newTypeDeclaration();
        adapterImpl2 = ast.newTypeDeclaration();
        Modifier publicModifier1 = ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
        Modifier publicModifier2 = ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
        adapterImpl1.modifiers().add(publicModifier1);
        adapterImpl2.modifiers().add(publicModifier2);
        adapterImpl1.setName(ast.newSimpleName(adapterImplName1));
        adapterImpl2.setName(ast.newSimpleName(adapterImplName2));

        Type interfaceType = ast.newSimpleType(ast.newSimpleName(adapter.getName().getIdentifier()));
        ParameterizedType parameterizedType = ast.newParameterizedType((Type) ASTNode.copySubtree(ast, interfaceType));
        adapterImpl1.superInterfaceTypes().add(ASTNode.copySubtree(ast, parameterizedType));
        adapterImpl2.superInterfaceTypes().add(ASTNode.copySubtree(ast, parameterizedType));
    }

    public AST getAst() {
        return ast;
    }

    public void setModifier(Modifier.ModifierKeyword modifier) {
        templateMethod.modifiers().add(ast.newModifier(modifier));
    }

    public boolean containsTypePair(TypePair typePair) {
        return typeMap.containsKey(typePair);
    }

    public boolean containsGenericType(String type) {
        return clazzInstanceMap.containsKey(type);
    }

    public String getClazz(String type) {
        return clazzInstanceMap.get(type);
    }

    public String getGenericTypeName(TypePair typePair) {
        return typeMap.get(typePair);
    }

    public Type getTypeByInstanceCreation(ClassInstanceCreation instanceCreation) {
        return this.instanceCreationTypeMap.get(instanceCreation);
    }

    public void addStatement(Statement statement) {
        templateMethod.getBody().statements().add(statement);
    }

    public void addInstanceCreation(ClassInstanceCreation instanceCreation, Type type) {
        if (instanceCreation != null) {
            this.instanceCreationTypeMap.put(instanceCreation, type);
        }
    }

    public String resolveTypePair(TypePair typePair, boolean extendsCommonSuperClass) {
        if (!typeMap.containsKey(typePair)) {
            String typeName = TYPE_NAME_PREFIX + typeCount++;
            typeMap.put(typePair, typeName);
            addGenericType(typeName);

            // add common generic type bound
            if (extendsCommonSuperClass) {
                genericTypeMap.put(typeName, typePair);
                ITypeBinding commonSuperClass = getLowestCommonSubClass(typePair);
                if (commonSuperClass != null) {
                    addGenericTypeBound(typeName, commonSuperClass.getName());
                }
            }
        }
        return typeMap.get(typePair);
    }

    public ITypeBinding getLowestCommonSubClass(TypePair typePair) {
        ITypeBinding p1 = typePair.getType1();
        ITypeBinding p2 = typePair.getType2();

        while(p1 != null || p2 != null) {
            if (p1 != null && p2 != null && p1.getQualifiedName().equals(p2.getQualifiedName())) {
                return p1;

            } else {
                if (p1 == null) {
                    p1 = typePair.getType2();
                } else {
                    p1 = p1.getSuperclass();
                }

                if (p2 == null) {
                    p2 = typePair.getType1();
                } else {
                    p2 = p2.getSuperclass();
                }
            }
        }

        return null;
    }

    public boolean containsVariableNamePair(String name1, String name2) {
        String resolvedName1 = nameMap1.getOrDefault(name1, "");
        String resolvedName2 = nameMap2.getOrDefault(name2, "");
        assert resolvedName1.equals(resolvedName2);
        return !resolvedName1.equals("");
    }

    public String resolveVariableName(String name1, String name2, String prefix) {
        String resolvedName1 = nameMap1.getOrDefault(name1, "");
        String resolvedName2 = nameMap2.getOrDefault(name2, "");
        assert resolvedName1.equals(resolvedName2);

        if (resolvedName1.equals("")) {
            String commonName = RenameUtil.renameVariable(name1, name2, variableCount++, prefix);
            nameMap1.put(name1, commonName);
            nameMap2.put(name2, commonName);
            return commonName;

        } else {
            return resolvedName1;
        }
    }

    public String resolveGenericType(String genericType) {
        if (!clazzInstanceMap.containsKey(genericType)) {
            String clazzName = CLAZZ_NAME_PREFIX + clazzCount++;
            clazzInstanceMap.put(genericType, clazzName);
            addClazzInParameter(genericType);
        }
        return clazzInstanceMap.get(genericType);
    }

    private Type resolveAdapterActionArgumentType(Expression expr) {
        Type exprType = (Type) expr.getProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING);
        if (exprType != null) {
            if (typeMap.values().contains(exprType.toString())
                    && !adapterTypes.contains(exprType.toString())) {
                TypeParameter typeParameter = ast.newTypeParameter();
                typeParameter.setName(ast.newSimpleName(exprType.toString()));
                adapter.typeParameters().add(typeParameter);
                addAdapterVariableTypeParameter(exprType);
                adapterTypes.add(exprType.toString());
            }
            return exprType;

        } else {
            return ASTNodeUtil.typeFromBinding(ast, expr.resolveTypeBinding());
        }
    }

    public void addGenericTypeBound(String fromType, String toType) {
        List<TypeParameter> typeParameters = templateMethod.typeParameters();
        for (TypeParameter typeParameter: typeParameters) {
            if (typeParameter.getName().getIdentifier().equals(fromType)) {
                typeParameter.typeBounds().add(ast.newSimpleType(ast.newSimpleName(toType)));
                return;
            }
        }
    }

    private void addThrowsException() {
        if (templateMethod.thrownExceptionTypes().size() == 0) {
            templateMethod.thrownExceptionTypes().add(ast.newSimpleType(ast.newSimpleName(EXCEPTION_NAME)));
        }
    }

    private void addGenericType(String genericTypeName) {
        TypeParameter typeParameter = ast.newTypeParameter();
        typeParameter.setName(ast.newSimpleName(genericTypeName));
        templateMethod.typeParameters().add(typeParameter);
    }

    private void addClazzInParameter(String genericTypeName) {
        Type genericType = ast.newSimpleType(ast.newSimpleName(genericTypeName));
        Type classType = ast.newSimpleType(ast.newSimpleName(CLASS_NAME));
        ParameterizedType classTypeWithGenericType = ast.newParameterizedType(classType);
        classTypeWithGenericType.typeArguments().add(genericType);

        SimpleName clazzName = ast.newSimpleName(resolveGenericType(genericTypeName));
        addVariableParameter(classTypeWithGenericType, clazzName);

        addThrowsException();
    }

    public String addVariableParameter(Type type) {
        int count = parameterMap.getOrDefault(type.toString(), 0) + 1;
        parameterMap.put(type.toString(), count);
        String variableParameter = RenameUtil.rename(type, count);
        addVariableParameter(type, ast.newSimpleName(variableParameter));
        return variableParameter;
    }

    private void addVariableParameter(Type type, SimpleName name) {
        addVariableParameter(type, name, templateMethod.parameters().size());
    }

    private void addVariableParameter(Type type, SimpleName name, int index) {
        SingleVariableDeclaration variableParameter = ast.newSingleVariableDeclaration();
        variableParameter.setType(type);
        variableParameter.setName(name);
        templateMethod.parameters().add(index, variableParameter);
    }

    private void addAdapterVariableTypeParameter(Type type) {
        Type adapterType = adapterVariable.getType();
        if (adapterType.isSimpleType()) {
            ParameterizedType parameterizedType = ast.newParameterizedType((Type) ASTNode.copySubtree(ast, adapterType));
            parameterizedType.typeArguments().add(ASTNode.copySubtree(ast, type));
            adapterVariable.setType(parameterizedType);

        } else if (adapterType.isParameterizedType()) {
            ((ParameterizedType) adapterType).typeArguments().add(ASTNode.copySubtree(ast, type));
        } else {
            throw new IllegalStateException("unexpected adapter type");
        }
    }

    private void addMethodInAdapterInterface(SimpleName name, List<Type> argTypes, Type returnType) {

        MethodDeclaration methodDeclaration = ast.newMethodDeclaration();

        // set return type
        methodDeclaration.setReturnType2((Type) ASTNode.copySubtree(ast, returnType));

        // set interface action name
        methodDeclaration.setName((SimpleName) ASTNode.copySubtree(ast, name));

        Map<String, Integer> argMap = new HashMap<>();
        for (Type argType : argTypes) {

            SingleVariableDeclaration arg = ast.newSingleVariableDeclaration();

            // set arg type
            arg.setType((Type) ASTNode.copySubtree(ast, argType));

            // set arg name
            String argName = argType.toString().toLowerCase();
            int argCount = argMap.getOrDefault(argName, 1);
            argMap.put(argName, argCount + 1);
            arg.setName(ast.newSimpleName(RenameUtil.rename(argType, argCount)));

            // add parameter
            methodDeclaration.parameters().add(arg);
        }

        adapter.bodyDeclarations().add(methodDeclaration);
    }

    private MethodDeclaration addMethodInAdapterImpl(SimpleName actionName, List<Type> argTypes, Expression expr,
                                                    SimpleName name, List<Expression> arguments, Type returnType) {

        MethodDeclaration method = ast.newMethodDeclaration();
        method.setBody(ast.newBlock());

        // set return type
        method.setReturnType2((Type) ASTNode.copySubtree(ast, returnType));

        // set interface action name
        method.setName((SimpleName) ASTNode.copySubtree(ast, actionName));

        // set modifier
        method.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

        // create method invocation
        MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName((SimpleName) ASTNode.copySubtree(ast, name));
        Map<String, Integer> argMap = new HashMap<>();

        assert argTypes.size() == arguments.size() + 1;

        for (int i = 0; i < argTypes.size(); i++) {
            if (i == 0) {

            } else {

            }
        }

        // add expressionStmt to method
        ExpressionStatement expressionStmt = ast.newExpressionStatement(methodInvocation);
        method.getBody().statements().add(expressionStmt);

        return method;
    }

    private void addAdapterActionImpl(SimpleName actionName, List<Type> argTypes, MethodInvocationPair pair, Type returnType) {

        /*
        MethodDeclaration method1 = ast.newMethodDeclaration();
        method1.setBody(ast.newBlock());

        // set return type
        method1.setReturnType2((Type) ASTNode.copySubtree(ast, returnType));

        // set interface action name
        method1.setName((SimpleName) ASTNode.copySubtree(ast, actionName));

        // set modifier
        method1.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

        // create method invocation
        MethodInvocation methodInvocation1 = ast.newMethodInvocation();
        methodInvocation1.setName((SimpleName) ASTNode.copySubtree(ast, pair.getName1()));
        Map<String, Integer> argMap = new HashMap<>();

        // set method expr variable declaration type
        SingleVariableDeclaration exprVariableDeclaration = ast.newSingleVariableDeclaration();
        String exprName = argTypes.get(0).toString();
        if (genericTypeMap.containsKey(exprName)) {
            TypePair typePair = genericTypeMap.get(exprName);
            Type exprType1 = ASTNodeUtil.typeFromBinding(ast, typePair.getType1());
            exprVariableDeclaration.setType((Type) ASTNode.copySubtree(ast, exprType1));

        } else {
            exprVariableDeclaration.setType((Type) ASTNode.copySubtree(ast, argTypes.get(0)));
        }

        // set method expr variable name
        SimpleName exprSimpleName;
        if (pair.getExpr1() instanceof SimpleName) {
            exprSimpleName = (SimpleName) pair.getExpr1();

        } else {
            String argName = argTypes.get(0).toString().toLowerCase();
            int argCount = argMap.getOrDefault(argName, 1);
            exprSimpleName = ast.newSimpleName(RenameUtil.rename(argTypes.get(0), argCount));
        }
        exprVariableDeclaration.setName((SimpleName) ASTNode.copySubtree(ast, exprSimpleName));
        methodInvocation1.setName((SimpleName) ASTNode.copySubtree(ast, exprSimpleName));

        ExpressionStatement expressionStmt = ast.newExpressionStatement(methodInvocation1);
        method1.getBody().statements().add(expressionStmt);
        */

        MethodDeclaration method1 = addMethodInAdapterImpl(actionName, argTypes, pair.getExpr1(), pair.getName1(),
                pair.getArgument1(), returnType);
        adapterImpl1.bodyDeclarations().add(method1);
    }

    private void addAdapterVariableParameter() {
        if (adapter.bodyDeclarations().size() == 0) {
            templateMethod.parameters().add(0, adapterVariable);
        }
    }

    public MethodInvocation createAdapterActionMethod(Expression expr, List<Expression> arguments,
                                                      MethodInvocationPair pair, Type returnType) {

        addAdapterVariableParameter();

        // create new method invocation
        MethodInvocation newMethod = ast.newMethodInvocation();
        newMethod.setExpression(ast.newSimpleName(adapterVariable.getName().getIdentifier()));

        List<Expression> newArgs = newMethod.arguments();
        List<Type> argTypes = new ArrayList<>();

        // copy and resolve method expr
        newArgs.add((Expression) ASTNode.copySubtree(ast, expr));
        argTypes.add(resolveAdapterActionArgumentType(expr));

        // copy and resolve arguments
        for (Expression argument : arguments) {
            newArgs.add((Expression) ASTNode.copySubtree(ast, argument));
            argTypes.add(resolveAdapterActionArgumentType(argument));
        }

        // add method in adapter interface
        if (methodInvocationMap.containsKey(pair)) {
            newMethod.setName(ast.newSimpleName(methodInvocationMap.get(pair)));
        } else {

            // set adapter action name
            String newActionName = DEFAULT_ADAPTER_METHOD_NAME + actionCount++;
            newMethod.setName(ast.newSimpleName(newActionName));

            // add method in adapter interface
            addMethodInAdapterInterface(newMethod.getName(), argTypes, returnType);
            newMethod.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, ASTNode.copySubtree(ast, returnType));
            methodInvocationMap.put(pair, newActionName);

            // create adapter action impl
            addAdapterActionImpl(newMethod.getName(), argTypes, pair, returnType);
        }

        return newMethod;
    }

    public MethodInvocation createAdapterActionMethod(Expression e1, Expression e2, Type returnType) {

        addAdapterVariableParameter();

        MethodInvocation newMethod = ast.newMethodInvocation();
        newMethod.setExpression(ast.newSimpleName(adapterVariable.getName().getIdentifier()));

        String newActionName = DEFAULT_ADAPTER_METHOD_NAME + actionCount++;
        newMethod.setName(ast.newSimpleName(newActionName));

        List<Expression> newArgs = newMethod.arguments();
        List<Type> argTypes = new ArrayList<>();
        newArgs.add((Expression) ASTNode.copySubtree(ast, e1));
        newArgs.add((Expression) ASTNode.copySubtree(ast, e2));
        argTypes.add(resolveAdapterActionArgumentType(e1));
        argTypes.add(resolveAdapterActionArgumentType(e2));

        addMethodInAdapterInterface(newMethod.getName(), argTypes, returnType);

        return newMethod;
    }

    public MethodInvocation createAdapterActionMethod(Type returnType) {

        addAdapterVariableParameter();

        MethodInvocation newMethod = ast.newMethodInvocation();
        newMethod.setExpression(ast.newSimpleName(adapterVariable.getName().getIdentifier()));

        String newActionName = DEFAULT_ADAPTER_METHOD_NAME + actionCount++;
        newMethod.setName(ast.newSimpleName(newActionName));
        addMethodInAdapterInterface(newMethod.getName(), new ArrayList<>(), returnType);

        return newMethod;
    }

    @Override
    public String toString() {
        return templateMethod.toString() + "\n" + adapter.toString() + "\n"
                + adapterImpl1.toString() + "\n" + adapterImpl2.toString();
    }

}
