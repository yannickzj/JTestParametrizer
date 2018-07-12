package ca.uwaterloo.jrefactoring;

import ca.uwaterloo.jrefactoring.utility.FileLogger;
import org.eclipse.jdt.core.IJavaProject;
import org.slf4j.Logger;

import java.io.File;

public class MethodRefactor {

    private static Logger log = FileLogger.getLogger(MethodRefactor.class);

    public MethodRefactor() {
    }

    public void refactor(IJavaProject iJavaProject,
                          File originalExcelFile,
                          int startFromRow,
                          boolean appendResults,
                          int[] cloneGroupIDsToSkip,
                          int[] cloneGroupIDsToAnalyze,
                          String[] testPackages, String[] testSourceFolders) throws Exception {

        log.info(iJavaProject.getProject().getName());

    }



}
