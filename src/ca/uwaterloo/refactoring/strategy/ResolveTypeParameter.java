package ca.uwaterloo.refactoring.strategy;

import ca.uwaterloo.refactoring.node.RFNodeDifference;
import ca.uwaterloo.refactoring.template.RFTemplate;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class ResolveTypeParameter implements Strategy {

    private static int counter = 1;

    public void execute(RFNodeDifference diff) {
        RFTemplate template = diff.getTemplate();
        ITypeBinding type1 = diff.getExpr1().resolveTypeBinding();
        ITypeBinding type2 = diff.getExpr1().resolveTypeBinding();
        System.out.println("merge types: " + type1.getBinaryName() + ", " + type2.getBinaryName());

        String newGenericTypeName = "T" + counter++;
        System.out.println("resolve new generic type name: " + newGenericTypeName);

        Expression expr1 = diff.getExpr1();

        if (expr1 instanceof SimpleName) {
            SimpleName name = (SimpleName) expr1;
            name.setIdentifier(newGenericTypeName);
        } else {
            throw new IllegalStateException("unexpected expression: " + expr1 + "[Type: " + expr1.getNodeType() + "]");
        }
    }
}
