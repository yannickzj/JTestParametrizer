package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFEntity;
import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.template.MethodInvocationPair;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.*;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class RFVisitor extends ASTVisitor {

    private static Logger log = FileLogger.getLogger(RFVisitor.class);
    private static final String NEW_INSTANCE_METHOD_NAME = "newInstance";
    private static final String GET_DECLARED_CONSTRUCTOR_METHOD_NAME = "getDeclaredConstructor";

    protected RFTemplate template;
    protected AST ast;

    public RFVisitor(RFTemplate template) {
        this.template = template;
        this.ast = template.getAst();
    }

    public void preVisit(RFEntity node) {
        // default implementation: do nothing
    }

    public boolean preVisit2(RFEntity node) {
        preVisit(node);
        return true;
    }

    public void postVisit(RFEntity node) {
        // default implementation: do nothing
    }

    public boolean visit(RFStatement node) {
        return false;
    }

    public boolean visit(RFNodeDifference diff) {
        // refactor node difference
        //refactor(diff);
        //System.out.println();
        return false;
    }

    protected void pullUpToParameter(Expression node, DifferenceType diffType) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {

            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

            if (differenceTypes.contains(diffType) && differenceTypes.size() == 1) {
                Type type = ASTNodeUtil.typeFromBinding(ast, node.resolveTypeBinding());
                String variableParameter = template.addVariableParameter(type);
                SimpleName newNode = ast.newSimpleName(variableParameter);
                replaceNode(node, newNode, type);

            } else {
                throw new IllegalStateException("unexpected difference type [" + diffType + "] in expression [" + node + "]");
            }
        }

    }

    @Override
    public boolean visit(NumberLiteral node) {
        pullUpToParameter(node, DifferenceType.TYPE_COMPATIBLE_REPLACEMENT);
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {

            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

            if (differenceTypes.contains(DifferenceType.VARIABLE_NAME_MISMATCH)) {
                String name1 = node.getFullyQualifiedName();
                String name2 = ((Name) diff.getExpr2()).getFullyQualifiedName();
                String resolvedName = template.resolveVariableName(name1, name2);
                SimpleName newNode = ast.newSimpleName(resolvedName);

                Type type;
                if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                    String genericTypeName = template.resolveTypePair(diff.getTypePair());
                    type = ast.newSimpleType(ast.newSimpleName(genericTypeName));

                } else {
                    type = ASTNodeUtil.typeFromBinding(ast, node.resolveTypeBinding());
                }
                replaceNode(node, newNode, type);

            } else if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                String genericTypeName = template.resolveTypePair(diff.getTypePair());
                Type type = ast.newSimpleType(ast.newSimpleName(genericTypeName));

                if (node.getParent() instanceof Type) {
                    replaceNode(node, ast.newSimpleName(genericTypeName), type);
                } else {
                    // same variable name but different type
                    String name = node.getFullyQualifiedName();
                    template.resolveVariableName(name, name);
                    node.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, type);
                }

            } else {
                throw new IllegalStateException("unexpected name mismatch!");
            }

        }
        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        pullUpToParameter(node, DifferenceType.VARIABLE_NAME_MISMATCH);
        return false;
    }

    protected RFNodeDifference retrieveDiffInTypeNode(Type type) {
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType) type;
            return (RFNodeDifference) simpleType.getName().getProperty(ASTNodeUtil.PROPERTY_DIFF);

        } else {
            throw new IllegalStateException("unexpected Type type when retrieving diff!");
        }
    }

    protected void replaceNode(ASTNode oldNode, ASTNode newNode, Type newNodeType) {
        StructuralPropertyDescriptor structuralPropertyDescriptor = oldNode.getLocationInParent();
        newNode.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, newNodeType);
        if (structuralPropertyDescriptor.isChildListProperty()) {
            List<ASTNode> arguments = (List<ASTNode>) oldNode.getParent().getStructuralProperty(structuralPropertyDescriptor);
            int index = arguments.indexOf(oldNode);
            arguments.remove(oldNode);
            arguments.add(index, newNode);

        } else {
            oldNode.getParent().setStructuralProperty(structuralPropertyDescriptor, newNode);
        }
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {

        // visit arguments
        List<Expression> arguments = node.arguments();
        for (Expression argument : arguments) {
            argument.accept(this);
        }

        RFNodeDifference diff = retrieveDiffInTypeNode(node.getType());
        if (diff != null) {

            // resolve generic type
            String genericTypeName = template.resolveTypePair(diff.getTypePair());
            String clazzName = template.resolveGenericType(genericTypeName);

            // replace initializer
            MethodInvocation newInstanceMethodInvocation = ast.newMethodInvocation();
            MethodInvocation getDeclaredConstructorMethodInvocation = ast.newMethodInvocation();
            newInstanceMethodInvocation.setName(ast.newSimpleName(NEW_INSTANCE_METHOD_NAME));
            getDeclaredConstructorMethodInvocation.setName(ast.newSimpleName(GET_DECLARED_CONSTRUCTOR_METHOD_NAME));

            if (arguments.size() > 0) {
                newInstanceMethodInvocation.setExpression(getDeclaredConstructorMethodInvocation);
                getDeclaredConstructorMethodInvocation.setExpression(ast.newSimpleName(clazzName));
            } else {
                newInstanceMethodInvocation.setExpression(ast.newSimpleName(clazzName));
            }
            replaceNode(node, newInstanceMethodInvocation, ast.newSimpleType(ast.newSimpleName(genericTypeName)));

            // copy parameters
            for (Expression argument : arguments) {
                TypeLiteral typeLiteral = ast.newTypeLiteral();
                Type type = ASTNodeUtil.typeFromBinding(ast, argument.resolveTypeBinding());
                typeLiteral.setType(type);
                getDeclaredConstructorMethodInvocation.arguments().add(typeLiteral);
                Expression newArg = (Expression) ASTNode.copySubtree(ast, argument);
                newInstanceMethodInvocation.arguments().add(newArg);
            }

        }

        return false;
    }

    protected boolean containsDiff(Expression node) {
        if (node instanceof Name) {
            return node.getProperty(ASTNodeUtil.PROPERTY_DIFF) != null;
        } else if (node instanceof MethodInvocation) {
            return containsDiff(((MethodInvocation) node).getExpression())
                    || containsDiff(((MethodInvocation) node).getName());
        } else {
            throw new IllegalStateException("unexpected expression node: " + node);
        }
    }

    protected RFNodeDifference retrieveDiffInMethodInvacation(Expression node) {
        if (node == null)
            return null;

        if (node instanceof Name) {
            return (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        } else if (node instanceof MethodInvocation) {
            RFNodeDifference diff = retrieveDiffInMethodInvacation(((MethodInvocation) node).getExpression());
            if (diff != null) {
                return diff;
            } else {
                return retrieveDiffInMethodInvacation(((MethodInvocation) node).getName());
            }
        } else {
            throw new IllegalStateException("unexpected expression node: " + node);
        }
    }

    protected RFNodeDifference retrieveDiffInName(Name name) {
        return (RFNodeDifference) name.getProperty(ASTNodeUtil.PROPERTY_DIFF);
    }

    private List<String> getArgTypeNames(List<Expression> arguments) {
        List<String> argTypeNames = new ArrayList<>();
        for (Expression argument: arguments) {
            Type type = ASTNodeUtil.typeFromBinding(ast, argument.resolveTypeBinding());
            argTypeNames.add(type.toString());
        }
        return argTypeNames;
    }

    @Override
    public boolean visit(MethodInvocation node) {

        List<Expression> arguments1 = node.arguments();
        List<String> argTypeNames1 = getArgTypeNames(arguments1);

        // refactor arguments
        for (Expression argument : arguments1) {
            argument.accept(this);
        }

        Expression expr1 = node.getExpression();
        SimpleName name1 = node.getName();
        RFNodeDifference diffInExpr = retrieveDiffInMethodInvacation(expr1);
        RFNodeDifference diffInName = retrieveDiffInName(name1);
        if (diffInExpr != null || diffInName != null) {

            // retrieve paired method invocation node
            MethodInvocation pairedNode;
            if (diffInExpr != null) {
                pairedNode = (MethodInvocation) diffInExpr.getExpr2().getParent();
            } else {
                pairedNode = (MethodInvocation) diffInName.getExpr2().getParent();
            }

            // construct method invocation pair
            Type exprType1 = ASTNodeUtil.typeFromExpr(ast, expr1);
            Type exprType2 = ASTNodeUtil.typeFromExpr(ast, pairedNode.getExpression());
            SimpleName name2 = pairedNode.getName();
            List<String> argTypeNames2 = getArgTypeNames(pairedNode.arguments());
            MethodInvocationPair methodInvocationPair = new MethodInvocationPair(exprType1.toString(), name1.getIdentifier(), argTypeNames1,
                    exprType2.toString(), name2.getIdentifier(), argTypeNames2);

            // refactor the method invocation expression
            expr1.accept(this);

            // create new method invocation in adapter
            MethodInvocation newMethod = template.createAdapterActionMethod(node.getExpression(), arguments1, methodInvocationPair);

            // replace the old method
            Type type = ASTNodeUtil.typeFromBinding(ast, node.resolveTypeBinding());
            replaceNode(node, newMethod, type);

        }

        return false;
    }

    public void endVisit(RFStatement node) {
        // if current node is the top level statement, copy the refactored node to the template
        if (node.isTopStmt()) {
            node.getTemplate().addStatement((Statement) ASTNode.copySubtree(ast, node.getStatement1()));
        }
    }

    public void endVisit(RFNodeDifference node) {
    }

}
