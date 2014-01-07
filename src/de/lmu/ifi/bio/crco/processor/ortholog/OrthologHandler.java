package de.lmu.ifi.bio.crco.processor.ortholog;

import java.io.File;
import java.util.Set;

public interface OrthologHandler {
	public void importRelations(File  inparanoidBaseDir, Set<Integer> taxIdsOfInterest, File tmpFile ) throws Exception;
	public void importGenes(File  inparanoidBaseDir, Set<Integer> taxIdsOfInterest, File tmpFile ) throws Exception;
	
	public void importSourceIdentifierInformation(File  baseDir, Set<Integer> taxIdsOfInterest) throws Exception;
	
}
