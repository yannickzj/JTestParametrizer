package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;

public class ExpressionStmtVisitor extends RFVisitor {
    public ExpressionStmtVisitor() {
    }

    @Override
    public boolean visit(RFStatement node) {
        if (node.hasDifference()) {
            node.describe();
            /*
            for (RFNodeDifference diff : node.getNodeDifferences()) {
                diff.accept(this);
            }
            */
            ExpressionStatement expressionStatement = (ExpressionStatement) node.getStatement1();
            expressionStatement.getExpression().accept(this);
            System.out.println("expressionStmtVisitor finish visiting");
        }
        return true;
    }
}
