package ca.uwaterloo.eclipse.refactoring.rf.dom;

import ca.uwaterloo.eclipse.refactoring.rf.visitor.RFVisitor;
import gr.uom.java.ast.decomposition.StatementType;
import org.eclipse.jdt.core.dom.Statement;

import java.util.ArrayList;
import java.util.List;

public abstract class RFStatement {

    private StatementType statementType;
    private Statement statement1;
    private Statement statement2;
    private List<RFNodeDifference> mapping;

    public RFStatement() {
        statementType = StatementType.EMPTY;
        statement1 = null;
        statement2 = null;
        mapping = new ArrayList<>();
    }

    public RFStatement(StatementType statementType, Statement statement1, Statement statement2, List<RFNodeDifference> mapping) {
        this.statementType = statementType;
        this.statement1 = statement1;
        this.statement2 = statement2;
        this.mapping = mapping;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public Statement getStatement1() {
        return statement1;
    }

    public Statement getStatement2() {
        return statement2;
    }

    public List<RFNodeDifference> getMapping() {
        return mapping;
    }

    public void setStatementType(StatementType statementType) {
        this.statementType = statementType;
    }

    public void setStatement1(Statement statement1) {
        this.statement1 = statement1;
    }

    public void setStatement2(Statement statement2) {
        this.statement2 = statement2;
    }

    public void setMapping(List<RFNodeDifference> mapping) {
        this.mapping = mapping;
    }

    public void describe() {
        System.out.println("Describing current RFStatement: ");
        System.out.println("\ttype: " + statementType);
        System.out.println("\tstatement1: " + statement1);
        System.out.println("\tstatement2: " + statement2);
        System.out.println("\tmapping: ");
        for(RFNodeDifference difference: mapping) {
            System.out.println("\t\t" + difference);
        }
    }

    public final void accept(RFVisitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException();
        }
        // begin with the generic pre-visit
        if (visitor.preVisit2(this)) {
            // dynamic dispatch to internal method for type-specific visit/endVisit
            accept0(visitor);
        }
        // end with the generic post-visit
        visitor.postVisit(this);
    }

    abstract void accept0(RFVisitor visitor);
}
