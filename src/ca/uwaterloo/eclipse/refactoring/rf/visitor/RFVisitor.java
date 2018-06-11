package ca.uwaterloo.eclipse.refactoring.rf.visitor;

import ca.uwaterloo.eclipse.refactoring.rf.dom.RFEntity;
import ca.uwaterloo.eclipse.refactoring.rf.dom.RFNodeDifference;
import ca.uwaterloo.eclipse.refactoring.rf.dom.RFStatement;
import ca.uwaterloo.eclipse.refactoring.utility.FileLogger;
import org.slf4j.Logger;

public abstract class RFVisitor extends NonRecursiveASTNodeVisitor {

    private static Logger log = FileLogger.getLogger(RFVisitor.class);

    public RFVisitor() {}

    public void preVisit(RFEntity node) {
        // default implementation: do nothing
    }

    public boolean preVisit2(RFEntity node) {
        preVisit(node);
        return true;
    }

    public void postVisit(RFEntity node) {
        // default implementation: do nothing
    }

    public boolean visit(RFStatement node) {
        log.info("visiting RFStatement");
        return false;
    }

    public boolean visit(RFNodeDifference node) {
        return false;
    }

    public void endVisit(RFStatement node) {
        log.info("finish visiting RFStatement");
    }

    public void endVisit(RFNodeDifference node) {
    }

}
