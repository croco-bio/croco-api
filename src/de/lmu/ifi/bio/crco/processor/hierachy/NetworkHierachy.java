package de.lmu.ifi.bio.crco.processor.hierachy;

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

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.data.CroCoNode;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;
import de.lmu.ifi.bio.crco.util.ConsoleParameter;
import de.lmu.ifi.bio.crco.util.ConsoleParameter.CroCoOption;
import de.lmu.ifi.bio.crco.util.ontology.NetworkOntology;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Tuple;

/**
 * Processes the file based network hierarchy.
 * @author Robert Pesch
 *
 */
public class NetworkHierachy  {
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
		CommandLine cmdLine = parameter.parseCommandLine(args, NetworkHierachy.class);
		
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
			NetworkHierachy hierachy = new NetworkHierachy();
			
			
			hierachy.processHierachy(repositoryDir, new NetworkProcessor(repositoryDir,connection, networkFile,annotationFile), null);
			
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
			HashMap<Option, String> infoAnnotations = readInfoFile(infoFile);
			hierachy.setInt( 1, networkId);
			hierachy.setString(2, infoAnnotations.get(Option.NetworkName));
			hierachy.setInt(3,Integer.valueOf(infoAnnotations.get(Option.TaxId)));
			hierachy.setInt(4, NetworkType.valueOf(infoAnnotations.get(Option.NetworkType)).ordinal());
			hierachy.setString(5,  networkFile.toString().replace(repositoryDir.toString(), "") );
			long hash= Files.hash(networkFile,   Hashing.crc32()).padToLong();
			hierachy.setLong(6, hash);
			
			Network network = NetworkHierachy.getNetworkReader().setNetworkFile(networkFile).readNetwork();
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

	public static HashMap<Option,String> readInfoFile(File infoFile) throws IOException{
		HashMap<Option,String> ret = new HashMap<Option,String> ();
		BufferedReader br =new BufferedReader(new FileReader(infoFile));
		String line = null;
		while(( line = br.readLine())!=null){
			
			String[] tokens = line.split(":");
			Option option = Option.valueOf(tokens[0].trim());
			
			if ( option == Option.OpenChromMotifPVal)
			    option = Option.ConfidenceThreshold;
			
			if ( option == Option.OpenChromMotifSet)
			    option = Option.MotifSet;
			
			String value = tokens[1].trim();
			ret.put(option, value);
		}
		br.close();
		
		return ret;
	}

	public static NetworkReader getNetworkReader(){
		return new NetworkReader();
	}
	public static class NetworkReader{
		
		private Network network;
		private Integer groupId;
		private Set<Entity> factors;
		private HashMap<Option,String> infos;
		private Boolean gloablRepository = false;
		private File networkFile;
		private NetworkHierachyNode node;
		
		public NetworkReader setNetworkInfo(File networkInfoFile) throws IOException{
			this.infos = readInfoFile(networkInfoFile);
			return this;
		}
	
		public NetworkReader setNetworkHierachyNode(NetworkHierachyNode node){
			this.node = node;
			this.groupId = node.getGroupId();
			return this;
		}
		public NetworkReader setNetworkInfo(HashMap<Option,String> infos) throws IOException{
			this.infos = infos;
			return this;
		}
		public NetworkReader setNetwork(Network network){
			this.network = network;
			return this;
		}
		public NetworkReader setFactors(Set<Entity> factors){
			this.factors = factors;
			return this;
		}
		public NetworkReader setGloablRepository(Boolean gloablRepository){
			this.gloablRepository = gloablRepository;
			return this;
		}
		
		public NetworkReader setGroupId(Integer groupId){
			this.groupId = groupId;
			return this;
		}
		
		public NetworkReader setNetworkFile(File networkFile){
			this.networkFile = networkFile;
			return this;
		}
		
		public Network readNetwork() throws Exception {
			if ( network == null){
				if ( node != null){
					if ( infos != null){
						CroCoLogger.getLogger().warn("networkInfo and network hierachy node given (use hierachy node");
					}
					network = new DirectedNetwork(node,gloablRepository);
				}else{
					Integer taxId = null;
					String name = null;
					if ( infos != null){
						try{
							taxId = Integer.valueOf(infos.get(Option.TaxId));
						}catch(Exception e){
							throw new RuntimeException("Can not get taxId for" + networkFile);
						}
						name = infos.get(infos.get(Option.NetworkName));
						if ( name == null) name = infos.get(Option.networkFile);
					}
					network = new DirectedNetwork(name,taxId,gloablRepository);
				}
			}
			
			if ( networkFile != null){
				BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(networkFile))));
				String line = null;
			
				while((line=br.readLine())!=null){
					String[] tokens = line.split("\t");
					Entity factor = new Entity(tokens [0]);
					
					if ( factors != null && !factors.contains(factor)) continue;
					Entity target = new Entity(tokens[1]);
					if ( groupId != null)
						network.add(factor, target,groupId);
					else
						network.add(factor,target);
				}
				br.close();
				if ( this.infos != null) network.setNetworkInfo(this.infos);
				network.getOptionValues().put(Option.networkFile, networkFile.toString());
		
			}
			if ( network == null) CroCoLogger.getLogger().warn("No network read. Neither networkFile nor networkInfo given.");
			if ( network != null) network.setNetworkInfo(this.infos);
			if ( network.getOptionValues() != null && network.getOptionValues().size() ==0) network.setNetworkInfo(this.infos);
					
			//else
			//	CroCoLogger.getLogger().info("Network read with " + network.getSize()  + " edges");
			return network;
		}
		
		
	}

	public static void writeNetworkHierachyFile(Network network, File outputNetworkFile) throws IOException{
		BufferedWriter bwNetwork =null;
		if ( outputNetworkFile.getName().endsWith(".gz")){
			bwNetwork = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(outputNetworkFile)) ));
		}else{
			bwNetwork = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(outputNetworkFile)) );
		}
		
		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			bwNetwork.write(edge.getFirst().getIdentifier() + "\t" + edge.getSecond().getIdentifier() + "\n");
		}
		bwNetwork.flush();
		bwNetwork.close();
	}
	public static void writeNetworkHierachyAnnotationFile(Network network, File annotationOutputFile) throws FileNotFoundException, IOException {
		BufferedWriter bwAnnotation =null;
		if ( annotationOutputFile.getName().endsWith(".gz")){
			bwAnnotation = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(annotationOutputFile)) ));
		}else{
			bwAnnotation = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(annotationOutputFile)) );
		}
		bwAnnotation.write("#Factor Target Annotation\n" );
		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			
			TIntObjectHashMap<List<Object>> annotation = network.getAnnotation(edgeId);
			if ( annotation != null){
				for(int annotationId : annotation.keys()){
					EdgeOption edgeType = Network.EdgeOption.values()[annotationId];
					for(  Object  o: annotation.get(annotationId)){
						bwAnnotation.write(edge.getFirst().getIdentifier() + "\t" + edge.getSecond().getIdentifier() + "\t" + edgeType.name() + "\t" +o.toString() + "\n" );
											
					}
					
				}
			}
			
		}
		
		bwAnnotation.flush();
		bwAnnotation.close();
	}
}
