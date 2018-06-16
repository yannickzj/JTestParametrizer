package ca.uwaterloo.refactoring.node;

import ca.uwaterloo.refactoring.template.RFTemplate;
import ca.uwaterloo.refactoring.utility.DiffUtil;
import ca.uwaterloo.refactoring.visitor.RFVisitor;
import ca.uwaterloo.refactoring.utility.FileLogger;
import gr.uom.java.ast.decomposition.matching.Difference;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import org.eclipse.jdt.core.dom.Expression;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RFNodeDifference extends RFEntity {

    private static Logger log = FileLogger.getLogger(RFNodeDifference.class);

    private final Expression expr1;
    private final Expression expr2;
    private final List<Difference> differences;
    private RFStatement rfStatement;

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

    public RFStatement getRfStatement() {
        return rfStatement;
    }

    public void setRfStatement(RFStatement rfStatement) {
        this.rfStatement = rfStatement;
    }

    public RFTemplate getTemplate() {
        return rfStatement.getTemplate();
    }

    void accept0(RFVisitor visitor) {
        boolean visitChildren = visitor.visit(this);
        if (visitChildren) {
            // visit children
            log.info("doing nothing for RFVariableDeclarationStatement");
        }
        visitor.endVisit(this);
    }

    public Set<DifferenceType> getDifferenceTypes() {
        Set<DifferenceType> differenceTypes = new HashSet<>();
        for (Difference diff: differences) {
            differenceTypes.add(diff.getType());
        }
        return differenceTypes;
    }

    @Override
    public String toString() {
        return DiffUtil.displayNodeDiff(this);
    }
}
