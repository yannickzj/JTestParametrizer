package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.slf4j.Logger;


public class MethodVisitor extends ASTVisitor {

    private static Logger log = FileLogger.getLogger(MethodVisitor.class);
    private String typeName;
    private MethodDeclaration target;
    private MethodDeclaration result;
    private TypeDeclaration typeDeclaration;

    public MethodVisitor(String typeName, MethodDeclaration methodDeclaration) {
        this.typeName = typeName;
        this.target = methodDeclaration;
        this.typeDeclaration = null;
        this.result = null;
    }

    public MethodDeclaration getResult() {
        return result;
    }

    public TypeDeclaration getTypeDeclaration() {
        return typeDeclaration;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (node.getName().getIdentifier().equals(typeName)) {
            typeDeclaration = node;
            return true;
        } else {
            return false;
        }
    }

    private boolean isSameMethodSignature(MethodDeclaration m1, MethodDeclaration m2) {
        if (!m1.getName().getIdentifier().equals(m2.getName().getIdentifier())
                || m1.parameters().size() != m2.parameters().size()
                || !m1.getReturnType2().toString().equals(m2.getReturnType2().toString())) {
            return false;
        } else {
            for (int i = 0; i < m1.parameters().size(); i++) {
                SingleVariableDeclaration p1 = (SingleVariableDeclaration) m1.parameters().get(i);
                SingleVariableDeclaration p2 = (SingleVariableDeclaration) m2.parameters().get(i);
                if (!p1.getType().toString().equals(p2.getType().toString())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        if (isSameMethodSignature(node, target)) {
            result = node;
        }
        return false;
    }
}
