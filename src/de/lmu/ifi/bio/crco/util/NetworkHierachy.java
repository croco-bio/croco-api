package de.lmu.ifi.bio.crco.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.examples.CreateAnalogList;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;

public class NetworkHierachy  {
	public static void main(String[] args) throws Exception{
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("repositoryDir").withDescription("Repository directory").isRequired().hasArgs().create("repositoryDir"));
		options.addOption(OptionBuilder.withLongOpt("tmpDir").isRequired().hasArgs().create("tmpDir"));
		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + CreateAnalogList.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		Connection connection = DatabaseConnection.getConnection();
		Statement stat = connection.createStatement();
		stat.execute("DELETE FROM NetworkHierachy");
		stat.execute("DELETE FROM Network");
		stat.execute("DELETE FROM NetworkOption");
		stat.close();
		
		PreparedStatement hierachy = connection.prepareStatement("INSERT INTO NetworkHierachy(group_id, parent_group_id,name,tax_id,has_network,network_type,network_file_location) values(?,?,?,?,?,?,?)");
		PreparedStatement networkOptions = connection.prepareStatement("INSERT INTO NetworkOption(option_id,group_id,value) values(?,?,?)");

		
		int groupIdCounter=1;
		//root
		hierachy.setInt(groupIdCounter++, 1);
		hierachy.setInt(2, 0);
		hierachy.setString(3, "Root node");
		hierachy.setNull(4,java.sql.Types.INTEGER);
		hierachy.setBoolean(5, false);
		hierachy.setNull(6, java.sql.Types.INTEGER);
		hierachy.setNull(7, java.sql.Types.VARCHAR);
		hierachy.addBatch();
		
		Stack<Integer> parentIds = new Stack<Integer>();
		
		File repositoryDir = new File(line.getOptionValue("repositoryDir"));
		Stack<File> files = new Stack<File>();
		for(File file : repositoryDir.listFiles()){
			files.add(file);
			parentIds.add(1);
		}
		File tmpDir = new File(line.getOptionValue("tmpDir"));
		File networkFile = new File(tmpDir + "/networks" );
		BufferedWriter bwNetwork = new BufferedWriter(new FileWriter(networkFile));
		File bindingFile = new File(tmpDir + "/bindings" );
		BufferedWriter bwBindingAnnotation = new BufferedWriter(new FileWriter(bindingFile));
		
		int k = 1;
		while(!files.isEmpty()){
			File file = files.pop();
			Integer rootId = parentIds.pop();
			if ( k++ % 300 == 0){
				hierachy.executeBatch();
				networkOptions.executeBatch();
			}
			if ( file.getName().endsWith(".network.gz")){
				File infoFile = new File( file.toString().replace(".network.gz", ".info"));
				if (! infoFile.exists()){
					CroCoLogger.getLogger().warn("Network without info file:" +infoFile);
					continue;
				}
				Integer newRootId = groupIdCounter++;
				HashMap<Option, String> infoAnnotations = readInfoFile(infoFile);
				hierachy.setInt( 1, newRootId);
				hierachy.setInt(2, rootId);
				hierachy.setString(3, infoAnnotations.get(Option.NetworkName));
				hierachy.setInt(4,Integer.valueOf(infoAnnotations.get(Option.TaxId)));
				hierachy.setBoolean(5, true);
				hierachy.setInt(6, NetworkType.valueOf(infoAnnotations.get(Option.NetworkType)).ordinal());
				hierachy.setString(7, file.toString());
				for(Entry<Option, String>e  : infoAnnotations.entrySet()){
					if ( e.getKey().equals(Option.NetworkName)) continue;
					if ( e.getKey().equals(Option.TaxId)) continue;
					if ( e.getKey().equals(Option.NetworkType)) continue;

					
					networkOptions.setInt(1, e.getKey().ordinal());
					networkOptions.setInt(2,newRootId );
					networkOptions.setString(3, e.getValue());
					networkOptions.addBatch();
				}
				addtoNetwork(bwNetwork,newRootId,file);
				bwNetwork.flush();
				//File annotationFile = new File(file.toString().replace(".network.gz", "annotation.gz"));
				//if (annotationFile.exists() ) addtoBindingAnnotation(bwBindingAnnotation,newRootId,file);
				hierachy.addBatch();
				
			}else if ( file.isDirectory()){
				System.out.println("Processing:" +file);
				File infoFile = new File(file + "/.info");
				HashMap<Option, String> infoAnnotation = null;
				if ( infoFile.exists()){
					infoAnnotation = readInfoFile(infoFile);
				}
				Integer newRootId = groupIdCounter++;
				
				hierachy.setInt(1,newRootId);
				hierachy.setInt(2, rootId);
				if ( infoAnnotation != null){
					hierachy.setString(3, infoAnnotation.get(Option.NetworkName));
					hierachy.setInt(4,Integer.valueOf(infoAnnotation.get(Option.TaxId)));
				}else{
					hierachy.setString(3, file.getName());
					hierachy.setNull(4,java.sql.Types.INTEGER);
				}
				
				hierachy.setBoolean(5, false);
				hierachy.setNull(6, java.sql.Types.INTEGER);
				hierachy.setNull(7, java.sql.Types.VARCHAR);
				hierachy.addBatch();
				
				for(File f : file.listFiles()) {
					
					files.add(f);
					parentIds.add(newRootId);
				}
			}
		}	
		hierachy.executeBatch();
		hierachy.close();
		bwNetwork.flush();
		bwNetwork.close();
		
		System.out.println("Sleeping for 5sec");
		Thread.sleep(5000);
		
		System.out.println("Loading network into database");
		stat = connection.createStatement();
		stat.execute(String.format("LOAD DATA INFILE '%s' INTO TABLE Network (group_id,gene1,gene2)",networkFile.toString()) );
		stat.close();
		
		
		connection.close();
		
	
	}
	private static void addtoBindingAnnotation( BufferedWriter bwBindingAnnotation,int groupId, File file) throws Exception {
	/*	BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
		String line = null;
		while((line=br.readLine())!=null){
			if (line.startsWith("ChiPBinding")){
				bwBindingAnnotation.write(groupId + "\t"+ line );
			}
			String[] tokens = line.split("\t");
			String type = tokens[0];
			
			
			if (type.equals("ChiPBinding")){
				
			}
		}
		*/
		return;
	}
	private static void addtoNetwork(BufferedWriter bwNetwork, int groupId, File file) throws Exception{
		BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
		String line = null;
		while((line=br.readLine())!=null){
			bwNetwork.write(String.format("%d\t%s\n",groupId,line));
		}
		br.close();
	}
	private static HashMap<Option,String> readInfoFile(File infoFile) throws Exception{
		HashMap<Option,String> ret = new HashMap<Option,String> ();
		BufferedReader br =new BufferedReader(new FileReader(infoFile));
		String line = null;
		while(( line = br.readLine())!=null){
			String[] tokens = line.split(":");
			Option option = Option.valueOf(tokens[0].trim());
			String value = tokens[1].trim();
			ret.put(option, value);
		}
		return ret;
	}
	
	public static void writeNetworkHierachyFile(DirectedNetwork network, File networkFile) throws Exception{
		BufferedWriter bwNetwork = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(networkFile)) ));
		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			bwNetwork.write(edge.getFirst().getIdentifier() + "\t" + edge.getSecond().getIdentifier() + "\n");
		}
		bwNetwork.flush();
		bwNetwork.close();
	}
}
