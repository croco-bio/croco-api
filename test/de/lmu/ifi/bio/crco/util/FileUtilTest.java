package de.lmu.ifi.bio.crco.util;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class FileUtilTest {
	
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
		
		HashMap<String, Set<String>> mappingNN = new FileUtil.MappingFileReader("\t",0,1,tmpMappingFile).readNNMappingFile();
		System.out.println(mappingNN);
		assertEquals(mappingNN.size(),2);
		
		HashMap<String, String> mappingN1 = new FileUtil.MappingFileReader("\t",0,1,tmpMappingFile).readN1MappingFile();
		System.out.println(mappingN1);
		assertEquals(mappingN1.size(),1);
		
		tmpMappingFile.delete();
		
		
		
	}

}
