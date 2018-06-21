package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.template.RFTemplate;

public class IfStmtVisitor extends RFVisitor {
    public IfStmtVisitor(RFTemplate template) {
        super(template);
    }

    @Override
    public boolean visit(RFStatement node) {
        //node.describe();
        //System.out.println("ifStmtVisitor finish visiting");
        return true;
    }
}
