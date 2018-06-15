package ca.uwaterloo.eclipse.refactoring.rf.visitor;

import ca.uwaterloo.eclipse.refactoring.rf.node.RFStatement;

public class IfStmtVisitor extends RFVisitor {
    public IfStmtVisitor() {
    }

    @Override
    public boolean visit(RFStatement node) {
        //node.describe();
        //System.out.println("ifStmtVisitor finish visiting");
        return true;
    }
}
