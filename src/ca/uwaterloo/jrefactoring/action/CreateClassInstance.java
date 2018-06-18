package ca.uwaterloo.jrefactoring.action;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.template.TypePair;

public class CreateClassInstance implements Action {

    public void execute(RFNodeDifference diff) {

        TypePair typePair = diff.getTypePair();
        RFTemplate template = diff.getTemplate();
        String genericTypeName = template.resolveTypePair(typePair);
        String clazzName = template.resolveGenericType(genericTypeName);

        //System.out.println("clazz instance: " + clazzName);
        template.createClazzInstance(diff.getExpr1(), clazzName);

    }
}
