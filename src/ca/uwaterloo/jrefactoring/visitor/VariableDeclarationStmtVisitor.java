package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

public class VariableDeclarationStmtVisitor extends RFVisitor {

    private static Logger log = FileLogger.getLogger(VariableDeclarationStmtVisitor.class);

    public VariableDeclarationStmtVisitor(RFTemplate template) {
        super(template);
    }

    @Override
    public boolean visit(RFStatement node) {
        if (node.hasDifference()) {
            //node.describe();

            VariableDeclarationStatement stmt1 = (VariableDeclarationStatement) node.getStatement1();
            Type type = stmt1.getType();
            type.accept(this);

            for (Object fragment: stmt1.fragments()) {
                VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
                SimpleName name = variableDeclarationFragment.getName();
                name.accept(this);

                Expression initializer = variableDeclarationFragment.getInitializer();
                initializer.accept(this);
            }

            //System.out.println("variableDeclarationStmtVisitor finish visiting");
        }
        return true;
    }
}
