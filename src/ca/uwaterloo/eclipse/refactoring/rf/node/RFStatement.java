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

    private StatementType statementType;
    private Statement statement1;
    private Statement statement2;
    private List<RFNodeDifference> mapping;
    private RFTemplate template;

    public RFStatement() {
        statementType = StatementType.EMPTY;
        statement1 = null;
        statement2 = null;
        mapping = new ArrayList<>();
        template = null;
    }

    public RFStatement(StatementType statementType,
                       Statement statement1,
                       Statement statement2,
                       List<RFNodeDifference> mapping,
                       RFTemplate template) {
        this.statementType = statementType;
        this.statement1 = statement1;
        this.statement2 = statement2;
        this.mapping = mapping;
        this.template = template;

        for (RFNodeDifference diff: this.mapping) {
            diff.setRfStatement(this);
        }
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
        System.out.println("Describing current RFStatement: ");
        System.out.println("\ttype: " + statementType);
        System.out.println("\tstatement1: " + statement1);
        System.out.println("\tstatement2: " + statement2);
        System.out.println("\tmapping: ");
        for(RFNodeDifference difference: mapping) {
            System.out.println("\t\t" + difference);
        }
    }

    void accept0(RFVisitor visitor) {
        boolean visitChildren = visitor.visit(this);
        if (visitChildren) {
            // visit children
            log.info("doing nothing for RFVariableDeclarationStatement");
        }
        visitor.endVisit(this);
    }
}
