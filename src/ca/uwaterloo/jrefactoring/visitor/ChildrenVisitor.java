package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

public class ChildrenVisitor extends RFVisitor {

    private static Logger log = FileLogger.getLogger(ChildrenVisitor.class);

    public ChildrenVisitor(RFTemplate template) {
        super(template);
    }

    @Override
    public boolean visit(RFStatement node) {
        return true;
    }

}
