package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.node.RFStatement;

public class ExpressionStmtVisitor extends RFVisitor {
    public ExpressionStmtVisitor() {
    }

    @Override
    public boolean visit(RFStatement node) {
        if (node.hasDifference()) {
            node.describe();
            for (RFNodeDifference diff : node.getNodeDifferences()) {
                diff.accept(this);
            }
            System.out.println("expressionStmtVisitor finish visiting");
        }
        return true;
    }
}
