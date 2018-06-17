package ca.uwaterloo.jrefactoring.strategy;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.template.GenericManager;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.template.TypePair;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.Map;

public class CreateClassInstance implements Strategy {

    public void execute(RFNodeDifference diff) {

        String resolvedClassTypeParameter = ResolveTypeParameter.resovle(diff);

        RFTemplate template = diff.getTemplate();
        GenericManager genericManager = template.getGenericManager();

        Map<String, String> clazzInstanceMap = genericManager.getClazzInstanceMap();

        ITypeBinding type1 = diff.getExpr1().resolveTypeBinding();
        ITypeBinding type2 = diff.getExpr1().resolveTypeBinding();
        TypePair typePair = new TypePair(type1.getBinaryName(), type2.getBinaryName());

        if (!clazzInstanceMap.containsKey(resolvedClassTypeParameter)) {
            template.addClassParameter(resolvedClassTypeParameter);
        }

        System.out.println("clazz instance: " + clazzInstanceMap.get(resolvedClassTypeParameter));
    }
}
