package ca.uwaterloo.jrefactoring.utility;

import gr.uom.java.ast.*;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class ASTNodeUtil {

    public static final String PROPERTY_DIFF = "diff";
    public static final String PROPERTY_TYPE_BINDING = "type";
    public static final String PROPERTY_PAIR = "pair";

    public static Type typeFromBinding(AST ast, ITypeBinding typeBinding) {
        if( ast == null )
            throw new NullPointerException("ast is null");
        if( typeBinding == null )
            throw new NullPointerException("typeBinding is null");

        if( typeBinding.isPrimitive() ) {
            return ast.newPrimitiveType(
                    PrimitiveType.toCode(typeBinding.getName()));
        }

        if( typeBinding.isCapture() ) {
            ITypeBinding wildCard = typeBinding.getWildcard();
            WildcardType capType = ast.newWildcardType();
            ITypeBinding bound = wildCard.getBound();
            if( bound != null ) {
                capType.setBound(typeFromBinding(ast, bound), wildCard.isUpperbound());
            }
            return capType;
        }

        if( typeBinding.isArray() ) {
            Type elType = typeFromBinding(ast, typeBinding.getElementType());
            return ast.newArrayType(elType, typeBinding.getDimensions());
        }

        if( typeBinding.isParameterizedType() ) {
            ParameterizedType type = ast.newParameterizedType(
                    typeFromBinding(ast, typeBinding.getErasure()));

            @SuppressWarnings("unchecked")
            List<Type> newTypeArgs = type.typeArguments();
            for( ITypeBinding typeArg : typeBinding.getTypeArguments() ) {
                newTypeArgs.add(typeFromBinding(ast, typeArg));
            }

            return type;
        }

        // simple or raw type
        //String qualName = typeBinding.getQualifiedName();
        String name = typeBinding.getName();
        if( "".equals(name) ) {
            throw new IllegalArgumentException("No name for type binding.");
        }
        return ast.newSimpleType(ast.newName(name));
    }

    public static Type typeFromExpr(AST ast, Expression expr) {
        Type type = (Type) expr.getProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING);
        if (type != null) {
            return type;
        } else {
            return typeFromBinding(ast, expr.resolveTypeBinding());
        }
    }

    public static boolean hasPairedNode(ASTNode node) {
        if (node == null) return false;
        return node.getProperty(PROPERTY_PAIR) != null;
    }

    public static boolean pairNewNodes(ASTNode node1, ASTNode node2) {
        if (!hasPairedNode(node1) && !hasPairedNode(node2)) {
            node1.setProperty(PROPERTY_PAIR, node2);
            node2.setProperty(PROPERTY_PAIR, node1);
            return true;
        } else if (hasPairedNode(node1) && hasPairedNode(node2)) {
            return false;
        } else {
            throw new IllegalStateException("unexpected paired nodes: " + node1 + ", " + node2);
        }
    }

    public static void matchDiffNode(ASTNode node1, ASTNode node2, ASTNode root1, ASTNode root2) {
        if (pairNewNodes(node1, node2)) {
            if (node1 != root1 && node2 != root2) {
                matchDiffNode(node1.getParent(), node2.getParent(), root1, root2);
            } else if (node1 != root1 || node2 != root2) {
                throw new IllegalStateException("unmatched diff node structure: " + node1 + ", " + node2);
            }
        }
    }

    public static MethodDeclaration retrieveMethodDeclarationNode(IMethod iMethod, int startOffset, int endOffset, boolean clearCache)
            throws Exception {
        SystemObject systemObject = ASTReader.getSystemObject();
        AbstractMethodDeclaration methodObject = systemObject.getMethodObject(iMethod);

        if (methodObject != null && methodObject.getMethodBody() != null) {

            if (clearCache) {
                CompilationUnitCache.getInstance().clearCache();
            }
            ClassDeclarationObject classObject;

            if (iMethod.getDeclaringType().isAnonymous()) {
                classObject = systemObject.getAnonymousClassDeclaration(iMethod.getDeclaringType());
            } else {
                classObject = systemObject.getClassObject(methodObject.getClassName());
            }

            ASTNode node = NodeFinder.perform(classObject.getClassObject().getAbstractTypeDeclaration().getRoot(),
                    startOffset, endOffset - startOffset);

            if (node instanceof MethodDeclaration) {
                return (MethodDeclaration) node;
            } else {
                return null;
            }

        } else {
            return null;
        }
    }
}
