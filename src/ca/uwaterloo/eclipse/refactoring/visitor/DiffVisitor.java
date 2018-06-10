package ca.uwaterloo.eclipse.refactoring.visitor;

import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.Set;

public class DiffVisitor extends ASTVisitor {

    private Set<ASTNode> visited = new HashSet<>();

    public boolean visit(SimpleName node) {
        if (!visited.contains(node)) {
            System.out.println("visiting SimpleName: " + node);
            visited.add(node);
            node.getParent().accept(this);
            return true;
        } else {
            System.out.println("SimpleName visited: " + node);
            return false;
        }
    }

    public boolean visit(SimpleType node) {
        System.out.println("visiting SimpleType: " + node);
        //node.getParent().accept(this);
        return true;
    }

    public boolean visit(TypeLiteral node) {
        System.out.println("visiting TypeLiteral: " + node);
        return true;
    }

    public boolean visit(VariableDeclarationFragment node) {
        System.out.println("visiting VariableDeclarationFragment: " + node);
        return true;
    }

    public boolean visit(VariableDeclarationStatement node) {
        System.out.println("visiting VariableDeclarationStatement: " + node);
        return true;
    }

    public boolean visit(VariableDeclarationExpression node) {
        System.out.println("visiting VariableDeclarationExpression: " + node);
        return true;
    }

}
