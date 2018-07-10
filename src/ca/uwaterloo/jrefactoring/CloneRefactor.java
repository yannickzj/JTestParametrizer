package ca.uwaterloo.jrefactoring;

import ca.uwaterloo.jrefactoring.build.RFStatementBuilder;
import ca.uwaterloo.jrefactoring.detect.ClonePairInfo;
import ca.uwaterloo.jrefactoring.detect.InputMethods;
import ca.uwaterloo.jrefactoring.detect.PDGSubTreeMapperInfo;
import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import ca.uwaterloo.jrefactoring.utility.RenameUtil;
import ca.uwaterloo.jrefactoring.visitor.RFVisitor;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneType;
import gr.uom.java.ast.decomposition.cfg.mapping.DivideAndConquerMatcher;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

public class CloneRefactor {

    private static Logger log = FileLogger.getLogger(CloneRefactor.class);

    public static void refactor(ClonePairInfo pairInfo, InputMethods methodsInfo) throws Exception {

        String templateName = RenameUtil.getTemplateName(pairInfo.getFirstMethodSignature(), pairInfo.getSecondMethodSignature());
        String adapterName = RenameUtil.getAdapterName(pairInfo.getFirstClass(), pairInfo.getFirstPackage(),
                pairInfo.getSecondClass(), pairInfo.getSecondPackage());
        String[] adapterImplNamePair = RenameUtil.getAdapterImplNamePair(adapterName);

        assert pairInfo.getPDFSubTreeMappersInfoList().size() == 1;
        for (PDGSubTreeMapperInfo mapperInfo : pairInfo.getPDFSubTreeMappersInfoList()) {
            // achieve the clone structure root node and clone type
            DivideAndConquerMatcher matcher = mapperInfo.getMapper();
            CloneType cloneType = matcher.getCloneType();
            CloneStructureNode root = matcher.getCloneStructureRoot();

            // perform source-to-source refactoring transformation
            if (cloneType.equals(CloneType.TYPE_2)) {
                if (root != null) {

                    // create the refactoring template
                    Statement stmt1 = getStmt1(root);
                    Statement stmt2 = getStmt2(root);
                    AST ast1 = stmt1.getAST();
                    MethodDeclaration method1 = getMethod(stmt1);
                    MethodDeclaration method2 = getMethod(stmt2);
                    RFTemplate template = new RFTemplate(ast1, method1, method2, templateName, adapterName, adapterImplNamePair);

                    // construct the refactoring tree
                    RFStatement rfRoot = RFStatementBuilder.getInstance().build(root, template);

                    rfRoot.accept(new RFVisitor(template));

                    template.modifyTestMethods();
                    /*
                    MethodDeclaration method1 = ASTNodeUtil.retrieveMethodDeclarationNode(methodsInfo.getIMethod1(),
                            methodsInfo.getStartOffset1(), methodsInfo.getEndOffset1(), true);
                    MethodDeclaration method2 = ASTNodeUtil.retrieveMethodDeclarationNode(methodsInfo.getIMethod2(),
                            methodsInfo.getStartOffset2(), methodsInfo.getEndOffset2(), true);
                            */

                    template.updateSourceFiles();

                    // print out the refactoring template
                    System.out.println("----------------------------------------------------------");
                    System.out.println(template);

                }

            } else {
                log.info("Unhandled CloneType: " + cloneType.toString());
            }
        }
    }

    private static MethodDeclaration getMethod(Statement stmt) {
        ASTNode node = stmt;
        while(!(node instanceof MethodDeclaration)) {
            node = node.getParent();
        }
        return (MethodDeclaration) node;
    }

    private static Statement getStmt1(CloneStructureNode root) {
        if (root.getMapping() != null && root.getMapping().getNodeG1() != null) {
            return root.getMapping().getNodeG1().getASTStatement();
        } else {
            for (CloneStructureNode child: root.getChildren()) {
                Statement stmt = getStmt1(child);
                if (stmt != null) {
                    return stmt;
                }
            }
        }
        return null;
    }


    private static Statement getStmt2(CloneStructureNode root) {
        if (root.getMapping() != null && root.getMapping().getNodeG2() != null) {
            return root.getMapping().getNodeG2().getASTStatement();
        } else {
            for (CloneStructureNode child: root.getChildren()) {
                Statement stmt = getStmt2(child);
                if (stmt != null) {
                    return stmt;
                }
            }
        }
        return null;
    }
}
