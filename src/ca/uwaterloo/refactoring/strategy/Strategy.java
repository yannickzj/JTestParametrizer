package ca.uwaterloo.refactoring.strategy;

import ca.uwaterloo.refactoring.node.RFNodeDifference;

public interface Strategy {
    void execute(RFNodeDifference diff);
}
