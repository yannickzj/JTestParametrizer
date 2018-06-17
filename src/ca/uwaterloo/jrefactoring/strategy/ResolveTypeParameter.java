package ca.uwaterloo.jrefactoring.strategy;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.template.GenericManager;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.template.TypePair;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.Map;

public class ResolveTypeParameter implements Strategy {

    private static int counter = 1;

    public static String resovle(RFNodeDifference diff) {
        RFTemplate template = diff.getTemplate();
        GenericManager genericManager = template.getGenericManager();
        Map<TypePair, String> typePool = genericManager.getTypePool();

        ITypeBinding type1 = diff.getExpr1().resolveTypeBinding();
        ITypeBinding type2 = diff.getExpr1().resolveTypeBinding();
        TypePair typePair = new TypePair(type1.getBinaryName(), type2.getBinaryName());

        String genericTypeName;
        if (typePool.containsKey(typePair)) {
            genericTypeName = typePool.get(typePair);
        } else {
            System.out.println("merge types: " + type1.getBinaryName() + ", " + type2.getBinaryName());
            genericTypeName = "T" + counter++;
            System.out.println("resolve new generic type name: " + genericTypeName);
            template.addTypeParameter(genericTypeName);
            typePool.put(typePair, genericTypeName);
        }

        return genericTypeName;
    }

    public void execute(RFNodeDifference diff) {

        String genericTypeName = resovle(diff);
        Expression expr1 = diff.getExpr1();
        if (expr1 instanceof SimpleName) {
            SimpleName name = (SimpleName) expr1;
            name.setIdentifier(genericTypeName);
        } else {
            throw new IllegalStateException("unexpected expression: " + expr1 + "[Type: " + expr1.getNodeType() + "]");
        }
    }
}
