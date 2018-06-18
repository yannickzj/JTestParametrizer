package ca.uwaterloo.jrefactoring.action;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.template.TypePair;

public class ResolveTypeParameter implements Action {

    public void execute(RFNodeDifference diff) {
        TypePair typePair = diff.getTypePair();
        String genericTypeName = diff.getTemplate().resolveTypePair(typePair);
        diff.renameExpr1(genericTypeName);
    }
}
