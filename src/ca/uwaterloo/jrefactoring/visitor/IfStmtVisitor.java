package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFStatement;

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
