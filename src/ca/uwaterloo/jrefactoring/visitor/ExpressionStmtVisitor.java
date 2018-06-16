package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFStatement;

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
