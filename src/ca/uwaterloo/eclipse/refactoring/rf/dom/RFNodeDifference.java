package ca.uwaterloo.eclipse.refactoring.rf.dom;

import org.eclipse.jdt.core.dom.Expression;

public class RFNodeDifference {

    private final Expression expr1;
    private final Expression expr2;

    public RFNodeDifference(Expression expr1, Expression expr2) {
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    public Expression getExpr1() {
        return expr1;
    }

    public Expression getExpr2() {
        return expr2;
    }

    @Override
    public String toString() {
        return expr1.toString() + "  <--->  " + expr2.toString();

    }
}
