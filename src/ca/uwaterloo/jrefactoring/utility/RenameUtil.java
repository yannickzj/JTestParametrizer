package ca.uwaterloo.jrefactoring.utility;

import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameUtil {

    private static final String ADAPTER_INTERFACE_NAME = "Adapter";
    private static final String TEMPLATE_METHOD_NAME = "Template";
    private static final String ADAPTER_IMPL_NAME = "AdapterImpl";
    private static final String ARRAY_NAME = "Array";
    private static final String METHOD_TEST_PREFIX = "Test";
    private static final String METHOD_NAME_PATTERN= "[ \\f\\r\\t\\n]+(.+?)[ \\f\\r\\t\\n]*\\(";
    private static final String CAMELCASE_PATTERN = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";
    private static int adapterCount = 1;
    private static int templateCount = 1;
    private static Map<String, Integer> adapterNameCountMap = new HashMap<>();

    public static String[] splitCamelCaseName(String name) {
        if (name == null) return null;

        List<String> result = new ArrayList<>();
        int prev = 0;
        int i = 1;
        for (; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i))) {
                if (!Character.isUpperCase(name.charAt(i - 1))
                        || (i < name.length() - 1 && Character.isLowerCase(name.charAt(i + 1)))) {
                    result.add(name.substring(prev, i));
                    prev = i;
                }
            }
        }
        result.add(name.substring(prev, i));
        return result.toArray(new String[0]);
        //return name.split(CAMELCASE_PATTERN);
    }

    public static String constructCommonName(String name1, String name2, boolean isClass) {
        String[] list1 = splitCamelCaseName(name1);
        String[] list2 = splitCamelCaseName(name2);
        if (list1 != null && list2 != null) {
            Set<String> set = new HashSet<>();
            for (String s : list1) {
                set.add(s.toLowerCase());
            }

            List<String> commons = new ArrayList<>();
            for (String s : list2) {
                if (!set.add(s.toLowerCase())) {
                    commons.add(s);
                }
            }

            StringBuilder sb = new StringBuilder();
            if (commons.size() > 0) {
                if (isClass) {
                    String firstComponent = commons.get(0);
                    sb.append(firstComponent.substring(0, 1).toUpperCase() + firstComponent.substring(1));
                } else {
                    sb.append(commons.get(0).toLowerCase());
                }
            }
            for (int i = 1; i < commons.size(); i++) {
                sb.append(commons.get(i));
            }
            return sb.toString();

        } else {
            return "";
        }
    }

    public static String getMethodNameFromSignature(String signature) {
        Pattern pattern = Pattern.compile(METHOD_NAME_PATTERN);
        Matcher matcher = pattern.matcher(signature);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalStateException("cannot match method name in signature: " + signature);
        }
    }

    public static String renameVariable(String name1, String name2, int count, String prefix) {
        if (name1.equals(name2)) {
            return name1;
        } else {
            String commonName = constructCommonName(name1, name2, false);
            if (!commonName.equals("")) {
                return commonName + count;
            }
            return prefix + count;
        }
    }

    private static boolean endsWithDigit(String name) {
        return Character.isDigit(name.charAt(name.length() - 1));
    }

    private static String getPrimitiveTypeShortName(PrimitiveType primitiveType) {
        if (primitiveType.getPrimitiveTypeCode() == PrimitiveType.BYTE) {
            return primitiveType.toString().toLowerCase();
        } else {
            return primitiveType.toString().substring(0, 1);
        }
    }

    public static String rename(Type type, int count) {

        if (type.isPrimitiveType()) {
            return getPrimitiveTypeShortName((PrimitiveType) type) + count;

        } else if (type.isArrayType()) {
            ArrayType arrayType = (ArrayType) type;
            String elementTypeName = arrayType.getElementType().toString().substring(0, 1).toLowerCase()
                    + arrayType.getElementType().toString().substring(1);
            return elementTypeName + ARRAY_NAME + count;

        } else if (type.isParameterizedType()) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            String elementTypeName = parameterizedType.getType().toString().substring(0, 1).toLowerCase()
                    + parameterizedType.getType().toString().substring(1);
            StringBuilder sb = new StringBuilder();
            List<Type> typeArgs = parameterizedType.typeArguments();
            for (Type typeArg : typeArgs) {
                sb.append(typeArg.toString());
            }
            return elementTypeName + sb.toString() + count;

        } else {
            String typeName = type.toString().substring(0, 1).toLowerCase() + type.toString().substring(1);
            if (endsWithDigit(typeName)) {
                return typeName + "_" + count;
            } else {
                return typeName + count;
            }
        }
    }

    public static String getTemplateName(String class1, String methodSig1, String class2, String methodSig2) {
        String method1 = getMethodNameFromSignature(methodSig1);
        String method2 = getMethodNameFromSignature(methodSig2);

        String classCommonName = constructCommonName(class1, class2, false);
        String methodCommonName = constructCommonName(method1, method2, true);
        /*
        if (methodCommonName.startsWith(METHOD_TEST_PREFIX)) {
            methodCommonName = methodCommonName.substring(methodCommonName.indexOf(METHOD_TEST_PREFIX) + METHOD_TEST_PREFIX.length());
        }
        */

        String commonName = classCommonName + methodCommonName;
        if (!commonName.equals("")) {
            return commonName + TEMPLATE_METHOD_NAME;
        } else {
            return TEMPLATE_METHOD_NAME.toLowerCase() + templateCount++;
        }
    }

    private static String getDefaultAdapterNameByPackage(String packageName) {
        int count = adapterNameCountMap.getOrDefault(packageName, 1);
        adapterNameCountMap.put(packageName, count + 1);
        return ADAPTER_INTERFACE_NAME + count;
    }

    public static String getAdapterName(String class1, String methodSig1, String package1,
                                        String class2, String methodSig2, String package2) {

        String method1 = getMethodNameFromSignature(methodSig1);
        String method2 = getMethodNameFromSignature(methodSig2);

        String classCommonName = constructCommonName(class1, class2, true);
        String methodCommonName = constructCommonName(method1, method2, true);

        String commonName = classCommonName + methodCommonName;
        String commonPackage = ASTNodeUtil.getCommonPackageName(package1, package2);

        if (!commonName.equals("")) {
            if (commonName.toLowerCase().contains("test") && commonName.length() < 6) {
                return getDefaultAdapterNameByPackage(commonPackage);
            } else {
                return commonName + ADAPTER_INTERFACE_NAME;
            }

        } else {
            return getDefaultAdapterNameByPackage(commonPackage);
        }
    }

    public static String[] getAdapterImplNamePair(String adapterName, String class1, String class2,
                                                  String signature1, String signature2) {
        String[] namePair = new String[2];
        String method1 = getMethodNameFromSignature(signature1);
        String method2 = getMethodNameFromSignature(signature2);
        method1 = method1.substring(0, 1).toUpperCase() + method1.substring(1);
        method2 = method2.substring(0, 1).toUpperCase() + method2.substring(1);

        if (!class1.equals(class2) || !method1.equals(method2)) {
            namePair[0] = class1 + method1 + ADAPTER_IMPL_NAME;
            namePair[1] = class2 + method2 + ADAPTER_IMPL_NAME;

            /*
        } else if (!method1.equals(method2)) {
            namePair[0] = method1 + ADAPTER_IMPL_NAME;
            namePair[1] = method2 + ADAPTER_IMPL_NAME;
            */

        } else {
            namePair[0] = adapterName + "Impl1";
            namePair[1] = adapterName + "Impl2";
        }
        return namePair;
    }

}
