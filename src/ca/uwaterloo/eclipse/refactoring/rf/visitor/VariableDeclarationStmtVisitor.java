package ca.uwaterloo.eclipse.refactoring.rf.visitor;

import ca.uwaterloo.eclipse.refactoring.rf.node.RFNodeDifference;
import ca.uwaterloo.eclipse.refactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.slf4j.Logger;

public class VariableDeclarationStmtVisitor extends RFVisitor {

    private static Logger log = FileLogger.getLogger(VariableDeclarationStmtVisitor.class);

    public VariableDeclarationStmtVisitor() {
    }

    @Override
    public boolean visit(RFNodeDifference diff) {
        Expression expr1 = diff.getExpr1();
        Expression expr2 = diff.getExpr2();

        expr1.accept(this);
        expr2.accept(this);

        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        System.out.println("SimpleName location [" + node + "] in parent: " + node.getLocationInParent());
        node.getParent().accept(this);
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        System.out.println("SimpleType location [" + node + "] in parent: " + node.getLocationInParent());
        node.getParent().accept(this);
        return false;
    }

}
