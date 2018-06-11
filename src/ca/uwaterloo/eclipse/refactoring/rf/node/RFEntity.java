package ca.uwaterloo.eclipse.refactoring.rf.node;

import ca.uwaterloo.eclipse.refactoring.rf.visitor.RFVisitor;

public abstract class RFEntity {

    public final void accept(RFVisitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException();
        }
        // begin with the generic pre-visit
        if (visitor.preVisit2(this)) {
            // dynamic dispatch to internal method for type-specific visit/endVisit
            accept0(visitor);
        }
        // end with the generic post-visit
        visitor.postVisit(this);
    }

    abstract void accept0(RFVisitor visitor);

}
