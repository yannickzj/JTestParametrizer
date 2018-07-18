package ca.uwaterloo.jrefactoring.template;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import org.eclipse.jdt.core.dom.Expression;

public class NodePair {

    private Expression node1;
    private Expression node2;
    private RFNodeDifference diff;

    public NodePair(Expression node1, Expression node2, RFNodeDifference diff) {
        this.node1 = node1;
        this.node2 = node2;
        this.diff = diff;
    }

    public Expression getNode1() {
        return node1;
    }

    public Expression getNode2() {
        return node2;
    }

    public RFNodeDifference getDiff() {
        return diff;
    }

    @Override
    public String toString() {
        if (diff != null) {
            return diff.toString();
        } else {
            return node1.toString() + "  <--->  " + node2.toString();
        }
    }
}
