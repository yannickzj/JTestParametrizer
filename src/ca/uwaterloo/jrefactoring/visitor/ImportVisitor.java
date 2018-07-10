package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.ASTNodeUtil;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

public class ImportVisitor extends ASTVisitor {

    private static Logger log = FileLogger.getLogger(ImportVisitor.class);
    private static final String IGNORE_PACKAGE = "java.lang";
    private RFTemplate template;
    private AST ast;
    private CompilationUnit templateCU;

    public ImportVisitor(RFTemplate template) {
        this.template = template;
        this.ast = template.getAst();
        this.templateCU = template.getTemplateCU();
    }

    @Override
    public boolean visit(SimpleType node) {
        if (!ASTNodeUtil.hasPairedNode(node)) {
            if (node.resolveBinding() != null && !node.resolveBinding().getPackage().getName().equals(IGNORE_PACKAGE)) {
                //log.info("simple type [" + node + "] import: " + node.resolveBinding().getQualifiedName());
                template.addImportDeclaration(templateCU,
                        ASTNodeUtil.createPackageName(ast, node.resolveBinding().getQualifiedName()), false);
            }
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
                    //log.info("simple name[" + node + "] import: " + node.resolveTypeBinding().getQualifiedName());

                } else if (node.resolveBinding().getKind() == 4) {
                    IMethodBinding iMethodBinding = (IMethodBinding) node.resolveBinding();
                    int modifier = iMethodBinding.getModifiers();
                    if (Modifier.isStatic(modifier) && Modifier.isPublic(modifier)) {
                        String staticImportName = iMethodBinding.getDeclaringClass().getQualifiedName()
                                + "." + iMethodBinding.getName();
                        template.addImportDeclaration(templateCU, ASTNodeUtil.createPackageName(ast, staticImportName), true);
                        //log.info("simple name[" + node + "] static import: " + staticImportName);
                    }
                }
            }
        }
        return false;
    }

}
