package ca.uwaterloo.eclipse.refactoring.rf.strategy;

import ca.uwaterloo.eclipse.refactoring.rf.node.RFNodeDifference;

public interface Strategy {
    void execute(RFNodeDifference diff);
}
