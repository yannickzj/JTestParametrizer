package ca.uwaterloo.jrefactoring.utility;

import ca.uwaterloo.jrefactoring.visitor.MethodVisitor;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ASTNodeUtil {

    private static Logger log = FileLogger.getLogger(ASTNodeUtil.class);
    public static final String PROPERTY_DIFF = "diff";
    public static final String PROPERTY_TYPE_BINDING = "type";
    public static final String PROPERTY_QUALIFIED_NAME = "qualifiedName";
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
        if (typeBinding.isNested()) {
            StringBuilder sb = new StringBuilder();
            for (String cmp : typeBinding.getBinaryName().split("\\.")) {
                if (cmp.length() > 0 && Character.isUpperCase(cmp.charAt(0))) {
                    if (cmp.contains("$")) {
                        for (String name: cmp.split("\\$")) {
                            sb.append(name);
                            sb.append(".");
                        }
                    } else {
                        sb.append(cmp);
                        sb.append(".");
                    }
                }
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            //System.out.println("nest type binding name: " + sb.toString());
            SimpleType simpleType = ast.newSimpleType(createPackageName(ast, sb.toString()));
            simpleType.setProperty(PROPERTY_QUALIFIED_NAME, typeBinding.getQualifiedName());
            return simpleType;
        }

        String name = typeBinding.getName();
        if( "".equals(name) ) {
            throw new IllegalArgumentException("No name for type binding.");
        }
        SimpleType simpleType = ast.newSimpleType(ast.newName(name));
        simpleType.setProperty(PROPERTY_QUALIFIED_NAME, typeBinding.getQualifiedName());
        return simpleType;
    }

    public static Type typeFromExpr(AST ast, Expression expr) {
        Type type = (Type) expr.getProperty(ASTNodeUtil.PROPERTY_TYPE_BINDING);
        if (type != null) {
            return type;
        } else {
            return typeFromBinding(ast, expr.resolveTypeBinding());
        }
    }

    public static Type copyTypeWithProperties(AST ast, Type type) {
        Type copy = (Type) ASTNode.copySubtree(ast, type);

        Map<String, Object> properties = type.properties();
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                copy.setProperty(entry.getKey(), entry.getValue());
            }
        }

        return copy;
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

    public static String getCommonPackageName(String package1, String package2) {
        if (package1.equals(package2)) {
            return package1;
        } else {
            String[] packageList1 = package1.split("\\.");
            String[] packageList2 = package2.split("\\.");

            int len = Math.min(packageList1.length, packageList2.length);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < len; i++) {
                if (packageList1[i].equals(packageList2[i])) {
                    sb.append(packageList1[i]);
                    sb.append(".");

                } else {
                    break;
                }
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        }
    }

    public static Name createPackageName(AST ast, String packageName) {
        if (packageName == null)
            return null;
        String[] nameList = packageName.split("\\.");
        if (nameList.length > 0) {
            return nameCreationHelper(ast, nameList, nameList.length - 1);
        } else {
            return null;
        }
    }

    private static Name nameCreationHelper(AST ast, String[] nameList, int index) {
        SimpleName name = ast.newSimpleName(nameList[index]);
        if (index == 0) {
            return name;
        } else {
            Name qualifer = nameCreationHelper(ast, nameList, index -1);
            return ast.newQualifiedName(qualifer, name);
        }
    }

    /*
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
    */

    public static MethodVisitor retrieveMethodDeclaration(CompilationUnit cu, MethodDeclaration method) {
        String cuName = cu.getJavaElement().getElementName();
        MethodVisitor visitor = new MethodVisitor(cuName.split("\\.")[0], method);
        cu.accept(visitor);
        return visitor;
    }

    public static boolean hasAncestor(ITypeBinding iTypeBinding, String qualifiedName) {
        if (qualifiedName == null) {
            return false;
        }
        Queue<ITypeBinding> queue = new LinkedList<>();
        if (iTypeBinding.getSuperclass() != null) {
            queue.offer(iTypeBinding.getSuperclass());
        }
        for (ITypeBinding cur : iTypeBinding.getInterfaces()) {
            queue.offer(cur);
        }
        while(!queue.isEmpty()) {
            ITypeBinding cur = queue.poll();
            if (cur.getBinaryName().equals(qualifiedName)) {
                return true;
            } else {
                if (cur.getSuperclass() != null) {
                    queue.offer(cur.getSuperclass());
                }
                for (ITypeBinding curInterface: cur.getInterfaces()) {
                    queue.offer(curInterface);
                }
            }
        }
        return false;
    }

    /*
    public static boolean checkSameMethodSignature(IMethodBinding method1, IMethodBinding method2) {
        if (method1 == null || method2 == null) return false;

        if (!method1.getName().equals(method2.getName())
                || method1.getParameterTypes().length != method2.getParameterTypes().length) {
            return false;
        }

        ITypeBinding[] parameterTypes1 = method1.getParameterTypes();
        ITypeBinding[] parameterTypes2 = method1.getParameterTypes();
        for (int i = 0; i < parameterTypes1.length; i++) {
            if (!parameterTypes1[i].getBinaryName().equals(parameterTypes2[i].getBinaryName())) {
                return false;
            }
        }

        return true;
    }
    */

    /*
    public static boolean checkSameITypeBinding(ITypeBinding typeBinding1, ITypeBinding typeBinding2) {

        if (typeBinding1 == null || typeBinding2 == null) {
            return false;
        }

        if (typeBinding1.isCapture() && typeBinding2.isCapture()) {
            ITypeBinding bound1 = typeBinding1.getWildcard().getBound();
            ITypeBinding bound2 = typeBinding2.getWildcard().getBound();
            log.info("typeBinding1 bound: " + bound1.getQualifiedName());
            log.info("typeBinding2 bound: " + bound2.getQualifiedName());
            if (typeBinding1.getWildcard().isUpperbound() != typeBinding2.getWildcard().isUpperbound()) {
                return false;
            }
            return checkSameITypeBinding(bound1, bound2);
        }

        if (typeBinding1.isParameterizedType() && typeBinding2.isParameterizedType()) {
            log.info("typeBinding1: " + typeBinding1.getQualifiedName());
            log.info("typeBinding2: " + typeBinding2.getQualifiedName());

            if (!checkSameITypeBinding(typeBinding1.getErasure(), typeBinding2.getErasure())) {
                return false;
            }
            ITypeBinding[] typeArgs1 = typeBinding1.getTypeArguments();
            ITypeBinding[] typeArgs2 = typeBinding1.getTypeArguments();
            log.info("typeArg1 length: " + typeArgs1.length);
            log.info("typeArg2 length: " + typeArgs2.length);
            if (typeArgs1.length != typeArgs2.length) {
                return false;
            } else {
                for (int i = 0; i < typeArgs1.length; i++) {
                    log.info("typeArgs1: " + typeArgs1[i].isCapture());
                    log.info("typeArgs1 name: " + typeArgs1[i].getQualifiedName());
                    log.info("typeArgs1 bound: " + typeArgs1[i].getWildcard().getBound().getQualifiedName());
                    log.info("typeArgs2: " + typeArgs2[i].isCapture());
                    log.info("typeArgs2 name: " + typeArgs2[i].getQualifiedName());
                    log.info("typeArgs2 bound: " + typeArgs2[i].getWildcard().getBound().getQualifiedName());
                    if (!checkSameITypeBinding(typeArgs1[i], typeArgs2[i])) {
                        return false;
                    }
                }
            }
            return true;
        }

        if (typeBinding1.isArray() && typeBinding2.isArray()) {
            return checkSameITypeBinding(typeBinding1.getElementType(), typeBinding2.getElementType());
        }

        if (typeBinding1.getQualifiedName() != null && typeBinding2.getQualifiedName() != null) {
            log.info("typeBinding1 qualified name: " + typeBinding1.getQualifiedName());
            log.info("typeBinding2 qualified name: " + typeBinding2.getQualifiedName());
            return typeBinding1.getQualifiedName().equals(typeBinding2.getQualifiedName());
        }

        return false;
    }
    */

}
