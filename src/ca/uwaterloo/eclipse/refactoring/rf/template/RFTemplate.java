package ca.uwaterloo.eclipse.refactoring.rf.template;

public class RFTemplate {

    private GenericManager genericManager;

    public RFTemplate(GenericManager genericManager) {
        this.genericManager = genericManager;
    }

    public GenericManager getGenericManager() {
        return genericManager;
    }

    @Override
    public String toString() {
        return "this is a refactorable template";
    }

}
