package de.lmu.ifi.bio.croco.util;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.croco.category.UnitTest;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.data.genome.Transcript;
import de.lmu.ifi.bio.croco.intervaltree.Interval;
import de.lmu.ifi.bio.croco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Promoter;

@Category(UnitTest.class)
public class GenomeUtilTest {
    File testGTF = new File("data/test/STAT1_RAB1A_Homo_sapiens.GRCh37.73.gtf");
    
    /*
	@Test
	public void testGTFNoneOverlap() throws Exception{
		
		List<Gene> genes = FileUtil.getGenes(testGTF, "protein_coding", null);
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
    */

	@Test
	public void testRead() throws Exception
	{
	    List<Gene> genes = FileUtil.getGenes(testGTF, null, null);
        assertEquals(2,genes.size());
        
        for(Gene gene : genes)
        {
            if ( gene.getName().equals("STAT1"))
            {
                assertEquals(gene.getTranscripts().size(),12);
                
                for(Transcript t : gene.getTranscripts())
                {
                    if ( t.getIdentifier().equals("ENST00000415035"))
                    {
                        CroCoLogger.getLogger().debug("Check transcript");
                        assertEquals(191839556,t.getExons().get(0).getStart());
                        assertEquals(191839661,t.getExons().get(0).getEnd());
                    }
                }
            }
        }
        
        genes = FileUtil.getGenes(testGTF, "protein_coding", null);
        for(Gene gene : genes)
        {
            if ( gene.getName().equals("STAT1"))
            {
                assertEquals(gene.getTranscripts().size(),8);
            }
        }
	}
	@Test
	public void testPromoter() throws Exception
	{
	    List<Gene> genes = FileUtil.getGenes(testGTF,null, null);
        
	    HashMap<String, IntervalTree<Promoter>> promoterTree = GenomeUtil.createPromoterIntervalTree(genes,1,1,false);
        
	    List<Promoter> sa = promoterTree.get("2").searchAll(new Interval(191839600,191839700));
	    assertEquals(1,sa.size());
	    assertEquals(1,sa.get(0).getTranscripts().size());
	    assertEquals("ENST00000415035",sa.get(0).getTranscripts().iterator().next().getName());
	}
	
	@Test
	public void testOverlap() throws Exception{
		
		List<Gene> genes = FileUtil.getGenes(testGTF, "protein_coding", null);
		for(Gene gene : genes)
		{
		    for(Transcript t :gene.getTranscripts())
		    {
		        System.out.println(t.getIdentifier() + "\t"  + t.getExons());
		    }
		}
		
		assertEquals(2,genes.size());
		HashMap<String, IntervalTree<Promoter>> promoterTree = GenomeUtil.createPromoterIntervalTree(genes,5000,5000,false);
		
		Interval peak = new Interval(65316073, 65316204);
		int position = (int) ((peak.getHigh() + peak.getLow())/2);
		List<Promoter> res = promoterTree.get("2").searchAll(peak);
		
		for(Promoter promoter : res){
			for(Transcript transcript : promoter.getTranscripts()){
				System.out.println(transcript.getParentGene());
				System.out.println(Transcript.getDistanceToTssStart(transcript, position));
			}
		}
	}
}
