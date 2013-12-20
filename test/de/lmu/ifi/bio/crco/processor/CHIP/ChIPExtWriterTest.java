package de.lmu.ifi.bio.crco.processor.CHIP;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.intervaltree.Interval;
import de.lmu.ifi.bio.crco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Promoter;
import de.lmu.ifi.bio.crco.test.IntegrationTest;
import de.lmu.ifi.bio.crco.util.FileUtil;
import de.lmu.ifi.bio.crco.util.GenomeUtil;

@Category(IntegrationTest.class)
public class ChIPExtWriterTest {

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
