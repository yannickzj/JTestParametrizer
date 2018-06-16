package ca.uwaterloo.jrefactoring.utility;

import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.strategy.DefaultStrategy;
import ca.uwaterloo.jrefactoring.strategy.Strategy;

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
