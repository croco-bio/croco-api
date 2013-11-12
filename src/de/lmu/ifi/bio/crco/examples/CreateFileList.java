package de.lmu.ifi.bio.crco.examples;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;

public class CreateFileList {
	public static void main(String[] args) throws Exception{
	
		LocalService service = new LocalService(DatabaseConnection.getConnection());
		
		HashMap<Entity,List<File>> humanFiles = getFiles(new File("/home/users/pesch/Databases/ENCODE/CHIPSEQ-PEAKS/Human/"));
		HashMap<Entity,List<File>> mouseFiles = getFiles(new File("/home/users/pesch/Databases/ENCODE/CHIPSEQ-PEAKS/Mouse/"));
		HashMap<Entity,List<File>> liftOver = getFiles(new File("/home/users/pesch/Databases/modEncode/Mouse/LiftOver/0.50/"));
		
		
		List<OrthologMappingInformation> mappingInformation = service.getOrthologMappingInformation(OrthologDatabaseType.InParanoid, new Species(9606), new Species(10090));
		OrthologMapping orthologs = service.getOrthologMapping(mappingInformation.get(0));
		
		BufferedWriter bw = new BufferedWriter(new FileWriter("/home/users/pesch/workspace/croco-api/bin/files"));
		for(Entry<Entity, List<File>> e : humanFiles.entrySet()){
			Set<Entity> mappings = orthologs.getOrthologs(e.getKey());
			if ( mappings == null){
				System.out.println(e);
				continue;
			}
			for(Entity mapping : mappings){
				if ( mouseFiles.containsKey(mapping)){
					for(File f : e.getValue()){
						bw.write("Human" + "\t" + e.getKey() + "\t" + f+ "\n" );
					}
					for(File f : mouseFiles.get(mapping)){
						bw.write("Mouse" + "\t" + mapping + "\t" + f+ "\n" );
					}
					for(File f : liftOver.get(mapping)){
						bw.write("LiftOver" + "\t" + e.getKey() + "\t" + f+ "\n" );
					}
				}
			}
		}
		bw.flush();
		bw.close();
		
	}
	private static HashMap<Entity,List<File>> getFiles(File dir) throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(new File(dir + "/data")));
		String line = null;
		HashMap<Entity,List<File>> files = new HashMap<Entity,List<File>> ();
		while (( line = br.readLine())!=null){
			String[] tokens = line.split("\t");
			File f = new File(dir + "/" + tokens[0]);
			String cell = tokens[2].toUpperCase().trim();
			if ( !cell.equals("MEL") && !cell.equals("K562")) continue;
			Entity factor = new Entity(tokens[tokens.length-1]);
			if (! files.containsKey(factor)){
				files.put(factor, new ArrayList<File>());
			}
			files.get(factor).add(f);
		}
		br.close();
		return files;
	}
}
