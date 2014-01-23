package de.lmu.ifi.bio.crco.util;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.util.FileUtil.MappingFileReader;

public class FileUtilTest {
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
		
		HashMap<String, String> mappingN1 = new FileUtil.MappingFileReader(0,1,tmpMappingFile).readN1MappingFile();
		System.out.println(mappingN1);
		assertEquals(mappingN1.size(),1);
		
		tmpMappingFile.delete();
		
	}

}
