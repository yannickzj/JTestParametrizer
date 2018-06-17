package ca.uwaterloo.jrefactoring.template;

import java.util.HashMap;
import java.util.Map;

public class GenericManager {

    private Map<TypePair, String> typePool;
    private Map<String, String> clazzInstanceMap;

    public GenericManager() {
        typePool = new HashMap<>();
        clazzInstanceMap = new HashMap<>();
    }

    public Map<TypePair, String> getTypePool() {
        return typePool;
    }

    public Map<String, String> getClazzInstanceMap() {
        return clazzInstanceMap;
    }
}
