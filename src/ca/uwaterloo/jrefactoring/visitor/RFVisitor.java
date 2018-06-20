package ca.uwaterloo.jrefactoring.visitor;

import ca.uwaterloo.jrefactoring.node.RFEntity;
import ca.uwaterloo.jrefactoring.node.RFNodeDifference;
import ca.uwaterloo.jrefactoring.node.RFStatement;
import ca.uwaterloo.jrefactoring.action.*;
import ca.uwaterloo.jrefactoring.template.RFTemplate;
import ca.uwaterloo.jrefactoring.template.TypePair;
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

    public RFVisitor() {
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

    /*
    protected List<Action> selectActions(int contextNodyType, Set<DifferenceType> differenceTypes) {

        List<Action> strategies = new ArrayList<>();

        if (differenceTypes.contains(DifferenceType.VARIABLE_NAME_MISMATCH)) {
            if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                strategies.add(new CreateMethodInvocationAction());
            } else {
                strategies.add(new ResolveName());
            }
        } else if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
            if (contextNodyType == ASTNode.CLASS_INSTANCE_CREATION) {
                strategies.add(new CreateClassInstance());
            } else {
                strategies.add(new ResolveTypeParameter());
            }
        }

        return strategies;
    }
    */

    /*
    protected void refactor(RFNodeDifference diff) {
        // validate node difference
        ContextUtil.validateNodeDiff(diff);

        // search context node
        int nodeType = ContextUtil.getContextNodeType(diff);
        //log.info("refactor difference: " + diff.toString());
        //log.info("contextNode: " + ASTNode.nodeClassForType(nodeType).getName());

        // collect all the difference types in the diff node
        Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

        // select refactoring strategy based on difference types and context node
        List<Action> actions = selectActions(nodeType, differenceTypes);

        // take refactoring actions
        for (Action action : actions) {
            Transformer.setAction(action);
            Transformer.transform(diff);
        }
    }
    */

    @Override
    public boolean visit(SimpleName node) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodePropertyName.DIFF);
        if (diff != null) {

            RFTemplate template = diff.getTemplate();
            AST ast = template.getAst();
            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

            if (differenceTypes.contains(DifferenceType.VARIABLE_NAME_MISMATCH)) {
                String name1 = node.getFullyQualifiedName();
                String name2 = ((Name) diff.getExpr2()).getFullyQualifiedName();
                String resolvedName = template.resolveVariableName(name1, name2);
                replaceNode(node, ast.newSimpleName(resolvedName));

            } else if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH)) {
                String genericTypeName = template.resolveTypePair(diff.getTypePair());
                replaceNode(node, ast.newSimpleName(genericTypeName));

            } else {
                throw new IllegalStateException("unexpected name mismatch!");
            }

        }
        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodePropertyName.DIFF);
        if (diff != null) {

            RFTemplate template = diff.getTemplate();
            AST ast = template.getAst();
            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

            if (differenceTypes.contains(DifferenceType.VARIABLE_NAME_MISMATCH)
                    && differenceTypes.size() == 1) {
                Type type = Transformer.typeFromBinding(ast, node.resolveTypeBinding());
                String variableParameter = template.addVariableParameter(type);
                replaceNode(node, ast.newSimpleName(variableParameter));

            } else {
                throw new IllegalStateException("unexpected qualified name mismatch!");
            }
        }
        return false;
    }

    protected RFNodeDifference retrieveDiffInTypeNode(Type type) {
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType) type;
            return (RFNodeDifference) simpleType.getName().getProperty(ASTNodePropertyName.DIFF);

        } else {
            throw new IllegalStateException("unexpected Type type when retrieving diff!");
        }
    }

    protected void replaceNode(ASTNode oldNode, ASTNode newNode) {
        StructuralPropertyDescriptor structuralPropertyDescriptor = oldNode.getLocationInParent();
        if (structuralPropertyDescriptor.isChildListProperty()) {
            List<ASTNode> arguments = (List<ASTNode>) oldNode.getParent().getStructuralProperty(structuralPropertyDescriptor);
            arguments.remove(oldNode);
            arguments.add(newNode);

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
            RFTemplate template = diff.getTemplate();
            String genericTypeName = template.resolveTypePair(diff.getTypePair());
            String clazzName = template.resolveGenericType(genericTypeName);

            // replace initializer
            AST ast = template.getAst();
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
            replaceNode(node, newInstanceMethodInvocation);

            // copy parameters
            for (Expression argument : arguments) {
                TypeLiteral typeLiteral = ast.newTypeLiteral();
                Type type = Transformer.typeFromBinding(ast, argument.resolveTypeBinding());
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
            return node.getProperty(ASTNodePropertyName.DIFF) != null;
        } else if (node instanceof MethodInvocation) {
            return containsDiff(((MethodInvocation) node).getExpression())
                    || containsDiff(((MethodInvocation) node).getName());
        } else {
            throw new IllegalStateException("unexpected expression node: " + node);
        }
    }

    protected RFNodeDifference retrieveDiffInMethodInvacation(Expression node) {
        if (node instanceof Name) {
            return (RFNodeDifference) node.getProperty(ASTNodePropertyName.DIFF);
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
        return (RFNodeDifference) name.getProperty(ASTNodePropertyName.DIFF);
    }

    @Override
    public boolean visit(MethodInvocation node) {

        // visit arguments
        List<Expression> arguments = node.arguments();
        for (Expression argument : arguments) {
            argument.accept(this);
        }

        RFNodeDifference diffInExpr = retrieveDiffInMethodInvacation(node.getExpression());
        RFNodeDifference diffInName = retrieveDiffInName(node.getName());
        if (diffInExpr != null || diffInName != null) {

            node.getExpression().accept(this);

            RFTemplate template;
            if (diffInExpr != null) {
                template = diffInExpr.getTemplate();
            } else {
                template = diffInName.getTemplate();
            }

            AST ast = template.getAst();
            MethodInvocation newNode = ast.newMethodInvocation();
            newNode.setName(ast.newSimpleName("action1"));
            newNode.setExpression(ast.newSimpleName("adapter"));
            List<Expression> newNodeArgs = newNode.arguments();

            newNodeArgs.add((Expression) ASTNode.copySubtree(ast, node.getExpression()));
            for (Expression argument: arguments) {
                Expression newArg = (Expression) ASTNode.copySubtree(ast, argument);
                newNodeArgs.add(newArg);
            }

            replaceNode(node, newNode);

        }

        return false;
    }

    public void endVisit(RFStatement node) {
        // if current node is the top level statement, copy the refactored node to the template
        if (node.isTopStmt()) {
            AST ast = node.getTemplate().getAst();
            node.getTemplate().addStatement((Statement) ASTNode.copySubtree(ast, node.getStatement1()));
        }
    }

    public void endVisit(RFNodeDifference node) {
    }

}
