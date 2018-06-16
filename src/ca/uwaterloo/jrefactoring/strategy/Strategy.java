package ca.uwaterloo.jrefactoring.strategy;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;

public interface Strategy {
    void execute(RFNodeDifference diff);
}
