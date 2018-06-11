package ca.uwaterloo.eclipse.refactoring.rf.build;

import ca.uwaterloo.eclipse.refactoring.rf.node.*;
import gr.uom.java.ast.decomposition.StatementType;
import gr.uom.java.ast.decomposition.cfg.mapping.*;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.Difference;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

import java.util.ArrayList;
import java.util.List;

public class RFStatementBuilder {

    private static RFStatementBuilder builder;

    private RFStatementBuilder() {}

    public static RFStatementBuilder getInstance() {
        if (builder == null) {
            builder = new RFStatementBuilder();
        }
        return builder;
    }

    public RFStatement build(NodeMapping nodeMapping) {

        if (nodeMapping instanceof PDGNodeMapping) {
            return buildFromPDGNodeMapping(nodeMapping);

        } else if (nodeMapping instanceof PDGElseMapping) {
            System.out.println("PDG else mapping");
            return null;

        } else if (nodeMapping instanceof PDGNodeGap) {
            System.out.println("PDG node gap");
            return null;

        } else if (nodeMapping instanceof PDGElseGap) {
            System.out.println("PDG else gap");
            return null;

        } else {
            System.out.println("unknown nodeMapping");
            return null;
        }
    }

    private RFStatement buildFromPDGNodeMapping(NodeMapping nodeMapping) {
        List<RFNodeDifference> mapping = new ArrayList<>();
        for (ASTNodeDifference astNodeDifference: nodeMapping.getNodeDifferences()) {
            Expression expr1 = astNodeDifference.getExpression1().getExpression();
            Expression expr2 = astNodeDifference.getExpression2().getExpression();
            List<Difference> differences = astNodeDifference.getDifferences();
            mapping.add(new RFNodeDifference(expr1, expr2, differences));
        }

        Statement statement1 = nodeMapping.getNodeG1().getASTStatement();
        Statement statement2 = nodeMapping.getNodeG2().getASTStatement();

        StatementType statementType = nodeMapping.getNodeG1().getStatement().getType();

        return new RFStatement(statementType, statement1, statement2, mapping);
    }

}
