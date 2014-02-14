package de.lmu.ifi.bio.crco.processor.hierachy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
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
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.stat.PairwiseFeatures;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;
import de.lmu.ifi.bio.crco.util.Tuple;

/**
 * Processes the file based network hierarchy.
 * @author Robert Pesch
 *
 */
public class NetworkHierachy  {
	/**
	 * Imports a file based croco hierarchy into a SQL database.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("repositoryDir").withDescription("Repository directory").isRequired().hasArgs(1).create("repositoryDir"));
		options.addOption(OptionBuilder.withLongOpt("tmpDir").withDescription("Temporary directory").hasArgs(1).create("tmpDir"));
		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + NetworkHierachy.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		
		File repositoryDir = new File(line.getOptionValue("repositoryDir"));
		File tmpDir = null;
		if ( line.hasOption("tmpDir")) tmpDir = new File(line.getOptionValue("tmpDir"));
		File networkFile =File.createTempFile("networks.", ".croco",tmpDir) ;
		File statFile =File.createTempFile("stat.", ".croco",tmpDir) ;
		File annotationFile = File.createTempFile("annotation.", ".croco",tmpDir);
		
		CroCoLogger.getLogger().info(String.format("Repository dir: %s",repositoryDir));
		CroCoLogger.getLogger().info(String.format("Temp networkFile: %s",networkFile));
		CroCoLogger.getLogger().info(String.format("Temp statFile: %s",statFile));
		CroCoLogger.getLogger().info(String.format("Annotation file: %s",annotationFile));
		
		CroCoLogger.getLogger().info("Cleaning data");
		Connection connection = DatabaseConnection.getConnection();
		Statement stat = connection.createStatement();
		stat.execute("DELETE FROM NetworkHierachy");
		stat.execute("DELETE FROM Network");
		stat.execute("DELETE FROM NetworkOption");
		stat.execute("DELETE FROM NetworkSimilarity");
		stat.execute("DELETE FROM Network2Binding");
		
		stat.close();
		
		
		try{
			NetworkHierachy hierachy = new NetworkHierachy();
			
			
			hierachy.processHierachy(repositoryDir, hierachy.new NetworkProcessor(repositoryDir,connection, networkFile,statFile,annotationFile), hierachy.new SubFolderProcess(connection));
			
		}catch(Exception e){
			throw new RuntimeException(e);
		}finally{
			connection.close();
			networkFile.delete();
			statFile.delete();
		}

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
		int nextFreeId =rootId;
		for(File file : repositoryDir.listFiles()){
			files.add(file);
			parentIds.add(nextFreeId);
		}
		nextFreeId = nextFreeId+1;
		
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
				File statFile = new File(file.toString().replace(".network.gz", ".stat"));
				File annotationFile = new File(file.toString().replace(".network.gz", ".annotation.gz"));
				
				if ( networkFileHandler != null)networkFileHandler.process(currentRootId,nextFreeId++, file, infoFile,statFile,annotationFile);
				
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
				
				for(File f : file.listFiles()) {
					if ( ignoreList.contains(f.getName())){
						CroCoLogger.getLogger().debug(String.format("Ignore sub folder: %s", f.toString()));
						continue;
					}
					files.add(f);
					parentIds.add(nextFreeId);
				}
				File statFile = new File(file.toString().replace(".network.gz", ".stat"));
				if ( folderHandler != null) folderHandler.process(currentRootId,nextFreeId++ , file, infoFile,statFile,null);
				
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
		public void process(Integer rootId, Integer networkId, File networkFile, File infoFile, File statFile, File annotationFile) throws Exception;
		public void finish() throws Exception;
		
	}
	class SubFolderProcess implements CroCoRepositoryProcessor{
		private PreparedStatement hierachy;
		public SubFolderProcess(Connection connection) throws SQLException{
			this.hierachy =  connection.prepareStatement("INSERT INTO NetworkHierachy(group_id, parent_group_id,name,tax_id,has_network,network_type,network_file_location) values(?,?,?,?,?,?,?)");
		}
		
		@Override
		public void init(Integer rootId) throws Exception {}

		@Override
		public void process(Integer rootId, Integer networkId, File networkFile, File infoFile, File statFile,File annotationFile) throws SQLException,IOException {
			HashMap<Option, String> infoAnnotation = null;
			if ( infoFile.exists()){
				infoAnnotation = readInfoFile(infoFile);
			}
			
			hierachy.setInt(1,networkId);
			hierachy.setInt(2, rootId);
			if ( infoAnnotation != null){
				hierachy.setString(3, infoAnnotation.get(Option.NetworkName));
				hierachy.setInt(4,Integer.valueOf(infoAnnotation.get(Option.TaxId)));
			}else{
				hierachy.setString(3, networkFile.getName());
				hierachy.setNull(4,java.sql.Types.INTEGER);
			}
			
			hierachy.setBoolean(5, false);
			hierachy.setNull(6, java.sql.Types.INTEGER);
			hierachy.setNull(7, java.sql.Types.VARCHAR);
			hierachy.execute();
			
		}

		@Override
		public void finish() throws Exception {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	class NetworkProcessor implements CroCoRepositoryProcessor{
		private PreparedStatement hierachy;
		private PreparedStatement networkOptions;
		
		private List<File> statFiles;
		private BufferedWriter bwNetwork;
		private BufferedWriter bwAnnotation;
		private BufferedWriter bwStat;
		
		private int counter = 0;
		
		private Connection connection;
		
		private File repositoryDir;
		private File networkFile;
		private File statFile;
		private File annotationFile;
		
		
		public NetworkProcessor(File repositoryDir,Connection connection, File networkFile,File statFile, File annotationFile) throws Exception{
			this.repositoryDir = repositoryDir;
			this.networkFile = networkFile;
			this.annotationFile = annotationFile;
			
			this.statFiles = new ArrayList<File>();
			
			this.connection = connection;
			this.statFile = statFile;
			this.hierachy = connection.prepareStatement("INSERT INTO NetworkHierachy(group_id, parent_group_id,name,tax_id,has_network,network_type,network_file_location) values(?,?,?,?,?,?,?)");
			
			
			bwStat = new BufferedWriter(new FileWriter(statFile));
			bwNetwork = new BufferedWriter(new FileWriter(networkFile));
			bwAnnotation = new BufferedWriter(new FileWriter(annotationFile));
			networkOptions = connection.prepareStatement("INSERT INTO NetworkOption(option_id,group_id,value) values(?,?,?)");

		}
		private void addtoNetwork(int groupId, File file) throws IOException{
			BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
			String line = null;
			while((line=br.readLine())!=null){
				bwNetwork.write(String.format("%d\t%s\n",groupId,line));
			}
			br.close();
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
		public void process(Integer rootId ,Integer networkId, File networkFile, File infoFile, File statFile, File annotationFile) throws Exception {
			CroCoLogger.getLogger().debug("Process:" + networkFile);
			
			if ( counter++ % 300 == 0){
				hierachy.executeBatch();
				networkOptions.executeBatch();
			}
			if ( !statFile.exists()){
				CroCoLogger.getLogger().warn(String.format("Stat file %s does not exist",statFile));
			}else{
				this.statFiles.add(statFile);
			}
			
			
			//Integer newRootId = groupIdCounter++;
			HashMap<Option, String> infoAnnotations = readInfoFile(infoFile);
			hierachy.setInt( 1, networkId);
			hierachy.setInt(2, rootId);
			hierachy.setString(3, infoAnnotations.get(Option.NetworkName));
			hierachy.setInt(4,Integer.valueOf(infoAnnotations.get(Option.TaxId)));
			hierachy.setBoolean(5, true);
			hierachy.setInt(6, NetworkType.valueOf(infoAnnotations.get(Option.NetworkType)).ordinal());
			hierachy.setString(7, networkFile.toString());
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
			bwNetwork.flush();
			bwAnnotation.flush();
			hierachy.addBatch();
			addtoNetwork(networkId,networkFile);
			if ( annotationFile != null && annotationFile.exists()){
				addToAnnotation(networkId,annotationFile);
			}
			
			
		}


		@Override
		public void init(Integer rootId) throws Exception {
			//root
			hierachy.setInt(rootId, 1);
			hierachy.setInt(2, 0);
			hierachy.setString(3, "Root node");
			hierachy.setNull(4,java.sql.Types.INTEGER);
			hierachy.setBoolean(5, false);
			hierachy.setNull(6, java.sql.Types.INTEGER);
			hierachy.setNull(7, java.sql.Types.VARCHAR);
			hierachy.addBatch();
		}


		@Override
		public void finish() throws Exception {
			hierachy.executeBatch();
			hierachy.close();
			bwNetwork.flush();
			bwNetwork.close();
			bwAnnotation.flush();
			bwAnnotation.close();
			
			HashMap<String,Integer> fileIdMapping =  new HashMap<String,Integer>();
			Statement stat = connection.createStatement();
			stat.execute("SELECT group_id, network_file_location FROM NetworkHierachy where has_network = 1" );
			ResultSet res = stat.getResultSet();
			while(res.next()){
				Integer groupId = res.getInt(1);
				File file = new File(res.getString(2));
				
				fileIdMapping.put(file.toString().replace(repositoryDir.toString(), ""), groupId);
			}
			res.close();
			stat.close();
			Locale.setDefault(Locale.US);
			CroCoLogger.getLogger().info("Processing stat files");
			
			for(File file : statFiles){
				CroCoLogger.getLogger().debug(String.format("Process: %s",file.getName()));
				HashMap<Pair<String, Option>, Float> computations  = PairwiseFeatures.readStatFile(repositoryDir,file);
				int i = 0;
				for(Entry<Pair<String, Option>, Float>  e: computations.entrySet()){
					Option option =e.getKey().getSecond();
					String file1 = file.toString().replace(repositoryDir.toString(), "").replace(".stat", ".network.gz");
					String file2 = e.getKey().getFirst();
					Float value = e.getValue();
					Integer groupId1 = fileIdMapping.get(file1);
					Integer groupId2 = fileIdMapping.get(file2);
					if ( groupId1 == null)  {
						CroCoLogger.getLogger().debug(String.format("No id mapping for %s in %s",file1,file));
						continue;
					}
					if ( groupId2 == null)  {
						CroCoLogger.getLogger().debug(String.format("No id mapping for %s in %s",file2,file));
						continue;
					}
					bwStat.write(String.format("%d\t%d\t%d\t%f\n",groupId1,groupId2,option.ordinal(),value));
					i++;
					if (!groupId1.equals(groupId2) && !option.equals(Option.explainability)){ //TODO: solve that!
						bwStat.write(String.format("%d\t%d\t%d\t%f\n",groupId2,groupId1,option.ordinal(),value));
						
					}
				}
				CroCoLogger.getLogger().info(String.format("Imported %d of %d pair-wise stat information from %s",i,computations.size(),file));
				bwStat.flush();
			}
		
			
			bwStat.flush();
			bwStat.close();
			
			System.out.println("Sleeping for 5sec");
			Thread.sleep(5000);
			
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
		}
		
	}

	public static void processHierachy(){
		
	}
	

	public static Network getNetwork(File infoFile, File networkFile,boolean gloablRepository) throws Exception {
		HashMap<Option, String> infos = readInfoFile(infoFile);
		if ( networkFile != null) infos.put(Option.networkFile, networkFile.toString());
		return getNetwork(networkFile,infos,gloablRepository);
	}
	private static Network getNetwork(File networkFile,HashMap<Option,String> infos, boolean gloablRepository) throws Exception{
		Integer taxId = null;
		try{
			taxId = Integer.valueOf(infos.get(Option.TaxId));
		}catch(Exception e){
			throw new RuntimeException("Can not get taxId for" + networkFile);
		}
		String name = infos.get(infos.get(Option.NetworkName));
		if ( name == null) name = infos.get(Option.networkFile);
		Network network = new DirectedNetwork(name,taxId,gloablRepository);
		network.setNetworkInfo(infos);
		
		if ( networkFile != null) readNetwork(network,networkFile);
		
		return network;
	}
	private static HashMap<Option,String> readInfoFile(File infoFile) throws IOException{
		HashMap<Option,String> ret = new HashMap<Option,String> ();
		BufferedReader br =new BufferedReader(new FileReader(infoFile));
		String line = null;
		while(( line = br.readLine())!=null){
			String[] tokens = line.split(":");
			Option option = Option.valueOf(tokens[0].trim());
			String value = tokens[1].trim();
			ret.put(option, value);
		}
		br.close();
		return ret;
	}
	public static void writeNetworkHierachyFile(DirectedNetwork network, File outputNetworkFile) throws Exception{
		BufferedWriter bwNetwork = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(outputNetworkFile)) ));
		network.addNetworkInfo(Option.networkFile, outputNetworkFile.toString());
		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			bwNetwork.write(edge.getFirst().getIdentifier() + "\t" + edge.getSecond().getIdentifier() + "\n");
		}
		bwNetwork.flush();
		bwNetwork.close();
	}
	public static void readNetwork(Network network, File file,Set<Entity> factors) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
		String line = null;
		while((line=br.readLine())!=null){
			String[] tokens = line.split("\t");
			Entity factor = new Entity(tokens [0]);
			if (!factors.contains(factor)) continue;
			Entity target = new Entity(tokens[1]);
			network.add(factor, target);
		}
		br.close();
	}
	public static void readNetwork(Network network, File file) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
		String line = null;
		while((line=br.readLine())!=null){
			String[] tokens = line.split("\t");
			Entity factor = new Entity(tokens [0]);;
			Entity target = new Entity(tokens[1]);
			network.add(factor, target);
		}
		br.close();
	}
}
