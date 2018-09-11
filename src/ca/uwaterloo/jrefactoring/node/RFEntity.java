package ca.uwaterloo.jrefactoring.node;

import ca.uwaterloo.jrefactoring.visitor.RFVisitor;

public interface RFEntity {
    void accept(RFVisitor visitor);
}
