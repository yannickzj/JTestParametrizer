package ca.uwaterloo.eclipse.refactoring.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeLiteral;

public class DiffVisitor extends ASTVisitor {

    public boolean visit(SimpleName node) {
        System.out.println("visiting SimpleName!");
        return true;
    }

    public boolean visit(SimpleType node) {
        System.out.println("visiting SimpleType!");
        return true;
    }

    public boolean visit(TypeLiteral node) {
        System.out.println("visiting TypeLiteral!");
        return true;
    }

}
