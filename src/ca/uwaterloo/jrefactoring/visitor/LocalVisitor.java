package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.slf4j.Logger;

public class LocalVisitor extends ASTVisitor {

    private static Logger log = FileLogger.getLogger(LocalVisitor.class);
    private boolean containsLocal;

    public LocalVisitor() {
        this.containsLocal = false;
    }

    public boolean containsLocal() {
        return this.containsLocal;
    }

    @Override
    public boolean visit(SimpleName node) {
        if (node.resolveBinding().getKind() == 3) {
            IVariableBinding iVariableBinding = (IVariableBinding) node.resolveBinding();
            if (!iVariableBinding.isField()) {
                containsLocal = true;
            }
        }
        return false;
    }

}
