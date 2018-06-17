package ca.uwaterloo.jrefactoring.utility;

import ca.uwaterloo.jrefactoring.build.RFStatementBuilder;
import ca.uwaterloo.jrefactoring.mapping.ClonePairInfo;
import ca.uwaterloo.jrefactoring.mapping.PDGSubTreeMapperInfo;
import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.template.GenericManager;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.visitor.ChildrenVisitor;
import gr.uom.java.ast.decomposition.cfg.mapping.CloneStructureNode;
import gr.uom.java.ast.decomposition.cfg.mapping.DivideAndConquerMatcher;
import org.slf4j.Logger;

public class TemplateRefactor {

    private static Logger log = FileLogger.getLogger(TemplateRefactor.class);

    public static void refactor(ClonePairInfo pairInfo) {
        for (PDGSubTreeMapperInfo mapperInfo : pairInfo.getPDFSubTreeMappersInfoList()) {
            DivideAndConquerMatcher mapper = mapperInfo.getMapper();
            log.info("CloneType: " + mapper.getCloneType().toString());
            analyze(mapper.getCloneStructureRoot());
        }
    }

    private static void analyze(CloneStructureNode root) {
        if (root != null) {
            int apilevel = getAPILevel(root);
            log.info("AST API level: " + apilevel);
            RFTemplate template = new RFTemplate(apilevel, new GenericManager());

            RFStatement rfRoot = RFStatementBuilder.getInstance().build(root, template);
            rfRoot.accept(new ChildrenVisitor());

            System.out.println("----------------------------------------------------------");
            System.out.println(template);
        }
    }

    private static int getAPILevel(CloneStructureNode root) {
        int apilevel = -1;
        if (root.getMapping() != null && root.getMapping().getNodeG1() != null) {
            return root.getMapping().getNodeG1().getASTStatement().getAST().apiLevel();
        } else {
            for (CloneStructureNode child: root.getChildren()) {
                apilevel = getAPILevel(child);
                if (apilevel != -1) {
                    return apilevel;
                }
            }
        }
        return apilevel;
    }

}
