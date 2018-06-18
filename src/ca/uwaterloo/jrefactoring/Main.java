package ca.uwaterloo.jrefactoring;

import ca.uwaterloo.jrefactoring.ast.ASTReader;
import ca.uwaterloo.jrefactoring.cli.CLIParser;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import ca.uwaterloo.jrefactoring.mapping.RefactoringMapper;
import gr.uom.java.ast.CompilationErrorDetectedException;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.AST;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;

public class Main implements IApplication {

    private static Logger log = FileLogger.getLogger(Main.class);
    private static CLIParser cliParser;

    @Override
    public Object start(IApplicationContext context) throws Exception {

        // get the commandline parser
        cliParser = new CLIParser((String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS));

        if (cliParser.showHelp()) {
            cliParser.printHelp();
        } else {

            // load java project from the workspace
            String projectName = "";
            IJavaProject jProject = null;

            if (cliParser.getProjectDescritionFile() != null) {

                IProjectDescription projectDescription = ResourcesPlugin.getWorkspace().
                        loadProjectDescription(new Path(cliParser.getProjectDescritionFile()));

                projectName = projectDescription.getName();
                IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

                if (!project.exists()) {
                    project.create(projectDescription, null);
                }
                if (!project.isOpen()) {
                    project.open(null);
                }
                if (project.hasNature(JavaCore.NATURE_ID)) {
                    jProject = JavaCore.create(project);
                }

            } else if (cliParser.getProjectName() != null) {
                projectName = cliParser.getProjectName();
                jProject = findJavaProjectInWorkspace(projectName);
            }

            if (jProject == null) {
                throw new RuntimeException("The project \"" + projectName + "\" is not opened in the workspace. Cannot continue.");
            }

            parseJavaProject(jProject);

            IProject project = jProject.getProject();
            project.setDescription(project.getDescription(), ~IProject.KEEP_HISTORY, new NullProgressMonitor());

            // get excel file
            File excelFile = new File(cliParser.getExcelFilePath());

            if (!excelFile.exists()) {
                throw new FileNotFoundException("Excel file " + excelFile.getAbsolutePath() + " was not found.");
            } else {
                log.info("Excel file found: " + excelFile.getAbsolutePath());
            }

            // add log file
            if (cliParser.hasLogToFile()) {
                String logPath = excelFile.getParentFile().getAbsolutePath() + "/log.log";
                FileLogger.addFileAppender(logPath, false);
                log.info("log file in " + logPath);
            }

            int startFrom = cliParser.getStartingRow();
            boolean appendResults = cliParser.getAppendResults();
            int[] cloneGroupIDsToSkip = cliParser.getCloneGroupIDsToSkip();
            int[] cloneGroupIdsToAnalyze = cliParser.getCloneGroupIDsToAnalyze();
            String[] testPackages = cliParser.getTestPackages();
            String[] testSourceFolders = cliParser.getTestSourceFolders();

            RefactoringMapper mapper = RefactoringMapper.getInstance();
            mapper.testRefactoring(jProject, excelFile, startFrom, appendResults, cloneGroupIDsToSkip, cloneGroupIdsToAnalyze, testPackages, testSourceFolders);

        }

        return IApplication.EXIT_OK;
    }

    private IJavaProject findJavaProjectInWorkspace(String projectName) throws CoreException {
        IJavaProject jProject = null;
        for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID) && project.getName().equals(projectName)) {
                jProject = JavaCore.create(project);
                log.info("Project " + projectName + " was found in the workspace");
                break;
            }
        }
        return jProject;
    }

    private void parseJavaProject(IJavaProject jProject) {
        log.info("Now parsing the project");
        try {
            ASTReader.JLS = AST.JLS8;
            if (ASTReader.getSystemObject() != null && jProject.equals(ASTReader.getExaminedProject())) {
                new ASTReader(jProject, ASTReader.getSystemObject(), null);
            } else {
                new ASTReader(jProject, null);
            }
        } catch (CompilationErrorDetectedException e) {
            log.info("Project contains compilation errors");
        }
        log.info("Finished parsing");
    }

    @Override
    public void stop() {
    }
}
