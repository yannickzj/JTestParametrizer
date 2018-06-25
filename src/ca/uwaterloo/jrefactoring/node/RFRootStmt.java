package ca.uwaterloo.jrefactoring.node;

import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.visitor.RFVisitor;

public class RFRootStmt extends RFStatement {

    public RFRootStmt(RFTemplate template) {
        super(template);
    }

    @Override
    void accept0(RFVisitor visitor) {
        // visit children
        for (RFStatement child : children) {
            child.accept(visitor);
        }
        visitor.endVisit(this);
    }

}
