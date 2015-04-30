package de.lmu.ifi.bio.croco.processor.CHIP;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.croco.category.IntegrationTest;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.intervaltree.Interval;
import de.lmu.ifi.bio.croco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Promoter;
import de.lmu.ifi.bio.croco.util.FileUtil;
import de.lmu.ifi.bio.croco.util.GenomeUtil;

@Category(IntegrationTest.class)
public class ChIPExtWriterTest {

	@Test
	public void testMouse() throws Exception{
		List<Gene> genes = FileUtil.getGenes(new File("data/GeneAnnotation/Mus_musculus.NCBIM37.67.gtf"), null, null);
		HashMap<String,IntervalTree<Promoter>> promoterTrees = GenomeUtil.createPromoterIntervalTree(genes,5000,5000,false);
		File exp = new File("/home/users/pesch/Databases/ENCODE/CHIPSEQ-PEAKS/Mouse/wgEncodeSydhTfbs/wgEncodeSydhTfbsCh12Znfmizdcp1ab65767IggrabPk.narrowPeak.gz");
		
		HashMap<String, IntervalTree<Peak>> peaks = GenomeUtil.createPeakIntervalTree(exp,0,1,2,-1,null); //max size == we want to ignore very long peaks
	//	List<TFBSPeak> targets = ChIPExtWriter.getTFBSPeaks(new Entity("test"),"test",peaks,promoterTrees, "chr",new HashMap<String,String>());
		//for(TFBSPeak tfbsPeak  : targets){
			//Entity factor = tfbsPeak.getFactors().get(0); //can be only 1
			//Entity target = new Entity( tfbsPeak.getTarget().getParentGene().getIdentifier());
			
		//	System.out.println(target);
		//}
	}
	
	@Test
	public void testWorm() throws Exception {

		List<Gene> genes = FileUtil.getGenes(new File("/home/users/pesch/git/croco-api/data/GeneAnnotation/Caenorhabditis_elegans.WS220.66.gtf"), null, null);
		HashMap<String,IntervalTree<Promoter>> promoterTrees = GenomeUtil.createPromoterIntervalTree(genes,500,500,false);
	
		List<Promoter> promoters = promoterTrees.get("IV").searchAll(new Interval(13889146, 	13894776 ));
		for(Promoter promoter : promoters){
			System.out.println(promoter + "\t" + promoter.getTranscripts());
		}
	}

}
