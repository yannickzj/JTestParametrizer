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

    private void pullUpToParameter(Expression node) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {
            Type type = ASTNodeUtil.typeFromExpr(ast, node);
            String variableParameter = template.addVariableParameter(type);
            SimpleName newNode = ast.newSimpleName(variableParameter);
            replaceNode(node, newNode, type);
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
        pullUpToParameter(node);
        return false;
    }

    @Override
    public boolean visit(BooleanLiteral node) {
        pullUpToParameter(node);
        return false;
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        pullUpToParameter(node);
        return false;
    }

    @Override
    public boolean visit(StringLiteral node) {
        pullUpToParameter(node);
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {

            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

            // resolve type
            Type type;
            if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)
                    || differenceTypes.contains(DifferenceType.VARIABLE_TYPE_MISMATCH)) {
                String genericTypeName = template.resolveTypePair(diff.getTypePair(), true);
                log.info("genericTypeName: " + genericTypeName);
                type = ast.newSimpleType(ast.newSimpleName(genericTypeName));
            } else {
                type = ASTNodeUtil.typeFromBinding(ast, node.resolveTypeBinding());
            }

            // resolve name
            if (differenceTypes.contains(DifferenceType.VARIABLE_NAME_MISMATCH)) {
                String name1 = node.getFullyQualifiedName();
                String name2 = ((Name) diff.getExpr2()).getFullyQualifiedName();
                String resolvedName = template.resolveVariableName(name1, name2);
                SimpleName newNode = ast.newSimpleName(resolvedName);
                replaceNode(node, newNode, type);

            } else {
                node.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, type);
            }

            /*
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
            */

        }
        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        pullUpToParameter(node);
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        Name name = node.getName();
        RFNodeDifference diff = (RFNodeDifference) name.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {

            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();
            if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                String genericTypeName = template.resolveTypePair(diff.getTypePair(), true);
                Type type = ast.newSimpleType(ast.newSimpleName(genericTypeName));
                replaceNode(name, ast.newSimpleName(genericTypeName), type);

            } else {
                throw new IllegalStateException("unexpected difference type in SimpleType!");
            }
        }
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
            node.describe();
            VariableDeclarationStatement stmt1 = (VariableDeclarationStatement) node.getStatement1();

            // refactor type
            stmt1.getType().accept(this);

            List<VariableDeclarationFragment> fragments = stmt1.fragments();
            for (VariableDeclarationFragment fragment : fragments) {

                // refactor name
                SimpleName name = fragment.getName();
                name.accept(this);

                Expression initializer = fragment.getInitializer();

                /**
                 * if initializer contains TYPE_COMPATIBAL_REPLACEMENT diff, create empter adapter action,
                 * action arguments should be specified manually
                 */
                RFNodeDifference diff = (RFNodeDifference) initializer.getProperty(ASTNodeUtil.PROPERTY_DIFF);
                if (diff != null) {
                    if (diff.getDifferenceTypes().contains(DifferenceType.TYPE_COMPATIBLE_REPLACEMENT)) {
                        Type type = stmt1.getType();
                        MethodInvocation methodInvocation = template.createAdapterActionMethod(type);
                        log.info("adapter action arguments should be specified manually in " + methodInvocation);
                        replaceNode(initializer, methodInvocation, type);
                        continue;
                    }
                }

                // register ClassInstanceCreation initializer
                if (initializer instanceof ClassInstanceCreation) {
                    template.addInstanceCreation((ClassInstanceCreation) initializer, stmt1.getType());
                }
                initializer.accept(this);
            }
        }
        return true;
    }

    public boolean visit(RFIfStmt node) {
        if (node.hasDifference()) {
            IfStatement ifStatement = (IfStatement) node.getStatement1();
            // only visit expression
            ifStatement.getExpression().accept(this);
        }
        return true;
    }

    public boolean visit(RFForStmt node) {
        if (node.hasDifference()) {
            ForStatement forStatement = (ForStatement) node.getStatement1();

            // refactor initializers
            List<Expression> initializers = forStatement.initializers();
            for (Expression initializer : initializers) {
                initializer.accept(this);
            }

            // refactor expression
            forStatement.getExpression().accept(this);

            // refactor updaters
            List<Expression> updates = forStatement.updaters();
            for (Expression update : updates) {
                update.accept(this);
            }
        }
        return true;
    }

    public boolean visit(RFEnhancedForStmt node) {
        if (node.hasDifference()) {
            EnhancedForStatement enhancedForStatement = (EnhancedForStatement) node.getStatement1();

            // only visit expression and parameter, ignore body
            enhancedForStatement.getExpression().accept(this);
            enhancedForStatement.getParameter().accept(this);
        }
        return true;
    }

    public boolean visit(RFWhileStmt node) {
        if (node.hasDifference()) {
            Statement statement = node.getStatement1();
            if (statement instanceof WhileStatement) {
                WhileStatement whileStatement = (WhileStatement) statement;
                // only visit expression
                whileStatement.getExpression().accept(this);

            } else if (statement instanceof DoStatement) {
                DoStatement doStatement = (DoStatement) statement;
                // only visit expression
                doStatement.getExpression().accept(this);

            } else {
                throw new IllegalStateException("unexpected RFWhileStmt: " + node);
            }
        }
        return true;
    }

    public boolean visit(RFTryStmt node) {
        if (node.hasDifference()) {

            // ignore body and finally
            TryStatement tryStatement = (TryStatement) node.getStatement1();

            // refactor resources
            List<Expression> resources = tryStatement.resources();
            for (Expression resource : resources) {
                resource.accept(this);
            }

            // refactor catchClauses
            List<CatchClause> catchClauses = tryStatement.catchClauses();
            for (CatchClause catchClause : catchClauses) {
                catchClause.accept(this);
            }
        }
        return true;
    }

    public boolean visit(RFLabeledStmt node) {
        if (node.hasDifference()) {
            LabeledStatement labeledStatement = (LabeledStatement) node.getStatement1();
            // only visit label
            labeledStatement.getLabel().accept(this);
        }

        return true;
    }

    public boolean visit(RFSynchronizedStmt node) {
        if (node.hasDifference()) {
            SynchronizedStatement synchronizedStatement = (SynchronizedStatement) node.getStatement1();
            // only visit expression
            synchronizedStatement.getExpression().accept(this);
        }

        return true;
    }

    public boolean visit(RFSwitchStmt node) {
        if (node.hasDifference()) {
            SwitchStatement switchStatement = (SwitchStatement) node.getStatement1();
            // only visit expression, ignore switch case
            switchStatement.getExpression().accept(this);
        }
        return true;
    }

    public boolean visit(RFDefaultStmt node) {
        if (node.hasDifference()) {
            node.getStatement1().accept(this);
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
