package ca.uwaterloo.refactoring.visitor;

import ca.uwaterloo.refactoring.node.RFStatement;

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
