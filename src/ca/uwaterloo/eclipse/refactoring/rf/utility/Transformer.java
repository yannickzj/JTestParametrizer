package ca.uwaterloo.eclipse.refactoring.rf.utility;

import ca.uwaterloo.eclipse.refactoring.rf.node.RFNodeDifference;
import ca.uwaterloo.eclipse.refactoring.rf.strategy.DefaultStrategy;
import ca.uwaterloo.eclipse.refactoring.rf.strategy.Strategy;

public class Transformer {

    private static Strategy strategy = new DefaultStrategy();

    public static Strategy getStrategy() {
        return strategy;
    }

    public static void setStrategy(Strategy strategy) {
        Transformer.strategy = strategy;
    }

    public static void transform(RFNodeDifference diff) {
        strategy.execute(diff);
    }
}
