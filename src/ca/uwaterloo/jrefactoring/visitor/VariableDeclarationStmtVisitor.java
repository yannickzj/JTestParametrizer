package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.strategy.*;
import ca.uwaterloo.jrefactoring.utility.ContextUtil;
import ca.uwaterloo.jrefactoring.utility.DiffUtil;
import ca.uwaterloo.jrefactoring.utility.Transformer;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VariableDeclarationStmtVisitor extends RFVisitor {

    private static Logger log = FileLogger.getLogger(VariableDeclarationStmtVisitor.class);

    public VariableDeclarationStmtVisitor() {
    }

    @Override
    public boolean visit(RFStatement node) {
        if (node.hasDifference()) {
            System.out.println("-----------------------------------------------------------");
            node.describeStatements();
            for (RFNodeDifference diff: node.getNodeDifferences()) {
                diff.accept(this);
            }
            System.out.println("variableDeclarationStmtVisitor finish visiting");
        }
        return true;
    }

    @Override
    public boolean visit(RFNodeDifference diff) {

        // validate node difference
        ContextUtil.validateNodeDiff(diff);

        // refactor node difference
        refactor(diff);

        System.out.println();
        return false;
    }

    private List<Strategy> selectStrategies(int contextNodyType, Set<DifferenceType> differenceTypes) {

        List<Strategy> strategies = new ArrayList<>();

        if (differenceTypes.contains(DifferenceType.VARIABLE_NAME_MISMATCH)) {
            if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                strategies.add(new CreateMethodInvocationAction());
            } else {
                strategies.add(new ResolveName());
            }
        } else {
            if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                if (contextNodyType == ASTNode.CLASS_INSTANCE_CREATION) {
                    strategies.add(new CreateClassInstance());
                } else {
                    strategies.add(new ResolveTypeParameter());
                }
            }
        }

        return strategies;
    }

    private void refactor(RFNodeDifference diff) {
        log.info("refactor difference: " + DiffUtil.displayNodeDiff(diff));

        // search context node
        int nodeType = ContextUtil.getContextNodeType(diff);
        log.info("contextNode: " + ASTNode.nodeClassForType(nodeType).getName());

        Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

        // select refactoring strategy based on difference types and context node
        List<Strategy> strategies = selectStrategies(nodeType, differenceTypes);

        // take refactoring actions
        for(Strategy strategy: strategies) {
            Transformer.setStrategy(strategy);
            Transformer.transform(diff);
        }
    }

    @Override
    public boolean visit(SimpleName node) {
        System.out.println("SimpleName location [" + node + "] in parent: " + node.getLocationInParent());
        //System.out.println("SimpleName: " + node);
        //System.out.println("Start position: " + node.getStartPosition());
        node.getParent().accept(this);
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        //System.out.println("SimpleType location [" + node + "] in parent: " + node.getLocationInParent());
        node.getParent().accept(this);
        return false;
    }

}
