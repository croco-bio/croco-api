package de.lmu.ifi.bio.croco.processor.ontology;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import de.lmu.ifi.bio.croco.connector.DatabaseConnection;
import de.lmu.ifi.bio.croco.data.CroCoNode;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.croco.data.NetworkType;
import de.lmu.ifi.bio.croco.data.Option;
import de.lmu.ifi.bio.croco.network.DirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.network.Network.EdgeOption;
import de.lmu.ifi.bio.croco.util.ConsoleParameter;
import de.lmu.ifi.bio.croco.util.ConsoleParameter.CroCoOption;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.Tuple;
import de.lmu.ifi.bio.croco.util.ontology.NetworkOntology;

/**
 * Processes the file based network hierarchy.
 * @author pesch
 *
 */
public class NetworkOntologyWriter  {
	private static CroCoOption<File> TMP_DIR = new CroCoOption<File>("tmpDir",new ConsoleParameter.FileExistHandler()).isRequired().setArgs(1).setDescription("Gene name to ensembl mapping");
	private static CroCoOption<Boolean> IMPORT_ONLY_HIERACHY = new CroCoOption<Boolean>("only_hir",new ConsoleParameter.FlagHandler()).setDescription("(Re)-import only the hierachy including the meta-data.");
    


	/**
	 * Imports a file based croco hierarchy into a SQL database.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
	
	    ConsoleParameter parameter = new ConsoleParameter();
		parameter.register(
				ConsoleParameter.repositoryDir,
				TMP_DIR,IMPORT_ONLY_HIERACHY
		);
		CommandLine cmdLine = parameter.parseCommandLine(args, NetworkOntologyWriter.class);
		
		Boolean onlyHierachy = IMPORT_ONLY_HIERACHY.getValue(cmdLine);
		
		File repositoryDir = new File(cmdLine.getOptionValue("repositoryDir"));
		File tmpDir = null;
		if ( cmdLine.hasOption("tmpDir")) tmpDir = new File(cmdLine.getOptionValue("tmpDir"));

		File networkFile = null;
		File annotationFile = null;
		
		CroCoLogger.getLogger().info(String.format("Repository dir: %s",repositoryDir));
		
		Connection connection = DatabaseConnection.getConnection();
		
		CroCoLogger.getLogger().info("Cleaning data");
        
		Statement stat = connection.createStatement();
		
		stat.execute("DELETE FROM NetworkHierachy");
		stat.execute("DELETE FROM NetworkOption");
        
		if ( !onlyHierachy)
		{
		    System.out.print("Networks/Bindings will be deleted. Write OK to continue:");
		    Scanner scan=new Scanner(System.in);
		    if (! scan.nextLine().equals("OK"))
		    {
		        System.exit(1);
		    }
		    scan.close();
		 
	       networkFile =File.createTempFile("networks.", ".croco",tmpDir) ;
           annotationFile = File.createTempFile("annotation.", ".croco",tmpDir);
           CroCoLogger.getLogger().info(String.format("Temp networkFile: %s",networkFile));
           CroCoLogger.getLogger().info(String.format("Annotation file: %s",annotationFile));
           
		    
		    System.exit(1);
    		stat.execute("DELETE FROM Network");
    		stat.execute("DELETE FROM Network2Binding");
		}
		stat.close();
		
		
		try{
		    NetworkOntologyWriter writer = new NetworkOntologyWriter();
			
			
		    writer.processHierachy(repositoryDir, new NetworkProcessor(repositoryDir,connection, networkFile,annotationFile), null);
			
		}catch(Exception e){
			throw new RuntimeException(e);
		}finally{
			
			connection.close();
			/*
			networkFile.delete();
			statFile.delete();
			*/
		}
		CroCoLogger.getLogger().info("Create network ontology");
        NetworkOntology onto = new NetworkOntology();
        
        CroCoNode root = onto.createNetworkOntology();
        onto.persistNetworkOntology(root);
	}
	
	private static int getNextId(AtomicInteger nextId)
	{
	    return nextId.incrementAndGet();
	}
	/**
	 * Processes the file based representation of the croco repository
	 * @param repositoryDir the repo folder
	 * @param fileHandler a handler to process networks, may be null
	 * @param folderHandler a handler to process network sub folders, may be null
	 * @throws Exception
	 */
	public void processHierachy(File repositoryDir, CroCoRepositoryProcessor networkFileHandler, CroCoRepositoryProcessor folderHandler ) throws Exception{ 
		
	
		Stack<Integer> parentIds = new Stack<Integer>();
		Stack<File> files = new Stack<File>();
		
		int rootId =1;
		AtomicInteger nextId = new AtomicInteger(rootId);
		
		for(File file : repositoryDir.listFiles()){
			files.add(file);
			parentIds.add(rootId);
		}
		
		if ( networkFileHandler != null) networkFileHandler.init(rootId);
		if ( folderHandler != null) folderHandler.init(rootId);
		
		while(!files.isEmpty()){
			File file = files.pop();
			Integer currentRootId = parentIds.pop();
			if ( file.getName().endsWith(".network.gz")){
				File infoFile = new File( file.toString().replace(".network.gz", ".info"));
				if (! infoFile.exists()){
					CroCoLogger.getLogger().warn("Network without info file:" +infoFile);
					return;
				}
				File annotationFile = new File(file.toString().replace(".network.gz", ".annotation.gz"));
				if ( networkFileHandler != null)
				    networkFileHandler.process(currentRootId,getNextId(nextId), file, infoFile,annotationFile);
				
			}else if ( file.isDirectory()){
				File infoFile = new File(file + "/.info");
				File ignoreFile = new File(file + "/.ignore");
				HashSet<String> ignoreList =new HashSet<String>();
				if ( ignoreFile.exists()){
					BufferedReader br = new BufferedReader(new FileReader(ignoreFile));
					String line = null;
					while (( line=br.readLine())!=null){
						ignoreList.add(line);
					}
					br.close();
				}
				List<File> currentFiles = Arrays.asList(file.listFiles()); //sort lexci.
				Collections.sort(currentFiles);
				
				int folderId = getNextId(nextId);
				for(File f : currentFiles) {
					if ( ignoreList.contains(f.getName())){
						CroCoLogger.getLogger().debug(String.format("Ignore sub folder: %s", f.toString()));
						continue;
					}
					files.add(f);
					parentIds.add(folderId);
				}
				File statFile = new File(file.toString().replace(".network.gz", ".stat"));
				if ( folderHandler != null) folderHandler.process(currentRootId,folderId , file, infoFile,null);
				
			}
		}	
		if ( networkFileHandler != null)networkFileHandler.finish();
		if ( folderHandler != null) folderHandler.finish();
		
	}
	/**
	 * Processor for networks and folders in the croco repository
	 * @author pesch
	 *
	 */
	public interface CroCoRepositoryProcessor {
		
		public void init(Integer rootId) throws Exception;
		public void process(Integer rootId, Integer networkId, File networkFile, File infoFile, File annotationFile) throws Exception;
		public void finish() throws Exception;
		
	}

	
	static class NetworkProcessor implements CroCoRepositoryProcessor{
		private PreparedStatement hierachy;
		private PreparedStatement networkOptions;
		
		private BufferedWriter bwNetwork;
		private BufferedWriter bwAnnotation;
		
		private int counter = 0;
		
		private Connection connection;
		
		private File repositoryDir;
		private File networkFile;
		private File annotationFile;
		
		
		public NetworkProcessor(File repositoryDir,Connection connection, File networkFile, File annotationFile) throws Exception{
			this.repositoryDir = repositoryDir;
			this.networkFile = networkFile;
			this.annotationFile = annotationFile;
			
			
			this.connection = connection;
			this.hierachy = connection.prepareStatement("INSERT INTO NetworkHierachy(group_id, name,tax_id,network_type,network_file_location,hash) values(?,?,?,?,?,?)");
			
			if ( networkFile != null)
			    bwNetwork = new BufferedWriter(new FileWriter(networkFile));
			if ( annotationFile != null)
			    bwAnnotation = new BufferedWriter(new FileWriter(annotationFile));
			networkOptions = connection.prepareStatement("INSERT INTO NetworkOption(option_id,group_id,value) values(?,?,?)");

		}
		private void addtoNetwork(int groupId,Network network) throws IOException{
			/*BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
			String line = null;
			while((line=br.readLine())!=null){
				bwNetwork.write(String.format("%d\t%s\n",groupId,line));
			}
			br.close();
		    */
		    for(int edgeId : network.getEdgeIds())
		    {
		        Tuple<Entity, Entity> edge = network.getEdge(edgeId);
		        bwNetwork.write(String.format("%d\t%s\t%s\n",groupId,edge.getFirst().toString(),edge.getSecond().toString()));
		    }
		}
		private Float getValue(String value){
			if ( value.trim().equals("NaN"))
				return null;
			if ( value.trim().equals("-"))
				return null;
			if( value.trim().equals(".") )
				return null;
			if( value.trim().length() == 0)
				return null;
			if ( Float.valueOf(value).intValue() == -1){
				return null;
			}
			return Float.valueOf(value);
		}
		private void addToAnnotation(int groupId, File file) throws IOException{
			BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
			String line = null;
			while((line=br.readLine())!=null){
				String[] tokens = line.split("\t");
				String type = tokens[0];
				
				String factor = tokens[1];
				String target = tokens[2];
				String bindingPartner = null;
				Float bindingPValue = null;
				String chrom = null;
				Integer bindingStart = null;
				Integer bindingEnd = null;
		
				Integer openChromStart = null;
				Integer openChromEnd = null;
				
				Float openChromPValue = null;
				
				if ( type.equals("TFBS") || type.equals("TBFS") ){
					bindingPartner = tokens[3];
					bindingPValue = Float.valueOf(tokens[7]);
					chrom = tokens[8];
					bindingStart = Integer.valueOf(tokens[9]);
					bindingEnd = Integer.valueOf(tokens[10]);
				}else if ( type.equals("CHIP")){
					bindingPartner = tokens[3];
					bindingPValue = getValue(tokens[6]);
					if ( bindingPValue != null){
						bindingPValue = (float) Math.pow(bindingPValue, -10);
					}
					chrom = tokens[7];
					bindingStart = Integer.valueOf(tokens[8]);
					bindingEnd = Integer.valueOf(tokens[9]);	
				}else if ( type.equals("OpenChromTFBS")){
					bindingPartner = tokens[3];
					bindingPValue = getValue(tokens[7]);
					chrom = tokens[8];
					bindingStart = Integer.valueOf(tokens[9]);
					bindingEnd = Integer.valueOf(tokens[10]);
					
					openChromStart = Integer.valueOf(tokens[11]);
					openChromEnd = Integer.valueOf(tokens[12]);
					/*
					openChromPValue = getValue(tokens[13]);
					if ( openChromPValue != null){
						openChromPValue = (float) Math.pow(openChromPValue, -10);
					}
					*/
					openChromPValue = null;
				}else{
					br.close();
 					throw new IOException("Unknown type:" + type);
				}
				String out = groupId + "\t" + factor + "\t" + target + "\t" + chrom + "\t" +
							 bindingStart + "\t" +bindingEnd + "\t"+ (bindingPValue==null?"\\N":bindingPValue +"")+ "\t" +bindingPartner + "\t" + 
							 (openChromPValue==null?"\\N":openChromPValue) + "\t" + (openChromStart==null?"\\N":openChromStart) + "\t" + (openChromEnd==null?"\\N":openChromEnd);
 				bwAnnotation.write(out + "\n");
			}
			
			br.close();
		}
		@Override
		public void process(Integer rootId ,Integer networkId, File networkFile, File infoFile, File annotationFile) throws Exception {
			CroCoLogger.getLogger().debug("Process:" + networkFile);
			
			if ( counter++ % 300 == 0){
				hierachy.executeBatch();
				networkOptions.executeBatch();
			}
			
			
			
			//Integer newRootId = groupIdCounter++;
			HashMap<Option, String> infoAnnotations = Network.readInfoFile(infoFile);
			hierachy.setInt( 1, networkId);
			hierachy.setString(2, infoAnnotations.get(Option.NetworkName));
			hierachy.setInt(3,Integer.valueOf(infoAnnotations.get(Option.TaxId)));
			hierachy.setInt(4, NetworkType.valueOf(infoAnnotations.get(Option.NetworkType)).ordinal());
			hierachy.setString(5,  networkFile.toString().replace(repositoryDir.toString(), "") );
			long hash= Files.hash(networkFile,   Hashing.crc32()).padToLong();
			hierachy.setLong(6, hash);
			
			Network network = Network.getNetworkReader().setNetworkFile(networkFile).readNetwork();
            int edges = network.size();
            int nodes = network.getNodes().size();
            
            infoAnnotations.put(Option.numberOfInteractions, edges +"");
            infoAnnotations.put(Option.numberOfNodes, nodes+"");
            
			for(Entry<Option, String>e  : infoAnnotations.entrySet()){
				if ( e.getKey().equals(Option.FactorList)) continue;
				if ( e.getKey().equals(Option.NetworkName)) continue;
				if ( e.getKey().equals(Option.TaxId)) continue;
				if ( e.getKey().equals(Option.NetworkType)) continue;

				
				networkOptions.setInt(1, e.getKey().ordinal());
				networkOptions.setInt(2,networkId );
				networkOptions.setString(3, e.getValue());
				networkOptions.addBatch();
			}
			
			
			
			if ( bwNetwork != null)
			    bwNetwork.flush();
			if ( bwAnnotation != null)
			    bwAnnotation.flush();
			hierachy.addBatch();
			
			if ( this.bwNetwork != null)
			    addtoNetwork(networkId,network);
			if ( this.bwAnnotation != null && annotationFile != null && annotationFile.exists()){
				addToAnnotation(networkId,annotationFile);
			}
			
			
		}


		@Override
		public void init(Integer rootId) throws Exception {
			
		}


		@Override
		public void finish() throws Exception {
		    networkOptions.executeBatch();
		    networkOptions.close();
		    
			hierachy.executeBatch();
			hierachy.close();
			if ( bwNetwork != null)
			{
    			bwNetwork.flush();
    			bwNetwork.close();
    			bwAnnotation.flush();
    			bwAnnotation.close();
    		}
			
			System.out.println("Sleeping for 5sec");
			Thread.sleep(5000);
			/*
			System.out.println("Loading network into database");
			stat = connection.createStatement();
			stat.execute(String.format("LOAD DATA INFILE '%s' INTO TABLE Network (group_id,gene1,gene2);  ",networkFile.toString()) );
			stat.close();
			
			System.out.println("Loading stats into database");
			stat = connection.createStatement();
			stat.execute(String.format("LOAD DATA INFILE '%s' INTO TABLE NetworkSimilarity (group_id_1,group_id_2,option_id,value);",statFile.toString()) );
			stat.close();
			
			System.out.println("Loading annotation into database");
			stat = connection.createStatement();
			stat.execute(String.format("LOAD DATA INFILE '%s' INTO TABLE Network2Binding;",annotationFile.toString()) );
			stat.close();
		*/
		}
		
	}




}
