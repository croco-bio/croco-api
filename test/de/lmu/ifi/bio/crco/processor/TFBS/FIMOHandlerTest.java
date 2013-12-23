package de.lmu.ifi.bio.crco.processor.TFBS;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.util.FileUtil;

public class FIMOHandlerTest {

	@Test
	public void testMatch() throws Exception {
		Float pValueThreshold = 0.00001f;
		File regionFile = new File("/home/proj/pesch/TFBS/Human/promoter/Homo_sapiens.GRCh37.73.regions");
		List<Gene> genes = FileUtil.getGenes(new File("/home/proj/biosoft/GENOMIC/HUMAN/Homo_sapiens.GRCh37.73.gtf"), "protein_coding", null);
		HashMap<String, Set<String>> mapping = new HashMap<String, Set<String>>();
		
		//	public FIMOHandler(File regionFile, Float pValueThreshold, List<Gene> genes, HashMap<String, Set<String>> motifIdMapping,Integer upstream,Integer downstream){

		FIMOHandler handler = new FIMOHandler(regionFile, pValueThreshold, genes, mapping,5000,5000);
		
	}

}
