package ca.uwaterloo.eclipse.refactoring.rf.visitor;

import org.eclipse.jdt.core.dom.*;

public class DiffVisitor extends ASTVisitor {

    @Override
    public boolean visit(SimpleName node) {
        System.out.println("visiting SimpleName: " + node);
        node.getParent().accept(this);
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        System.out.println("visiting SimpleType: " + node);
        node.getParent().accept(this);
        //System.out.println("Simple Type [" + node + "]'s parent node type: " + node.getParent().getNodeType());
        return false;
    }

    @Override
    public boolean visit(TypeLiteral node) {
        System.out.println("visiting TypeLiteral: " + node);
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        System.out.println("visiting VariableDeclarationFragment: " + node);
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        System.out.println("visiting VariableDeclarationStatement: " + node);
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationExpression node) {
        System.out.println("visiting VariableDeclarationExpression: " + node);
        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        System.out.println("visiting QualifiedName: " + node);
        return false;
    }

    @Override
    public boolean visit(QualifiedType node) {
        System.out.println("visiting QualifiedType: " + node);
        return false;
    }

    @Override
    public boolean visit(Assignment node) {
        System.out.println("visiting Assignment: " + node);
        return false;
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        System.out.println("visiting ExpressionStatement: " + node);
        return false;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        System.out.println("visiting MethodInvocation: " + node);
        return false;
    }

    @Override
    public boolean visit(InfixExpression node) {
        System.out.println("visiting InfixExpression: " + node);
        return false;
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        System.out.println("visiting ClassInstanceCreation: " + node);
        return false;
    }

    @Override
    public boolean visit(ParameterizedType node) {
        System.out.println("visiting ParameterizedType: " + node);
        return false;
    }
}
