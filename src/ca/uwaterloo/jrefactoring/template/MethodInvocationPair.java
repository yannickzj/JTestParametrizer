package ca.uwaterloo.jrefactoring.template;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MethodInvocationPair {
    private String exprType1;
    private String exprType2;
    private String name1;
    private String name2;
    private List<String> argTypeNames1;
    private List<String> argTypeNames2;

    public MethodInvocationPair(String expr1, String name1, List<String> argTypeNames1,
                                String expr2, String name2, List<String> argTypeNames2) {
        this.exprType1 = expr1;
        this.name1 = name1;
        this.argTypeNames1 = argTypeNames1;

        this.exprType2 = expr2;
        this.name2 = name2;
        this.argTypeNames2 = argTypeNames2;
    }

    @Override
    public int hashCode() {
        int hash1 = Arrays.hashCode(argTypeNames1.toArray());
        int hash2 = Arrays.hashCode(argTypeNames2.toArray());
        return Objects.hash(exprType1, exprType2, name1, name2, hash1, hash2);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MethodInvocationPair)) {
            return false;
        }
        MethodInvocationPair other = (MethodInvocationPair) o;
        if (argTypeNames1.size() != other.argTypeNames1.size() || argTypeNames2.size() != other.argTypeNames2.size()) {
            return false;
        } else {
            for (int i = 0; i < argTypeNames1.size(); i++) {
                if (!argTypeNames1.get(i).equals(other.argTypeNames1.get(i))) {
                    return false;
                }
            }
            for (int i = 0; i < argTypeNames2.size(); i++) {
                if (!argTypeNames2.get(i).equals(other.argTypeNames2.get(i))) {
                    return false;
                }
            }
        }

        return exprType1.equals(other.exprType1) && exprType2.equals(other.exprType2)
                && name1.equals(other.name1) && name2.equals(other.name2);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(exprType1 + " <---> " + exprType2 + ", ");
        sb.append(name1 + " <---> " + name2 + ", ");
        for (int i = 0; i < argTypeNames1.size(); i++) {
            sb.append(argTypeNames1.get(i) + " <---> " + argTypeNames2.get(i) + "\n");
        }
        return sb.toString();
    }
}
