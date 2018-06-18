package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFEntity;
import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.action.*;
import ca.uwaterloo.jrefactoring.utility.ContextUtil;
import ca.uwaterloo.jrefactoring.utility.DiffUtil;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import ca.uwaterloo.jrefactoring.utility.Transformer;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    List<Action> selectActions(int contextNodyType, Set<DifferenceType> differenceTypes) {

        List<Action> strategies = new ArrayList<>();

        if (differenceTypes.contains(DifferenceType.VARIABLE_NAME_MISMATCH)) {
            if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                strategies.add(new CreateMethodInvocationAction());
            } else {
                strategies.add(new ResolveName());
            }
        } else if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
            if (contextNodyType == ASTNode.CLASS_INSTANCE_CREATION) {
                strategies.add(new CreateClassInstance());
            } else {
                strategies.add(new ResolveTypeParameter());
            }
        }

        return strategies;
    }

    void refactor(RFNodeDifference diff) {
        log.info("refactor difference: " + DiffUtil.displayNodeDiff(diff));

        // search context node
        int nodeType = ContextUtil.getContextNodeType(diff);
        log.info("contextNode: " + ASTNode.nodeClassForType(nodeType).getName());

        Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

        // select refactoring strategy based on difference types and context node
        List<Action> actions = selectActions(nodeType, differenceTypes);

        // take refactoring actions
        for (Action action : actions) {
            Transformer.setAction(action);
            Transformer.transform(diff);
        }
    }

    public void endVisit(RFStatement node) {
        if (node.isTopStmt()) {
            //log.info("copy current RFStatement");
            AST ast = node.getTemplate().getAst();
            node.getTemplate().addStatement((Statement)ASTNode.copySubtree(ast, node.getStatement1()));
        }
    }

    public void endVisit(RFNodeDifference node) {
    }

}
