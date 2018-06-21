package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import org.eclipse.jdt.core.dom.ExpressionStatement;

public class ExpressionStmtVisitor extends RFVisitor {
    public ExpressionStmtVisitor(RFTemplate template) {
        super(template);
    }

    @Override
    public boolean visit(RFStatement node) {
        if (node.hasDifference()) {
            node.describe();
            ExpressionStatement expressionStatement = (ExpressionStatement) node.getStatement1();
            expressionStatement.getExpression().accept(this);
            System.out.println("expressionStmtVisitor finish visiting");
        }
        return true;
    }
}
