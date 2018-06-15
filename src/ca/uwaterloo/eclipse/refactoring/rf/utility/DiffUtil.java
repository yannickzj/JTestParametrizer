package ca.uwaterloo.eclipse.refactoring.rf.utility;

import ca.uwaterloo.eclipse.refactoring.rf.node.RFNodeDifference;
import ca.uwaterloo.eclipse.refactoring.rf.strategy.Strategy;
import gr.uom.java.ast.decomposition.matching.Difference;
import org.eclipse.jdt.core.dom.Expression;

public class DiffUtil {

    public static String displayNodeDiff(RFNodeDifference difference) {
        StringBuffer sb = new StringBuffer();
        for (Difference diff: difference.getDifferences()) {
            sb.append(diff.getType().name());
            sb.append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }

        Expression expr1 = difference.getExpr1();
        Expression expr2 = difference.getExpr2();
        return expr1.toString() + "  <--->  " + expr2.toString() + "  (" + sb.toString() +  ")";
    }

}
