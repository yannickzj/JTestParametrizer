package ca.uwaterloo.refactoring.utility;

import ca.uwaterloo.refactoring.node.RFNodeDifference;
import ca.uwaterloo.refactoring.strategy.DefaultStrategy;
import ca.uwaterloo.refactoring.strategy.Strategy;

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
