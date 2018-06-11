package ca.uwaterloo.eclipse.refactoring.rf.build;

import ca.uwaterloo.eclipse.refactoring.rf.dom.*;
import gr.uom.java.ast.decomposition.StatementType;
import gr.uom.java.ast.decomposition.cfg.mapping.*;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
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
            mapping.add(new RFNodeDifference(expr1, expr2));
        }

        Statement statement1 = nodeMapping.getNodeG1().getASTStatement();
        Statement statement2 = nodeMapping.getNodeG2().getASTStatement();

        StatementType statementType = nodeMapping.getNodeG1().getStatement().getType();

        return createRFStatement(statementType, statement1, statement2, mapping);
    }

    private RFStatement createRFStatement(
            StatementType statementType,
            Statement statement1,
            Statement statement2,
            List<RFNodeDifference> mapping) {

        switch(statementType) {
            case VARIABLE_DECLARATION:
                return new RFVariableDeclarationStatement(statementType, statement1, statement2, mapping);
            case EXPRESSION:
                return new RFExpressionStatement(statementType, statement1, statement2, mapping);
            case IF:
                return new RFIfStatement(statementType, statement1, statement2, mapping);
            default:
                System.out.println("unknown statement type");
                return null;
        }
    }

}
