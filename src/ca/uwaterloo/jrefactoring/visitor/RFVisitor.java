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

import java.util.*;

public class RFVisitor extends ASTVisitor {

    private static Logger log = FileLogger.getLogger(RFVisitor.class);
    private static final String NEW_INSTANCE_METHOD_NAME = "newInstance";
    private static final String GET_DECLARED_CONSTRUCTOR_METHOD_NAME = "getDeclaredConstructor";
    private static final String DEFAULT_JAVA_PACKAGE = "java.lang";
    private static final String CLASS_NAME = "Class";
    private static final String JAVA_OBJECT_FULL_NAME = "java.lang.Object";

    private RFTemplate template;
    private AST ast;
    private PreprocessVisitor preprocessVisitor;

    public RFVisitor(RFTemplate template) {
        this.template = template;
        this.ast = template.getAst();
        this.preprocessVisitor = new PreprocessVisitor(template);
    }

    private void preVisit(RFEntity node) {
        if (node instanceof RFStatement) {
            RFStatement rfStatement = (RFStatement) node;
            if (rfStatement.isTopStmt()) {
                rfStatement.getStatement1().accept(preprocessVisitor);
            }
        }
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
        if (newNodeType.resolveBinding() != null) {
            newNode.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, ASTNodeUtil.typeFromBinding(ast, newNodeType.resolveBinding()));
        } else {
            newNode.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, ASTNodeUtil.copyTypeWithProperties(ast, newNodeType));
        }
        //newNode.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, newNodeType);
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
        //RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        Expression pairNode = (Expression) node.getProperty(ASTNodeUtil.PROPERTY_PAIR);
        if (pairNode != null) {
            template.addTemplateArgumentPair(node, pairNode);
            Type type;
            if (node.resolveTypeBinding().isPrimitive() && pairNode.resolveTypeBinding().isPrimitive()) {
                type = getCompatibleType(new TypePair(node.resolveTypeBinding(), pairNode.resolveTypeBinding()));
            } else {
                type = ASTNodeUtil.typeFromExpr(ast, node);
            }
            if (type.getProperty(ASTNodeUtil.PROPERTY_QUALIFIED_NAME) != null) {
                String qualifiedName = (String) type.getProperty(ASTNodeUtil.PROPERTY_QUALIFIED_NAME);
                template.addImportDeclaration(template.getTemplateCU(),
                        ASTNodeUtil.createPackageName(ast, qualifiedName), false);
            }
            String variableParameter = template.addVariableParameter(type);
            SimpleName newNode = ast.newSimpleName(variableParameter);
            replaceNode(node, newNode, type);
        }
    }

    private List<String> getArgTypeNames(ITypeBinding[] arguments) {
        List<String> argTypeNames = new ArrayList<>();
        for (ITypeBinding argument : arguments) {
            Type type = ASTNodeUtil.typeFromBinding(ast, argument);
            argTypeNames.add(type.toString());
        }
        return argTypeNames;
    }

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
        ITypeBinding commonSuperClass = template.getLowestCommonSubClass(typePair);
        ITypeBinding commonInterface = template.getLowestCommonInterface(typePair);

        if (commonSuperClass != null) {
            return ASTNodeUtil.typeFromBinding(ast, commonSuperClass);

        } else if (commonInterface != null) {
            return ASTNodeUtil.typeFromBinding(ast, commonInterface);

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
    public boolean visit(InfixExpression node) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {
            LocalVisitor localVisitor1 = new LocalVisitor();
            LocalVisitor localVisitor2 = new LocalVisitor();
            node.accept(localVisitor1);
            diff.getExpr2().accept(localVisitor2);
            if (localVisitor1.containsLocal() || localVisitor2.containsLocal()) {
                log.info("unrefactorable InfixExpr node pair containing local variables: " + diff.toString());
                template.markAsUnrefactorable();
            } else {
                pullUpToParameter(node);
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean visit(NullLiteral node) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);

        if (diff != null) {
            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();
            if (differenceTypes.contains(DifferenceType.TYPE_COMPATIBLE_REPLACEMENT)) {
                template.addUnrefactoredNodePair(node, diff.getExpr2(), diff);
                log.info("non-refactored node pair with NullLiteral node: " + diff.toString());
                return false;
            }
        }

        return false;
    }

    @Override
    public boolean visit(TypeLiteral node) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {
            // resolve clazz name
            TypeLiteral pairNode = (TypeLiteral) diff.getExpr2();
            TypePair typePair = new TypePair(node.getType().resolveBinding(), pairNode.getType().resolveBinding());
            String genericTypeName = template.resolveTypePair(typePair, false);
            String clazzName = template.resolveGenericType(genericTypeName);

            // construct parameterized type
            Type genericType = ast.newSimpleType(ast.newSimpleName(genericTypeName));
            Type classType = ast.newSimpleType(ast.newSimpleName(CLASS_NAME));
            ParameterizedType classTypeWithGenericType = ast.newParameterizedType(classType);
            classTypeWithGenericType.typeArguments().add(genericType);

            // replace node
            replaceNode(node, ast.newSimpleName(clazzName), classTypeWithGenericType);
        }
        return false;
    }

    @Override
    public boolean visit(PrefixExpression node) {
        pullUpToParameter(node);
        return false;
    }

    @Override
    public boolean visit(CastExpression node) {
        Type type = node.getType();
        CastExpression pairNode = (CastExpression) node.getProperty(ASTNodeUtil.PROPERTY_PAIR);
        if (ASTNodeUtil.hasPairedNode(type) && node.getExpression() instanceof NullLiteral
                && pairNode != null && pairNode.getExpression() instanceof NullLiteral) {

            Type pairType = pairNode.getType();
            ITypeBinding commonSuperClass = template.getLowestCommonSubClass(
                    new TypePair(type.resolveBinding(), pairType.resolveBinding()));
            if (commonSuperClass == null || commonSuperClass.getBinaryName().equals(JAVA_OBJECT_FULL_NAME)) {
                log.info("unrefactorable castExpr node pair: " + node + ", " + pairNode);
                template.markAsUnrefactorable();
                return false;
            } else {
                replaceNode(node, ast.newNullLiteral(), ASTNodeUtil.typeFromBinding(ast, commonSuperClass));
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean visit(SimpleName node) {
        RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);
        if (diff != null) {

            Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();

            if (differenceTypes.contains(DifferenceType.TYPE_COMPATIBLE_REPLACEMENT)) {
                template.addUnrefactoredNodePair(node, diff.getExpr2(), diff);
                log.info("non-refactored node pair with SimpleName node: " + diff.toString());
                return false;
            }

            SimpleName pairNode = (SimpleName) diff.getExpr2();
            if (node.resolveBinding().getKind() == 2 && pairNode.resolveBinding().getKind() == 2) {
                log.info("Skip refactoring type level SimpleName node pair: " + node + ", " + pairNode);
                return false;
            }

            if (node.resolveBinding().getKind() == 3 && pairNode.resolveBinding().getKind() == 3) {
                IVariableBinding iVariableBinding1 = (IVariableBinding) node.resolveBinding();
                IVariableBinding iVariableBinding2 = (IVariableBinding) pairNode.resolveBinding();
                if (iVariableBinding1.isField() && iVariableBinding2.isField()) {
                    if (differenceTypes.contains(DifferenceType.SUBCLASS_TYPE_MISMATCH) && differenceTypes.size() == 1) {
                        template.addUnrefactoredNodePair(node, diff.getExpr2(), diff);
                        log.info("non-refactored node pair with SimpleName field node: " + diff.toString());
                    } else if (!Modifier.isAbstract(iVariableBinding1.getDeclaringClass().getModifiers())
                            && !Modifier.isAbstract(iVariableBinding2.getDeclaringClass().getModifiers())
                            && differenceTypes.contains(DifferenceType.VARIABLE_NAME_MISMATCH) && differenceTypes.size() == 1) {
                        pullUpToParameter(node);
                    } else {
                        log.info("Skip refactoring field level SimpleName node pair: " + node + ", " + pairNode);
                    }
                    return false;

                } else if (iVariableBinding1.isField() || iVariableBinding2.isField()) {
                    template.addUnrefactoredNodePair(node, diff.getExpr2(), diff);
                    log.info("non-refactored node pair with SimpleName field node: " + diff.toString());
                    return false;
                }
            }


            String name1 = node.getIdentifier();
            String name2 = ((SimpleName) diff.getExpr2()).getIdentifier();

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
        }
        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        pullUpToParameter(node);
        return false;
    }

    @Override
    public boolean visit(ArrayAccess node) {
        if (node.getArray() instanceof QualifiedName) {
            pullUpToParameter(node);
            return false;
        }
        return true;
    }

    @Override
    public boolean visit(Assignment node) {

        // check if left hand side is generic type
        Expression pairedLeftHandSide = (Expression) node.getLeftHandSide().getProperty(ASTNodeUtil.PROPERTY_PAIR);
        if (pairedLeftHandSide != null) {
            TypePair typePair = new TypePair(node.getLeftHandSide().resolveTypeBinding(), pairedLeftHandSide.resolveTypeBinding());
            if (template.containsTypePair(typePair)) {

                // get generic type
                String genericTypeName = template.resolveTypePair(typePair, false);
                Type genericType = ast.newSimpleType(ast.newSimpleName(genericTypeName));

                if (node.getRightHandSide() instanceof CastExpression) {
                    // set CastExpression Type
                    CastExpression castExpression = (CastExpression) node.getRightHandSide();
                    castExpression.setType(genericType);
                    castExpression.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, genericType);

                    // refactor right hand side CastExpression expr
                    castExpression.getExpression().accept(this);

                } else if (!(node.getRightHandSide() instanceof NullLiteral)) {
                    // refactor right hand side
                    node.getRightHandSide().accept(this);

                    // wrap CastExpression
                    Type rightHandType = ASTNodeUtil.typeFromExpr(ast, node.getRightHandSide());
                    if (!genericTypeName.equals(rightHandType.toString())) {
                        CastExpression castExpression = wrapCastExpression(genericType, node.getRightHandSide());
                        replaceNode(node.getRightHandSide(), castExpression, genericType);
                    }
                }

                // refactor left hand side
                node.getLeftHandSide().accept(this);

                return false;
            }
        }

        return true;
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
                type.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, type);
                replaceNode(node, type, type);
                //replaceNode(name, ast.newSimpleName(genericTypeName), type);

            } else {
                throw new IllegalStateException("unexpected difference type in SimpleType!");
            }
        }
        return false;
    }

    @Override
    public boolean visit(CatchClause node) {
        if (ASTNodeUtil.hasPairedNode(node.getException().getName())) {
            log.info("unable to refactor CatchClause SingleVariableDeclaration name difference");
            template.markAsUnrefactorable();
            return false;
        }
        return true;
    }

    @Override
    public boolean visit(ParameterizedType node) {
        ParameterizedType pairNode = (ParameterizedType) node.getProperty(ASTNodeUtil.PROPERTY_PAIR);
        if (pairNode != null) {
            TypePair typePair = new TypePair(node.resolveBinding(), pairNode.resolveBinding());
            String genericTypeName = template.resolveTypePair(typePair, true);
            Type type = ast.newSimpleType(ast.newSimpleName(genericTypeName));
            type.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, type);
            replaceNode(node, type, type);
        }
        return false;
    }

    private ITypeBinding getCommonInteface(ITypeBinding interface1, ITypeBinding interface2) {

        ITypeBinding commonInterface = null;

        if (interface1 != null && interface2 != null) {
            Set<String> interfaceBinaryNames = new HashSet<>();
            Queue<ITypeBinding> queue = new LinkedList<>();
            queue.offer(interface2);
            while (!queue.isEmpty()) {
                ITypeBinding cur = queue.poll();
                for (ITypeBinding iTypeBinding : cur.getInterfaces()) {
                    queue.offer(iTypeBinding);
                }
                interfaceBinaryNames.add(cur.getQualifiedName());
            }

            queue.clear();
            queue.offer(interface1);
            while (!queue.isEmpty()) {
                ITypeBinding cur = queue.poll();
                if (interfaceBinaryNames.contains(cur.getQualifiedName())) {
                    commonInterface = cur;
                    break;
                }
                for (ITypeBinding iTypeBinding : cur.getInterfaces()) {
                    queue.offer(iTypeBinding);
                }
            }
        }

        return commonInterface;
    }

    private List<ITypeBinding> getParameterTypes(ITypeBinding[] parameterTypes1, ITypeBinding[] parameterTypes2) {

        List<ITypeBinding> parameterTypes = new ArrayList<>();
        for (int i = 0; i < parameterTypes1.length; i++) {
            ITypeBinding typeBinding1 = parameterTypes1[i];
            ITypeBinding typeBinding2 = parameterTypes2[i];

            // check interfaces
            if (typeBinding1.isInterface() && typeBinding2.isInterface()) {
                ITypeBinding commonInterface = getCommonInteface(typeBinding1, typeBinding2);
                if (commonInterface == null) {
                    template.markAsUnrefactorable();
                    log.info("cannot find common interface: " +
                            typeBinding1.getBinaryName() + ", " + typeBinding2.getBinaryName());
                }
                parameterTypes.add(commonInterface);
                continue;

            } else if (typeBinding1.isInterface()) {
                if (typeBinding2.isPrimitive() || !typeBinding1.isCastCompatible(typeBinding2)) {
                    template.markAsUnrefactorable();
                }
                parameterTypes.add(typeBinding1);
                //parameterTypes.add(ASTNodeUtil.typeFromBinding(ast, typeBinding1));
                continue;

            } else if (typeBinding2.isInterface()) {
                if (typeBinding1.isPrimitive() || !typeBinding2.isCastCompatible(typeBinding1)) {
                    template.markAsUnrefactorable();
                }
                parameterTypes.add(typeBinding2);
                //parameterTypes.add(ASTNodeUtil.typeFromBinding(ast, typeBinding2));
                continue;
            }

            // check common super class
            if (typeBinding1.getBinaryName().equals(typeBinding2.getBinaryName())) {
                parameterTypes.add(typeBinding1);
                //parameterTypes.add(ASTNodeUtil.typeFromBinding(ast, typeBinding1));

            } else {
                TypePair typePair = new TypePair(typeBinding1, typeBinding2);
                ITypeBinding commonSuperClass = template.getLowestCommonSubClass(typePair);
                if (commonSuperClass != null) {
                    parameterTypes.add(commonSuperClass);

                } else if (typeBinding1.isPrimitive() && typeBinding2.isPrimitive()) {
                    Type t1 = ASTNodeUtil.typeFromBinding(ast, typePair.getType1());
                    Type t2 = ASTNodeUtil.typeFromBinding(ast, typePair.getType2());
                    Type compatiblePrimitiveType = getCompatibleType(typePair);
                    byte n1 = getPrimitiveNum((PrimitiveType) t1);
                    byte n2 = getPrimitiveNum((PrimitiveType) t2);
                    byte n = getPrimitiveNum((PrimitiveType) compatiblePrimitiveType);
                    if (n == n1) {
                        parameterTypes.add(typeBinding1);
                    } else if (n == n2) {
                        parameterTypes.add(typeBinding2);
                    } else {
                        template.markAsUnrefactorable();
                        log.info("no common super class for parameter types: "
                                + typeBinding1.getBinaryName() + ", " + typeBinding2.getBinaryName());
                    }

                } else {
                    template.markAsUnrefactorable();
                    log.info("no common super class for parameter types: "
                            + typeBinding1.getBinaryName() + ", " + typeBinding2.getBinaryName());
                }
            }
        }

        return parameterTypes;

    }

    private void refactorArgsWithCastExpr(List<Expression> arguments, List<ITypeBinding> parameterTypes) {
        for (int i = 0; i < arguments.size(); i++) {
            Expression argument = arguments.get(i);
            if (ASTNodeUtil.hasPairedNode(argument)) {
                argument.accept(this);

                // update current node
                argument = arguments.get(i);

                // wrap cast expression
                Type argType = (Type) argument.getProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING);
                if (argType != null) {
                    String argTypeFullName = (String) argType.getProperty(ASTNodeUtil.PROPERTY_QUALIFIED_NAME);
                    ITypeBinding parameterTypeBinding = parameterTypes.get(i);
                    TypePair typePair = template.getTypePairFromGenericMap(argType.toString());

                    // don't wrap cast expression if parameter type is Class parameterized type
                    if (parameterTypeBinding.isParameterizedType()
                            && parameterTypeBinding.getErasure().getName().equals(CLASS_NAME)) {
                        return;
                    }

                    // don't wrap cast expression if parameter type is ancestor of generic type bound
                    if (typePair != null) {
                        ITypeBinding commonSuperClass = template.getLowestCommonSubClass(typePair);
                        if (commonSuperClass != null
                                && template.containTypeBound(argType.toString(), commonSuperClass.getName())
                                && (ASTNodeUtil.hasAncestor(commonSuperClass, parameterTypeBinding.getQualifiedName())
                                || commonSuperClass.getQualifiedName().equals(parameterTypeBinding.getQualifiedName()))) {
                            return;
                        }
                    }

                    if (typePair != null || ASTNodeUtil.hasAncestor(parameterTypeBinding, argTypeFullName)) {

                        // get cast type full name
                        Type castType = ASTNodeUtil.typeFromBinding(ast, parameterTypeBinding);
                        String name = parameterTypeBinding.getBinaryName();

                        // add import declaration
                        if (!name.startsWith(DEFAULT_JAVA_PACKAGE) && !parameterTypeBinding.isPrimitive()) {
                            if (template.getTemplateCU() != null) {
                                template.addImportDeclaration(template.getTemplateCU(),
                                        ASTNodeUtil.createPackageName(ast, name), false);
                            } else {
                                template.addImportInTemplateImportList(ASTNodeUtil.createPackageName(ast, name));
                            }
                        }

                        // replace argument
                        CastExpression castExpression = wrapCastExpression(castType, argument);
                        replaceNode(argument, castExpression, castType);
                    }
                }
            }
        }
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {

        if (ASTNodeUtil.hasPairedNode(node)) {

            // check TYPE_COMPATIBLE_REPLACEMENT difference type
            RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);

            if (diff != null) {
                Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();
                if (differenceTypes.contains(DifferenceType.TYPE_COMPATIBLE_REPLACEMENT)) {
                    template.addUnrefactoredNodePair(node, diff.getExpr2(), diff);
                    log.info("non-refactored node pair with ClassInstanceCreation node: " + diff.toString());
                    return false;
                }
            }

            // get common parameter type binding
            ClassInstanceCreation pairNode = (ClassInstanceCreation) node.getProperty(ASTNodeUtil.PROPERTY_PAIR);
            ITypeBinding[] parameterTypes1 = node.resolveConstructorBinding().getParameterTypes();
            ITypeBinding[] parameterTypes2 = pairNode.resolveConstructorBinding().getParameterTypes();
            if (parameterTypes1.length != parameterTypes2.length) {
                template.addUnrefactoredNodePair(node, pairNode, diff);
                return false;
            }

            List<ITypeBinding> parameterTypes = getParameterTypes(parameterTypes1, parameterTypes2);
            if (!template.isRefactorable()) {
                template.addUnrefactoredNodePair(node, pairNode, diff);
                return false;
            }

            // refactor arguments
            List<Expression> arguments = node.arguments();
            refactorArgsWithCastExpr(arguments, parameterTypes);

            // refactor ClassInstanceCreation type
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
                for (int i = 0; i < arguments.size(); i++) {
                    Expression argument = arguments.get(i);
                    TypeLiteral typeLiteral = ast.newTypeLiteral();
                    Type parameterType = ASTNodeUtil.typeFromBinding(ast, parameterTypes.get(i));
                    typeLiteral.setType(parameterType);

                    // add import declaration
                    String qualifiedName = (String) parameterType.getProperty(ASTNodeUtil.PROPERTY_QUALIFIED_NAME);
                    if (qualifiedName != null) {
                        template.addImportDeclaration(template.getTemplateCU(),
                                ASTNodeUtil.createPackageName(ast, qualifiedName), false);
                    }

                    if (parameterTypes1[i].isPrimitive() && parameterTypes2[i].isPrimitive()
                            && !parameterTypes1[i].getName().equals(parameterTypes2[i].getName())) {
                        // resolve clazz name
                        TypePair tp = new TypePair(parameterTypes1[i], parameterTypes2[i]);
                        String genericName = template.resolveTypePair(tp, false);
                        String cn = template.resolveGenericType(genericName);

                        // construct parameterized type
                        Type genericType = ast.newSimpleType(ast.newSimpleName(genericName));
                        Type classType = ast.newSimpleType(ast.newSimpleName(CLASS_NAME));
                        ParameterizedType classTypeWithGenericType = ast.newParameterizedType(classType);
                        classTypeWithGenericType.typeArguments().add(genericType);
                        SimpleName simpleName = ast.newSimpleName(cn);
                        simpleName.setProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING, classTypeWithGenericType);
                        getDeclaredConstructorMethodInvocation.arguments().add(simpleName);

                    } else {
                        getDeclaredConstructorMethodInvocation.arguments().add(typeLiteral);
                    }

                    // add null parameter object
                    Expression newArg;
                    if (argument instanceof NullLiteral) {
                        template.addNullParameterObject();
                        newArg = ast.newSimpleName(template.NULL_PARAMETER_OBJECT);
                    } else {
                        newArg = (Expression) ASTNode.copySubtree(ast, argument);
                    }
                    newInstanceMethodInvocation.arguments().add(newArg);
                }

            }
        }

        return false;
    }

    private ITypeBinding[] extendParameterTypeBinding(ITypeBinding[] iTypeBindings, int expectedLength) {
        List<ITypeBinding> typeBindingList = new ArrayList<>();
        for (int i = 0; i < iTypeBindings.length; i++) {
            if (!iTypeBindings[i].isArray()) {
                typeBindingList.add(iTypeBindings[i]);
            } else {
                for (int j = 0; j < expectedLength - i; j++) {
                    typeBindingList.add(iTypeBindings[i].getElementType());
                }
                break;
            }
        }
        return typeBindingList.toArray(new ITypeBinding[0]);
    }

    @Override
    public boolean visit(MethodInvocation node) {

        if (ASTNodeUtil.hasPairedNode(node)) {

            RFNodeDifference diff = (RFNodeDifference) node.getProperty(ASTNodeUtil.PROPERTY_DIFF);

            if (diff != null) {
                Set<DifferenceType> differenceTypes = diff.getDifferenceTypes();
                if (differenceTypes.contains(DifferenceType.TYPE_COMPATIBLE_REPLACEMENT)
                        || differenceTypes.contains(DifferenceType.ARGUMENT_NUMBER_MISMATCH)
                        || differenceTypes.contains(DifferenceType.MISSING_METHOD_INVOCATION_EXPRESSION)) {
                    Expression pairNode = (Expression) node.getProperty(ASTNodeUtil.PROPERTY_PAIR);
                    template.addUnrefactoredNodePair(node, pairNode, diff);
                    log.info("non-refactored node pair with MethodInvocation node: " + diff.toString());
                    return false;
                }
            }

            Expression expr1 = node.getExpression();
            SimpleName name1 = node.getName();
            List<Expression> arguments1 = node.arguments();

            // get original argument type names
            IMethodBinding iMethodBinding = node.resolveMethodBinding();
            List<String> argTypeNames1 = getArgTypeNames(iMethodBinding.getParameterTypes());
            List<Expression> originalArgs1 = ASTNode.copySubtrees(ast, arguments1);

            // get common parameter types
            MethodInvocation pairNode = (MethodInvocation) node.getProperty(ASTNodeUtil.PROPERTY_PAIR);
            ITypeBinding[] parameterTypes1 = node.resolveMethodBinding().getParameterTypes();
            if (node.resolveMethodBinding().isVarargs()) {
                parameterTypes1 = extendParameterTypeBinding(parameterTypes1, arguments1.size());
            }
            ITypeBinding[] parameterTypes2 = pairNode.resolveMethodBinding().getParameterTypes();
            if (pairNode.resolveMethodBinding().isVarargs()) {
                parameterTypes2 = extendParameterTypeBinding(parameterTypes2, pairNode.arguments().size());
            }
            List<ITypeBinding> parameterTypes = getParameterTypes(parameterTypes1, parameterTypes2);
            if (!template.isRefactorable()) {
                template.addUnrefactoredNodePair(node, pairNode, diff);
                return false;
            }

            // refactor arguments
            refactorArgsWithCastExpr(arguments1, parameterTypes);

            // check if common super class can be used
            if (!ASTNodeUtil.hasPairedNode(name1) && ASTNodeUtil.hasPairedNode(expr1)) {

                Expression expr2 = (Expression) expr1.getProperty(ASTNodeUtil.PROPERTY_PAIR);
                TypePair typePair = new TypePair(expr1.resolveTypeBinding(), expr2.resolveTypeBinding());

                if (!typePair.isSame()) {
                    ITypeBinding commonSuperClass = template.getLowestCommonSubClass(typePair);
                    if (commonSuperClass != null) {
                        for (IMethodBinding methodBinding : commonSuperClass.getDeclaredMethods()) {
                            if (methodBinding.getName().equals(name1.getIdentifier())
                                    && Modifier.isPublic(methodBinding.getModifiers())) {
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
                List<String> argTypeNames2 = getArgTypeNames(pairedNode.resolveMethodBinding().getParameterTypes());
                MethodInvocationPair methodInvocationPair = new MethodInvocationPair(expr1, name1,
                        pairedNode.getExpression(), pairedNode.getName(), argTypeNames1, argTypeNames2,
                        originalArgs1, originalArgs2, node, pairedNode);

                // refactor the method invocation expression
                if (expr1 != null) {
                    expr1.accept(this);
                }

                // create new method invocation in adapter
                //Type returnType = ASTNodeUtil.typeFromExpr(ast, node);
                //ITypeBinding returnType = node.resolveTypeBinding();
                TypePair returnTypePair = new TypePair(node.resolveTypeBinding(), pairedNode.resolveTypeBinding());
                MethodInvocation newMethod = template.createAdapterActionMethod(node.getExpression(), arguments1,
                        methodInvocationPair, returnTypePair);

                // replace the old method
                Type type = (Type) newMethod.getProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING);
                if (type == null) {
                    type = ASTNodeUtil.typeFromBinding(ast, node.resolveTypeBinding());
                }
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
                log.info("non-refactored node pair with initializer node: " + diff.toString());
                template.addUnrefactoredNodePair(initializer, diff.getExpr2(), diff);
                //MethodInvocation methodInvocation = template.createAdapterActionMethod(type);
                //log.info("adapter action arguments should be specified manually in " + methodInvocation);
                //replaceNode(initializer, methodInvocation, type);
                return true;
            }
        }

        return false;
    }

    private CastExpression wrapCastExpression(Type type, Expression expression) {
        CastExpression castExpression = ast.newCastExpression();
        Type curType = ASTNodeUtil.copyTypeWithProperties(ast, type);
        castExpression.setType(curType);
        castExpression.setExpression((Expression) ASTNode.copySubtree(ast, expression));
        return castExpression;
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

                    // wrap CastExpression if initializer is still ClassInstanceCreation
                    initializer = fragment.getInitializer();
                    if ((initializer instanceof ClassInstanceCreation || initializer.resolveTypeBinding() != null)
                            && template.containsGenericNameInMap(type.toString())) {
                        CastExpression castExpression = wrapCastExpression(type, initializer);
                        replaceNode(initializer, castExpression, type);
                    }

                } else {
                    // wrap CastExpression
                    if (template.containsGenericNameInMap(type.toString()) && !(initializer instanceof NullLiteral)) {
                        CastExpression castExpression = wrapCastExpression(type, initializer);
                        replaceNode(initializer, castExpression, type);
                    }
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

            // ignore body
            TryStatement tryStatement = (TryStatement) node.getStatement1();
            TryStatement pairTryStmt = (TryStatement) tryStatement.getProperty(ASTNodeUtil.PROPERTY_PAIR);

            // check if finally block has difference
            if (tryStatement.getFinally() != null && pairTryStmt.getFinally() != null) {
                List<Statement> finallyStmt = tryStatement.getFinally().statements();
                List<Statement> pairFinallyStmt = pairTryStmt.getFinally().statements();
                if (finallyStmt.size() != pairFinallyStmt.size()) {
                    template.markAsUnrefactorable();
                    log.info("unrefactorable finally block: " + tryStatement.getFinally() + " <---> " + pairTryStmt.getFinally());
                    return false;
                }
                for (int i = 0; i < finallyStmt.size(); i++) {
                    if (!finallyStmt.get(i).toString().equals(pairFinallyStmt.get(i).toString())) {
                        template.markAsUnrefactorable();
                        log.info("unrefactorable finally block: " + tryStatement.getFinally() + " <---> " + pairTryStmt.getFinally());
                        return false;
                    }
                }
            }

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
            //node.describe();
            node.getStatement1().accept(this);
        }
        return true;
    }

    public void endVisit(RFStatement node) {
        // if current node is the top level statement, copy the refactored node to the template
        if (node.isTopStmt()) {
            node.getTemplate().addStatement((Statement) ASTNode.copySubtree(ast, node.getStatement1()));
            //log.info("statement1: " + node.getStatement1());
        }
    }

}
