package ca.uwaterloo.jrefactoring.node;

import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.visitor.RFVisitor;
import gr.uom.java.ast.decomposition.StatementType;
import org.eclipse.jdt.core.dom.Statement;

import java.util.List;

public class RFThrowStmt extends RFStatement {

    public RFThrowStmt(StatementType statementType, Statement statement1, Statement statement2, List<RFNodeDifference> nodeDifferences, RFTemplate template) {
        super(statementType, statement1, statement2, nodeDifferences, template);
    }

    @Override
    void accept0(RFVisitor visitor) {
        boolean visitChildren = visitor.visit(this);
        if (visitChildren) {
            // visit children
            for (RFStatement child : children) {
                child.accept(visitor);
            }
        }
        visitor.endVisit(this);
    }
}
