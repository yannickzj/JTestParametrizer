package ca.uwaterloo.jrefactoring.template;

import org.eclipse.jdt.core.dom.*;

public class RFTemplate {

    private AST ast;
    private GenericManager genericManager;
    private MethodDeclaration templateMethod;
    private int clazzCount;

    public RFTemplate(int apilevel, GenericManager genericManager) {
        ast = AST.newAST(apilevel);
        this.genericManager = genericManager;
        clazzCount = 1;
        init();
    }

    public GenericManager getGenericManager() {
        return genericManager;
    }

    public AST getAst() {
        return ast;
    }

    private void init() {
        templateMethod = ast.newMethodDeclaration();
        Modifier privateModifier = ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
        templateMethod.modifiers().add(privateModifier);
        templateMethod.setBody(ast.newBlock());
        templateMethod.setName(ast.newSimpleName("template1"));
    }

    public void addStatement(Statement statement) {
        templateMethod.getBody().statements().add(statement);
    }

    public void addTypeParameter(String typeName) {
        TypeParameter typeParameter = ast.newTypeParameter();
        typeParameter.setName(ast.newSimpleName(typeName));
        templateMethod.typeParameters().add(typeParameter);
    }

    public void setModifier(Modifier.ModifierKeyword modifier) {
        templateMethod.modifiers().add(ast.newModifier(modifier));
    }

    public void addClassParameter(String typeName) {
        Type type = ast.newSimpleType(ast.newSimpleName(typeName));
        Type classType = ast.newSimpleType(ast.newSimpleName("Class"));
        ParameterizedType parameterizedType = ast.newParameterizedType(classType);
        parameterizedType.typeArguments().add(type);

        SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
        //parameter.setVarargs(true);
        parameter.setType(parameterizedType);
        SimpleName parameterName = ast.newSimpleName("clazz" + clazzCount++);
        parameter.setName(parameterName);
        genericManager.getClazzInstanceMap().put(typeName, parameterName.getIdentifier());
        templateMethod.parameters().add(parameter);
    }

    @Override
    public String toString() {
        return templateMethod.toString();
    }

}
