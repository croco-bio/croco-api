package de.lmu.ifi.bio.crco.util;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.data.genome.Transcript;
import de.lmu.ifi.bio.crco.intervaltree.Interval;
import de.lmu.ifi.bio.crco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Promoter;
import de.lmu.ifi.bio.crco.test.UnitTest;

@Category(UnitTest.class)
public class GenomeUtilTest {

	@Test
	public void testGTFNoneOverlap() throws Exception{
		File gtf = new File("data/GeneAnnotation/Homo_sapiens.GRCh37.73.gtf");
		
		List<Gene> genes = FileUtil.getGenes(gtf, "protein_coding", null);
		HashMap<String, IntervalTree<Promoter>> promoterTree = GenomeUtil.createPromoterIntervalTree(genes,5000,5000,false);
		List<Promoter> res = promoterTree.get("7").searchAll(new Interval(30890444,	30890453));
		assertEquals(res.size(),2);
		assertEquals(res.get(0).getTranscripts().size(),1);
		assertEquals(res.get(1).getTranscripts().size(),1);
		
		promoterTree = GenomeUtil.createPromoterIntervalTree(genes,5000,5000,true);
		res = promoterTree.get("7").searchAll(new Interval(30890444,	30890453));
		assertEquals(res.size(),1);
		assertEquals(res.get(0).getTranscripts().size(),2);
	}

	@Test
	public void testOverlap() throws Exception{
		File gtf = new File("data/GeneAnnotation/Homo_sapiens.GRCh37.73.gtf");
		
		List<Gene> genes = FileUtil.getGenes(gtf, "protein_coding", null);
		HashMap<String, IntervalTree<Promoter>> promoterTree = GenomeUtil.createPromoterIntervalTree(genes,5000,5000,false);
		Interval peak = new Interval(58911156,	58911166);
		int position = (int) ((peak.getHigh() + peak.getLow())/2);
		List<Promoter> res = promoterTree.get("19").searchAll(peak);
		System.out.println(res);
		for(Promoter promoter : res){
			for(Transcript transcript : promoter.getTranscripts()){
				System.out.println(transcript.getParentGene());
				System.out.println(Transcript.getDistanceToTssStart(transcript, position));
			}
		}
	}
}
