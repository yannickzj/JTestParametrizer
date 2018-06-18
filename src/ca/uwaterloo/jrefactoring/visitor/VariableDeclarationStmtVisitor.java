package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.utility.ContextUtil;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

public class VariableDeclarationStmtVisitor extends RFVisitor {

    private static Logger log = FileLogger.getLogger(VariableDeclarationStmtVisitor.class);

    public VariableDeclarationStmtVisitor() {
    }

    @Override
    public boolean visit(RFStatement node) {
        if (node.hasDifference()) {
            System.out.println("-----------------------------------------------------------");
            node.describeStatements();
            node.describeDifference();
            for (RFNodeDifference diff : node.getNodeDifferences()) {
                diff.accept(this);
            }
            System.out.println("variableDeclarationStmtVisitor finish visiting");
        }
        return true;
    }

    @Override
    public boolean visit(RFNodeDifference diff) {

        // validate node difference
        ContextUtil.validateNodeDiff(diff);

        // refactor node difference
        refactor(diff);

        System.out.println();
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        System.out.println("SimpleName location [" + node + "] in parent: " + node.getLocationInParent());
        //System.out.println("SimpleName: " + node);
        //System.out.println("Start position: " + node.getStartPosition());
        node.getParent().accept(this);
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        //System.out.println("SimpleType location [" + node + "] in parent: " + node.getLocationInParent());
        node.getParent().accept(this);
        return false;
    }

}
