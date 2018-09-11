package ca.uwaterloo.jrefactoring.node;

import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import ca.uwaterloo.jrefactoring.visitor.RFVisitor;
import gr.uom.java.ast.decomposition.StatementType;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public abstract class RFStatement implements RFEntity {

    private static Logger log = FileLogger.getLogger(RFStatement.class);

    RFStatement parent;
    List<RFStatement> children;
    StatementType statementType;
    Statement statement1;
    Statement statement2;
    List<RFNodeDifference> nodeDifferences;
    RFTemplate template;

    public RFStatement(RFTemplate template) {
        parent = null;
        statementType = null;
        statement1 = null;
        statement2 = null;
        this.template = template;
        nodeDifferences = new ArrayList<>();
        children = new ArrayList<>();
    }

    public RFStatement(StatementType statementType,
                       Statement statement1,
                       Statement statement2,
                       List<RFNodeDifference> nodeDifferences,
                       RFTemplate template) {
        this.parent = null;
        this.statementType = statementType;
        this.statement1 = statement1;
        this.statement2 = statement2;
        this.nodeDifferences = nodeDifferences;
        this.template = template;
        this.children = new ArrayList<>();

        for (RFNodeDifference diff : this.nodeDifferences) {
            diff.setRfStatement(this);
        }
    }

    @Override
    public void accept(RFVisitor visitor) {
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

    public void setParent(RFStatement parent) {
        this.parent = parent;
    }

    public void addChild(RFStatement child) {
        if (child == null) {
            throw new IllegalArgumentException();
        }
        children.add(child);
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public String getStatementTypeString() {
        if (isRoot()) {
            return "root";
        } else {
            return statementType == null ? "else" : statementType.toString();
        }
    }

    public RFStatement getParent() {
        return parent;
    }

    public boolean isTopStmt() {
        return parent != null && parent.isRoot();
    }

    public Statement getStatement1() {
        return statement1;
    }

    public Statement getStatement2() {
        return statement2;
    }

    public List<RFNodeDifference> getNodeDifferences() {
        return nodeDifferences;
    }

    public RFTemplate getTemplate() {
        return template;
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

    public void setNodeDifferences(List<RFNodeDifference> nodeDifferences) {
        this.nodeDifferences = nodeDifferences;
    }

    public void setTemplate(RFTemplate template) {
        this.template = template;
    }

    public void describe() {
        System.out.println("-----------------------------------------------------------");
        describeStatements();
        describeDifference();
        System.out.println();
    }

    private void describeStatements() {
        System.out.println("Describing RFStatement [Type: " + getStatementTypeString() + "]:");
        System.out.println("\t\tStatement1: " + (statement1 == null ? "null" : statement1.toString()));
        System.out.println("\t\tStatement2: " + (statement2 == null ? "null" : statement2.toString()));
    }

    private void describeDifference() {
        if (nodeDifferences.size() > 0) {
            System.out.println("Describing RFStatement differences: ");
            System.out.println("\tDifferences: ");
            for (RFNodeDifference difference : nodeDifferences) {
                System.out.println("\t\t" + difference);
            }
        } else {
            System.out.println("Current RFStatement has no difference");
        }
    }

    public boolean hasChildren() {
        return children.size() > 0;
    }

    public boolean hasDifference() {
        return nodeDifferences.size() > 0;
    }

}
