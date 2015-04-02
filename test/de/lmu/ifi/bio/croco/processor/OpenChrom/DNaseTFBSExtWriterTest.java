package de.lmu.ifi.bio.croco.processor.OpenChrom;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.croco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.croco.processor.TFBS.FIMOHandler;
import de.lmu.ifi.bio.croco.test.IntegrationTest;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.FileUtil;
import de.lmu.ifi.bio.croco.util.GenomeUtil;

@IntegrationTest
public class DNaseTFBSExtWriterTest {

	@Test
	public void testCreateNetwork() throws IOException {
		File file = new File("/home/users/pesch/Databases/ENCODE/DNASE-PEAKS/Human/wgEncodeUwDgf/wgEncodeUwDgfNhlfPk.narrowPeak.gz");
		HashMap<String, IntervalTree<Peak>> peaks = GenomeUtil.createPeakIntervalTree(file,0,1,2,null,null); 
		
		File gtfFile = new File("data/GeneAnnotation/ENSG00000125970.gtf");
		List<Gene> genes = FileUtil.getGenes(gtfFile, null, null);
		Float pValueCutOf = 0.00001f;
		Integer upstream = 5000;
		Integer downstream = 5000;
		File tfbsRegion = new File("/home/proj/pesch/TFBS/Human/promoter/Homo_sapiens.GRCh37.73.regions");
		File motifMappingFile = new File("/home/proj/pesch/TFBS/data/Motif/mapping/wei2010_human_mws.mapping");
		File tfbsFile = new File("/home/proj/pesch/TFBS/Human/scan/human_10K_promoter_wei2010");
		String chromosomNamePrefix = "chr";
		
		HashMap<String, Set<String>> motifIdMapping = new FileUtil.MappingFileReader(0,2,motifMappingFile).setColumnSeperator("\\s+").includeAllColumnsAfterToIndex(true).readNNMappingFile();
		HashMap<String, IntervalTree<TFBSPeak>> matchTree = new FIMOHandler(tfbsRegion,pValueCutOf,upstream,downstream).readHits(tfbsFile);
	
		for(String chrom : peaks.keySet()){
			IntervalTree<Peak> openChromPeaks = peaks.get(chrom);
			if (!matchTree.containsKey(chrom) ){
				chrom =chrom.replace( chromosomNamePrefix , "");
				if (!matchTree.containsKey(chrom) ){
					CroCoLogger.getLogger().warn(String.format("No TFBS predictions for chrom: %s",chrom));
					continue;
				}
			}
			
			IntervalTree<TFBSPeak> tfbsPeaks = matchTree.get(chrom);
			for(Peak openChromPeak :openChromPeaks.getObjects() ){
				if ( openChromPeak == null) continue;
				List<TFBSPeak> openChromChipEnriched = tfbsPeaks.searchAll(openChromPeak);
				
				
				
			}

		
		}
	}

}
