package ca.uwaterloo.jrefactoring.node;

import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.template.TypePair;
import ca.uwaterloo.jrefactoring.utility.ASTNodeUtil;
import ca.uwaterloo.jrefactoring.visitor.RFVisitor;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import gr.uom.java.ast.decomposition.matching.Difference;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RFNodeDifference extends RFEntity {

    private static Logger log = FileLogger.getLogger(RFNodeDifference.class);

    private final Expression expr1;
    private final Expression expr2;
    private final List<Difference> differences;
    private RFStatement rfStatement;

    public RFNodeDifference(Expression expr1, Expression expr2, List<Difference> differences) {
        this.expr1 = expr1;
        this.expr2 = expr2;
        this.differences = differences;
        expr1.setProperty(ASTNodeUtil.PROPERTY_DIFF, this);
    }

    public Expression getExpr1() {
        return expr1;
    }

    public Expression getExpr2() {
        return expr2;
    }

    public List<Difference> getDifferences() {
        return differences;
    }

    public RFStatement getRfStatement() {
        return rfStatement;
    }

    public void setRfStatement(RFStatement rfStatement) {
        this.rfStatement = rfStatement;
    }

    public RFTemplate getTemplate() {
        return rfStatement.getTemplate();
    }

    void accept0(RFVisitor visitor) {
        /*
        boolean visitChildren = visitor.visit(this);
        if (visitChildren) {
            // visit children
            log.info("doing nothing for RFNodeDifference children!");
        }
        visitor.endVisit(this);
        */
    }

    public TypePair getTypePair() {
        return new TypePair(expr1.resolveTypeBinding().getQualifiedName(), expr2.resolveTypeBinding().getQualifiedName());
    }

    public Set<DifferenceType> getDifferenceTypes() {
        Set<DifferenceType> differenceTypes = new HashSet<>();
        for (Difference diff: differences) {
            differenceTypes.add(diff.getType());
        }
        return differenceTypes;
    }

    public void renameExpr1(String newIdentifier) {
        if (expr1 instanceof SimpleName) {
            SimpleName name = (SimpleName) expr1;
            name.setIdentifier(newIdentifier);
        } else {
            throw new IllegalStateException("renaming unexpected expression: " + expr1 + "[Type: " + expr1.getNodeType() + "]");
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Difference diff: differences) {
            sb.append(diff.getType().name());
            sb.append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        return expr1.toString() + "  <--->  " + expr2.toString() + "  (" + sb.toString() +  ")";
    }
}
