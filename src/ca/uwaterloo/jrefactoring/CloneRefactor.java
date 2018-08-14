package ca.uwaterloo.jrefactoring;

import ca.uwaterloo.jrefactoring.build.RFStatementBuilder;
import ca.uwaterloo.jrefactoring.detect.ClonePairInfo;
import ca.uwaterloo.jrefactoring.detect.PDGSubTreeMapperInfo;
import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.ASTNodeUtil;
import ca.uwaterloo.jrefactoring.utility.ExcelFileColumns;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import ca.uwaterloo.jrefactoring.utility.RenameUtil;
import ca.uwaterloo.jrefactoring.visitor.RFVisitor;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneType;
import gr.uom.java.ast.decomposition.cfg.mapping.DivideAndConquerMatcher;
import jxl.write.WritableSheet;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import jxl.write.Number;

import java.util.*;

public class CloneRefactor {

    private static Logger log = FileLogger.getLogger(CloneRefactor.class);
    private static List<RFTemplate> refactorableTemplates = new ArrayList<>();
    private static Map<String, String> templateNamingMap = new HashMap<>();
    private static List<String[]> repeatedNamingInfo = new ArrayList<>();
    private static int countType1 = 0;
    private static int countType2 = 0;
    private static int countType3 = 0;
    private static int countSkip = 0;
    private static int countGapNode = 0;
    private static int countNonTestCase = 0;
    private static int countNonRefactorable = 0;
    private static int countRepeatedNaming = 0;

    public static void refactor(ClonePairInfo pairInfo, int firstCloneNumber, int secondCloneNumber, int firstCloneRow,
                                WritableSheet copySheet) throws Exception {

        // skip some cases
        if (!pairInfo.getFirstPackage().equals(pairInfo.getSecondPackage())) {
            log.info("skip method pair not in the same package");
            return;
        } else if (pairInfo.getFirstClass().equals(pairInfo.getSecondClass())
                && pairInfo.getFirstMethodSignature().equals(pairInfo.getSecondMethodSignature())) {
            log.info("skip the pairs with same method");
            return;
        }

        // get naming
        String templateName = RenameUtil.getTemplateName(pairInfo.getFirstClass(), pairInfo.getFirstMethodSignature(),
                pairInfo.getSecondClass(), pairInfo.getSecondMethodSignature());

        String adapterName = RenameUtil.getAdapterName(pairInfo.getFirstClass(), pairInfo.getFirstMethodSignature(),
                pairInfo.getFirstPackage(), pairInfo.getSecondClass(), pairInfo.getSecondMethodSignature(),
                pairInfo.getSecondPackage());

        String[] adapterImplNamePair = RenameUtil.getAdapterImplNamePair(adapterName, pairInfo.getFirstClass(),
                pairInfo.getSecondClass(), pairInfo.getFirstMethodSignature(), pairInfo.getSecondMethodSignature());

        assert pairInfo.getPDFSubTreeMappersInfoList().size() == 1;

        log.info("start to refactor clone pair");
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

                    // check if methods are test cases
                    if (!ASTNodeUtil.isTestCase(method1) || !ASTNodeUtil.isTestCase(method2)) {
                        log.info("method pair are not test cases!");
                        countNonTestCase++;
                        countSkip++;
                        return;
                    }

                    // init refactoring template
                    ICompilationUnit cu1 = pairInfo.getICompilationUnitFirst();
                    ICompilationUnit cu2 = pairInfo.getICompilationUnitSecond();
                    RFTemplate template = new RFTemplate(ast1, method1, method2, templateName, adapterName,
                            adapterImplNamePair, cu1, cu2);

                    // construct the refactoring tree
                    RFStatement rfRoot = RFStatementBuilder.getInstance().build(root, template);

                    // check gap node
                    if (!template.isRefactorable()) {
                        countGapNode++;
                        countSkip++;
                        continue;
                    }

                    // start refactoring
                    try {
                        rfRoot.accept(new RFVisitor(template));
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.info("exception: " + e.toString());
                        countNonRefactorable++;
                        countSkip++;
                        continue;
                    }
                    template.modifyTestMethods();

                    // check repeated naming
                    if (templateNamingMap.containsKey(template.getTemplatName())) {
                        String pair1 = templateNamingMap.get(template.getTemplatName());
                        String pair2 = getMethodPairInfo(pairInfo);
                        repeatedNamingInfo.add(new String[] {pair1, pair2});
                        countRepeatedNaming++;
                        countSkip++;
                        continue;
                    } else {
                        templateNamingMap.put(template.getTemplatName(), getMethodPairInfo(pairInfo));
                    }

                    // check refactorability
                    if (template.isRefactorable()) {
                        refactorableTemplates.add(template);
                        markRefactorability(firstCloneNumber, secondCloneNumber, firstCloneRow, copySheet, 1.0);
                    } else {
                        markRefactorability(firstCloneNumber, secondCloneNumber, firstCloneRow, copySheet, -1.0);
                        countNonRefactorable++;
                        countSkip++;
                    }

                    // print out the template
                    System.out.println("----------------------------------------------------------");
                    //System.out.println(template);

                }

            } else {
                countType3++;
                log.info("Non refactorable CloneType: " + cloneType.toString());
            }
        }
    }

    public static void applyChanges() throws Exception {
        logRefactoringInfo();
        for (RFTemplate template : refactorableTemplates) {
            template.updateSourceFiles();
        }
    }

    public static void logRefactoringInfo() {
        log.info("refactoring " + refactorableTemplates.size() + " clone method pairs.");
        log.info("skipping " + countSkip + " clone method pairs:");
        log.info("\t" + countNonTestCase + " not test cases");
        log.info("\t" + countGapNode + " containing gap nodes (type3)");
        log.info("\t" + countNonRefactorable + " not refactorable");
        log.info("\t" + countRepeatedNaming + " repeated naming");
        for (String[] pair : repeatedNamingInfo) {
            log.info("\t *" + pair[0]);
            log.info("\t  " + pair[1]);
        }
        log.info("type1 count: " + countType1);
        log.info("type2 count: " + countType2);
        log.info("type3 count: " + countType3);
        int effective = refactorableTemplates.size();
        int total = effective + countNonRefactorable;
        log.info(String.format("effective refactoring ratio: %d/%d = %.2f%%",
                effective, total, effective * 100.0 / total));
    }

    private static void markRefactorability(int firstCloneNumber, int secondCloneNumber, int firstCloneRow,
                                            WritableSheet copySheet, double val) throws Exception {
        if (firstCloneNumber == 0 && secondCloneNumber == 1) {
            Number number = new Number(ExcelFileColumns.REFACTORABILITY.getColumnNumber(), firstCloneRow, val);
            copySheet.addCell(number);
        }
    }

    private static String getMethodPairInfo(ClonePairInfo pairInfo) {
        return pairInfo.getFirstPackage() + "." + pairInfo.getFirstClass() + ": " + pairInfo.getFirstMethodSignature() + " <---> "
                + pairInfo.getSecondPackage() + "." + pairInfo.getSecondClass() + ": " + pairInfo.getSecondMethodSignature();
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
