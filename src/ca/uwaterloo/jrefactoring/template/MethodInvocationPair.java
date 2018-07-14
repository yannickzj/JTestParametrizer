package ca.uwaterloo.jrefactoring.template;

import ca.uwaterloo.jrefactoring.utility.ASTNodeUtil;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MethodInvocationPair {
    private Expression expr1;
    private Expression expr2;
    private SimpleName name1;
    private SimpleName name2;
    private List<Expression> argument1;
    private List<Expression> argument2;
    private List<String> argTypeNames1;
    private List<String> argTypeNames2;
    //private List<Expression> argument;
    private Type exprType1;
    private Type exprType2;

    public MethodInvocationPair(Expression expr1, SimpleName name1, Expression expr2, SimpleName name2,
                                List<String> argTypeNames1, List<String> argTypeNames2,
                                List<Expression> argument1, List<Expression> argument2) {
        this.expr1= expr1;
        this.name1 = name1;
        this.expr2 = expr2;
        this.name2 = name2;
        this.argTypeNames1 = argTypeNames1;
        this.argTypeNames2 = argTypeNames2;
        this.argument1 = argument1;
        this.argument2 = argument2;
        if (expr1 != null) {
            this.exprType1 = ASTNodeUtil.typeFromExpr(expr1.getAST(), expr1);
        }
        if (expr2 != null) {
            this.exprType2 = ASTNodeUtil.typeFromExpr(expr2.getAST(), expr2);
        }
    }

    public Expression getExpr1() {
        return expr1;
    }

    public Expression getExpr2() {
        return expr2;
    }

    public SimpleName getName1() {
        return name1;
    }

    public SimpleName getName2() {
        return name2;
    }

    public List<String> getArgTypeNames1() {
        return argTypeNames1;
    }

    public List<String> getArgTypeNames2() {
        return argTypeNames2;
    }

    public Type getExprType1() {
        return exprType1;
    }

    public Type getExprType2() {
        return exprType2;
    }

    public List<Expression> getArgument1() {
        return argument1;
    }

    public List<Expression> getArgument2() {
        return argument2;
    }

    @Override
    public int hashCode() {
        int hash1 = Arrays.hashCode(argTypeNames1.toArray());
        int hash2 = Arrays.hashCode(argTypeNames2.toArray());
        if (exprType1 != null && exprType2 != null) {
            return Objects.hash(exprType1.toString(), exprType2.toString(),
                    name1.getIdentifier(), name2.getIdentifier(), hash1, hash2);
        } else {
            return Objects.hash(name1.getIdentifier(), name2.getIdentifier(), hash1, hash2);
        }
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

        if (exprType1 == other.exprType1 && exprType2 == other.exprType2) {
            return name1.getIdentifier().equals(other.name1.getIdentifier())
                 && name2.getIdentifier().equals(other.name2.getIdentifier());
        }

        return exprType1.toString().equals(other.exprType1.toString())
                && exprType2.toString().equals(other.exprType2.toString())
                && name1.getIdentifier().equals(other.name1.getIdentifier())
                && name2.getIdentifier().equals(other.name2.getIdentifier());
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
