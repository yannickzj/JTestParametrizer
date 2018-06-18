package ca.uwaterloo.jrefactoring.action;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;

public interface Action {
    void execute(RFNodeDifference diff);
}
