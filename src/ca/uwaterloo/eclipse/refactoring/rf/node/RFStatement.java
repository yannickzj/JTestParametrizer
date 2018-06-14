package ca.uwaterloo.eclipse.refactoring.rf.node;

import ca.uwaterloo.eclipse.refactoring.rf.template.RFTemplate;
import ca.uwaterloo.eclipse.refactoring.rf.visitor.ExpressionStmtVisitor;
import ca.uwaterloo.eclipse.refactoring.rf.visitor.IfStmtVisitor;
import ca.uwaterloo.eclipse.refactoring.rf.visitor.RFVisitor;
import ca.uwaterloo.eclipse.refactoring.rf.visitor.VariableDeclarationStmtVisitor;
import ca.uwaterloo.eclipse.refactoring.utility.FileLogger;
import gr.uom.java.ast.decomposition.StatementType;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RFStatement extends RFEntity {

    private static Logger log = FileLogger.getLogger(RFStatement.class);

    private RFStatement parent;
    private List<RFStatement> children;
    private StatementType statementType;
    private Statement statement1;
    private Statement statement2;
    private List<RFNodeDifference> mapping;
    private RFTemplate template;

    public RFStatement(RFTemplate template) {
        parent = null;
        statementType = null;
        statement1 = null;
        statement2 = null;
        this.template = template;
        mapping = new ArrayList<>();
        children = new ArrayList<>();
    }

    public RFStatement(StatementType statementType,
                       Statement statement1,
                       Statement statement2,
                       List<RFNodeDifference> mapping,
                       RFTemplate template) {
        this.parent = null;
        this.statementType = statementType;
        this.statement1 = statement1;
        this.statement2 = statement2;
        this.mapping = mapping;
        this.template = template;
        this.children = new ArrayList<>();

        for (RFNodeDifference diff : this.mapping) {
            diff.setRfStatement(this);
        }
    }

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

    public Statement getStatement1() {
        return statement1;
    }

    public Statement getStatement2() {
        return statement2;
    }

    public List<RFNodeDifference> getMapping() {
        return mapping;
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

    public void setMapping(List<RFNodeDifference> mapping) {
        this.mapping = mapping;
    }

    public void setTemplate(RFTemplate template) {
        this.template = template;
    }

    public RFVisitor selectVisitor() {
        switch (statementType) {
            case VARIABLE_DECLARATION:
                return new VariableDeclarationStmtVisitor();
            case EXPRESSION:
                return new ExpressionStmtVisitor();
            case IF:
                return new IfStmtVisitor();
            default:
                throw new IllegalStateException();
        }
    }

    public void describe() {
        System.out.println("-----------------------------------------------------------");
        describeStatements();
        describeDifference();
    }

    public void describeStatements() {
        System.out.println("Describing RFStatement [Type: " + getStatementTypeString() + "]:");
        System.out.println("\t\tStatement1: " + (statement1 == null ? "null" : statement1.toString()));
        System.out.println("\t\tStatement2: " + (statement2 == null ? "null" : statement2.toString()));
    }

    public void describeDifference() {
        if (mapping.size() > 0) {
            System.out.println("Describing RFStatement differences: ");
            System.out.println("\tDifferences: ");
            for (RFNodeDifference difference : mapping) {
                System.out.println("\t\t" + difference);
            }
        } else {
            System.out.println("Current RFStatement has no difference");
        }
    }

    void accept0(RFVisitor visitor) {
        boolean visitChildren = visitor.visit(this);
        if (visitChildren) {
            // visit children
            for (RFStatement child: children) {
                child.accept(visitor);
            }
            //log.info("doing nothing for RFVariableDeclarationStatement");
        }
        visitor.endVisit(this);
    }
}
