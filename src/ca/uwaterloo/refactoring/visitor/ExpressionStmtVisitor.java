package ca.uwaterloo.refactoring.visitor;

import ca.uwaterloo.refactoring.node.RFStatement;

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
