package ca.uwaterloo.eclipse.refactoring.rf.visitor;

import ca.uwaterloo.eclipse.refactoring.rf.dom.RFExpressionStatement;
import ca.uwaterloo.eclipse.refactoring.rf.dom.RFIfStatement;
import ca.uwaterloo.eclipse.refactoring.rf.dom.RFStatement;
import ca.uwaterloo.eclipse.refactoring.rf.dom.RFVariableDeclarationStatement;
import ca.uwaterloo.eclipse.refactoring.utility.FileLogger;
import org.slf4j.Logger;

public abstract class RFVisitor extends DefaultVisitor {

    private static Logger log = FileLogger.getLogger(RFVisitor.class);

    public RFVisitor() {}

    public void preVisit(RFStatement node) {
        // default implementation: do nothing
    }

    public boolean preVisit2(RFStatement node) {
        preVisit(node);
        return true;
    }

    public void postVisit(RFStatement node) {
        // default implementation: do nothing
    }

    public boolean visit(RFExpressionStatement node) {
        log.info("visiting RFExpressionStatement");
        return false;
    }

    public boolean visit(RFIfStatement node) {
        log.info("visiting RFIfStatement");
        return false;
    }

    public boolean visit(RFVariableDeclarationStatement node) {
        log.info("visiting RFVariableDeclarationStatement");
        return false;
    }

    public boolean endVisit(RFExpressionStatement node) {
        log.info("finish visiting RFExpressionStatement");
        return false;
    }

    public boolean endVisit(RFIfStatement node) {
        log.info("finish visiting RFIfStatement");
        return false;
    }

    public boolean endVisit(RFVariableDeclarationStatement node) {
        log.info("finish visiting RFVariableDeclarationStatement");
        return false;
    }

}
