package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.ASTNodeUtil;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

public class PreprocessVisitor extends ASTVisitor {

    private static Logger log = FileLogger.getLogger(PreprocessVisitor.class);
    private static final String IGNORE_PACKAGE = "java.lang";
    private RFTemplate template;
    private AST ast;
    private CompilationUnit templateCU;

    public PreprocessVisitor(RFTemplate template) {
        this.template = template;
        this.ast = template.getAst();
        this.templateCU = template.getTemplateCU();
    }

    @Override
    public boolean visit(SimpleType node) {
        if (!ASTNodeUtil.hasPairedNode(node)) {
            if (node.getName() instanceof QualifiedName) {
                return true;
            }
            if (node.resolveBinding() != null && !node.resolveBinding().getPackage().getName().equals(IGNORE_PACKAGE)) {
                template.addImportDeclaration(templateCU,
                        ASTNodeUtil.createPackageName(ast, node.resolveBinding().getQualifiedName()), false);
            }
        }
        return false;
    }

    @Override
    public boolean visit(ThisExpression node) {
        if (template.getTemplateCU() != null) {
            template.markAsUnrefactorable();
            log.info("Containing unrefactorable ThisExpression: " + node);
        }
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        if (!ASTNodeUtil.hasPairedNode(node)) {
            if (node.resolveBinding() != null) {
                if (node.resolveBinding().getKind() == 2) {
                    template.addImportDeclaration(templateCU,
                            ASTNodeUtil.createPackageName(ast, node.resolveTypeBinding().getQualifiedName()), false);

                } else if (node.resolveBinding().getKind() == 4) {
                    IMethodBinding iMethodBinding = (IMethodBinding) node.resolveBinding();
                    int modifier = iMethodBinding.getModifiers();
                    if (Modifier.isStatic(modifier) && Modifier.isPublic(modifier)) {
                        String staticImportName = iMethodBinding.getDeclaringClass().getQualifiedName()
                                + "." + iMethodBinding.getName();
                        template.addImportDeclaration(templateCU, ASTNodeUtil.createPackageName(ast, staticImportName), true);
                    }
                }
            }
        }

        if (node.resolveBinding().getKind() == 3) {
            IVariableBinding iVariableBinding = (IVariableBinding) node.resolveBinding();
            if (iVariableBinding.isField()) {
                if (Modifier.isPrivate(iVariableBinding.getModifiers()) && template.getTemplateCU() != null) {
                    template.markAsUnrefactorable();
                    log.info("Containing unrefactorable private field access: " + node);
                    return false;
                }

                String name;
                boolean isStatic;
                if (!iVariableBinding.getType().isPrimitive()) {
                    name = iVariableBinding.getType().getBinaryName();
                    isStatic = false;
                } else {
                    name = iVariableBinding.getDeclaringClass().getBinaryName() + "." + iVariableBinding.getName();
                    isStatic = true;
                }
                template.addImportDeclaration(templateCU, ASTNodeUtil.createPackageName(ast, name), isStatic);
            }
        }
        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        if (!ASTNodeUtil.hasPairedNode(node)) {
            if (node.getQualifier().resolveTypeBinding() != null) {
                log.info("import qualified name: " + node.getQualifier().resolveTypeBinding().getBinaryName());
                template.addImportDeclaration(templateCU,
                        ASTNodeUtil.createPackageName(ast, node.getQualifier().resolveTypeBinding().getBinaryName()), false);
            }
        }
        return false;
    }

}
