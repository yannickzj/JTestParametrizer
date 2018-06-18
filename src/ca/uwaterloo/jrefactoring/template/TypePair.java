package ca.uwaterloo.jrefactoring.template;

import java.util.Objects;

public class TypePair {

    private String type1;
    private String type2;

    public TypePair(String type1, String type2) {
        this.type1 = type1;
        this.type2 = type2;
    }

    public String getType1() {
        return type1;
    }

    public String getType2() {
        return type2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type1, type2);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TypePair)) {
            return false;
        }
        TypePair other = (TypePair) o;
        return type1.equals(other.type1) && type2.equals(other.type2);
    }

    @Override
    public String toString() {
        return "TypePair: " + type1 + ", " + type2;
    }
}
