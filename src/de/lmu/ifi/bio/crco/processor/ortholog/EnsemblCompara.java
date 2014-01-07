package de.lmu.ifi.bio.crco.processor.ortholog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.data.IdentifierType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;

public class EnsemblCompara implements OrthologHandler {

	@Override
	public void importRelations(File baseDir, Set<Integer> taxIdsOfInterest, File tmpFile) throws Exception {
		int currentGroupId = 0;
		HashMap<Integer,List<String>> memberMapping = new HashMap<Integer,List<String>>();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile));
		BufferedReader br = new BufferedReader(new FileReader(baseDir + "/data"));
		String line = br.readLine();
		int k  = 1;
		
		while (( line = br.readLine())!=null){
			String[] tokens= line.split("\t");
			Integer hId = Integer.valueOf(tokens[3]);
			Integer taxId = Integer.valueOf(tokens[5]);
			Integer membeId = Integer.valueOf(tokens[6]);
			String id = tokens[7];

			if ( !hId.equals(currentGroupId)){
				if ( memberMapping.size() > 1){
					List<Integer> taxIdsInGroup = new ArrayList<Integer>(memberMapping.keySet());
					Collections.sort(taxIdsInGroup);
					
					for(int i =  0 ; i < taxIdsInGroup.size();i++){
						for(String gene1 : memberMapping.get(taxIdsInGroup.get(i))){
							for(int j =  i+1 ; j < taxIdsInGroup.size();j++){
								for(String gene2 : memberMapping.get(taxIdsInGroup.get(j))){
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
			memberMapping.get(taxId).add(id);
		}
		System.out.println();
		br.close();
		bw.flush();
		bw.close();
		
		Connection connection = DatabaseConnection.getConnection();

		Statement stat = connection.createStatement();
		System.out.println("Import relations <stopping 3s> (" + k +"  genes)");
		Thread.sleep(3000);
		System.out.println("Importing data");
		String sql = String.format("LOAD DATA INFILE '%s' INTO TABLE Ortholog",tmpFile.getAbsoluteFile());
		System.out.println(sql);
		stat.execute(sql);
		stat.executeBatch();
		stat.close();
		
	}

	@Override
	public void importSourceIdentifierInformation(File baseDir, Set<Integer> taxIdsOfInterest) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(baseDir + "/species"));
		String line =br.readLine();
		
		
		Connection connection = DatabaseConnection.getConnection();
		PreparedStatement stat = connection.prepareStatement("INSERT INTO OrthologMappingInformation values(?,?,?,?,?,?,?)");
	
		List<Integer> taxIds = new ArrayList<Integer>();
		while((line = br.readLine())!= null){
			String[] tokens = line.split("\t");
			Integer taxId = null;
			try{
				 taxId=Integer.valueOf( tokens[1] ) ;
			}catch(NumberFormatException e){
				continue;
			}
			if (! taxIdsOfInterest.contains(taxId)){
				br.close();
				throw new RuntimeException("Seems as EnsemblCompara includes non-eukaryotes?");
			}
			taxIds.add(taxId);
		}
		Collections.sort(taxIds);
		for(int i =  0 ; i< taxIds.size(); i++){
			for(int j =  i+1 ; j< taxIds.size(); j++){
				stat.setInt(1, OrthologDatabaseType.EnsemblCompara.ordinal());
				stat.setInt(2, taxIds.get(i));
				stat.setInt(3, IdentifierType.ENSEMBLGENE.ordinal());
				stat.setString(4, "Ensembl Gene");
				stat.setInt(5, taxIds.get(j));
				stat.setInt(6, IdentifierType.ENSEMBLGENE.ordinal());
				stat.setString(7, "Ensembl Gene");
				stat.execute();
			}
		}	
		
	}


	@Override
	public void importGenes(File baseDir, Set<Integer> taxIdsOfInterest, File tmpFile) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(baseDir + "/members"));
		String line =br.readLine();
		
		
	
		int k = 0;
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile));
		while((line = br.readLine())!= null){
			String[] tokens = line.split("\t");
			String stableId = tokens[0];
			Integer taxId =Integer.valueOf( tokens[1] ) ;
			if (! taxIdsOfInterest.contains(taxId)){
				br.close();
				throw new RuntimeException("Seems as EnsemblCompara includes non-eukaryotes?");
			}
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
