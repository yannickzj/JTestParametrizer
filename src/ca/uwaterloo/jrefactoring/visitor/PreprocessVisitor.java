package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.ASTNodeUtil;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

public class PreprocessVisitor extends ASTVisitor {

    private static Logger log = FileLogger.getLogger(PreprocessVisitor.class);
    private static final String IGNORE_PACKAGE = "java.lang";
    private RFTemplate template;
    private AST ast;
    private CompilationUnit templateCU;
    private ICompilationUnit iCU1;
    private ICompilationUnit iCU2;

    public PreprocessVisitor(RFTemplate template) {
        this.template = template;
        this.ast = template.getAst();
        this.templateCU = template.getTemplateCU();
        this.iCU1 = template.getiCU1();
        this.iCU2 = template.getiCU2();
    }

    @Override
    public boolean visit(SimpleType node) {
        if (!ASTNodeUtil.hasPairedNode(node)) {
            if (node.getName() instanceof QualifiedName) {
                return true;
            }
            if (node.resolveBinding() != null && !node.resolveBinding().getPackage().getName().equals(IGNORE_PACKAGE)) {
                template.addImportDeclaration(templateCU,
                        ASTNodeUtil.createPackageName(ast, node.resolveBinding().getBinaryName().replaceAll("\\$", ".")),
                        false);
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
                    if (Modifier.isStatic(modifier) && Modifier.isPublic(modifier)
                            && !iMethodBinding.getDeclaringClass().getPackage().getName().equals(IGNORE_PACKAGE)) {
                        String staticImportName = iMethodBinding.getDeclaringClass().getQualifiedName()
                                + "." + iMethodBinding.getName();
                        template.addImportDeclaration(templateCU, ASTNodeUtil.createPackageName(ast, staticImportName), true);
                    }
                }
            }
            if (node.resolveBinding().getKind() == 3) {
                template.addVariableName(node.getIdentifier());
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

                String name = null;
                boolean isStatic;
                if (!iVariableBinding.getType().isPrimitive()) {
                    if (!iVariableBinding.getType().isArray()) {
                        name = iVariableBinding.getType().getBinaryName();
                    }
                    isStatic = false;
                } else {
                    name = iVariableBinding.getDeclaringClass().getBinaryName() + "." + iVariableBinding.getName();
                    isStatic = true;
                }
                if (templateCU != null && name != null) {
                    template.addImportDeclaration(templateCU, ASTNodeUtil.createPackageName(ast, name), isStatic);
                }
            }
        }
        return false;
    }

    @Override
    public boolean visit(ParameterizedType node) {
        if (!ASTNodeUtil.hasPairedNode(node)) {
            ITypeBinding typeBinding = node.getType().resolveBinding();
            if (typeBinding != null && !typeBinding.getPackage().getName().equals(IGNORE_PACKAGE)) {
                template.addImportDeclaration(templateCU,
                        ASTNodeUtil.createPackageName(ast, typeBinding.getBinaryName()), false);
            }
        }
        return true;
    }

    @Override
    public boolean visit(QualifiedName node) {
        if (!ASTNodeUtil.hasPairedNode(node)) {
            if (node.getQualifier().resolveTypeBinding() != null && node.getQualifier().resolveBinding().getKind() == 2
                    && node.resolveBinding().getKind() <= 3) {
                //log.info("import qualified name: " + node.getQualifier().resolveTypeBinding().getBinaryName());
                template.addImportDeclaration(templateCU,
                        ASTNodeUtil.createPackageName(ast, node.getQualifier().resolveTypeBinding().getBinaryName()), false);
            }
        }
        return false;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        if (templateCU != null && node.getExpression() == null) {
            try {
                String declaringClassName = node.resolveMethodBinding().getDeclaringClass().getQualifiedName();
                MethodInvocation pairNode = (MethodInvocation) node.getProperty(ASTNodeUtil.PROPERTY_PAIR);
                String methodName = node.getName().getIdentifier();
                for (IType iType : iCU1.getAllTypes()) {
                    if (declaringClassName.equals(iType.getFullyQualifiedName())) {
                        if (pairNode != null) {
                            methodName = methodName + ", " + pairNode.getName().getIdentifier();
                        }
                        log.info("different local method access: " + methodName);
                        template.markAsUnrefactorable();
                        return false;
                    }
                }
            } catch (Exception e) {
                log.info("cannot get types from iCU1");
                template.markAsUnrefactorable();
                return false;
            }
        }
        return true;
    }

}
