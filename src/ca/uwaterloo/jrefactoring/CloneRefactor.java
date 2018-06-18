package ca.uwaterloo.jrefactoring;

import ca.uwaterloo.jrefactoring.build.RFStatementBuilder;
import ca.uwaterloo.jrefactoring.detect.ClonePairInfo;
import ca.uwaterloo.jrefactoring.detect.PDGSubTreeMapperInfo;
import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import ca.uwaterloo.jrefactoring.visitor.ChildrenVisitor;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneType;
import gr.uom.java.ast.decomposition.cfg.mapping.DivideAndConquerMatcher;
import org.eclipse.jdt.core.dom.AST;
import org.slf4j.Logger;

public class CloneRefactor {

    private static Logger log = FileLogger.getLogger(CloneRefactor.class);

    public static void refactor(ClonePairInfo pairInfo) {

        for (PDGSubTreeMapperInfo mapperInfo : pairInfo.getPDFSubTreeMappersInfoList()) {
            // achieve the clone structure root node and clone type
            DivideAndConquerMatcher matcher = mapperInfo.getMapper();
            CloneType cloneType = matcher.getCloneType();
            CloneStructureNode root = matcher.getCloneStructureRoot();

            // perform source-to-source refactoring transformation
            log.info("CloneType: " + cloneType.toString());
            transform(root);
        }
    }

    private static void transform(CloneStructureNode root) {
        if (root != null) {
            // create the refactoring template
            RFTemplate template = new RFTemplate(getAST1(root));

            // construct the refactoring tree
            RFStatement rfRoot = RFStatementBuilder.getInstance().build(root, template);

            // let the root node accept the children visitor
            rfRoot.accept(new ChildrenVisitor());

            // print out the refactoring template
            System.out.println("----------------------------------------------------------");
            System.out.println(template);
        }
    }

    private static AST getAST1(CloneStructureNode root) {
        AST ast = null;
        if (root.getMapping() != null && root.getMapping().getNodeG1() != null) {
            return root.getMapping().getNodeG1().getASTStatement().getAST();
        } else {
            for (CloneStructureNode child: root.getChildren()) {
                ast = getAST1(child);
                if (ast != null) {
                    return ast;
                }
            }
        }
        return ast;
    }
}
