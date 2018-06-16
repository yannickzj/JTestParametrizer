package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFEntity;
import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;
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
        if (node.isTopStmt()) {
            //log.info("copy current RFStatement");
            AST ast = node.getTemplate().getAst();
            node.getTemplate().addStatement((Statement)ASTNode.copySubtree(ast, node.getStatement1()));
        }
    }

    public void endVisit(RFNodeDifference node) {
    }

}
