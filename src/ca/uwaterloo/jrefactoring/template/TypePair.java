package ca.uwaterloo.jrefactoring.template;

import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.Objects;

public class TypePair {

    private ITypeBinding type1;
    private ITypeBinding type2;

    public TypePair(ITypeBinding type1, ITypeBinding type2) {
        this.type1 = type1;
        this.type2 = type2;
    }

    public ITypeBinding getType1() {
        return type1;
    }

    public ITypeBinding getType2() {
        return type2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type1.getQualifiedName(), type2.getQualifiedName());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TypePair)) {
            return false;
        }
        TypePair other = (TypePair) o;
        return type1.getQualifiedName().equals(other.type1.getQualifiedName())
                && type2.getQualifiedName().equals(other.type2.getQualifiedName());
    }

    @Override
    public String toString() {
        return "TypePair: " + type1.getQualifiedName() + ", " + type2.getQualifiedName();
    }
}
