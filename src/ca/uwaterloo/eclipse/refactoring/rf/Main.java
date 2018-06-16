package ca.uwaterloo.eclipse.refactoring.rf;

import ca.uwaterloo.eclipse.refactoring.cli.CLIParser;
import ca.uwaterloo.eclipse.refactoring.cloneinfowriter.*;
import ca.uwaterloo.eclipse.refactoring.coverage.TestReportResults;
import ca.uwaterloo.eclipse.refactoring.parsers.ExcelFileColumns;
import ca.uwaterloo.eclipse.refactoring.utility.FileLogger;
import gr.uom.java.ast.*;
import gr.uom.java.ast.decomposition.cfg.CFG;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.mapping.DivideAndConquerMatcher;
import gr.uom.java.jdeodorant.refactoring.manipulators.ExtractCloneRefactoring;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

            testRefactoring(jProject, excelFile, startFrom, appendResults, cloneGroupIDsToSkip, cloneGroupIdsToAnalyze, testPackages, testSourceFolders);

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

    private void testRefactoring(IJavaProject iJavaProject,
                                 File originalExcelFile,
                                 int startFromRow,
                                 boolean appendResults,
                                 int[] cloneGroupIDsToSkip,
                                 int[] cloneGroupIDsToAnalyze,
                                 String[] testPackages, String[] testSourceFolders) throws Exception {

        log.info("Testing refactorabiliy of clones in " + originalExcelFile.getAbsolutePath());

        //TestReportResults originalTestReport = null;
        /*
        if (cliParser.runTests()) {
            originalTestReport = runUnitTests(iJavaProject, ApplicationRunner.TestReportFileType.ORIGINAL);
        }
        */

        /*
         * If we have to append the results, first we check to see
         * whether a file named {original file name}-analyzed.xls exists in this folder
         * or not. If so, this file is treated as the original excel file and new data
         * will be added to this file.
         */

        // create new xls file
        Workbook originalWorkbook;
        String originalExcelFileName = originalExcelFile.getName().substring(0, originalExcelFile.getName().lastIndexOf('.'));
        File copyWorkBookFile = new File(originalExcelFile.getParentFile().getAbsolutePath() + "/" + originalExcelFileName + "-analyzed.xls");

        /*
         * This temporary file is the one we use to read the initial data from. Its
         * the same file as originalExcelFile, except when we are going to append data to
         * an existing -analyzed excel file.
         */
        File temporaryFile = new File(originalExcelFile.getParentFile().getAbsolutePath() + "/" + originalExcelFile.getName() + "-temp.xls");
        temporaryFile.deleteOnExit();

        if (appendResults && copyWorkBookFile.exists() && copyWorkBookFile.length() > 0) {
            Files.copy(copyWorkBookFile.toPath(), temporaryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.copy(originalExcelFile.toPath(), temporaryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Make a copy of the original excel file
        originalWorkbook = Workbook.getWorkbook(temporaryFile);
        WritableWorkbook copyWorkbook = Workbook.createWorkbook(copyWorkBookFile, originalWorkbook);
        Sheet originalSheet = originalWorkbook.getSheet(0);
        WritableSheet copySheet = copyWorkbook.getSheet(0);

        // Sort the array containing clone group IDs to skip, in order to be able to do Binary Search
        Arrays.sort(cloneGroupIDsToSkip);
        Arrays.sort(cloneGroupIDsToAnalyze);

        String projectName = iJavaProject.getElementName();

        // Object used to create HTML reports and CSV files
        CloneInfoWriter htmlWriter = new CloneInfoHTMLWriter(originalExcelFile.getParentFile().getAbsolutePath(), projectName, originalExcelFileName);

        /*
        List<CloneInfoWriter> infoWriters = new ArrayList<>();

        infoWriters.add(new CloneInfoHTMLWriter(originalExcelFile.getParentFile().getAbsolutePath(), projectName, originalExcelFileName));
        infoWriters.add(new CloneInfoCSVWriter(originalExcelFile.getParentFile().getAbsolutePath(), projectName, originalExcelFileName));
        */

        int numberOfRows = originalSheet.getRows();

        int cloneNumber = 1;
        try {

            // process refactorable candidates row by row
            for (int cloneGroupStartingRowNumber = startFromRow - 1; cloneGroupStartingRowNumber < numberOfRows; cloneGroupStartingRowNumber++) {

                int cloneGroupID = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.CLONE_GROUP_ID.getColumnNumber(), cloneGroupStartingRowNumber).getContents());
                int cloneGroupSize = 0;
                try {
                    cloneGroupSize = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.CLONE_GROUP_SIZE.getColumnNumber(), cloneGroupStartingRowNumber).getContents());
                } catch (NumberFormatException nfe) {
                    log.warn(String.format("Could not read clone group size from row %s (clone group ID %s)",
                            cloneGroupStartingRowNumber, cloneGroupID));
                    continue;
                }

                // skip the cloneGroup not in cloneGroupIDsToAnalyze
                if (cloneGroupIDsToAnalyze.length > 0) {
                    if (Arrays.binarySearch(cloneGroupIDsToAnalyze, cloneGroupID) < 0) {
                        if (cloneGroupID > cloneGroupIDsToAnalyze[cloneGroupIDsToAnalyze.length - 1]) {
                            // Just finish the loop
                            cloneGroupStartingRowNumber = numberOfRows;
                            continue;
                        } else {
                            if (cloneGroupStartingRowNumber + cloneGroupSize == numberOfRows) {
                                cloneGroupStartingRowNumber = numberOfRows;
                            } else {
                                cloneGroupStartingRowNumber = cloneGroupStartingRowNumber + cloneGroupSize - 1;
                            }
                            continue;
                        }
                    }
                }

                String cloneType = originalSheet.getCell(ExcelFileColumns.CLONE_GROUP_INFO.getColumnNumber(), cloneGroupStartingRowNumber).getContents();


                boolean userSkippedGroup = Arrays.binarySearch(cloneGroupIDsToSkip, cloneGroupID) >= 0;
                boolean repeatedCloneGroup = cloneType.equals("Repeated");

                boolean classLevelClone = false;
                if (!userSkippedGroup) {
                    int numberOfBlankMethods = 0;
                    for (int cloneIndex = 0; cloneIndex < cloneGroupSize; cloneIndex++) {
                        String methodStr = originalSheet.getCell(ExcelFileColumns.METHOD_NAME.getColumnNumber(), cloneGroupStartingRowNumber + cloneIndex).getContents();
                        if (methodStr.equals("")) {
                            numberOfBlankMethods++;
                        }
                    }
                    // If all the methods inside the clone group are empty, the clone group is a class-level clone group.
                    classLevelClone = numberOfBlankMethods == cloneGroupSize;
                }

                if (userSkippedGroup || repeatedCloneGroup || classLevelClone) {

                    String status = "";

                    if (userSkippedGroup)
                        status = "user has marked this clone group to be skipped";
                    else if (repeatedCloneGroup)
                        status = "this is a repeated clone";
                    else if (classLevelClone)
                        status = "this is a class-level clone group";

                    log.warn(String.format("%s%%: Skipping clone group %s (row %s to %s), since %s",
                            Math.round(100 * (float) cloneGroupStartingRowNumber / numberOfRows),
                            cloneGroupID,
                            cloneGroupStartingRowNumber + 1,
                            cloneGroupStartingRowNumber + cloneGroupSize,
                            status));

                    for (int firstCloneNumber = 0; firstCloneNumber < cloneGroupSize - 1; firstCloneNumber++) {

                        String fullName1 = originalSheet.getCell(ExcelFileColumns.PACKAGE_NAME.getColumnNumber(), cloneGroupStartingRowNumber + firstCloneNumber).getContents().replace(".", "/") +
                                "/" + originalSheet.getCell(ExcelFileColumns.CLASS_NAME.getColumnNumber(), cloneGroupStartingRowNumber + firstCloneNumber).getContents() + ".java";
                        ICompilationUnit iCompilationUnit1 = getICompilationUnit(iJavaProject, fullName1);
                        if (iCompilationUnit1 == null) {
                            log.warn(String.format("ICompilationUnit was not found for %s, skipping clone at row %s", fullName1, cloneGroupStartingRowNumber + firstCloneNumber + 1));
                            continue;
                        }

                        int firstStartOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.START_OFFSET.getColumnNumber(), cloneGroupStartingRowNumber + firstCloneNumber).getContents());
                        int firstEndOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.END_OFFSET.getColumnNumber(), cloneGroupStartingRowNumber + firstCloneNumber).getContents());

                        for (int secondCloneNumber = firstCloneNumber + 1; secondCloneNumber < cloneGroupSize; secondCloneNumber++) {
                            String fullName2 = originalSheet.getCell(ExcelFileColumns.PACKAGE_NAME.getColumnNumber(), cloneGroupStartingRowNumber + secondCloneNumber).getContents().replace(".", "/") +
                                    "/" + originalSheet.getCell(ExcelFileColumns.CLASS_NAME.getColumnNumber(), cloneGroupStartingRowNumber + secondCloneNumber).getContents() + ".java";
                            ICompilationUnit iCompilationUnit2 = getICompilationUnit(iJavaProject, fullName2);
                            if (iCompilationUnit2 == null) {
                                log.warn(String.format("ICompilationUnit was not found for %s, skipping clone pair at rows %s and %s",
                                        fullName2, cloneGroupStartingRowNumber + firstCloneNumber + 1, cloneGroupStartingRowNumber + secondCloneNumber + 1));
                                continue;
                            }
                            int secondStartOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.START_OFFSET.getColumnNumber(), cloneGroupStartingRowNumber + secondCloneNumber).getContents());
                            int secondEndOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.END_OFFSET.getColumnNumber(), cloneGroupStartingRowNumber + secondCloneNumber).getContents());

                            ClonePairInfo clonePairInfo = new ClonePairInfo();
                            clonePairInfo.setICompilationUnitFirst(iCompilationUnit1);
                            clonePairInfo.setICompilationUnitSecond(iCompilationUnit2);
                            clonePairInfo.setCloneGroupID(cloneGroupID);
                            clonePairInfo.setProjectName(projectName);
                            clonePairInfo.setCloneFragment1ID(firstCloneNumber + 1);
                            clonePairInfo.setCloneFragment2ID(secondCloneNumber + 1);
                            clonePairInfo.setStartOffsetOfFirstCodeFragment(firstStartOffset);
                            clonePairInfo.setEndOffsetOfFirstCodeFragment(firstEndOffset);
                            clonePairInfo.setStartOffsetOfSecondCodeFragment(secondStartOffset);
                            clonePairInfo.setEndOffsetOfSecondCodeFragment(secondEndOffset);

                            // Only write information to the HTML report, not the CSV files
                            htmlWriter.writeCloneInfo(clonePairInfo);
                            //infoWriters.get(0).writeCloneInfo(clonePairInfo);

                            /*
                            addHyperlinkToTheExcelFile(copySheet,
                                    cloneGroupStartingRowNumber + firstCloneNumber,
                                    ExcelFileColumns.DETAILS.getColumnNumber() + secondCloneNumber - firstCloneNumber - 1,
                                    CloneInfoHTMLWriter.PATH_TO_HTML_REPORTS + "/" + cloneGroupID + "-" + clonePairInfo.getClonePairID() + ".html",
                                    cloneGroupID + "-" + clonePairInfo.getClonePairID(),
                                    Colour.WHITE);
                                    */
                        }
                    }

                    cloneGroupStartingRowNumber += cloneGroupSize - 1;
                    cloneNumber++;
                    continue;
                }

                int numberOfRefactorablePairs = 0;

                PDG[] pdgArray = new PDG[cloneGroupSize];

                for (int firstCloneNumber = 0; firstCloneNumber < cloneGroupSize - 1; firstCloneNumber++) {

                    int firstCloneRow = cloneGroupStartingRowNumber + firstCloneNumber;
                    log.info(String.format("%s%%: Reading information from row %s (Clone group ID %s, clone #%s)",
                            Math.round(100 * (float) firstCloneRow / numberOfRows),
                            firstCloneRow + 1, cloneGroupID, firstCloneNumber + 1));

                    String firstClassName = originalSheet.getCell(ExcelFileColumns.CLASS_NAME.getColumnNumber(), firstCloneRow).getContents();
                    String firstPackageName = originalSheet.getCell(ExcelFileColumns.PACKAGE_NAME.getColumnNumber(), firstCloneRow).getContents();
                    String firstFullName = firstClassName;
                    if (!firstPackageName.trim().equals("")) {
                        firstFullName = firstPackageName + "." + firstClassName;
                    }
                    String firstMethodName = originalSheet.getCell(ExcelFileColumns.METHOD_NAME.getColumnNumber(), firstCloneRow).getContents();
                    String firstMethodSignature = originalSheet.getCell(ExcelFileColumns.METHOD_SIGNATURE.getColumnNumber(), firstCloneRow).getContents();
                    int firstStartOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.START_OFFSET.getColumnNumber(), firstCloneRow).getContents());
                    int firstEndOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.END_OFFSET.getColumnNumber(), firstCloneRow).getContents());
                    String firstSrcFolder = originalSheet.getCell(ExcelFileColumns.SOURCE_FOLDER.getColumnNumber(), firstCloneRow).getContents();
                    String firstCloneCoverageText = originalSheet.getCell(ExcelFileColumns.LINE_COVERAGE_PERCENTAGE.getColumnNumber(), firstCloneRow).getContents();
                    float firstCloneCoverage = 0;
                    if (!"".equals(firstCloneCoverageText)) {
                        firstCloneCoverage = Float.parseFloat(firstCloneCoverageText);
                    }

					/*
					if ("".equals(firstMethodSignature)) {
						log.warn(String.format("No method could be found in file '%s' inside offsets %s to %s; " +
								"so this is a class-level clone. Skipping clone at row %s",
								firstFullName, firstStartOffset, firstEndOffset, firstCloneRow));
						continue;
					}
					*/

                    IMethod firstIMethod = getIMethod(iJavaProject, firstFullName, firstMethodName, firstMethodSignature, firstStartOffset, firstEndOffset);

                    firstStartOffset = getMethodStartPosition(firstIMethod);
                    firstEndOffset = getMethodEndPosition(firstIMethod);


                    if (firstIMethod == null) {
                        log.info(String.format("IMethod could not be retrieved for method %s in %s, skipping clone at row %s",
                                firstMethodName, firstFullName, firstCloneRow + 1));
                        continue;
                    }

                    if (pdgArray[firstCloneNumber] == null) {
                        log.info(String.format("%s%%: Generating PDG for method \"%s\" in \"%s\"",
                                Math.round(100 * (float) cloneGroupStartingRowNumber / numberOfRows),
                                firstMethodName, firstFullName));
                        pdgArray[firstCloneNumber] = getPDG(firstIMethod);
                    }

                    PDG pdg1 = pdgArray[firstCloneNumber];

                    for (int secondCloneNumber = firstCloneNumber + 1; secondCloneNumber < cloneGroupSize; secondCloneNumber++) {

                        int secondCloneRow = cloneGroupStartingRowNumber + secondCloneNumber;
                        log.info(String.format("%s%%: Reading information from row %s (Clone group ID %s, clone #%s)",
                                Math.round(100 * (float) firstCloneRow / numberOfRows),
                                secondCloneRow + 1, cloneGroupID, firstCloneNumber + secondCloneNumber + 1));

                        String secondClassName = originalSheet.getCell(ExcelFileColumns.CLASS_NAME.getColumnNumber(), secondCloneRow).getContents();
                        String secondPackageName = originalSheet.getCell(ExcelFileColumns.PACKAGE_NAME.getColumnNumber(), secondCloneRow).getContents();
                        String secondFullName = secondClassName;
                        if (!secondPackageName.trim().equals("")) {
                            secondFullName = secondPackageName + "." + secondFullName;
                        }
                        String secondMethodName = originalSheet.getCell(ExcelFileColumns.METHOD_NAME.getColumnNumber(), secondCloneRow).getContents();
                        String secondMethodSignature = originalSheet.getCell(ExcelFileColumns.METHOD_SIGNATURE.getColumnNumber(), secondCloneRow).getContents();
                        int secondStartOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.START_OFFSET.getColumnNumber(), secondCloneRow).getContents());
                        int secondEndOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.END_OFFSET.getColumnNumber(), secondCloneRow).getContents());
                        String secondSrcFolder = originalSheet.getCell(ExcelFileColumns.SOURCE_FOLDER.getColumnNumber(), secondCloneRow).getContents();
                        float secondCloneCoverage = 0;
                        String secondCloneCoverageText = originalSheet.getCell(ExcelFileColumns.LINE_COVERAGE_PERCENTAGE.getColumnNumber(), secondCloneRow).getContents();
                        if (!"".equals(secondCloneCoverageText)) {
                            secondCloneCoverage = Float.parseFloat(secondCloneCoverageText);
                        }


                        // Check if two clones overlap, we will skip such a case
                        /*
						if (firstFullName.equals(secondFullName) &&
								((firstStartOffset >= secondStartOffset && firstStartOffset <= secondEndOffset) ||
								 (secondStartOffset >= firstStartOffset && secondStartOffset <= firstEndOffset)
								)
							) {
							log.warn(String.format("Clones %s and %s in group %s overlap, skipping clone pair at rows %s-%s",
									firstCloneNumber + 1, secondCloneNumber + 1,
									cloneGroupID,
									firstCloneRow + 1, secondCloneRow + 1));
							continue;
						}

						if ("".equals(secondMethodSignature)) {
							log.warn(String.format("No method could be found in file '%s' inside offsets %s to %s ," +
									"so this is a class-level clone. Skipping clone pair at rows %s-%s",
									secondFullName, secondStartOffset, secondEndOffset, firstCloneRow, secondCloneRow));
							continue;
						}
						*/

                        IMethod secondIMethod = getIMethod(iJavaProject, secondFullName, secondMethodName, secondMethodSignature, secondStartOffset, secondEndOffset);

                        secondStartOffset = getMethodStartPosition(secondIMethod);
                        secondEndOffset = getMethodEndPosition(secondIMethod);


                        if (secondIMethod == null) {
                            log.warn(String.format("IMethod could not be retrieved for method %s in %s, skipping clone pair at rows %s-%s",
                                    firstMethodName, firstFullName, firstCloneRow + 1, secondCloneRow + 1));
                            continue;
                        }

                        PDG pdg2;

                        if (!firstIMethod.equals(secondIMethod)) {
                            if (pdgArray[secondCloneNumber] == null) {
                                log.info(String.format("%s%%: Generating PDG for method \"%s\" in \"%s\"",
                                        Math.round(100 * (float) cloneGroupStartingRowNumber / numberOfRows),
                                        secondMethodName, secondFullName));
                                pdgArray[secondCloneNumber] = getPDG(secondIMethod);
                            }

                            pdg2 = pdgArray[secondCloneNumber];
                        } else
                            pdg2 = pdg1;


                        InputMethods methodsInfo = new InputMethods(firstIMethod, secondIMethod,
                                firstStartOffset, firstEndOffset,
                                secondStartOffset, secondEndOffset,
                                pdg1, pdg2);

                        ClonePairInfo clonePairInfo = new ClonePairInfo();
                        clonePairInfo.setContaingingIMethodFirst(firstIMethod);
                        clonePairInfo.setContainingIMethodSecond(secondIMethod);
                        clonePairInfo.setCloneGroupID(cloneGroupID);
                        clonePairInfo.setProjectName(projectName);
                        clonePairInfo.setCloneFragment1ID(firstCloneNumber + 1);
                        clonePairInfo.setCloneFragment2ID(secondCloneNumber + 1);
                        clonePairInfo.setFirstSourceFolder(firstSrcFolder);
                        clonePairInfo.setSecondSrcFolder(secondSrcFolder);
                        clonePairInfo.setFirstClass(firstClassName);
                        clonePairInfo.setFirstPackage(firstPackageName);
                        clonePairInfo.setSecondClass(secondClassName);
                        clonePairInfo.setSecondPackage(secondPackageName);
                        clonePairInfo.setTestPackages(testPackages);
                        clonePairInfo.setTestSourceFolders(testSourceFolders);

                        if (firstIMethod != null && secondIMethod != null) {
                            log.info(String.format("%s%%: Analyzing Clone #%s (Group %s, Pair %s-%s): %s#%s (row %s) and %s#%s (row %s)",
                                    Math.round(100 * (float) firstCloneRow / numberOfRows), cloneNumber,
                                    cloneGroupID, firstCloneNumber + 1, secondCloneNumber + 1,
                                    firstFullName, firstMethodName, firstCloneRow + 1,
                                    secondFullName, secondMethodName, secondCloneRow + 1));
                            //getOptimalSolution(methodsInfo, clonePairInfo);
                        }

                        if (clonePairInfo.getRefactorable())
                            numberOfRefactorablePairs++;

                        boolean clonesCoveredByTests = firstCloneCoverage > 0 || secondCloneCoverage > 0;
                        if (!cliParser.runTests() || (cliParser.runTests() && clonesCoveredByTests)) {
                            for (PDGSubTreeMapperInfo pdgSubTreeMapperInfo : clonePairInfo.getPDFSubTreeMappersInfoList()) {
                                if (pdgSubTreeMapperInfo.getMapper().getMaximumStateWithMinimumDifferences() != null) {
                                    // Create a list with one mapper, because ExtractCloneRefactoring needs a list
                                    List<DivideAndConquerMatcher> mappers = new ArrayList<>();
                                    mappers.add(pdgSubTreeMapperInfo.getMapper());

                                    ExtractCloneRefactoring refactoring = new ExtractCloneRefactoring(mappers);
                                    refactoring.setExtractedMethodName("ExtractedMethod");
                                    IProgressMonitor npm = new NullProgressMonitor();
                                    try {
                                        RefactoringStatus refStatus = refactoring.checkFinalConditions(npm);

                                        if (refStatus.isOK()) {
                                            pdgSubTreeMapperInfo.setRefactoringWasOK(true);
                                            log.info("Started refactoring");
                                            Change change = refactoring.createChange(npm);
                                            Change undoChange = change.perform(npm);
                                            log.info("Finished Refactoring");
                                            /*
                                            List<IMarker> markers = buildProject(iJavaProject, npm);
                                            // Check for compile errors
                                            if (markers.size() > 0) {
                                                for (IMarker marker : markers) {
                                                    //String message = marker.getAttributes().get("message").toString();
                                                    pdgSubTreeMapperInfo.addFileHavingCompileError(marker.getResource().getFullPath().toOSString());
                                                }
                                                log.warn("Compile errors occured during refactoring");
                                            } else {
                                                if (cliParser.runTests()) {
                                                    // Run tests here and see if they pass
                                                    TestReportResults newTestReport = runUnitTests(iJavaProject, ApplicationRunner.TestReportFileType.AFTER_REFACTORING);
                                                    log.info("Comparing test results");
                                                    List<TestReportResults.TestReportDifference> compareTestResults = newTestReport.compareTestResults(originalTestReport);
                                                    if (compareTestResults.size() != 0) {
                                                        log.warn("Tests failed after refactoring");
                                                        pdgSubTreeMapperInfo.setTestDifferences(compareTestResults);
                                                    } else {
                                                        log.info("Tests passed after refactoring");
                                                    }
                                                }
                                            }
                                            */

                                            log.info("Started undoing refactoring");
                                            boolean shouldRetry = true;
                                            do {
                                                try {
                                                    undoChange.perform(npm);
                                                    shouldRetry = false;
                                                } catch (ResourceException rex) {
                                                    log.warn("Exception while deleting resources, retrying...");
                                                    Thread.sleep(500);
                                                }
                                            } while (shouldRetry);
                                            log.info("Finished undoing refactoring");
                                            /*
                                            markers = buildProject(iJavaProject, npm);
                                            if (markers.size() > 0) {
                                                // Is it possible to have compile errors after undoing?
                                                log.error("Compiler errors after undoing refactorings");
                                            }
                                            */
                                            iJavaProject.getProject().deleteMarkers(null, true, IResource.DEPTH_INFINITE);
                                            iJavaProject.getProject().clearHistory(new NullProgressMonitor());
                                        } else {
                                            pdgSubTreeMapperInfo.setRefactoringWasOK(false);
                                            log.warn("Refactoring was not applied due to precondition violations");
                                        }
                                    } catch (MalformedTreeException mte) {
                                        // Overlapping text edits
                                        pdgSubTreeMapperInfo.addFileHavingCompileError("Overlapping text edits");
                                    }
                                } else {
                                    log.info("No statements mapped");
                                }
                            }
                        } else {
                            log.info("Did not apply refactoring on the current clone pair, because none of the clones is covered by unit tests");
                        }

                        CompilationUnitCache.getInstance().releaseLock();

                        if (firstCloneNumber == 0) {

                            if (secondCloneNumber == 1) {
                                Number number = new Number(ExcelFileColumns.NUMBER_OF_PDG_NODES.getColumnNumber(), firstCloneRow,
                                        clonePairInfo.getNumberOfPDGNodesInFirstMethod());
                                copySheet.addCell(number);

                                number = new Number(ExcelFileColumns.NUMBER_OF_STATEMENTS.getColumnNumber(), firstCloneRow,
                                        clonePairInfo.getNumberOfCloneStatementsInFirstCodeFragment());
                                copySheet.addCell(number);
                            }

                            Number number = new Number(ExcelFileColumns.NUMBER_OF_PDG_NODES.getColumnNumber(), secondCloneRow,
                                    clonePairInfo.getNumberOfPDGNodesInSecondMethod());
                            copySheet.addCell(number);

                            number = new Number(ExcelFileColumns.NUMBER_OF_STATEMENTS.getColumnNumber(), secondCloneRow,
                                    clonePairInfo.getNumberOfCloneStatementsInSecondCodeFragment());
                            copySheet.addCell(number);
                        }


                        // Write the stuff to the HTML files and CSV files
                        for (CloneInfoWriter cloneInfoWriter : infoWriters)
                            cloneInfoWriter.writeCloneInfo(clonePairInfo);

                        /*
                        addHyperlinkToTheExcelFile(copySheet,
                                firstCloneRow,
                                ExcelFileColumns.DETAILS.getColumnNumber() + secondCloneNumber - firstCloneNumber - 1,
                                CloneInfoHTMLWriter.PATH_TO_HTML_REPORTS + "/" + cloneGroupID + "-" + clonePairInfo.getClonePairID() + ".html",
                                cloneGroupID + "-" + clonePairInfo.getClonePairID(),
                                clonePairInfo.getRefactorable() ? Colour.LIGHT_GREEN : Colour.RED);
                                */

                    }
                    pdgArray[firstCloneNumber] = null;
                }

                Number number = new Number(ExcelFileColumns.NUMBER_OF_REFACTORABLE_PAIRS.getColumnNumber(), cloneGroupStartingRowNumber, numberOfRefactorablePairs);
                copySheet.addCell(number);

                if (cloneGroupStartingRowNumber + cloneGroupSize == numberOfRows)
                    cloneGroupStartingRowNumber = numberOfRows;
                else
                    cloneGroupStartingRowNumber = cloneGroupStartingRowNumber + cloneGroupSize - 1;

                cloneNumber++;

                for (CloneInfoWriter cloneInfoWriter : infoWriters)
                    cloneInfoWriter.closeMedia(appendResults);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            copyWorkbook.write();
            copyWorkbook.close();
            iJavaProject.getProject().getWorkspace().save(true, new NullProgressMonitor());
        }

        log.info("Finished testing refactorabiliy of clones in " + originalExcelFile.getAbsolutePath() + ", output file: " + copyWorkBookFile.getAbsolutePath());

    }

    private ICompilationUnit getICompilationUnit(IJavaProject iJavaProject, String fullName1) {
        try {
            IClasspathEntry[] classpathEntries = iJavaProject.getResolvedClasspath(true);
            for (int i = 0; i < classpathEntries.length; i++) {
                IClasspathEntry entry = classpathEntries[i];

                if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE) {
                    IPath path = entry.getPath();
                    if (path.toString().length() > iJavaProject.getProject().getName().length() + 2) {
                        String fullPath = path.toString().substring(iJavaProject.getProject().getName().length() + 2) + "/" + fullName1;

                        ICompilationUnit iCompilationUnit = (ICompilationUnit) JavaCore.create(iJavaProject.getProject().getFile(fullPath));
                        if (iCompilationUnit != null && iCompilationUnit.exists())
                            return iCompilationUnit;
                    }
                }
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
        return null;
    }

    private IMethod recursiveGetIMethod(IType type, IJavaProject jProject, String methodName, String methodSignature, int start, int end) throws JavaModelException {
        IMethod innerMethod = null;
        for (IType innerType : type.getCompilationUnit().getAllTypes()) {
            if (!methodSignature.equals("")) {
                innerMethod = getIMethodWithSignature(jProject, innerType, methodName, methodSignature, start, end);
                if (innerMethod != null)
                    return innerMethod;
            }
        }
        return null;
    }

    private IMethod getIMethod(IJavaProject jProject, String typeName, String methodName, String methodSignature, int start, int end)
            throws JavaModelException {
        IType type = jProject.findType(typeName);
        if (type == null) {
            IPath path = new Path("/" + jProject.getElementName() + "/" + typeName.substring(0, typeName.lastIndexOf(".")));
            IPackageFragment packageFragment = jProject.findPackageFragment(path);
            if (packageFragment != null)
                type = jProject.findPackageFragment(path).getCompilationUnit(typeName.substring(typeName.lastIndexOf(".") + 1) + ".java").findPrimaryType();
            else
                return null;
        }
        IMethod iMethod = null;
        if (!methodSignature.equals("")) {
            iMethod = getIMethodWithSignature(jProject, type, methodName, methodSignature, start, end);
        }

        if (iMethod == null) {
            iMethod = recursiveGetIMethod(type, jProject, methodName, methodSignature, start, end);
        }
        return iMethod;
    }

    private IMethod getIMethodWithSignature(IJavaProject jProject, IType type, String methodName, String methodSignature, int start, int end)
            throws JavaModelException {

        SystemObject systemObject = ASTReader.getSystemObject();
        List<IMethod> methods = new ArrayList<IMethod>();
        if (type.exists()) {
            for (IMethod method : type.getMethods()) {
                methods.add(method);
            }
        } else {
            IJavaElement typeParent = type.getParent();
            if (typeParent != null && typeParent instanceof ICompilationUnit) {
                ICompilationUnit iCompilationUnit = (ICompilationUnit) typeParent;
                IType[] allTypes = iCompilationUnit.getAllTypes();
                for (IType iType : allTypes) {
                    for (IMethod iMethod : iType.getMethods()) {
                        methods.add(iMethod);
                    }
                }
            }
        }
        IMethod iMethod = null;
        for (IMethod method : methods) {
            SourceMethod sm = (SourceMethod) method;
            IJavaElement[] smChildren = sm.getChildren();
            if (smChildren.length != 0) {
                if (method.getSignature().equals(methodSignature) && method.getElementName().equals(methodName)) {
                    iMethod = method;
                    break;
                }

                for (int i = 0; i < smChildren.length; i++) {
                    if (smChildren[i] instanceof SourceType) {
                        SourceType st = (SourceType) smChildren[i];
                        for (IMethod im : st.getMethods()) {
                            if (im.getSignature().equals(methodSignature) && im.getElementName().equals(methodName)) {
                                iMethod = im;
                                return iMethod;
                            }
                        }
                    }
                }
            } else if (method.getSignature().equals(methodSignature) && method.getElementName().equals(methodName)) {
                iMethod = method;
                break;
            }
        }
        return iMethod;
    }

    private int getMethodStartPosition(IMethod iMethod) {
        SystemObject systemObject = ASTReader.getSystemObject();
        AbstractMethodDeclaration abstractMethodDeclaration = systemObject.getMethodObject(iMethod);
        MethodDeclaration methodAST = abstractMethodDeclaration.getMethodDeclaration();
        return methodAST.getStartPosition();
    }

    private int getMethodEndPosition(IMethod iMethod) {
        SystemObject systemObject = ASTReader.getSystemObject();
        AbstractMethodDeclaration abstractMethodDeclaration = systemObject.getMethodObject(iMethod);
        MethodDeclaration methodAST = abstractMethodDeclaration.getMethodDeclaration();
        return methodAST.getStartPosition() + methodAST.getLength();
    }

    private PDG getPDG(IMethod iMethod) throws Exception {
        SystemObject systemObject = ASTReader.getSystemObject();
        AbstractMethodDeclaration methodObject = systemObject.getMethodObject(iMethod);
        ClassDeclarationObject classObject = null;

        if (iMethod.getDeclaringType().isAnonymous()) {
            classObject = systemObject.getAnonymousClassDeclaration(iMethod.getDeclaringType());
        } else {
            classObject = systemObject.getClassObject(methodObject.getClassName());
        }

        ITypeRoot typeRoot = classObject.getITypeRoot();
        CompilationUnitCache.getInstance().lock(typeRoot);
        CFG cfg = new CFG(methodObject);
        final PDG pdg = new PDG(cfg, classObject.getIFile(), classObject.getFieldsAccessedInsideMethod(methodObject), null);
        return pdg;
    }

    @Override
    public void stop() {

    }
}
