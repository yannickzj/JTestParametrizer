package ca.uwaterloo.jrefactoring.utility;

import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameUtil {

    private static final String ADAPTER_INTERFACE_NAME = "Adapter";
    private static final String TEMPLATE_METHOD_NAME = "Template";
    private static final String DEFAULT_VARIABLE_PREFIX = "v";
    private static final String METHOD_NAME_PATTERN= "[ \\f\\r\\t\\n]+(.+?)[ \\f\\r\\t\\n]*\\(";
    private static int adapterCount = 1;
    private static int templateCount = 1;

    public static String getMethodNameFromSignature(String signature) {
        Pattern pattern = Pattern.compile(METHOD_NAME_PATTERN);
        Matcher matcher = pattern.matcher(signature);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalStateException("cannot match method name in signature: " + signature);
        }
    }

    public static String renameVariable(String name1, String name2, int count) {
        if (name1.equals(name2)) {
            return name1;
        } else {
            return DEFAULT_VARIABLE_PREFIX + count;
        }
    }

    public static String renameVariable(String name1, String name2, int count, String prefix) {
        if (name1.equals(name2)) {
            return name1;
        } else {
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

        String typeName = type.toString().toLowerCase();
        if (type.isPrimitiveType()) {
            return getPrimitiveTypeShortName((PrimitiveType) type) + count;

        } else {
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
        if (method1.equals(method2)) {
            return method1 + TEMPLATE_METHOD_NAME;
        } else {
            String commonSubString = getCommonSubString(method1, method2);
            if (commonSubString.equals("")) {
                return TEMPLATE_METHOD_NAME.toLowerCase() + templateCount++;

            } else {
                return commonSubString + TEMPLATE_METHOD_NAME;
            }
        }
    }

    public static String getCommonSubString(String str1, String str2) {
        return "";
    }

    public static String getAdapterName(String class1, String package1, String class2, String package2) {
        if (class1.equals(class2)) {
            return class1 + ADAPTER_INTERFACE_NAME;
        } else {
            String commonSubString = getCommonSubString(class1, class2);
            if (commonSubString.equals("")) {
                return ADAPTER_INTERFACE_NAME + adapterCount++;
            } else {
                return commonSubString + ADAPTER_INTERFACE_NAME;
            }
        }
    }

    public static String[] getAdapterImplNamePair(String adapterName) {
        String[] namePair = new String[2];
        namePair[0] = adapterName + "Impl1";
        namePair[1] = adapterName + "Impl2";
        return namePair;
    }

}
