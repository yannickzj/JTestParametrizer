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
            node.describe();
            /*
            System.out.println("-----------------------------------------------------------");
            node.describeStatements();
            node.describeDifference();
            */
            for (RFNodeDifference diff : node.getNodeDifferences()) {
                diff.accept(this);
            }
            System.out.println("variableDeclarationStmtVisitor finish visiting");
        }
        return true;
    }
}
