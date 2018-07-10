package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class ImportVisitor extends ASTVisitor {

    private static Logger log = FileLogger.getLogger(ImportVisitor.class);
    private static final String IGNORE_PACKAGE = "java.lang";
    private Set<String> importNames;
    private Set<String> staticImportNames;

    public ImportVisitor() {
        importNames = new HashSet<>();
        staticImportNames = new HashSet<>();
    }

    public Set<String> getImportNames() {
        return importNames;
    }

    public Set<String> getStaticImportNames() {
        return staticImportNames;
    }

    @Override
    public boolean visit(SimpleType node) {
        if (node.resolveBinding() != null && !node.resolveBinding().getPackage().getName().equals(IGNORE_PACKAGE)) {
            importNames.add(node.resolveBinding().getQualifiedName());
        }
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        if (node.resolveBinding() != null) {
            if (node.resolveBinding().getKind() == 2) {
                importNames.add(node.resolveTypeBinding().getQualifiedName());
            } else if (node.resolveBinding().getKind() == 4) {
                IMethodBinding iMethodBinding = (IMethodBinding) node.resolveBinding();
                int modifier = iMethodBinding.getModifiers();
                if (Modifier.isStatic(modifier) && Modifier.isPublic(modifier)) {
                    staticImportNames.add(iMethodBinding.getDeclaringClass().getQualifiedName()
                            + "." + iMethodBinding.getName());
                }
            }
        }
        return false;
    }

}
