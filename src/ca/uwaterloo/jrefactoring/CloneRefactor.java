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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CloneRefactor {

    private static Logger log = FileLogger.getLogger(CloneRefactor.class);
    private static List<RFTemplate> refactorableTemplates = new ArrayList<>();
    private static Set<String> templateNames = new HashSet<>();
    private static int countType1 = 0;
    private static int countType2 = 0;
    private static int countType3 = 0;
    private static int countSkip = 0;

    public static void refactor(ClonePairInfo pairInfo, InputMethods methodsInfo) throws Exception {

        // skip some cases
        if (!pairInfo.getFirstPackage().equals(pairInfo.getSecondPackage())) {
            log.info("skip method pair not in the same package");
            return;
        } else if (pairInfo.getFirstClass().equals(pairInfo.getSecondClass())
                && pairInfo.getFirstMethodSignature().equals(pairInfo.getSecondMethodSignature())) {
            log.info("skip the pairs with same method");
            return;
        }

        log.info("start to refactor clone pair");

        String templateName = RenameUtil.getTemplateName(pairInfo.getFirstClass(), pairInfo.getFirstMethodSignature(),
                pairInfo.getSecondClass(), pairInfo.getSecondMethodSignature());

        String adapterName = RenameUtil.getAdapterName(pairInfo.getFirstClass(), pairInfo.getFirstMethodSignature(),
                pairInfo.getFirstPackage(), pairInfo.getSecondClass(), pairInfo.getSecondMethodSignature(),
                pairInfo.getSecondPackage());

        String[] adapterImplNamePair = RenameUtil.getAdapterImplNamePair(adapterName, pairInfo.getFirstClass(),
                pairInfo.getSecondClass(), pairInfo.getFirstMethodSignature(), pairInfo.getSecondMethodSignature());

        assert pairInfo.getPDFSubTreeMappersInfoList().size() == 1;
        for (PDGSubTreeMapperInfo mapperInfo : pairInfo.getPDFSubTreeMappersInfoList()) {
            // achieve the clone structure root node and clone type
            DivideAndConquerMatcher matcher = mapperInfo.getMapper();
            CloneType cloneType = matcher.getCloneType();
            CloneStructureNode root = matcher.getCloneStructureRoot();

            // perform source-to-source refactoring transformation
            if (!cloneType.equals(CloneType.TYPE_3)) {
                if (root != null) {

                    // count clone types
                    if (cloneType.equals(CloneType.TYPE_1)) countType1++;
                    if (cloneType.equals(CloneType.TYPE_2)) countType2++;

                    // create the refactoring template
                    Statement stmt1 = getStmt1(root);
                    Statement stmt2 = getStmt2(root);
                    AST ast1 = stmt1.getAST();
                    MethodDeclaration method1 = getMethod(stmt1);
                    MethodDeclaration method2 = getMethod(stmt2);

                    if (Modifier.isPrivate(method1.getModifiers()) || Modifier.isPrivate(method2.getModifiers())) {
                        log.info("private method pair are not test cases!");
                        countSkip++;
                        return;
                    }

                    ICompilationUnit cu1 = pairInfo.getICompilationUnitFirst();
                    ICompilationUnit cu2 = pairInfo.getICompilationUnitSecond();
                    RFTemplate template = new RFTemplate(ast1, method1, method2, templateName, adapterName,
                            adapterImplNamePair, cu1, cu2);

                    // construct the refactoring tree
                    RFStatement rfRoot = RFStatementBuilder.getInstance().build(root, template);
                    if (!template.isRefactorable()) {
                        countSkip++;
                        continue;
                    }

                    rfRoot.accept(new RFVisitor(template));

                    template.modifyTestMethods();
                    /*
                    MethodDeclaration method1 = ASTNodeUtil.retrieveMethodDeclarationNode(methodsInfo.getIMethod1(),
                            methodsInfo.getStartOffset1(), methodsInfo.getEndOffset1(), true);
                    MethodDeclaration method2 = ASTNodeUtil.retrieveMethodDeclarationNode(methodsInfo.getIMethod2(),
                            methodsInfo.getStartOffset2(), methodsInfo.getEndOffset2(), true);
                            */

                    //template.updateSourceFiles();
                    if (!template.hasUnrefactorableNodePair()
                            && templateNames.add(template.getTemplatName())
                            && template.comesFromSamePackage()
                            && template.isRefactorable()) {
                        refactorableTemplates.add(template);
                    } else {
                        countSkip++;
                    }

                    // print out the refactoring template
                    System.out.println("----------------------------------------------------------");
                    //System.out.println(template);

                }

            } else {
                countType3++;
                log.info("Unable to handle CloneType: " + cloneType.toString());
            }
        }
    }

    public static void applyChanges() throws Exception {
        log.info("refactoring " + refactorableTemplates.size() + " duplicate method pairs.");
        log.info("skipping " + countSkip + " duplicate method pairs.");
        log.info("type1 count: " + countType1);
        log.info("type2 count: " + countType2);
        log.info("type3 count: " + countType3);
        for (RFTemplate template : refactorableTemplates) {
            template.updateSourceFiles();
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
