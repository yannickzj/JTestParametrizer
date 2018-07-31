package ca.uwaterloo.jrefactoring;

import ca.uwaterloo.jrefactoring.detect.ClonePairInfo;
import ca.uwaterloo.jrefactoring.detect.InputMethods;
import ca.uwaterloo.jrefactoring.detect.PDGSubTreeMapperInfo;
import ca.uwaterloo.jrefactoring.diff.TextDiff;
import ca.uwaterloo.jrefactoring.utility.ExcelFileColumns;
import ca.uwaterloo.jrefactoring.utility.FileLogger;
import gr.uom.java.ast.*;
import gr.uom.java.ast.decomposition.cfg.CFG;
import gr.uom.java.ast.decomposition.cfg.PDG;
import gr.uom.java.ast.decomposition.cfg.PDGMethodEntryNode;
import gr.uom.java.ast.decomposition.cfg.PDGNode;
import gr.uom.java.ast.decomposition.cfg.mapping.*;
import gr.uom.java.ast.decomposition.matching.NodePairComparisonCache;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.slf4j.Logger;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ProjectRefactor {

    private static Logger log = FileLogger.getLogger(ProjectRefactor.class);
    private static ProjectRefactor refactor;

    private ProjectRefactor() {
    }

    public static ProjectRefactor getInstance() {
        if (refactor == null) {
            refactor = new ProjectRefactor();
        }
        return refactor;
    }

    public void refactor(IJavaProject iJavaProject,
                         File originalExcelFile,
                         int startFromRow,
                         boolean appendResults,
                         int[] cloneGroupIDsToSkip,
                         int[] cloneGroupIDsToAnalyze,
                         String[] testPackages,
                         String[] testSourceFolders,
                         String packageName,
                         boolean applyChanges) throws Exception {

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
        //CloneInfoWriter htmlWriter = new CloneInfoHTMLWriter(originalExcelFile.getParentFile().getAbsolutePath(), projectName, originalExcelFileName);

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
                            //CloneRefactor.refactor(clonePairInfo);
                            //htmlWriter.writeCloneInfo(clonePairInfo);
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
                    //int firstStartOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.START_OFFSET.getColumnNumber(), firstCloneRow).getContents());
                    //int firstEndOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.END_OFFSET.getColumnNumber(), firstCloneRow).getContents());
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

					if (packageName != null && !firstPackageName.startsWith(packageName)) {
					    log.info("first package name is not equal to target package name: " + packageName);
					    continue;
                    }

                    IMethod firstIMethod = getIMethod(iJavaProject, firstFullName, firstMethodName, firstMethodSignature);

                    int firstStartOffset = getMethodStartPosition(firstIMethod);
                    int firstEndOffset = getMethodEndPosition(firstIMethod);


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
                        //int secondStartOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.START_OFFSET.getColumnNumber(), secondCloneRow).getContents());
                        //int secondEndOffset = Integer.parseInt(originalSheet.getCell(ExcelFileColumns.END_OFFSET.getColumnNumber(), secondCloneRow).getContents());
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
							) {if (packageName != null && !firstPackageName.equals(packageName)) {
					    continue;
                    }
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

                        if (packageName != null && !secondPackageName.startsWith(packageName)) {
                            log.info("second package name is not equal to target package name: " + packageName);
                            continue;
                        }

                        IMethod secondIMethod = getIMethod(iJavaProject, secondFullName, secondMethodName, secondMethodSignature);

                        int secondStartOffset = getMethodStartPosition(secondIMethod);
                        int secondEndOffset = getMethodEndPosition(secondIMethod);


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
                            getOptimalSolution(methodsInfo, clonePairInfo);
                        }

                        /*
                        if (clonePairInfo.getRefactorable())
                            numberOfRefactorablePairs++;

                        boolean clonesCoveredByTests = firstCloneCoverage > 0 || secondCloneCoverage > 0;
                        if (!cliParser.runTests() || (cliParser.runTests() && clonesCoveredByTests)) {
                            for (PDGSubTreeMapperInfo pdgSubTreeMapperInfo : clonePairInfo.getPDFSubTreeMappersInfoList()) {
                                if (pdgSubTreeMapperInfo.getMapper().getMaximumStateWithMinimumDifferences() != null) {
                                    // Create a list with one refactor, because ExtractCloneRefactoring needs a list
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
                                            markers = buildProject(iJavaProject, npm);
                                            if (markers.size() > 0) {
                                                // Is it possible to have compile errors after undoing?
                                                log.error("Compiler errors after undoing refactorings");
                                            }
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
                        */

                        CompilationUnitCache.getInstance().releaseLock();

                        /*
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
                        */


                        // Write the stuff to the HTML files and CSV files
                        CloneRefactor.refactor(clonePairInfo, firstCloneNumber, secondCloneNumber, firstCloneRow, copySheet);
                        //htmlWriter.writeCloneInfo(clonePairInfo);
                        /*
                        for (CloneInfoWriter cloneInfoWriter : infoWriters)
                            cloneInfoWriter.writeCloneInfo(clonePairInfo);
                            */

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

                //htmlWriter.closeMedia(appendResults);
                /*
                for (CloneInfoWriter cloneInfoWriter : infoWriters)
                    cloneInfoWriter.closeMedia(appendResults);
                    */
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            copyWorkbook.write();
            copyWorkbook.close();
            iJavaProject.getProject().getWorkspace().save(true, new NullProgressMonitor());
        }

        if (applyChanges) {
            log.info("Applying changes to source files");
            CloneRefactor.applyChanges();
        }

        log.info("Finished testing refactorabiliy of clones in " + originalExcelFile.getAbsolutePath() + ", output file: " + copyWorkBookFile.getAbsolutePath());

    }

    private void getOptimalSolution(InputMethods inputMethodsInfo, ClonePairInfo pairInfo) throws Exception {

        int firstStartOffset = inputMethodsInfo.getStartOffset1();
        int firstEndOffset = inputMethodsInfo.getEndOffset1();
        int secondStartOffset = inputMethodsInfo.getStartOffset2();
        int secondEndOffset = inputMethodsInfo.getEndOffset2();
        SystemObject systemObject = ASTReader.getSystemObject();

        AbstractMethodDeclaration methodObject1 = systemObject.getMethodObject(inputMethodsInfo.getIMethod1());
        AbstractMethodDeclaration methodObject2 = systemObject.getMethodObject(inputMethodsInfo.getIMethod2());

        if (methodObject1 != null && methodObject2 != null && methodObject1.getMethodBody() != null && methodObject2.getMethodBody() != null) {
            CompilationUnitCache.getInstance().clearCache();
            ClassDeclarationObject classObject1 = null;
            ClassDeclarationObject classObject2 = null;

            if (inputMethodsInfo.getIMethod1().getDeclaringType().isAnonymous()) {
                classObject1 = systemObject.getAnonymousClassDeclaration(inputMethodsInfo.getIMethod1().getDeclaringType());
            } else {
                classObject1 = systemObject.getClassObject(methodObject1.getClassName());
            }

            if (inputMethodsInfo.getIMethod2().getDeclaringType().isAnonymous()) {
                classObject2 = systemObject.getAnonymousClassDeclaration(inputMethodsInfo.getIMethod2().getDeclaringType());

            } else {
                classObject2 = systemObject.getClassObject(methodObject2.getClassName());
            }

            ITypeRoot typeRoot1 = classObject1.getITypeRoot();
            ITypeRoot typeRoot2 = classObject2.getITypeRoot();
            CompilationUnitCache.getInstance().lock(typeRoot1);
            CompilationUnitCache.getInstance().lock(typeRoot2);

            ASTNode node1 = NodeFinder.perform(classObject1.getClassObject().getAbstractTypeDeclaration().getRoot(), firstStartOffset, firstEndOffset - firstStartOffset);
            ExtractStatementsVisitor visitor1 = new ExtractStatementsVisitor(node1);
            node1.accept(visitor1);

            if (visitor1.getStatementsList().size() == 0)
                node1.getParent().accept(visitor1);

            ASTNode node2 = NodeFinder.perform(classObject2.getClassObject().getAbstractTypeDeclaration().getRoot(), secondStartOffset, secondEndOffset - secondStartOffset);
            ExtractStatementsVisitor visitor2 = new ExtractStatementsVisitor(node2);
            node2.accept(visitor2);

            if (visitor2.getStatementsList().size() == 0)
                node2.getParent().accept(visitor2);

            // These two contain the entire nesting structure of the methods
            ControlDependenceTreeNode controlDependenceTreePDG1 = new ControlDependenceTreeGenerator(inputMethodsInfo.getFirstPDG()).getRoot();
            ControlDependenceTreeNode controlDependenceTreePDG2 = new ControlDependenceTreeGenerator(inputMethodsInfo.getSecondPDG()).getRoot();

            log.info("CDT 1 depth = " + controlDependenceTreePDG1.getMaxLevel() +
                    ", CDT 2 depth = " + controlDependenceTreePDG2.getMaxLevel());

            log.info("CDT 1 leaves = " + controlDependenceTreePDG1.getLeaves().size() +
                    ", CDT 2 leaves = " + controlDependenceTreePDG2.getLeaves().size());

            // Get the control predicate nodes inside the ASTNode returned by Eclipse's NodeFinder
            List<ASTNode> controlASTNodes1X = visitor1.getControlStatementsList();
            List<ASTNode> controlASTNodes2X = visitor2.getControlStatementsList();

            // Get the control predicate nodes inside the clone fragments
            List<ASTNode> controlASTNodes1 = new ArrayList<ASTNode>();
            List<ASTNode> controlASTNodes2 = new ArrayList<ASTNode>();

            for (ASTNode astNode : controlASTNodes1X) {
                if (isInside(astNode, firstStartOffset, firstEndOffset, pairInfo.getICompilationUnitFirst()))
                    controlASTNodes1.add(astNode);
            }

            for (ASTNode astNode : controlASTNodes2X) {
                if (isInside(astNode, secondStartOffset, secondEndOffset, pairInfo.getICompilationUnitSecond()))
                    controlASTNodes2.add(astNode);
            }

            // Get all statement nodes (including control and leaf nodes) inside the ASTNode returned by Eclipse's NodeFinder
            List<ASTNode> ASTNodes1X = visitor1.getStatementsList();
            List<ASTNode> ASTNodes2X = visitor2.getStatementsList();

            // Get all statement nodes inside the clone fragments
            List<ASTNode> ASTNodes1 = new ArrayList<ASTNode>();
            List<ASTNode> ASTNodes2 = new ArrayList<ASTNode>();

            for (ASTNode astNode : ASTNodes1X) {
                if (isInside(astNode, firstStartOffset, firstEndOffset, pairInfo.getICompilationUnitFirst())) {
                    ASTNodes1.add(astNode);
                }
            }

            for (ASTNode astNode : ASTNodes2X) {
                if (isInside(astNode, secondStartOffset, secondEndOffset, pairInfo.getICompilationUnitSecond())) {
                    ASTNodes2.add(astNode);
                }
            }


            // Get the real offsets of the AST nodes being analyzed (inside the code fragments)
            int minStart1 = Integer.MAX_VALUE;
            int minStart2 = Integer.MAX_VALUE;
            int maxEnd1 = -1;
            int maxEnd2 = -1;

            for (ASTNode node : ASTNodes1) {
                if (minStart1 > node.getStartPosition())
                    minStart1 = node.getStartPosition();
                if (maxEnd1 < node.getStartPosition() + node.getLength())
                    maxEnd1 = node.getStartPosition() + node.getLength();
            }

            pairInfo.setStartOffsetOfFirstCodeFragment(minStart1);
            pairInfo.setEndOffsetOfFirstCodeFragment(maxEnd1);

            for (ASTNode node : ASTNodes2) {
                if (minStart2 > node.getStartPosition())
                    minStart2 = node.getStartPosition();
                if (maxEnd2 < node.getStartPosition() + node.getLength())
                    maxEnd2 = node.getStartPosition() + node.getLength();
            }

            pairInfo.setStartOffsetOfSecondCodeFragment(minStart2);
            pairInfo.setEndOffsetOfSecondCodeFragment(maxEnd2);


            // Get all the control predicate nodes inside the methods containing the clone fragments
            List<ControlDependenceTreeNode> CDTNodes1 = controlDependenceTreePDG1.getNodesInBreadthFirstOrder();
            List<ControlDependenceTreeNode> CDTNodes2 = controlDependenceTreePDG2.getNodesInBreadthFirstOrder();

            // Get the control dependence tree nodes in the clone fragments
            List<ControlDependenceTreeNode> subTreeCDTNodes1 = getSubTreeCDTNodes(CDTNodes1, controlASTNodes1);
            List<ControlDependenceTreeNode> subTreeCDTNodes2 = getSubTreeCDTNodes(CDTNodes2, controlASTNodes2);

            pairInfo.setNumberOfCloneStatementsInFirstCodeFragment(ASTNodes1.size());
            pairInfo.setNumberOfCloneStatementsInSecondCodeFragment(ASTNodes2.size());
            pairInfo.setNumberOfStatementsToBeRefactored(Math.min(ASTNodes1.size(), ASTNodes2.size()));

            if (pairInfo.getNumberOfStatementsToBeRefactored() == 0) {
                NodePairComparisonCache.getInstance().clearCache();
                CompilationUnitCache.getInstance().releaseLock();
                pairInfo.setStatus(ClonePairInfo.AnalysisStatus.NOT_ANALYZED);
                return;
            }

            ICompilationUnit iCompilationUnit1 = (ICompilationUnit) JavaCore.create(classObject1.getIFile());
            ICompilationUnit iCompilationUnit2 = (ICompilationUnit) JavaCore.create(classObject2.getIFile());

            pairInfo.setNumberOfPDGNodesInFirstMethod(inputMethodsInfo.getFirstPDG().getTotalNumberOfStatements());
            pairInfo.setNumberOfPDGNodesInSecondMethod(inputMethodsInfo.getSecondPDG().getTotalNumberOfStatements());

            // Used for measuring time
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

            // If one of the clone fragments contain no control predicate nodes, create a dummy CDT
            if (subTreeCDTNodes1.size() == 0 || subTreeCDTNodes2.size() == 0) {

                // Get the control parent (or method) containing the clone fragments
                ASTNode parent1 = getControlParent(ASTNodes1);
                ASTNode parent2 = getControlParent(ASTNodes2);

                if (allStatementsAreInAnonymousClassDeclarationOrCatchClauseOrFinallyBlock(ASTNodes1) ||
                        allStatementsAreInAnonymousClassDeclarationOrCatchClauseOrFinallyBlock(ASTNodes2)) {
                    NodePairComparisonCache.getInstance().clearCache();
                    CompilationUnitCache.getInstance().releaseLock();
                    pairInfo.setStatus(ClonePairInfo.AnalysisStatus.NOT_ANALYZED);
                    log.info("All statements in at least one of the code fragments are inside anonymous class declaration, or catch clause, or finally block, skipping this clone pair");
                    return;
                }

                // Get the ControlDependenceTreeNode corresponding to the parent nodes.
                // If all the ASTNodes are nested under an "else", it returns the "else" ControlDependenceTreeNode instead of the "if"
                ControlDependenceTreeNode controlDependenceSubTreePDG1X = getSubTreeCDTNode(CDTNodes1, parent1, allNodesNestedUnderElse(ASTNodes1));
                ControlDependenceTreeNode controlDependenceSubTreePDG2X = getSubTreeCDTNode(CDTNodes2, parent2, allNodesNestedUnderElse(ASTNodes2));

                // Get all the control dependence tree nodes under the obtained ControlDependenceTreeNode
                List<ControlDependenceTreeNode> CDTNodesList1 = controlDependenceSubTreePDG1X.getNodesInBreadthFirstOrder();
                List<ControlDependenceTreeNode> CDTNodesList2 = controlDependenceSubTreePDG2X.getNodesInBreadthFirstOrder();

                // If the ControlDependenceTreeNodeX is a method entry node, then remove its children cdt nodes
                if (controlDependenceSubTreePDG1X.getNode() instanceof PDGMethodEntryNode) {
                    ListIterator<ControlDependenceTreeNode> iterator = CDTNodesList1.listIterator();
                    while (iterator.hasNext()) {
                        ControlDependenceTreeNode currentCDTNode = iterator.next();
                        PDGNode node = null;
                        if (currentCDTNode.isElseNode()) {
                            node = currentCDTNode.getIfParent().getNode();
                        } else {
                            node = currentCDTNode.getNode();
                        }
                        if (!node.equals(controlDependenceSubTreePDG1X.getNode())) {
                            iterator.remove();
                        }
                    }
                }

                if (controlDependenceSubTreePDG2X.getNode() instanceof PDGMethodEntryNode) {
                    ListIterator<ControlDependenceTreeNode> iterator = CDTNodesList2.listIterator();
                    while (iterator.hasNext()) {
                        ControlDependenceTreeNode currentCDTNode = iterator.next();
                        PDGNode node = null;
                        if (currentCDTNode.isElseNode()) {
                            node = currentCDTNode.getIfParent().getNode();
                        } else {
                            node = currentCDTNode.getNode();
                        }
                        if (!node.equals(controlDependenceSubTreePDG2X.getNode())) {
                            iterator.remove();
                        }
                    }
                }

                // Create CDT subtree with containing only the filtered CDTNodes
                ControlDependenceTreeNode controlDependenceSubTreePDG1 = generateControlDependenceSubTreeWithTheFirstNodeAsRoot(controlDependenceTreePDG1, CDTNodesList1);
                ControlDependenceTreeNode controlDependenceSubTreePDG2 = generateControlDependenceSubTreeWithTheFirstNodeAsRoot(controlDependenceTreePDG2, CDTNodesList2);

                // We didn't do any bottom-up matching, hence:
                pairInfo.setSubtreeMatchingTime(-1);

                // Try to map!
                long startTime = threadMXBean.getCurrentThreadCpuTime();
                PDGRegionSubTreeMapper mapper = new PDGRegionSubTreeMapper(inputMethodsInfo.getFirstPDG(), inputMethodsInfo.getSecondPDG(), iCompilationUnit1, iCompilationUnit2, controlDependenceSubTreePDG1, controlDependenceSubTreePDG2, ASTNodes1, ASTNodes2, true, null);
                long endTime = threadMXBean.getCurrentThreadCpuTime();

                if (mapper.hasMappedNodes()) {
                    // Create a new refactor information object (contains the real refactor + the time elapsed for mapping)
                    PDGSubTreeMapperInfo mapperInfo = new PDGSubTreeMapperInfo(mapper);
                    mapperInfo.setTimeElapsedForMapping(endTime - startTime);

                    // Add this (and the only) refactor informations (PDGMapper + time) to the pair information object
                    pairInfo.addMapperInfo(mapperInfo);
                }
                pairInfo.setStatus(ClonePairInfo.AnalysisStatus.NORMAL);

            } else { // If we have a control structure

                long startThreadime = threadMXBean.getCurrentThreadCpuTime();
                long startWallNanoTime = System.nanoTime();

                // Remove the CDT subtree nodes being part of an incomplete if-else-if chain
                List<ControlDependenceTreeNode> subTreeCDTNodes1Copy = new ArrayList<ControlDependenceTreeNode>(subTreeCDTNodes1);
                for (ControlDependenceTreeNode subTreeCDTNode1 : subTreeCDTNodes1Copy) {
                    if (subTreeCDTNode1.ifStatementInsideElseIfChain()) {
                        List<ControlDependenceTreeNode> ifParents = subTreeCDTNode1.getIfParents();
                        List<ControlDependenceTreeNode> elseIfChildren = subTreeCDTNode1.getElseIfChildren();
                        List<ControlDependenceTreeNode> treeChain = new ArrayList<ControlDependenceTreeNode>();
                        for (ControlDependenceTreeNode ifParent : ifParents) {
                            if (subTreeCDTNodes1Copy.contains(ifParent)) {
                                treeChain.add(ifParent);
                            }
                        }
                        for (ControlDependenceTreeNode elseIfChild : elseIfChildren) {
                            if (subTreeCDTNodes1Copy.contains(elseIfChild)) {
                                treeChain.add(elseIfChild);
                            }
                        }
                        if (!subTreeCDTNodes1Copy.containsAll(treeChain)) {
                            subTreeCDTNodes1.remove(subTreeCDTNode1);
                            subTreeCDTNodes1.removeAll(subTreeCDTNode1.getDescendants());
                        }
                    }
                }

                List<ControlDependenceTreeNode> subTreeCDTNodes2Copy = new ArrayList<ControlDependenceTreeNode>(subTreeCDTNodes2);
                for (ControlDependenceTreeNode subTreeCDTNode2 : subTreeCDTNodes2Copy) {
                    if (subTreeCDTNode2.ifStatementInsideElseIfChain()) {
                        List<ControlDependenceTreeNode> ifParents = subTreeCDTNode2.getIfParents();
                        List<ControlDependenceTreeNode> elseIfChildren = subTreeCDTNode2.getElseIfChildren();
                        List<ControlDependenceTreeNode> treeChain = new ArrayList<ControlDependenceTreeNode>();
                        for (ControlDependenceTreeNode ifParent : ifParents) {
                            if (subTreeCDTNodes1Copy.contains(ifParent)) {
                                treeChain.add(ifParent);
                            }
                        }
                        for (ControlDependenceTreeNode elseIfChild : elseIfChildren) {
                            if (subTreeCDTNodes1Copy.contains(elseIfChild)) {
                                treeChain.add(elseIfChild);
                            }
                        }
                        if (!subTreeCDTNodes2Copy.containsAll(treeChain)) {
                            subTreeCDTNodes2.remove(subTreeCDTNode2);
                            subTreeCDTNodes2.removeAll(subTreeCDTNode2.getDescendants());
                        }
                    }
                }

                if (subTreeCDTNodes1.size() > 0 && subTreeCDTNodes2.size() > 0) {
                    // Create CDT subtree with containing only the filtered CDTNodes
                    ControlDependenceTreeNode controlDependenceSubTreePDG1X = generateControlDependenceSubTree(controlDependenceTreePDG1, subTreeCDTNodes1);
                    ControlDependenceTreeNode controlDependenceSubTreePDG2X = generateControlDependenceSubTree(controlDependenceTreePDG2, subTreeCDTNodes2);

                    // Nodes of original CDTs in Breadth First order
                    List<ControlDependenceTreeNode> CDTNodesList1 = controlDependenceSubTreePDG1X.getNodesInBreadthFirstOrder();
                    List<ControlDependenceTreeNode> CDTNodesList2 = controlDependenceSubTreePDG2X.getNodesInBreadthFirstOrder();

                    // Do the bottom up mapping and get all the pairs of mapped CDT subtrees
                    BottomUpCDTMapper bottomUpCDTMapper = new BottomUpCDTMapper(iCompilationUnit1, iCompilationUnit2, controlDependenceSubTreePDG1X, controlDependenceSubTreePDG2X, false);

                    // Get the solutions
                    List<CompleteSubTreeMatch> bottomUpSubTreeMatches = bottomUpCDTMapper.getSolutions();

                    long endThreadTime = threadMXBean.getCurrentThreadCpuTime();
                    long endWallNanoTime = System.nanoTime();

                    pairInfo.setSubtreeMatchingTime(endThreadTime - startThreadime);
                    pairInfo.setSubtreeMatchingWallNanoTime(endWallNanoTime - startWallNanoTime);

                    if (bottomUpSubTreeMatches.size() == 0) {

                        pairInfo.setStatus(ClonePairInfo.AnalysisStatus.NO_COMMON_SUBTREE_FOUND);
                        if (ASTNodes1.size() == inputMethodsInfo.getFirstPDG().getTotalNumberOfStatements() && ASTNodes2.size() == inputMethodsInfo.getSecondPDG().getTotalNumberOfStatements()) {
                            //the entire method is cloned
                            startThreadime = threadMXBean.getCurrentThreadCpuTime();
                            startWallNanoTime = System.nanoTime();
                            log.info("Start mapping");
                            PDGRegionSubTreeMapper mapper = new PDGRegionSubTreeMapper(inputMethodsInfo.getFirstPDG(), inputMethodsInfo.getSecondPDG(), iCompilationUnit1, iCompilationUnit2,
                                    controlDependenceTreePDG1, controlDependenceTreePDG2, ASTNodes1, ASTNodes2, true, null);
                            log.info("End mapping");
                            endThreadTime = threadMXBean.getCurrentThreadCpuTime();
                            endWallNanoTime = System.nanoTime();
                            if (mapper.hasMappedNodes()) {
                                PDGSubTreeMapperInfo mapperInfo = new PDGSubTreeMapperInfo(mapper);
                                mapperInfo.setTimeElapsedForMapping(endThreadTime - startThreadime);
                                mapperInfo.setWallNanoTimeElapsedForMapping(endWallNanoTime - startWallNanoTime);
                                pairInfo.addMapperInfo(mapperInfo);
                            }
                            pairInfo.setStatus(ClonePairInfo.AnalysisStatus.NORMAL);
                        }

                    } else {

                        // For each solution in the bottom-up matching, do the PDG mapping
                        for (CompleteSubTreeMatch subTreeMatch : bottomUpSubTreeMatches) {

                            // Time for mapping PDGs in the CDT nodes starts here
                            startThreadime = threadMXBean.getCurrentThreadCpuTime();
                            startWallNanoTime = System.nanoTime();

                            TreeSet<ControlDependenceTreeNodeMatchPair> matchPairs = new TreeSet<ControlDependenceTreeNodeMatchPair>();
                            List<ControlDependenceTreeNode> subTreeMatchNodes1 = new ArrayList<ControlDependenceTreeNode>();
                            List<ControlDependenceTreeNode> subTreeMatchNodes2 = new ArrayList<ControlDependenceTreeNode>();

                            /*
                             * Filtering the nodes inside subTreeCDTNodes1 and subTreeCDTNodes2, keep only
                             * the nodes in the clone fragments
                             */
                            for (ControlDependenceTreeNodeMatchPair matchPair : subTreeMatch.getMatchPairs()) {
                                if (subTreeCDTNodes1.contains(matchPair.getNode1()) && subTreeCDTNodes2.contains(matchPair.getNode2())) {
                                    subTreeMatchNodes1.add(matchPair.getNode1());
                                    subTreeMatchNodes2.add(matchPair.getNode2());
                                    matchPairs.add(matchPair);
                                }
                            }

                            // If all the matched pairs are completely inside one of the code fragments
                            //if(matchPairs.size() == Math.min(subTreeCDTNodes1.size(), subTreeCDTNodes2.size())) {
                            boolean fullTreeMatch = (matchPairs.size() == Math.min(subTreeCDTNodes1.size(), subTreeCDTNodes2.size()));
                            // Get the nodes of the matched pairs in breadth first order
                            List<ControlDependenceTreeNode> orderedSubtreeMatchNodes1 = getCDTNodesInBreadthFirstOrder(CDTNodesList1, subTreeMatchNodes1);
                            List<ControlDependenceTreeNode> orderedSubtreeMatchNodes2 = getCDTNodesInBreadthFirstOrder(CDTNodesList2, subTreeMatchNodes2);

                            // Generate CDTs from the matched nodes
                            ControlDependenceTreeNode controlDependenceSubTreePDG1 = generateControlDependenceSubTree(controlDependenceTreePDG1, orderedSubtreeMatchNodes1);
                            // insert unmatched CDT nodes under matched ones
                            for (ControlDependenceTreeNode node : controlDependenceTreePDG1.getNodesInBreadthFirstOrder()) {
                                if (!orderedSubtreeMatchNodes1.contains(node)) {
                                    if (orderedSubtreeMatchNodes1.contains(node.getParent())) {
                                        insertCDTNodeInTree(node, controlDependenceSubTreePDG1);
                                        addNodeInOrder(orderedSubtreeMatchNodes1, node);
                                    } else if (orderedSubtreeMatchNodes1.contains(node.getPreviousSibling()) && node.getId() < orderedSubtreeMatchNodes1.get(orderedSubtreeMatchNodes1.size() - 1).getId()) {
                                        insertCDTNodeInTreeAfterSibling(node, node.getPreviousSibling(), controlDependenceSubTreePDG1);
                                        addNodeInOrder(orderedSubtreeMatchNodes1, node);
                                    }
                                }
                            }
                            ControlDependenceTreeNode controlDependenceSubTreePDG2 = generateControlDependenceSubTree(controlDependenceTreePDG2, orderedSubtreeMatchNodes2);
                            // insert unmatched CDT nodes under matched ones
                            for (ControlDependenceTreeNode node : controlDependenceTreePDG2.getNodesInBreadthFirstOrder()) {
                                if (!orderedSubtreeMatchNodes2.contains(node)) {
                                    if (orderedSubtreeMatchNodes2.contains(node.getParent())) {
                                        insertCDTNodeInTree(node, controlDependenceSubTreePDG2);
                                        addNodeInOrder(orderedSubtreeMatchNodes2, node);
                                    } else if (orderedSubtreeMatchNodes2.contains(node.getPreviousSibling()) && node.getId() < orderedSubtreeMatchNodes2.get(orderedSubtreeMatchNodes2.size() - 1).getId()) {
                                        insertCDTNodeInTreeAfterSibling(node, node.getPreviousSibling(), controlDependenceSubTreePDG2);
                                        addNodeInOrder(orderedSubtreeMatchNodes2, node);
                                    }
                                }
                            }
                            log.info("Start mapping");
                            PDGRegionSubTreeMapper mapper = new PDGRegionSubTreeMapper(inputMethodsInfo.getFirstPDG(), inputMethodsInfo.getSecondPDG(), iCompilationUnit1, iCompilationUnit2,
                                    controlDependenceSubTreePDG1, controlDependenceSubTreePDG2, ASTNodes1, ASTNodes2, fullTreeMatch, null);
                            log.info("End mapping");
                            endThreadTime = threadMXBean.getCurrentThreadCpuTime();
                            endWallNanoTime = System.nanoTime();
                            if (mapper.hasMappedNodes()) {
                                PDGSubTreeMapperInfo mapperInfo = new PDGSubTreeMapperInfo(mapper);
                                mapperInfo.setTimeElapsedForMapping(endThreadTime - startThreadime);
                                mapperInfo.setWallNanoTimeElapsedForMapping(endWallNanoTime - startWallNanoTime);

                                pairInfo.addMapperInfo(mapperInfo);
                            }
                            pairInfo.setStatus(ClonePairInfo.AnalysisStatus.NORMAL);

                            //						} else { // not matchPairs.size() == Math.min(subTreeCDTNodes1.size(), subTreeCDTNodes2.size())
                            //							pairInfo.setStatus(AnalysisStatus.NO_COMMON_SUBTREE_FOUND);
                            //						}
                        }

                    }
                } else {
                    pairInfo.setStatus(ClonePairInfo.AnalysisStatus.NO_COMMON_SUBTREE_FOUND);
                }
            }
            pairInfo.setNumberOfNodeComparisons(NodePairComparisonCache.getInstance().getMapSize());
            NodePairComparisonCache.getInstance().clearCache();

        } else { // not methodObject1 != null && methodObject2 != null && methodObject1.getMethodBody() != null && methodObject2.getMethodBody() != null
            pairInfo.setStatus(ClonePairInfo.AnalysisStatus.NOT_ANALYZED);
        }
    }

    private ASTNode getControlParent(List<ASTNode> ASTNodes) {
        Map<ASTNode, List<ASTNode>> parentMap = new LinkedHashMap<ASTNode, List<ASTNode>>();
        for (ASTNode astNode : ASTNodes) {
            ASTNode astParent = getParent(astNode);
            if (parentMap.containsKey(astParent)) {
                parentMap.get(astParent).add(astNode);
            } else {
                List<ASTNode> childNodes = new ArrayList<ASTNode>();
                childNodes.add(astNode);
                parentMap.put(astParent, childNodes);
            }
        }
        List<ASTNode> parentList = new ArrayList<ASTNode>(parentMap.keySet());
        if (parentMap.keySet().size() == 1) {
            return parentList.get(0);
        } else if (parentMap.keySet().size() == 2) {
            //check if the second parent key is the only child of the first parent key
            ASTNode secondParent = parentList.get(1);
            List<ASTNode> firstParentChildren = parentMap.get(parentList.get(0));
            if (firstParentChildren.size() == 1 && firstParentChildren.contains(secondParent)) {
                return secondParent;
            }
        }
        return getParent(ASTNodes.get(0));
    }

    private ASTNode getParent(ASTNode controlNode) {
        if (!(controlNode.getParent() instanceof Block))
            return controlNode.getParent();
        while (controlNode.getParent() instanceof Block) {
            controlNode = controlNode.getParent();
        }
        if (controlNode.getParent() instanceof CatchClause) {
            CatchClause catchClause = (CatchClause) controlNode.getParent();
            return catchClause.getParent();
        }
        return controlNode.getParent();
    }

    private boolean allStatementsAreInAnonymousClassDeclarationOrCatchClauseOrFinallyBlock(List<ASTNode> ASTNodes) {
        for (ASTNode astNode : ASTNodes) {
            if (!isNestedUnderAnonymousClassDeclarationOrCatchClauseOrFinallyBlock(astNode))
                return false;
        }
        return true;
    }

    private boolean allNodesNestedUnderElse(List<ASTNode> astNodes) {
        for (ASTNode astNode : astNodes) {
            if (!isNestedUnderElse(astNode))
                return false;
        }
        return true;
    }

    private boolean isNestedUnderElse(ASTNode astNode) {
        if (astNode.getParent() instanceof IfStatement) {
            IfStatement ifParent = (IfStatement) astNode.getParent();
            if (ifParent.getElseStatement() != null && ifParent.getElseStatement().equals(astNode))
                return true;
        }
        if (astNode.getParent() instanceof Block) {
            Block blockParent = (Block) astNode.getParent();
            if (blockParent.getParent() instanceof IfStatement) {
                IfStatement ifGrandParent = (IfStatement) blockParent.getParent();
                if (ifGrandParent.getElseStatement() != null && ifGrandParent.getElseStatement().equals(blockParent))
                    return true;
            }
        }
        return false;
    }

    private void addNodeInOrder(List<ControlDependenceTreeNode> matchedControlDependenceTreeNodes, ControlDependenceTreeNode node) {
        int indexToBeAdded = 0;
        for (ControlDependenceTreeNode matchedNode : matchedControlDependenceTreeNodes) {
            if (matchedNode.getId() < node.getId()) {
                indexToBeAdded++;
            }
        }
        matchedControlDependenceTreeNodes.add(indexToBeAdded, node);
    }

    private void insertCDTNodeInTree(ControlDependenceTreeNode cdtNode, ControlDependenceTreeNode root) {
        ControlDependenceTreeNode parent;
        if (cdtNode.getParent().isElseNode()) {
            parent = root.getElseNode(cdtNode.getParent().getIfParent().getNode());
        } else {
            parent = root.getNode(cdtNode.getParent().getNode());
        }
        ControlDependenceTreeNode newNode = new ControlDependenceTreeNode(parent, cdtNode.getNode());
        if (cdtNode.isElseNode()) {
            newNode.setElseNode(true);
            ControlDependenceTreeNode newIfParent = root.getNode(cdtNode.getIfParent().getNode());
            if (newIfParent != null) {
                newIfParent.setElseIfChild(newNode);
                newNode.setIfParent(newIfParent);
            }
        } else if (cdtNode.getIfParent() != null) {
            ControlDependenceTreeNode newIfParent = root.getNode(cdtNode.getIfParent().getNode());
            if (newIfParent != null) {
                newNode.setIfParentAndElseIfChild(newIfParent);
            }
        }
    }

    private void insertCDTNodeInTreeAfterSibling(ControlDependenceTreeNode cdtNode, ControlDependenceTreeNode previousSibling, ControlDependenceTreeNode root) {
        ControlDependenceTreeNode parent;
        if (cdtNode.getParent().isElseNode()) {
            parent = root.getElseNode(cdtNode.getParent().getIfParent().getNode());
        } else {
            parent = root.getNode(cdtNode.getParent().getNode());
        }
        ControlDependenceTreeNode newNode = new ControlDependenceTreeNode(parent, previousSibling, cdtNode.getNode());
        if (cdtNode.isElseNode()) {
            newNode.setElseNode(true);
            ControlDependenceTreeNode newIfParent = root.getNode(cdtNode.getIfParent().getNode());
            if (newIfParent != null) {
                newIfParent.setElseIfChild(newNode);
                newNode.setIfParent(newIfParent);
            }
        } else if (cdtNode.getIfParent() != null) {
            ControlDependenceTreeNode newIfParent = root.getNode(cdtNode.getIfParent().getNode());
            if (newIfParent != null) {
                newNode.setIfParentAndElseIfChild(newIfParent);
            }
        }
    }

    private boolean isNestedUnderAnonymousClassDeclarationOrCatchClauseOrFinallyBlock(ASTNode node) {
        ASTNode parent = node.getParent();
        while (parent != null) {
            if (parent instanceof AnonymousClassDeclaration || parent instanceof CatchClause ||
                    isFinallyBlockOfTryStatement(parent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private boolean isFinallyBlockOfTryStatement(ASTNode node) {
        ASTNode parent = node.getParent();
        if (parent != null && parent instanceof TryStatement) {
            TryStatement tryStatement = (TryStatement) parent;
            Block finallyBlock = tryStatement.getFinally();
            if (node instanceof Block && finallyBlock != null) {
                return finallyBlock.equals((Block) node);
            }
        }
        return false;
    }

    /**
     * This method returns the nodes of the second parameter in breadth first order according to the order of the first parameter
     *
     * @param OrderedCDTNodesList
     * @param UnorderedCDTNodesList
     * @return
     */
    private List<ControlDependenceTreeNode> getCDTNodesInBreadthFirstOrder(List<ControlDependenceTreeNode> OrderedCDTNodesList, List<ControlDependenceTreeNode> UnorderedCDTNodesList) {
        List<ControlDependenceTreeNode> newCDTNodesList = new ArrayList<ControlDependenceTreeNode>();
        for (ControlDependenceTreeNode CDTNode : OrderedCDTNodesList) {
            if (UnorderedCDTNodesList.contains(CDTNode)) {
                newCDTNodesList.add(CDTNode);
            }
        }
        return newCDTNodesList;
    }

    private ControlDependenceTreeNode getSubTreeCDTNode(
            List<ControlDependenceTreeNode> CDTNodes,
            ASTNode ASTNode, boolean allNodesUnderElse) {
        ControlDependenceTreeNode subTreeCDTNode = null;
        boolean found = false;
        for (ControlDependenceTreeNode CDTNode : CDTNodes) {
            if (CDTNode.getNode() instanceof PDGMethodEntryNode)
                continue;
            if (CDTNode.getNode() != null &&
                    CDTNode.getNode().getASTStatement().equals(ASTNode)) {
                subTreeCDTNode = CDTNode;
                if (allNodesUnderElse)
                    subTreeCDTNode = subTreeCDTNode.getElseIfChild();
				/*				if(CDTNode.getNode().getASTStatement() instanceof IfStatement) {
					if(CDTNode.getElseChild() != null)
						subTreeCDTNodes.add(CDTNode.getElseChild());
				}*/
                found = true;
                break;
            }
        }
        if (!found) {
            for (ControlDependenceTreeNode CDTNode : CDTNodes) {
                if (CDTNode.getNode() instanceof PDGMethodEntryNode)
                    continue;
                if (CDTNode.getNode() != null && CDTNode.getNode().getASTStatement().subtreeMatch(new ASTMatcher(), ASTNode)) {
                    subTreeCDTNode = CDTNode;
                    if (allNodesUnderElse)
                        subTreeCDTNode = subTreeCDTNode.getElseIfChild();
					/*					if(CDTNode.getNode().getASTStatement() instanceof IfStatement) {
						if(CDTNode.getElseChild() != null)
							subTreeCDTNodes.add(CDTNode.getElseChild());
					}*/
                    break;
                }
            }
        }
        if (subTreeCDTNode == null)
            subTreeCDTNode = CDTNodes.get(0);
        return subTreeCDTNode;
    }

    private List<ControlDependenceTreeNode> getSubTreeCDTNodes(
            List<ControlDependenceTreeNode> CDTNodes,
            List<ASTNode> controlASTNodes) {
        List<ControlDependenceTreeNode> subTreeCDTNodes = new ArrayList<ControlDependenceTreeNode>();
        for (ASTNode ASTNode : controlASTNodes) {
            boolean found = false;
            for (ControlDependenceTreeNode CDTNode : CDTNodes) {
                if (CDTNode.getNode() instanceof PDGMethodEntryNode)
                    continue;
                if (CDTNode.getNode() != null && CDTNode.getNode().getASTStatement().equals(ASTNode)) {
                    subTreeCDTNodes.add(CDTNode);
                    if (CDTNode.getNode().getASTStatement() instanceof IfStatement) {
                        if (CDTNode.getElseChild() != null)
                            subTreeCDTNodes.add(CDTNode.getElseChild());
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (ControlDependenceTreeNode CDTNode : CDTNodes) {
                    if (CDTNode.getNode() instanceof PDGMethodEntryNode)
                        continue;
                    if (CDTNode.getNode() != null && CDTNode.getNode().getASTStatement().subtreeMatch(new ASTMatcher(), ASTNode)) {
                        subTreeCDTNodes.add(CDTNode);
                        if (CDTNode.getNode().getASTStatement() instanceof IfStatement) {
                            if (CDTNode.getElseChild() != null)
                                subTreeCDTNodes.add(CDTNode.getElseChild());
                        }
                        break;
                    }
                }
            }
        }
        return subTreeCDTNodes;
    }

    private boolean hasOnlyKeyWord(String filtered) {
        String[] keyWords = new String[]{"return", "break", "continue"};
        for (String keyWord : keyWords)
            if (keyWord.equals(filtered))
                return true;
        return false;
    }

    private boolean isInside(ASTNode astNode, int startOffset, int endOffset, ICompilationUnit iCompilationUnit) {

        int astNodeStartPosition = astNode.getStartPosition();
        int astNodeLength = astNode.getLength();

        // If the node is completely inside
        if (astNodeStartPosition >= startOffset && astNodeStartPosition + astNodeLength <= endOffset)
            return true;

        if (astNodeStartPosition >= startOffset && astNodeStartPosition <= endOffset) {
            IDocument iDocument = getIDocument(iCompilationUnit);
            try {
                String realSourceCode = iDocument.get(astNodeStartPosition, endOffset - astNodeStartPosition);
                String astNodeSourceCode = iDocument.get(astNodeStartPosition, astNodeLength);

                TextDiff td = new TextDiff();
                LinkedList<TextDiff.Diff> diffs = td.diff_main(realSourceCode, astNodeSourceCode, false);
                td.diff_cleanupSemantic(diffs);

                String commentRegularExpression = "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)";

                boolean realSourceCodeFound = false;
                for (TextDiff.Diff diff : diffs) {
                    switch (diff.operation) {
                        case EQUAL:
                            if (diff.text.equals(realSourceCode))
                                realSourceCodeFound = true;
                            break;
                        case DELETE:
                            return false;
                        case INSERT:
                            String filtered = diff.text.replaceAll(commentRegularExpression, "").replaceAll("\\s", "").replaceAll("\\}", "").replaceAll("\\)", "").replaceAll(";", "");
                            if (realSourceCodeFound && (filtered.isEmpty() || hasOnlyKeyWord(filtered)))
                                return true;
                            else
                                return false;
                    }
                }
                return realSourceCodeFound;
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        return false;
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

    private IMethod recursiveGetIMethod(IType type, IJavaProject jProject, String methodName, String methodSignature) throws JavaModelException {
        IMethod innerMethod = null;
        for (IType innerType : type.getCompilationUnit().getAllTypes()) {
            if (!methodSignature.equals("")) {
                innerMethod = getIMethodWithSignature(jProject, innerType, methodName, methodSignature);
                if (innerMethod != null)
                    return innerMethod;
            }
        }
        return null;
    }

    private IMethod getIMethod(IJavaProject jProject, String typeName, String methodName, String methodSignature)
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
            iMethod = getIMethodWithSignature(jProject, type, methodName, methodSignature);
        }

        if (iMethod == null) {
            iMethod = recursiveGetIMethod(type, jProject, methodName, methodSignature);
        }
        return iMethod;
    }

    private IMethod getIMethodWithSignature(IJavaProject jProject, IType type, String methodName, String methodSignature)
            throws JavaModelException {

        //SystemObject systemObject = ASTReader.getSystemObject();
        List<IMethod> methods = new ArrayList<>();
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

    private ControlDependenceTreeNode generateControlDependenceSubTree(ControlDependenceTreeNode completeTreeRoot, List<ControlDependenceTreeNode> subTreeNodes) {
        ControlDependenceTreeNode oldCDTNode = subTreeNodes.get(0);
        ControlDependenceTreeNode root = new ControlDependenceTreeNode(null, oldCDTNode.getParent().getNode());

        if (oldCDTNode.getParent().isElseNode()) {
            root.setElseNode(true);
            root.setIfParent(oldCDTNode.getParent().getIfParent());
        }
        for (ControlDependenceTreeNode cdtNode : subTreeNodes) {
            insertCDTNodeInTree(cdtNode, root);
        }
        return root;
    }

    private ControlDependenceTreeNode generateControlDependenceSubTreeWithTheFirstNodeAsRoot(ControlDependenceTreeNode completeTreeRoot, List<ControlDependenceTreeNode> subTreeNodes) {
        ControlDependenceTreeNode oldCDTNode = subTreeNodes.get(0);
        ControlDependenceTreeNode root = new ControlDependenceTreeNode(null, oldCDTNode.getNode());

        if (oldCDTNode.isElseNode()) {
            root.setElseNode(true);
            root.setIfParent(oldCDTNode.getIfParent());
        }

        for (int i = 1; i < subTreeNodes.size(); i++) {
            ControlDependenceTreeNode cdtNode = subTreeNodes.get(i);
            insertCDTNodeInTree(cdtNode, root);
        }
        return root;
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

    public static IDocument getIDocument(IJavaElement iJavaElement) {
        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        IPath path = iJavaElement.getPath();
        IDocument iDocument = null;
        try {
            bufferManager.connect(path, LocationKind.IFILE, null);
            ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path, LocationKind.IFILE);
            iDocument = textFileBuffer.getDocument();
        } catch (CoreException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferManager.disconnect(path, LocationKind.IFILE, null);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        return iDocument;
    }

}
