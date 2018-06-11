package ca.uwaterloo.eclipse.refactoring.rf.visitor;

import ca.uwaterloo.eclipse.refactoring.rf.dom.RFExpressionStatement;
import ca.uwaterloo.eclipse.refactoring.rf.dom.RFVariableDeclarationStatement;

public abstract class RFVisitor {

    public RFVisitor() {}

    public boolean visit(RFVariableDeclarationStatement statement) {
        return true;
    }

    public boolean visit(RFExpressionStatement statement) {
        return true;
    }

}
