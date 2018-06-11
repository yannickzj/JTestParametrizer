package ca.uwaterloo.eclipse.refactoring.rf.visitor;

import ca.uwaterloo.eclipse.refactoring.rf.dom.RFNodeDifference;
import ca.uwaterloo.eclipse.refactoring.rf.dom.RFStatement;
import ca.uwaterloo.eclipse.refactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.slf4j.Logger;

public class TestVisitor extends RFVisitor {

    private static Logger log = FileLogger.getLogger(TestVisitor.class);

    public TestVisitor() {}

    @Override
    public boolean visit(RFStatement node) {
        node.describe();

        System.out.println();
        log.info("start to navigate the difference");

        RFVisitor visitor = node.selectVisitor();
        int i = 0;
        for (RFNodeDifference diff: node.getMapping()) {
            System.out.println();
            log.info("difference " + ++i);
            diff.accept(visitor);
        }
        System.out.println();
        log.info("finish navigating the difference");
        System.out.println();

        return false;
    }

    @Override
    public void endVisit(RFStatement node) {
        log.info("finish visiting RFVariableDeclarationStatement");
    }

    @Override
    public boolean visit(SimpleName node) {
        System.out.println("visiting SimpleName: " + node);
        node.getParent().accept(this);
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        System.out.println("visiting SimpleType: " + node);
        node.getParent().accept(this);
        //System.out.println("Simple Type [" + node + "]'s parent node type: " + node.getParent().getNodeType());
        return false;
    }

}
