package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.*;
import ca.uwaterloo.jrefactoring.template.MethodInvocationPair;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.utility.ASTNodeUtil;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import gr.uom.java.ast.decomposition.matching.DifferenceType;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RFVisitor extends ASTVisitor {

    private static Logger log = FileLogger.getLogger(RFVisitor.class);
    private static final String NEW_INSTANCE_METHOD_NAME = "newInstance";
    private static final String GET_DECLARED_CONSTRUCTOR_METHOD_NAME = "getDeclaredConstructor";

    protected RFTemplate template;
    protected AST ast;

    public RFVisitor(RFTemplate template) {
        this.template = template;
        this.ast = template.getAst();
    }

    private void preVisit(RFEntity node) {
        // default implementation: do nothing
    }

    public boolean preVisit2(RFEntity node) {
        preVisit(node);
        return true;
    }

    public void postVisit(RFEntity node) {
        // default implementation: do nothing
    }

    private void replaceNode(ASTNode oldNode, ASTNode newNode, Type newNodeType) {
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

    private void pullUpToParameter(Expression node, DifferenceType diffType) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {

            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

            if (diffType == null || (differenceTypes.contains(diffType) && differenceTypes.size() == 1)) {
                Type type = ASTNodeUtil.typeFromBinding(ast, node.resolveTypeBinding());
                String variableParameter = template.addVariableParameter(type);
                SimpleName newNode = ast.newSimpleName(variableParameter);
                replaceNode(node, newNode, type);

            } else {
                throw new IllegalStateException("unexpected difference type [" + diffType.name() + "] in expression [" + node + "]");
            }
        }

    }

    private List<String> getArgTypeNames(List<Expression> arguments) {
        List<String> argTypeNames = new ArrayList<>();
        for (Expression argument : arguments) {
            Type type = ASTNodeUtil.typeFromBinding(ast, argument.resolveTypeBinding());
            argTypeNames.add(type.toString());
        }
        return argTypeNames;
    }

    private RFNodeDifference retrieveDiffInTypeNode(Type type) {
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType) type;
            return (RFNodeDifference) simpleType.getName().getProperty(ASTNodeUtil.PROPERTY_DIFF);

        } else {
            throw new IllegalStateException("unexpected Type type when retrieving diff!");
        }
    }

    private RFNodeDifference retrieveDiffInMethodInvocation(Expression node) {
        if (node == null)
            return null;

        if (node instanceof Name) {
            return (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        } else if (node instanceof MethodInvocation) {
            RFNodeDifference diff = retrieveDiffInMethodInvocation(((MethodInvocation) node).getExpression());
            if (diff != null) {
                return diff;
            } else {
                return retrieveDiffInMethodInvocation(((MethodInvocation) node).getName());
            }
        } else {
            throw new IllegalStateException("unexpected expression node: " + node);
        }
    }

    private RFNodeDifference retrieveDiffInName(Name name) {
        return (RFNodeDifference) name.getProperty(ASTNodeUtil.PROPERTY_DIFF);
    }

    private boolean containsDiff(Expression node) {
        if (node instanceof Name) {
            return node.getProperty(ASTNodeUtil.PROPERTY_DIFF) != null;
        } else if (node instanceof MethodInvocation) {
            return containsDiff(((MethodInvocation) node).getExpression())
                    || containsDiff(((MethodInvocation) node).getName());
        } else {
            throw new IllegalStateException("unexpected expression node: " + node);
        }
    }

    @Override
    public boolean visit(NumberLiteral node) {
        pullUpToParameter(node, null);
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
                    String genericTypeName = template.resolveTypePair(diff.getTypePair(), true);
                    type = ast.newSimpleType(ast.newSimpleName(genericTypeName));

                } else {
                    type = ASTNodeUtil.typeFromBinding(ast, node.resolveTypeBinding());
                }
                replaceNode(node, newNode, type);

            } else if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                String genericTypeName = template.resolveTypePair(diff.getTypePair(), true);
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

    @Override
    public boolean visit(ClassInstanceCreation node) {

        // refactor arguments
        List<Expression> arguments = node.arguments();
        for (Expression argument : arguments) {
            argument.accept(this);
        }

        RFNodeDifference diff = retrieveDiffInTypeNode(node.getType());
        if (diff != null) {

            // resolve generic type
            String genericTypeName = template.resolveTypePair(diff.getTypePair(), false);
            String clazzName = template.resolveGenericType(genericTypeName);

            // add generic type relation if declaration type is not the same as the instance creation type
            Type declarationType = template.getTypeByInstanceCreation(node);
            if (declarationType != null && !declarationType.toString().equals(genericTypeName)) {
                template.addGenericTypeBound(genericTypeName, declarationType.toString());
            }

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
        RFNodeDifference diffInExpr = retrieveDiffInMethodInvocation(expr1);
        RFNodeDifference diffInName = retrieveDiffInName(name1);

        // check if common super class can be used
        if (diffInName == null && diffInExpr != null) {

            Set<DifferenceType> differenceTypes = diffInExpr.getDifferenceTypes();

            if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                ITypeBinding commonSuperClass = template.getLowestCommonSubClass(diffInExpr.getTypePair());
                if (commonSuperClass != null) {
                    for (IMethodBinding methodBinding : commonSuperClass.getDeclaredMethods()) {
                        if (methodBinding.getName().equals(name1.getIdentifier())) {
                            log.info("Same method found in common super class [" +
                                    commonSuperClass.getQualifiedName() + "]: " + methodBinding.getName());
                            expr1.accept(this);
                            return false;
                        }
                    }
                }

            } else {
                log.info("resolve method expression diff without creating new adapter action");
                expr1.accept(this);
                return false;
            }

        }

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
            MethodInvocationPair methodInvocationPair = new MethodInvocationPair(exprType1.toString(),
                    name1.getIdentifier(), argTypeNames1, exprType2.toString(), name2.getIdentifier(), argTypeNames2);

            // refactor the method invocation expression
            expr1.accept(this);

            // create new method invocation in adapter
            Type returnType = ASTNodeUtil.typeFromExpr(ast, node);
            MethodInvocation newMethod = template.createAdapterActionMethod(node.getExpression(), arguments1,
                    methodInvocationPair, returnType);

            // replace the old method
            Type type = ASTNodeUtil.typeFromBinding(ast, node.resolveTypeBinding());
            replaceNode(node, newMethod, type);

        }

        return false;
    }

    public boolean visit(RFVariableDeclarationStmt node) {
        if (node.hasDifference()) {
            //node.describe();

            VariableDeclarationStatement stmt1 = (VariableDeclarationStatement) node.getStatement1();
            Type type = stmt1.getType();
            type.accept(this);

            for (Object fragment : stmt1.fragments()) {
                VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
                SimpleName name = variableDeclarationFragment.getName();
                name.accept(this);

                Expression initializer = variableDeclarationFragment.getInitializer();
                if (initializer instanceof ClassInstanceCreation) {
                    template.addInstanceCreation((ClassInstanceCreation) initializer, stmt1.getType());
                }
                initializer.accept(this);
            }

            //System.out.println("variableDeclarationStmtVisitor finish visiting");
        }
        return true;
    }

    public boolean visit(RFExpressionStmt node) {
        if (node.hasDifference()) {
            //node.describe();
            ExpressionStatement expressionStatement = (ExpressionStatement) node.getStatement1();
            expressionStatement.getExpression().accept(this);
            //System.out.println("expressionStmtVisitor finish visiting");
        }
        return true;
    }

    public boolean visit(RFIfStmt node) {
        if (node.hasDifference()) {
            IfStatement ifStatement = (IfStatement) node.getStatement1();
            ifStatement.getExpression().accept(this);
        }
        return true;
    }

    public void endVisit(RFStatement node) {
        // if current node is the top level statement, copy the refactored node to the template
        if (node.isTopStmt()) {
            node.getTemplate().addStatement((Statement) ASTNode.copySubtree(ast, node.getStatement1()));
        }
    }

}
