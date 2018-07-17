package ca.uwaterloo.jrefactoring.utility;

import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameUtil {

    private static final String ADAPTER_INTERFACE_NAME = "Adapter";
    private static final String TEMPLATE_METHOD_NAME = "Template";
    private static final String METHOD_NAME_PATTERN= "[ \\f\\r\\t\\n]+(.+?)[ \\f\\r\\t\\n]*\\(";
    private static final String CAMELCASE_PATTERN = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";
    private static int adapterCount = 1;
    private static int templateCount = 1;

    public static String[] splitCamelCaseName(String name) {
        if (name == null) return null;
        return name.split(CAMELCASE_PATTERN);
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

        } else {
            String typeName = type.toString().substring(0, 1).toLowerCase() + type.toString().substring(1);
            if (endsWithDigit(typeName)) {
                return typeName + "_" + count;
            } else {
                return typeName + count;
            }
        }
    }

    public static String getTemplateName(String methodSig1, String methodSig2) {
        String method1 = getMethodNameFromSignature(methodSig1);
        String method2 = getMethodNameFromSignature(methodSig2);

        String commonName = constructCommonName(method1, method2, false);
        if (!commonName.equals("")) {
            return commonName + TEMPLATE_METHOD_NAME;
        } else {
            return TEMPLATE_METHOD_NAME.toLowerCase() + templateCount++;
        }
    }

    public static String getAdapterName(String class1, String package1, String class2, String package2) {
        String commonName = constructCommonName(class1, class2, true);
        if (!commonName.equals("")) {
            return commonName + ADAPTER_INTERFACE_NAME;
        } else {
            return ADAPTER_INTERFACE_NAME + adapterCount++;
        }
    }

    public static String[] getAdapterImplNamePair(String adapterName) {
        String[] namePair = new String[2];
        namePair[0] = adapterName + "Impl1";
        namePair[1] = adapterName + "Impl2";
        return namePair;
    }

}
