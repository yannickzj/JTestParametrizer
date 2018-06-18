package ca.uwaterloo.jrefactoring.utility;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;

public class ContextUtil {
    public static ASTNode getContextNode(Expression expr) {
        ASTNode contextNode = expr.getParent();
        while(contextNode.getNodeType() == ASTNode.SIMPLE_NAME
                || contextNode.getNodeType() == ASTNode.SIMPLE_TYPE
                || contextNode.getNodeType() == ASTNode.PARAMETERIZED_TYPE) {
            contextNode = contextNode.getParent();
        }
        return contextNode;
    }

    public static int getContextNodeType(RFNodeDifference diff) {
        Expression expr1 = diff.getExpr1();
        Expression expr2 = diff.getExpr2();
        ASTNode contextNode1 = ContextUtil.getContextNode(expr1);
        ASTNode contextNode2 = ContextUtil.getContextNode(expr2);
        assert contextNode1.getNodeType() == contextNode2.getNodeType();
        return contextNode1.getNodeType();
    }

    public static void validateNodeDiff(RFNodeDifference diff) {
        Expression expr1 = diff.getExpr1();
        Expression expr2 = diff.getExpr2();
        assert expr1.getNodeType() == expr2.getNodeType();
    }
}
