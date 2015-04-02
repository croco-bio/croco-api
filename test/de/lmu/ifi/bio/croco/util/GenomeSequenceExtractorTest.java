package de.lmu.ifi.bio.croco.util;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.croco.test.IntegrationTest;

@Category(IntegrationTest.class)
public class GenomeSequenceExtractorTest {

	@Test
	public void testExtraction()  throws Exception{
		GenomeSequenceExtractor extractor = new GenomeSequenceExtractor(new File("/home/proj/biosoft/GENOMIC/HUMAN/HUMAN_GENOME_FASTA"));
		
		//MA0002.2	0	5246	5256	+	12.0217	4.25768e-05	GGCTGTGGCTG
		String dna = extractor.getDNASequence("19", 37152725+5246-1, 37152725+5256);
		assertEquals("GGCTGTGGCTG",dna);
		
		//MA0002.2	0	7469	7479	+	12.0217	4.25768e-05	TGTTGTGGCTT	
		dna = extractor.getDNASequence("19", 37152725+7469-1, 37152725+7479);
		assertEquals("TGTTGTGGCTT",dna);
		
		//MA0002.2	0	4922	4932	-	10.8702	9.6078e-05	GCGTGTGGCT
		dna = extractor.getDNASequence("19", 37152725+4922-1, 37152725+4932);
		assertEquals("GCGTGTGGCTT",GenomeUtil.reverse(dna));
	}

}
