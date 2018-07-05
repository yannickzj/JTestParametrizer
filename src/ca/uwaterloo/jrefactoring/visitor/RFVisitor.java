package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.*;
import ca.uwaterloo.jrefactoring.template.MethodInvocationPair;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.template.TypePair;
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
            template.addTemplateArgumentPair(node, diff.getExpr2());
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

    /*
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
    */

    private byte getPrimitiveNum(PrimitiveType p) {
        if (p.getPrimitiveTypeCode() == PrimitiveType.BYTE) {
            return 1;
        } else if (p.getPrimitiveTypeCode() == PrimitiveType.SHORT) {
            return 2;
        } else if (p.getPrimitiveTypeCode() == PrimitiveType.CHAR) {
            return 3;
        } else if (p.getPrimitiveTypeCode() == PrimitiveType.INT) {
            return 4;
        } else if (p.getPrimitiveTypeCode() == PrimitiveType.LONG) {
            return 5;
        } else if (p.getPrimitiveTypeCode() == PrimitiveType.FLOAT) {
            return 6;
        } else if (p.getPrimitiveTypeCode() == PrimitiveType.DOUBLE) {
            return 7;
        } else if (p.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
            return 8;
        } else if (p.getPrimitiveTypeCode() == PrimitiveType.VOID) {
            return 9;
        } else {
            throw new IllegalArgumentException("unexpected primitive type: " + p.toString());
        }
    }

    private PrimitiveType.Code getPrimitiveCode(byte num) {
        switch (num) {
            case 1:
                return PrimitiveType.BYTE;
            case 2:
                return PrimitiveType.SHORT;
            case 3:
                return PrimitiveType.CHAR;
            case 4:
                return PrimitiveType.INT;
            case 5:
                return PrimitiveType.LONG;
            case 6:
                return PrimitiveType.FLOAT;
            case 7:
                return PrimitiveType.DOUBLE;
            case 8:
                return PrimitiveType.BOOLEAN;
            case 9:
                return PrimitiveType.VOID;
            default:
                throw new IllegalArgumentException("unexpected primitive num: " + num);
        }
    }

    private Type getCompatibleType(TypePair typePair) {
        ITypeBinding commonSubClass = template.getLowestCommonSubClass(typePair);
        if (commonSubClass != null) {
            return ASTNodeUtil.typeFromBinding(ast, commonSubClass);
        } else {
            Type t1 = ASTNodeUtil.typeFromBinding(ast, typePair.getType1());
            Type t2 = ASTNodeUtil.typeFromBinding(ast, typePair.getType2());
            if (t1.isPrimitiveType() && t2.isPrimitiveType()) {
                byte n1 = getPrimitiveNum((PrimitiveType) t1);
                byte n2 = getPrimitiveNum((PrimitiveType) t2);
                byte n = (byte) Math.max(n1, n2);
                if (n < 8) {

                    if (n == 3 && n1 != n2) {
                        n += 1;
                    }
                    return ast.newPrimitiveType(getPrimitiveCode(n));
                }
            }

            throw new IllegalStateException("can not get compatible type for type pair: "
                    + typePair.toString());
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

            String name1 = node.getIdentifier();
            String name2 = ((SimpleName) diff.getExpr2()).getIdentifier();
            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

            if (template.containsVariableNamePair(name1, name2)) {

                // resolve name
                String resolvedName = template.resolveVariableName(name1, name2, "v");

                // resolve type
                Type type;
                if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                    String genericTypeName = template.resolveTypePair(diff.getTypePair(), true);
                    type = ast.newSimpleType(ast.newSimpleName(genericTypeName));
                } else {
                    type = ASTNodeUtil.typeFromBinding(ast, node.resolveTypeBinding());
                }

                // set name and type
                node.setIdentifier(resolvedName);
                node.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, type);

            } else {
                if (node.getParent() instanceof Expression) {
                    // create new adapter action
                    Type returnType = getCompatibleType(diff.getTypePair());
                    MethodInvocation newMethod = template.createAdapterActionMethod(node, diff.getExpr2(), returnType);
                    replaceNode(node, newMethod, returnType);

                } else {
                    throw new IllegalStateException("unexpected diff in SimpleNode: " + diff.toString());
                }
            }

            /*
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
            */

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

        if (ASTNodeUtil.hasPairedNode(node)) {

            // refactor arguments
            List<Expression> arguments = node.arguments();
            for (Expression argument : arguments) {
                if (ASTNodeUtil.hasPairedNode(argument)) {
                    argument.accept(this);
                }
            }

            //RFNodeDifference diff = retrieveDiffInTypeNode(node.getType());
            Type type = node.getType();
            if (ASTNodeUtil.hasPairedNode(type)) {

                // resolve generic type
                Type pairedType = (Type) type.getProperty(ASTNodeUtil.PROPERTY_PAIR);
                TypePair typePair = new TypePair(type.resolveBinding(), pairedType.resolveBinding());
                String genericTypeName = template.resolveTypePair(typePair, false);
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
                    typeLiteral.setType(ASTNodeUtil.typeFromBinding(ast, argument.resolveTypeBinding()));
                    getDeclaredConstructorMethodInvocation.arguments().add(typeLiteral);
                    Expression newArg = (Expression) ASTNode.copySubtree(ast, argument);
                    newInstanceMethodInvocation.arguments().add(newArg);
                }

            }
        }

        return false;
    }

    @Override
    public boolean visit(MethodInvocation node) {

        if (ASTNodeUtil.hasPairedNode(node)) {

            Expression expr1 = node.getExpression();
            SimpleName name1 = node.getName();
            List<Expression> arguments1 = node.arguments();

            // get original argument type names
            List<String> argTypeNames1 = getArgTypeNames(arguments1);
            List<Expression> originalArgs1 = ASTNode.copySubtrees(ast, arguments1);

            // refactor arguments
            for (Expression argument : arguments1) {
                if (ASTNodeUtil.hasPairedNode(argument)) {
                    argument.accept(this);
                }
            }

            // check if common super class can be used
            if (!ASTNodeUtil.hasPairedNode(name1) && ASTNodeUtil.hasPairedNode(expr1)) {

                Expression expr2 = (Expression) expr1.getProperty(ASTNodeUtil.PROPERTY_PAIR);
                TypePair typePair = new TypePair(expr1.resolveTypeBinding(), expr2.resolveTypeBinding());

                if (!typePair.isSame()) {
                    ITypeBinding commonSuperClass = template.getLowestCommonSubClass(typePair);
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
                    log.info("continue to resolve inner expression diff without creating new adapter action");
                    expr1.accept(this);
                    return false;
                }

            }

            // create new adapter action
            if (ASTNodeUtil.hasPairedNode(name1) || ASTNodeUtil.hasPairedNode(expr1)) {

                // retrieve paired method invocation node
                MethodInvocation pairedNode = (MethodInvocation) node.getProperty(ASTNodeUtil.PROPERTY_PAIR);

                // construct method invocation pair
                List<Expression> originalArgs2 = pairedNode.arguments();
                List<String> argTypeNames2 = getArgTypeNames(pairedNode.arguments());
                MethodInvocationPair methodInvocationPair = new MethodInvocationPair(expr1, name1,
                        pairedNode.getExpression(), pairedNode.getName(), argTypeNames1, argTypeNames2,
                        originalArgs1, originalArgs2);

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
        }

        return false;
    }

    @Override
    public boolean visit(VariableDeclarationExpression node) {

        // refactor type
        Type type = node.getType();
        if (ASTNodeUtil.hasPairedNode(type)) {
            type.accept(this);
        }

        // refactor fragments
        refactorVariableDeclarationFragment(node.getType(), node.fragments(), "i");

        return false;
    }

    private boolean refactorTypeCompatibleReplacement(Expression initializer, Type type) {
        /**
         * if initializer contains TYPE_COMPATIBAL_REPLACEMENT diff, create empter adapter action,
         * action arguments should be specified manually
         */
        RFNodeDifference diff = (RFNodeDifference) initializer.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {
            if (diff.getDifferenceTypes().contains(DifferenceType.TYPE_COMPATIBLE_REPLACEMENT)) {
                MethodInvocation methodInvocation = template.createAdapterActionMethod(type);
                log.info("adapter action arguments should be specified manually in " + methodInvocation);
                replaceNode(initializer, methodInvocation, type);
                return true;
            }
        }

        return false;
    }

    private void refactorVariableDeclarationFragment(Type type, List<VariableDeclarationFragment> fragments, String prefix) {

        for (VariableDeclarationFragment fragment : fragments) {

            if (ASTNodeUtil.hasPairedNode(fragment)) {

                // refactor name and register variable
                SimpleName simpleName = fragment.getName();
                if (ASTNodeUtil.hasPairedNode(simpleName)) {
                    registerVariablePair(simpleName, prefix);
                }

                Expression initializer = fragment.getInitializer();
                if (ASTNodeUtil.hasPairedNode(initializer)) {
                    // check TYPE_COMPATIBLE_REPLACEMENT diff
                    if (refactorTypeCompatibleReplacement(initializer, type)) {
                        continue;
                    }

                    // register ClassInstanceCreation initializer
                    if (initializer instanceof ClassInstanceCreation) {
                        template.addInstanceCreation((ClassInstanceCreation) initializer, type);
                    }

                    // refactor initializer
                    initializer.accept(this);
                }
            }
        }
    }

    private void registerVariablePair(SimpleName node, String prefix) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {

            // register variable pair and set identifier, do not set the type in AST node
            String name1 = node.getIdentifier();
            String name2 = ((SimpleName) diff.getExpr2()).getIdentifier();
            String resolvedName = template.resolveVariableName(name1, name2, prefix);
            node.setIdentifier(resolvedName);

            // set type
            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();
            Type type;
            if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                String genericTypeName = template.resolveTypePair(diff.getTypePair(), true);
                type = ast.newSimpleType(ast.newSimpleName(genericTypeName));
            } else {
                type = ASTNodeUtil.typeFromBinding(ast, node.resolveTypeBinding());
            }
            node.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, type);

        }
    }

    public boolean visit(RFVariableDeclarationStmt node) {
        if (node.hasDifference()) {
            //node.describe();
            VariableDeclarationStatement stmt1 = (VariableDeclarationStatement) node.getStatement1();

            // refactor type
            Type type = stmt1.getType();
            if (ASTNodeUtil.hasPairedNode(type)) {
                type.accept(this);
            }

            // refactor fragments
            refactorVariableDeclarationFragment(stmt1.getType(), stmt1.fragments(), "v");

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
