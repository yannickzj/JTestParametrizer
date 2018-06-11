package ca.uwaterloo.eclipse.refactoring.rf.dom;

import ca.uwaterloo.eclipse.refactoring.rf.visitor.RFVisitor;
import ca.uwaterloo.eclipse.refactoring.utility.FileLogger;
import gr.uom.java.ast.decomposition.StatementType;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;

public class RFVariableDeclarationStatement extends RFStatement {

    private static Logger log = FileLogger.getLogger(RFVariableDeclarationStatement.class);

    public RFVariableDeclarationStatement(
            StatementType statementType,
            Statement statement1,
            Statement statement2,
            java.util.List<RFNodeDifference> mapping) {
        super(statementType, statement1, statement2, mapping);
    }

    void accept0(RFVisitor visitor) {
        boolean visitChildren = visitor.visit(this);
        if (visitChildren) {
            // visit children
            log.info("doing nothing for RFVariableDeclarationStatement");
        }
        visitor.endVisit(this);
    }
}
