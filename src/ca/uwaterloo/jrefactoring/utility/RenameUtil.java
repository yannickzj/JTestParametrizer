package ca.uwaterloo.jrefactoring.utility;

public class RenameUtil {

    public static String rename(String name1, String name2) {
        return name1 + "_" + name2;
    }

    private static boolean endsWithDigit(String name) {
        return Character.isDigit(name.charAt(name.length() - 1));
    }

    public static String rename(String typeName, int count) {
        if (endsWithDigit(typeName)) {
            return typeName + "_" + count;
        } else {
            return typeName + count;
        }
    }

}
