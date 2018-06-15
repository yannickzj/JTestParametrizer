package ca.uwaterloo.eclipse.refactoring.rf.visitor;

import ca.uwaterloo.eclipse.refactoring.rf.node.RFStatement;

public class ExpressionStmtVisitor extends RFVisitor {
    public ExpressionStmtVisitor() {
    }

    @Override
    public boolean visit(RFStatement node) {
        //node.describe();
        //System.out.println("expressionStmtVisitor finish visiting");
        return true;
    }
}
