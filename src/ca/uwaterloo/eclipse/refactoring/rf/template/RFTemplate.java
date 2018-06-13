package ca.uwaterloo.eclipse.refactoring.rf.template;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;

public class RFTemplate {

    private AST ast;
    private GenericManager genericManager;
    private MethodDeclaration methodDeclaration;

    public RFTemplate(int apilevel, GenericManager genericManager) {
        ast = AST.newAST(apilevel);
        this.genericManager = genericManager;
        init();
    }

    public GenericManager getGenericManager() {
        return genericManager;
    }

    public AST getAst() {
        return ast;
    }

    private void init() {
        methodDeclaration = ast.newMethodDeclaration();
        methodDeclaration.setBody(ast.newBlock());
        methodDeclaration.setName(ast.newSimpleName("template1"));
    }

    public void addStatement(Statement statement) {
        methodDeclaration.getBody().statements().add(statement);
    }

    @Override
    public String toString() {
        return methodDeclaration.toString();
    }

}
