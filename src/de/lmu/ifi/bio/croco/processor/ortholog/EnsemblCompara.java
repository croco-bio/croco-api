package de.lmu.ifi.bio.croco.processor.ortholog;

import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;

import de.lmu.ifi.bio.croco.connector.DatabaseConnection;
import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.IdentifierType;
import de.lmu.ifi.bio.croco.data.Species;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.croco.util.ConsoleParameter;

public class EnsemblCompara {
	public static void main(String[] args) throws Exception{
		ConsoleParameter parameter = new ConsoleParameter();
		ConsoleParameter.CroCoOption<File> baseDir =new ConsoleParameter.CroCoOption<File> ("baseDir",new ConsoleParameter.FolderExistHandler()).setArgs(1).isRequired();
		ConsoleParameter.CroCoOption<File> tmpDir =new ConsoleParameter.CroCoOption<File> ("tmpDir",new ConsoleParameter.FolderExistHandler()).setArgs(1).isRequired();
		
		parameter.register(baseDir);
		parameter.register(tmpDir);
		CommandLine cmdLine = parameter.parseCommandLine(args, EnsemblCompara.class);
		
		
		File tmpFile = File.createTempFile("croco.", ".orthologs",tmpDir.getValue(cmdLine));
		Set<Integer> taxIdSpecialSet = new HashSet<Integer>();
		for(Species species : Species.knownSpecies){
			taxIdSpecialSet.add(species.getTaxId());
		}
		System.out.println("TaxIDs:" + taxIdSpecialSet);
		Connection connection = DatabaseConnection.getConnection();
		Statement stat = connection.createStatement();
		//stat.execute("DELETE FROM Ortholog");
		stat.execute("DELETE FROM OrthologMappingInformation");
		stat.execute("DELETE FROM OrthologKnownGenes");
		
		stat.close();
		
		
		EnsemblCompara cmp = new EnsemblCompara();
		System.out.println("Import source information");
		cmp.importSourceIdentifierInformation(baseDir.getValue(cmdLine), taxIdSpecialSet);
		//System.out.println("Import genes");
		//cmp.importGenes(baseDir.getValue(cmdLine),tmpFile);
		//System.out.println("Import relations");
		//cmp.importRelations(baseDir.getValue(cmdLine), taxIdSpecialSet, tmpFile);
	}
	
	public void importRelations(File baseDir,Set<Integer> taxIdSpecialSet,  File tmpFile) throws Exception {
		int currentGroupId = 0;
		HashMap<Integer,List<String>> memberMapping = new HashMap<Integer,List<String>>();
		
		GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(baseDir + "/orthologs.gz"));
		BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
				
				
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile));
		String line = br.readLine();
		int k  = 1;
		//LocalService service = new LocalService();
		//HashSet<String> known = new HashSet<String>();
		//for(Entity e :  service.getEntities(null, "protein_coding", null)){
		//	known.add(e.getIdentifier());
		//}
		
		while (( line = br.readLine())!=null){
			String[] tokens= line.split("\t");
			Integer hId = Integer.valueOf(tokens[2]);
			Integer taxId = Integer.valueOf(tokens[0]);

			String id = tokens[3];
			
			String type = tokens[1];
		//	
			
			if ( !hId.equals(currentGroupId)){
				if ( memberMapping.size() > 1){
					List<Integer> taxIdsInGroup = new ArrayList<Integer>(memberMapping.keySet());
					Collections.sort(taxIdsInGroup);
					
					for(int i =  0 ; i < taxIdsInGroup.size();i++){
						for(String gene1 : memberMapping.get(taxIdsInGroup.get(i))){
							
							
							for(int j =  i+1 ; j < taxIdsInGroup.size();j++){
								for(String gene2 : memberMapping.get(taxIdsInGroup.get(j))){
									if( !taxIdSpecialSet.contains(taxIdsInGroup.get(i)) &&  !taxIdSpecialSet.contains(taxIdsInGroup.get(j)) ) continue;
									
									bw.write(gene1  + "\t" + taxIdsInGroup.get(i) + "\t" + gene2 + "\t" + taxIdsInGroup.get(j) + "\t" + OrthologDatabaseType.EnsemblCompara.ordinal() + "\n"  );
									
									if ( k % 50000 == 0){
										System.out.print(".");
										bw.flush();
									}
								}
							}
						}
					
					}
				}
				currentGroupId = hId;
				memberMapping= new HashMap<Integer,List<String>>();
			}
			
			if (! memberMapping.containsKey(taxId)){
				memberMapping.put(taxId, new ArrayList<String>());
			}
			if (! type.contains("ortholog")) continue;
			//if (! known.contains(id)) continue;
			memberMapping.get(taxId).add(id);
		}
		System.out.println();
		br.close();
		bw.flush();
		bw.close();
		
		Connection connection = DatabaseConnection.getConnection();

		Statement stat = connection.createStatement();
		System.out.println("Import relations <stopping 3s> )");
		Thread.sleep(3000);
		System.out.println("Importing data");
		String sql = String.format("LOAD DATA INFILE '%s' INTO TABLE Ortholog",tmpFile.getAbsoluteFile());
		System.out.println(sql);
		stat.execute(sql);
		stat.executeBatch();
		stat.close();
		
	}

	public void importSourceIdentifierInformation(File baseDir, Set<Integer> taxIdsOfInterest) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(baseDir + "/species"));
		String line =br.readLine();
		
		
		Connection connection = DatabaseConnection.getConnection();
		PreparedStatement stat = connection.prepareStatement("INSERT INTO OrthologMappingInformation values(?,?,?,?,?,?,?)");
	
		List<Integer> taxIdsAll = new ArrayList<Integer>();
		
		while((line = br.readLine())!= null){
			String[] tokens = line.split("\t");
			Integer taxId = null;
			try{
				 taxId=Integer.valueOf( tokens[1] ) ;
			}catch(NumberFormatException e){
				System.err.println("No taxId for species:" + line);
				continue;
			
			}
			//if ( taxIdsOfInterest.contains(taxId)) continue;
			taxIdsAll.add(taxId);
		}
		Collections.sort(taxIdsAll);
		for(int i = 0 ; i< taxIdsAll.size() ; i++){
			Integer taxId1 = taxIdsAll.get(i);
			for(int j=i+1;j <taxIdsAll.size();j++ ){
				Integer taxId2 = taxIdsAll.get(j);
				if ( !taxIdsOfInterest.contains(taxId1) && !taxIdsOfInterest.contains(taxId2)) continue;
				if (taxId1 < taxId2){
					stat.setInt(2, taxId1);
					stat.setInt(5, taxId2);
				}else{
					stat.setInt(2, taxId2);
					stat.setInt(5, taxId1);
				}
				
				stat.setInt(1, OrthologDatabaseType.EnsemblCompara.ordinal());
				
				stat.setInt(3, IdentifierType.ENSEMBLGENE.ordinal());
				stat.setString(4, "Ensembl Gene");
				
				stat.setInt(6, IdentifierType.ENSEMBLGENE.ordinal());
				stat.setString(7, "Ensembl Gene");
				stat.execute();
			}
		}	
		
	}


	public void importGenes(File baseDir, File tmpFile) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(baseDir + "/members"));
		String line =br.readLine();
		
		
	
		int k = 0;
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile));
		while((line = br.readLine())!= null){
			String[] tokens = line.split("\t");
			String stableId = tokens[0];
			Integer taxId =Integer.valueOf( tokens[1] ) ;
			bw.write(OrthologDatabaseType.EnsemblCompara.ordinal() + "\t" + stableId + "\t" + taxId + "\n");
			if ( k++ % 5000 == 0){
				bw.flush();
			}
		} 
		bw.flush();
		bw.close();
		br.close();
		
		Connection connection = DatabaseConnection.getConnection();

		Statement stat = connection.createStatement();
		System.out.println("Import genes <stopping 3s> (" + k +"  genes)");
		Thread.sleep(3000);
		System.out.println("Importing data");
		String sql = String.format("LOAD DATA INFILE '%s' INTO TABLE OrthologKnownGenes",tmpFile.getAbsoluteFile());
		System.out.println(sql);
		stat.execute(sql);
		stat.executeBatch();
		stat.close();
	}

}
