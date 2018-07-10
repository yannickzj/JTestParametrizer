package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.ASTNodeUtil;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class ImportVisitor extends ASTVisitor {

    private static Logger log = FileLogger.getLogger(ImportVisitor.class);
    private static final String IGNORE_PACKAGE = "java.lang";
    private RFTemplate template;
    private AST ast;
    private CompilationUnit templateCU;
    //private Set<String> importNames;
    //private Set<String> staticImportNames;

    public ImportVisitor(RFTemplate template) {
        this.template = template;
        this.ast = template.getAst();
        this.templateCU = template.getTemplateCU();
        //importNames = new HashSet<>();
        //staticImportNames = new HashSet<>();
    }

    /*
    public Set<String> getImportNames() {
        return importNames;
    }

    public Set<String> getStaticImportNames() {
        return staticImportNames;
    }
    */

    @Override
    public boolean visit(SimpleType node) {
        if (node.getProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING) != null) {
            template.addImportDeclaration(templateCU, ASTNodeUtil.createPackageName(ast, (String) node.getProperty(ASTNodeUtil.PROPERTY_QUALIFIED_NAME)), false);
            log.info("simple type import1: " + node.getProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING).toString());

        } else if (node.resolveBinding() != null && !node.resolveBinding().getPackage().getName().equals(IGNORE_PACKAGE)) {
            log.info("simple type [" + node + "] import: " + node.resolveBinding().getQualifiedName());
            template.addImportDeclaration(templateCU,
                    ASTNodeUtil.createPackageName(ast, node.resolveBinding().getQualifiedName()), false);
            //importNames.add(node.resolveBinding().getQualifiedName());
        }
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        if (node.resolveBinding() != null) {
            if (node.resolveBinding().getKind() == 2) {
                template.addImportDeclaration(templateCU,
                        ASTNodeUtil.createPackageName(ast, node.resolveTypeBinding().getQualifiedName()), false);
                log.info("simple name import: " + node.resolveTypeBinding().getQualifiedName());
                //importNames.add(node.resolveTypeBinding().getQualifiedName());

            } else if (node.resolveBinding().getKind() == 4) {
                IMethodBinding iMethodBinding = (IMethodBinding) node.resolveBinding();
                int modifier = iMethodBinding.getModifiers();
                if (Modifier.isStatic(modifier) && Modifier.isPublic(modifier)) {
                    String staticImportName = iMethodBinding.getDeclaringClass().getQualifiedName()
                            + "." + iMethodBinding.getName();
                    template.addImportDeclaration(templateCU, ASTNodeUtil.createPackageName(ast, staticImportName), true);
                    /*
                    staticImportNames.add(iMethodBinding.getDeclaringClass().getQualifiedName()
                            + "." + iMethodBinding.getName());
                            */
                }
            }
        }
        if (node.getProperty(ASTNodeUtil.PROPERTY_QUALIFIED_NAME) != null) {
            String qulifiedName = (String ) node.getProperty(ASTNodeUtil.PROPERTY_QUALIFIED_NAME);
            template.addImportDeclaration(templateCU, ASTNodeUtil.createPackageName(ast, qulifiedName), false);
            log.info("simple name import1:" + qulifiedName);
        }
        return false;
    }

}
