package ca.uwaterloo.jrefactoring.build;

import ca.uwaterloo.jrefactoring.node.*;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import gr.uom.java.ast.decomposition.StatementType;
import gr.uom.java.ast.decomposition.cfg.mapping.*;
import gr.uom.java.ast.decomposition.matching.ASTNodeDifference;
import gr.uom.java.ast.decomposition.matching.Difference;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RFStatementBuilder {

    private static RFStatementBuilder builder;
    private static Logger log = FileLogger.getLogger(RFStatementBuilder.class);

    private RFStatementBuilder() {
    }

    public static RFStatementBuilder getInstance() {
        if (builder == null) {
            builder = new RFStatementBuilder();
        }
        return builder;
    }

    public RFStatement build(CloneStructureNode root, RFTemplate template) {
        // build refactorable tree from root
        return build(root, template, null);
    }

    private RFStatement build(CloneStructureNode root, RFTemplate template, RFStatement parent) {

        // build current node
        RFStatement currentNode;
        if (parent == null) {
            //currentNode = new RFStatement(template);
            currentNode = new RFRootStmt(template);
        } else {
            assert root.getMapping() != null;
            currentNode = buildCurrent(root.getMapping(), template);
        }

        // ignore the gap node and its children
        if (currentNode == null) {
            return null;
        }

        // build children nodes
        currentNode.setParent(parent);
        for (CloneStructureNode child: root.getChildren()) {
            RFStatement childStmt = build(child, template, currentNode);
            if (childStmt != null) {
                currentNode.addChild(childStmt);
            }
        }

        return currentNode;
    }

    public RFStatement buildCurrent(NodeMapping nodeMapping, RFTemplate template) {

        if (nodeMapping instanceof PDGNodeMapping) {
            return buildFromPDGNodeMapping(nodeMapping, template);

        } else if (nodeMapping instanceof PDGElseMapping) {
            return buildFromPDGElseMapping(nodeMapping, template);

        } else if (nodeMapping instanceof PDGNodeGap) {
            log.error("TO DO: PDG node gap node: ");
            //log.error(nodeMapping.toString());
            /*
            PDGNodeGap pdgNodeGap = (PDGNodeGap) nodeMapping;
            if (pdgNodeGap.getNodeG1() != null) {
                log.info("stmt1: " + pdgNodeGap.getNodeG1().getASTStatement());
            }
            if (pdgNodeGap.getNodeG2() != null) {
                log.info("stmt2: " + pdgNodeGap.getNodeG2().getASTStatement());
            }
            for (ASTNodeDifference diff: pdgNodeGap.getNodeDifferences()) {
                log.info(diff.toString());
            }
            */
            return null;

        } else if (nodeMapping instanceof PDGElseGap) {
            log.error("TO DO: PDG else gap node");
            return null;

        } else {
            log.error("Unknown nodeMapping");
            return null;
        }
    }

    private RFStatement buildFromPDGNodeMapping(NodeMapping nodeMapping, RFTemplate template) {
        ArrayList<RFNodeDifference> nodeDifferences = new ArrayList<>();
        for (ASTNodeDifference astNodeDifference : nodeMapping.getNodeDifferences()) {
            Expression expr1 = astNodeDifference.getExpression1().getExpression();
            Expression expr2 = astNodeDifference.getExpression2().getExpression();
            List<Difference> differences = astNodeDifference.getDifferences();
            nodeDifferences.add(new RFNodeDifference(expr1, expr2, differences));
        }

        Statement statement1 = nodeMapping.getNodeG1().getASTStatement();
        Statement statement2 = nodeMapping.getNodeG2().getASTStatement();

        assert nodeMapping.getNodeG1().getStatement().getType() == nodeMapping.getNodeG2().getStatement().getType();
        StatementType statementType = nodeMapping.getNodeG1().getStatement().getType();

        return createRFStmtByStmtType(statementType, statement1, statement2, nodeDifferences, template);
    }

    private RFStatement createRFStmtByStmtType(StatementType stmtType, Statement stmt1, Statement stmt2,
                                               List<RFNodeDifference> nodeDifferences, RFTemplate template) {
        switch (stmtType) {
            case VARIABLE_DECLARATION:
                return new RFVariableDeclarationStmt(stmtType, stmt1, stmt2, nodeDifferences, template);
            case IF:
                return new RFIfStmt(stmtType, stmt1, stmt2, nodeDifferences, template);
            case FOR:
                return new RFForStmt(stmtType, stmt1, stmt2, nodeDifferences, template);
            case ENHANCED_FOR:
                return new RFEnhancedForStmt(stmtType, stmt1, stmt2, nodeDifferences, template);
            case DO:
            case WHILE:
                return new RFWhileStmt(stmtType, stmt1, stmt2, nodeDifferences, template);
            case TRY:
                return new RFTryStmt(stmtType, stmt1, stmt2, nodeDifferences, template);
            case LABELED:
                return new RFLabeledStmt(stmtType, stmt1, stmt2, nodeDifferences, template);
            case SYNCHRONIZED:
                return new RFSynchronizedStmt(stmtType, stmt1, stmt2, nodeDifferences, template);
            case SWITCH:
                return new RFSwitchStmt(stmtType, stmt1, stmt2, nodeDifferences, template);

            case EXPRESSION:
            case THROW:
            case BREAK:
            case CONTINUE:
            case RETURN:
            case ASSERT:
            case SWITCH_CASE:
            case CONSTRUCTOR_INVOCATION:
            case SUPER_CONSTRUCTOR_INVOCATION:
                // statements without body
                return new RFDefaultStmt(stmtType, stmt1, stmt2, nodeDifferences, template);

            case EMPTY:
                // ignore empty stmt
                return null;

            case BLOCK:
            case TYPE_DECLARATION:
            default:
                throw new IllegalStateException("unexpected statement type [" +
                        stmtType.name() + "] when creating RFStatement");
        }
    }

    private RFStatement buildFromPDGElseMapping(NodeMapping nodeMapping, RFTemplate template) {
        // Else RFStatement has no statement type but parent node
        return new RFElseStmt(null, null, null, new ArrayList<>(), template);
    }

}
