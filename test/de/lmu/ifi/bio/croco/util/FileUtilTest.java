package de.lmu.ifi.bio.croco.util;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.data.genome.Strand;
import de.lmu.ifi.bio.croco.data.genome.Transcript;
import de.lmu.ifi.bio.croco.intervaltree.Interval;
import de.lmu.ifi.bio.croco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Promoter;

public class FileUtilTest {

	
	@Test
	public void testGTFDistance5() throws Exception{
		
		File gtf = new File("data/GeneAnnotation/ENSG00000240583.gtf");
		
		List<Gene> genes = FileUtil.getGenes(gtf, "protein_coding", null);
		assertEquals(1,genes.size());
		
		Integer absolutMiddle = (30890444+	30890453)/2;
		Gene relevantGene = genes.get(0);
		for(Transcript transcript :relevantGene.getTranscripts()){
			
			/*Integer distanceToTss = 0;
			if (relevantGene.getStrand().equals(Strand.PLUS) ){
				distanceToTss = absolutMiddle-transcript.getStrandCorredStart();
			}else{
				distanceToTss = transcript.getStrandCorredStart()-absolutMiddle;
			}
			*/
			Integer distanceToTss = Transcript.getDistanceToTssStart(transcript, absolutMiddle);
			if (distanceToTss<0  && Math.abs(distanceToTss)<=5000  ) {
				System.out.println("Upstream:" +transcript.getIdentifier() + "\t" + distanceToTss);
				
			}else if (distanceToTss>=0 &&  distanceToTss<=5000  ){
				System.out.println("Downstream:" + transcript.getIdentifier() + "\t" + distanceToTss);
				
			}
			
		}
	}
	
	@Test
	public void testGTFDistance4() throws Exception{
		
		File gtf = new File("data/GeneAnnotation/ENSG00000137752.gtf");
		
		List<Gene> genes = FileUtil.getGenes(gtf, "protein_coding", null);
		assertEquals(1,genes.size());
		
		Integer absolutMiddle = (104975996	+104976005)/2;
		Gene relevantGene = genes.get(0);
		for(Transcript transcript :relevantGene.getTranscripts()){
			
			/*Integer distanceToTss = 0;
			if (relevantGene.getStrand().equals(Strand.PLUS) ){
				distanceToTss = absolutMiddle-transcript.getStrandCorredStart();
			}else{
				distanceToTss = transcript.getStrandCorredStart()-absolutMiddle;
			}
			*/
			Integer distanceToTss = Transcript.getDistanceToTssStart(transcript, absolutMiddle);
			if (distanceToTss<0  && Math.abs(distanceToTss)<=5000  ) {
				System.out.println("Upstream:" +transcript.getIdentifier() + "\t" + distanceToTss);
				
			}else if (distanceToTss>=0 &&  distanceToTss<=5000  ){
				System.out.println("Downstream:" + transcript.getIdentifier() + "\t" + distanceToTss);
				
			}
			
		}
	}
	
	@Test
	public void testGTFDistance3() throws Exception{
		File gtf = new File("data/GeneAnnotation/ENSG00000197948.gtf");
		
		List<Gene> genes = FileUtil.getGenes(gtf, "protein_coding", null);
		assertEquals(1,genes.size());
		
		Integer absolutMiddle = (141015146+	141015164)/2;
		System.out.println(absolutMiddle);
		Gene relevantGene = genes.get(0);
		for(Transcript transcript :relevantGene.getTranscripts()){
			Integer distanceToTss = 0;
			if (relevantGene.getStrand().equals(Strand.PLUS) ){
				distanceToTss = absolutMiddle-transcript.getStrandCorredStart();
			}else{
				distanceToTss = transcript.getStrandCorredStart()-absolutMiddle;
			}
			System.out.println(transcript.getStrandCorredStart());
			System.out.println(transcript.getExons());
			System.out.println(transcript.getStrandCorredStart());
			System.out.println(distanceToTss);
		}
	}
	
	@Test
	public void testGTFDistance2() throws Exception{
		
		File gtf = new File("data/GeneAnnotation/ENSG00000269407.gtf");
		
		List<Gene> genes = FileUtil.getGenes(gtf, "protein_coding", null);
		assertEquals(1,genes.size());
		
		Integer absolutMiddle = (23326705	+23326723)/2;
		System.out.println(absolutMiddle);
		Gene relevantGene = genes.get(0);
		for(Transcript transcript :relevantGene.getTranscripts()){
			Integer distanceToTss = 0;
			if (relevantGene.getStrand().equals(Strand.PLUS) ){
				distanceToTss = absolutMiddle-transcript.getStrandCorredStart();
			}else{
				distanceToTss = transcript.getStrandCorredStart()-absolutMiddle;
			}
			System.out.println(transcript.getStrandCorredStart());
			System.out.println(transcript.getExons());
			System.out.println(transcript.getStrandCorredStart());
			System.out.println(distanceToTss);
		}
	}
	@Test
	public void testGTFDistance1() throws Exception{
		File gtf = new File("data/GeneAnnotation/ENSG00000180071.gtf");
		
		List<Gene> genes = FileUtil.getGenes(gtf, "protein_coding", null);
		assertEquals(1,genes.size());
		
		Integer absolutMiddle = (38575065+38575083)/2;
		System.out.println(absolutMiddle);
		Gene relevantGene = genes.get(0);
		for(Transcript transcript :relevantGene.getTranscripts()){
			Integer distanceToTss = 0;
			if (relevantGene.getStrand().equals(Strand.PLUS) ){
				distanceToTss = absolutMiddle-transcript.getStrandCorredStart();
			}else{
				distanceToTss = transcript.getStrandCorredStart()-absolutMiddle;
			}
			System.out.println(transcript.getExons());
			System.out.println(transcript.getStrandCorredStart());
			System.out.println(distanceToTss);
		}
	}
	
	@Test
	public void testReadGTF() throws Exception{
		File gtf = new File("data/GeneAnnotation/Stat1.gtf.test");
		
		List<Gene> genes = FileUtil.getGenes(gtf, "protein_coding", null);
		assertEquals(1,genes.size());
		System.out.println(genes.get(0).getTranscripts());
	}
	
	@Test
	public void testMappingFileReader() throws Exception{
		File tmpMappingFile = File.createTempFile("croco", ".tmp");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpMappingFile));
		bw.write("ID1\ta\tb\tc\n");
		bw.write("ID2\td\n");
		
		bw.flush();
		bw.close();
		
		HashMap<String, Set<String>> mapping = FileUtil.mappingFileReader(0,1, tmpMappingFile).includeAllColumnsAfterToIndex(true).readNNMappingFile();
		System.out.println(mapping);
	}
	
	@Test
	public void testFileLookUp() throws Exception{
		File tmpMappingFile = File.createTempFile("croco", ".tmp");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpMappingFile));
		bw.write("C1\tC2\n");
		bw.write("1\tb\n");
		bw.write("2\tc\n");
		bw.flush();
		bw.close();
		
		List<HashMap<String, String>> lookUp = FileUtil.fileLookUp(tmpMappingFile, "\t").getLookUp();
		assertEquals("1",lookUp.get(0).get("C1"));
		assertEquals("b",lookUp.get(0).get("C2"));
		assertEquals("2",lookUp.get(1).get("C1"));
		assertEquals("c",lookUp.get(1).get("C2"));
		
		tmpMappingFile.delete();
	}
	@Test  
	public void testReaderIterator() throws IOException
	{
	    File file = new File("/home/proj/biosoft/ws/croco-web/factors.gz");
	    
	    Iterator<String> it = FileUtil.getLineIterator(file);
	    
	    while(it.hasNext())
	    {
	       // System.out.println(it.next());
	    }
	    
	}
	
	@Test
	public void readN1MappingFile()throws Exception {
		File tmpMappingFile = File.createTempFile("croco", ".tmp");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpMappingFile));
		bw.write("1\ta\n");
		bw.write("1\tb\n");
		bw.write("2\tc\n");
		bw.flush();
		bw.close();
		
		HashMap<String, Set<String>> mappingNN = new FileUtil.MappingFileReader(0,1,tmpMappingFile).readNNMappingFile();
		System.out.println(mappingNN);
		assertEquals(mappingNN.size(),2);
		
		HashMap<String, String> mappingN1 = new FileUtil.MappingFileReader(0,1,tmpMappingFile).readMappingFile();
		System.out.println(mappingN1);
		assertEquals(mappingN1.size(),1);
		
		tmpMappingFile.delete();
		
	}

}
