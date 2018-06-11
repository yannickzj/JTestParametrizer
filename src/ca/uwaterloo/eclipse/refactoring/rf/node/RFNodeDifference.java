package ca.uwaterloo.eclipse.refactoring.rf.node;

import ca.uwaterloo.eclipse.refactoring.rf.visitor.RFVisitor;
import ca.uwaterloo.eclipse.refactoring.utility.FileLogger;
import gr.uom.java.ast.decomposition.matching.Difference;
import org.eclipse.jdt.core.dom.Expression;
import org.slf4j.Logger;

import java.util.List;

public class RFNodeDifference extends RFEntity {

    private static Logger log = FileLogger.getLogger(RFNodeDifference.class);

    private final Expression expr1;
    private final Expression expr2;
    private final List<Difference> differences;

    public RFNodeDifference(Expression expr1, Expression expr2, List<Difference> differences) {
        this.expr1 = expr1;
        this.expr2 = expr2;
        this.differences = differences;
    }

    public Expression getExpr1() {
        return expr1;
    }

    public Expression getExpr2() {
        return expr2;
    }

    public List<Difference> getDifferences() {
        return differences;
    }

    void accept0(RFVisitor visitor) {
        boolean visitChildren = visitor.visit(this);
        if (visitChildren) {
            // visit children
            log.info("doing nothing for RFVariableDeclarationStatement");
        }
        visitor.endVisit(this);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Difference diff: differences) {
            sb.append(diff.getType().name());
            sb.append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        return expr1.toString() + "  <--->  " + expr2.toString() + "  (" + sb.toString() +  ")";
    }
}
