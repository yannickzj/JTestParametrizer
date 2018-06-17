package ca.uwaterloo.jrefactoring.mapping;

import gr.uom.java.ast.decomposition.cfg.mapping.DivideAndConquerMatcher;

import java.util.HashSet;
import java.util.Set;

public class PDGSubTreeMapperInfo {
	private final DivideAndConquerMatcher mapper;
	private long timeElapsedToCalculate;
	
	private long wallNanoTimeElapsedForMapping;
	private Set<String> filesHavingCompileErrors = new HashSet<>();
	private boolean refactoringWasOk;
	
	public long getWallNanoTimeElapsedForMapping() {
		return wallNanoTimeElapsedForMapping;
	}

	public void setWallNanoTimeElapsedForMapping(long wallNanoTime) {
		this.wallNanoTimeElapsedForMapping = wallNanoTime;
	}

	public PDGSubTreeMapperInfo(DivideAndConquerMatcher mapper) {
		this.mapper = mapper;
	}
	
	public DivideAndConquerMatcher getMapper() {
		return this.mapper;
	}

	/**
	 * Get time elapsed for only mapping phase (excluding the bottom-up subtree matching) 
	 * @return
	 */
	public long getTimeElapsedToMap() {
		return timeElapsedToCalculate;
	}

	/**
	 * Set time elapsed for only mapping phase (excluding the bottom-up subtree matching) 
	 * @return
	 */
	public void setTimeElapsedForMapping(long timeElapsedToCalculate) {
		this.timeElapsedToCalculate = timeElapsedToCalculate;
	}
	
	/**
	 * Is this mapper refactorable?
	 * @deprecated
	 */
	public boolean isRefactorable() {
		return this.mapper != null && this.mapper.getPreconditionViolations().size() == 0 &&
				this.mapper.getRemovableNodesG1().size() > 0 && this.mapper.getRemovableNodesG2().size() > 0;
	}
	

	public void addFileHavingCompileError(String path) {
		this.filesHavingCompileErrors.add(path);
	}
	
	public Iterable<String> getFilesHavingCompileError() {
		return this.filesHavingCompileErrors ;		
	}

	public void setRefactoringWasOK(boolean refactoringWasOk) {
		this.refactoringWasOk = refactoringWasOk;
	}
	
	public boolean getRefactoringWasOK() {
		return refactoringWasOk;
	}
	
	public boolean getHasCompileErrorsAfterRefactoring() {
		return filesHavingCompileErrors.size() > 0;
	}

}
