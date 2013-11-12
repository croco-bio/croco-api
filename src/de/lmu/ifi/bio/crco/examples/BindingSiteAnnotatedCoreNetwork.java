package de.lmu.ifi.bio.crco.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TransferredPeak;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;
import de.lmu.ifi.bio.crco.operation.ReadBindingNetwork;
import de.lmu.ifi.bio.crco.operation.Transfer;
import de.lmu.ifi.bio.crco.operation.TransferBindings;
import de.lmu.ifi.bio.crco.operation.Union;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Tuple;

public class BindingSiteAnnotatedCoreNetwork {
	
	
	public static void main(String[] args) throws Exception{
		
		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("humanNetworkRootId").withArgName("ID").isRequired().hasArgs(1).create("humanNetworkRootId"));
		options.addOption(OptionBuilder.withLongOpt("mouseNetworkRotoId").withArgName("ID").isRequired().hasArgs(1).create("mouseNetworkRotoId"));
		
		options.addOption(OptionBuilder.withLongOpt("liftOverExec").withArgName("FILE").withDescription("Lift over exceutable").isRequired().hasArgs(1).create("liftOverExec"));
		options.addOption(OptionBuilder.withLongOpt("chainFile").withArgName("FILE").withDescription("Chain file").isRequired().hasArgs(1).create("chainFile"));
		options.addOption(OptionBuilder.withLongOpt("minMatch").withDescription("Min match value (liftOver default = 0.1)").isRequired().hasArgs(1).create("minMatch"));	
		
		
		options.addOption(OptionBuilder.withLongOpt("outputHuman").isRequired().hasArgs(1).create("outputHuman"));
		options.addOption(OptionBuilder.withLongOpt("outputMouse").isRequired().hasArgs(1).create("outputMouse"));
		options.addOption(OptionBuilder.withLongOpt("outputConservation").isRequired().hasArgs(1).create("outputConservation"));
		
			
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + BindingSiteAnnotatedCoreNetwork.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		CroCoLogger.getLogger().info("Create Human/Mouse core network");
		
		
		File liftOver = new File(line.getOptionValue("liftOverExec") );
		if (! liftOver.canExecute()){
			System.err.println("Can not execture lift over:" + liftOver);
			System.exit(1);
		}
	
		File chainFile = new File(line.getOptionValue("chainFile"));
		if (! chainFile.isFile()){
			System.err.println(chainFile + " does not exist");
			System.exit(1);
		}
		Float minMatch = Float.valueOf(line.getOptionValue("minMatch"));
		
		Integer humanId = Integer.valueOf(line.getOptionValue("humanNetworkRootId"));//2658;
		Integer mouseId =  Integer.valueOf(line.getOptionValue("mouseNetworkRotoId"));//2954;
		File conservationOut = new File(line.getOptionValue("outputConservation"));
		File humanOut = new File(line.getOptionValue("outputHuman"));
		File mouseOut = new File(line.getOptionValue("outputMouse"));
		
		
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());

		Statement stat = DatabaseConnection.getConnection().createStatement();
		stat.execute("SELECT gene,gene_name FROM Gene where tax_id = 9606 or tax_id = 10090" );
		ResultSet res = stat.getResultSet();
		HashMap<String,String> idNameMapping = new HashMap<String,String>();
		while(res.next()){
			idNameMapping.put(res.getString(1),res.getString(2));
		}
		res.close();
		stat.close();
		
		NetworkHierachyNode hierachy = service.getNetworkHierachy(null);
		NetworkHierachyNode humanRoot = filter(hierachy,humanId);
		NetworkHierachyNode mouseRoot =  filter(hierachy,mouseId);
		
		CroCoLogger.getLogger().info(String.format("Read human networks (root group id: %d)" , humanRoot.getGroupId()));
		Network humanNetwork = getUnifiedNetwork(humanRoot,service);
		CroCoLogger.getLogger().info(String.format("Read human networks (root group id: %d)" , mouseRoot.getGroupId()));
		Network mouseNetwork = getUnifiedNetwork(mouseRoot,service);

		CroCoLogger.getLogger().info("Transfer mouse network");
		List<OrthologMappingInformation> orthologMappings = service.getOrthologMappingInformation(OrthologDatabaseType.InParanoid, Species.Human, Species.Mouse);
		orthologMappings.addAll(service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, Species.Human, Species.Mouse));
		

		CroCoLogger.getLogger().info("Transfer using:" + orthologMappings);
		
		Transfer transfer = new Transfer();
		transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
		transfer.setInput(Transfer.OrthologMappingInformation, orthologMappings);
		transfer.setInputNetwork(mouseNetwork);
		Network humanTransferredNetwork  =transfer.operate();
	

		TransferBindings bindingTransfer = new TransferBindings();
		bindingTransfer.setInput(TransferBindings.MinMatch, minMatch);
		bindingTransfer.setInput(TransferBindings.ChainFileFile, chainFile);
		bindingTransfer.setInput(TransferBindings.LiftOverExec, liftOver);
		bindingTransfer.setInputNetwork(humanTransferredNetwork);
	
		humanTransferredNetwork = bindingTransfer.operate();
		
		CroCoLogger.getLogger().info("Transferred network size:\t" + humanTransferredNetwork.getSize());
		
		CroCoLogger.getLogger().info("Write summary");
		BufferedWriter bwConserved = new BufferedWriter(new FileWriter(conservationOut));
		
		Set<Tuple<Entity, Entity>> edges = new HashSet<Tuple<Entity, Entity>>();
		
		CroCoLogger.getLogger().debug("Collecting edges");
		for(int edgeId: humanNetwork.getEdgeIds()){
			Tuple<Entity, Entity> edge = humanNetwork.getEdge(edgeId);
			edges.add(edge);
		}
		CroCoLogger.getLogger().debug("Human edges:" + edges.size());
		for(int edgeId: humanTransferredNetwork.getEdgeIds()){
			Tuple<Entity, Entity> edge = humanTransferredNetwork.getEdge(edgeId);
			edges.add(edge);
		}
		CroCoLogger.getLogger().debug(String.format("Additional edges: %d from %d", edges.size()- humanTransferredNetwork.size(),humanTransferredNetwork.size())  );
		bwConserved.write("Factor FactorName Target TargetName HumanSupport TFBSHumanSupport MouseSupport TFBSMouseTransferSupport TFBSCommonSupport\n");
		for(Tuple<Entity, Entity> edge : edges){
			
			Entity factor = edge.getFirst();
			Entity target = edge.getSecond();
			
			int supportHuman = 0;
			int maxHumanBindingSupport = 0;
			if ( humanNetwork.containsEdge(edge) ) {
				Integer humanEdgeId = humanNetwork.getEdgeId(edge);
				supportHuman= humanNetwork.getAnnotation(humanEdgeId, EdgeOption.GroupId).size();
				
				List<TFBSPeak> humanBindingSites = humanNetwork.getAnnotation(humanEdgeId,EdgeOption.BindingSite,TFBSPeak.class);
				maxHumanBindingSupport = Peak.getMaxBindingSupport((List)humanBindingSites).getSecond();
			}
			int supportMouse =0;
			int maxTransferredSupport= 0;
			if (humanTransferredNetwork.containsEdge(edge) ){
				Integer transferredEdgeId = humanTransferredNetwork.getEdgeId(edge);
				supportMouse = new HashSet<Integer>(humanTransferredNetwork.getAnnotation(transferredEdgeId,EdgeOption.GroupId,Integer.class)).size();
				List<TransferredPeak> mouseHumanTransferredBindingSites = humanTransferredNetwork.getAnnotation(transferredEdgeId,EdgeOption.TransferredSite,TransferredPeak.class);
				
				if ( mouseHumanTransferredBindingSites != null){
					maxTransferredSupport = Peak.getMaxBindingSupport((List)mouseHumanTransferredBindingSites).getSecond();
				}
			}
			int maxCommon = 0;
			if ( humanNetwork.containsEdge(edge) && humanTransferredNetwork.containsEdge(edge) ){
				Integer humanEdgeId = humanNetwork.getEdgeId(edge);
				Integer transferredEdgeId = humanTransferredNetwork.getEdgeId(edge);
				List<TFBSPeak> humanBindingSites = humanNetwork.getAnnotation(humanEdgeId,EdgeOption.BindingSite,TFBSPeak.class);
				List<TransferredPeak> mouseHumanTransferredBindingSites = humanTransferredNetwork.getAnnotation(transferredEdgeId,EdgeOption.TransferredSite,TransferredPeak.class);
				if ( mouseHumanTransferredBindingSites != null){
					maxCommon = Peak.getMaxBindingSupportOverlap((List)humanBindingSites, (List)mouseHumanTransferredBindingSites,50).getSecond();
				}
				
			}
			String str = String.format("%s %s %s %s %d %d %d %d %d", 
					factor.getName() , idNameMapping.get(factor.getName()) ,
					target.getName(),  idNameMapping.get(target.getName()) , 
					supportHuman , maxHumanBindingSupport,
					supportMouse , maxTransferredSupport,
					maxCommon
				);
			bwConserved.write(str + "\n");
			//bwConserved.write( factor.getName()  + "\t" + idNameMapping.get(factor.getName()) + "\t" + target.getName() + "\t" +  idNameMapping.get(target.getName())  +"\t" + supportHuman + "\t" +supportMouse  +  "\t" + "\n");
		}
		
	
		
		BufferedWriter bwHumanNetwork = new BufferedWriter(new FileWriter(humanOut));
		bwHumanNetwork.write("Factor FactorName Target TargetName Support TFBSSupport\n");
		for(int  edgeId : humanNetwork.getEdgeIds()){
		
			Tuple<Entity, Entity> edge = humanNetwork.getEdge(edgeId);
		
			Entity factor = edge.getFirst();
			Entity target = edge.getSecond();
			
			int support = humanNetwork.getAnnotation(edgeId, EdgeOption.GroupId).size();
			List<TFBSPeak> bindingSites = humanNetwork.getAnnotation(edgeId,EdgeOption.BindingSite,TFBSPeak.class);
			Integer maxBindingSuppport = Peak.getMaxBindingSupport((List)bindingSites).getSecond();
			String str = String.format("%s %s %s %s %d %d",
					factor.getName() , idNameMapping.get(factor.getName()) ,
					target.getName(),  idNameMapping.get(target.getName()) , 
					support,maxBindingSuppport
			);
					
			bwHumanNetwork.write(str+ "\n");
			
		}

		bwHumanNetwork.flush();
		bwHumanNetwork.close(); 
		bwConserved.flush();
		bwConserved.close();
		
		BufferedWriter bwMouseNetwork = new BufferedWriter(new FileWriter(mouseOut));
		bwMouseNetwork.write("Factor FactorName Target TargetName Support TFBSSupport\n");
		for(int  edgeId : mouseNetwork.getEdgeIds()){
			Tuple<Entity, Entity> edge = mouseNetwork.getEdge(edgeId);
			
			Entity factor = edge.getFirst();
			Entity target = edge.getSecond();
			
			int support = new HashSet<Integer>(mouseNetwork.getAnnotation(edgeId, EdgeOption.GroupId,Integer.class)).size();
			List<TFBSPeak> bindingSites = mouseNetwork.getAnnotation(edgeId,EdgeOption.BindingSite,TFBSPeak.class);
			Integer maxBindingSuppport = Peak.getMaxBindingSupport((List)bindingSites).getSecond();
			String str = String.format("%s %s %s %s %d %d",
					factor.getName() , idNameMapping.get(factor.getName()) ,
					target.getName(),  idNameMapping.get(target.getName()) , 
					support,maxBindingSuppport
			);
					
			bwMouseNetwork.write(str+ "\n");
		}

		bwMouseNetwork.flush();
		bwMouseNetwork.close();
	
		
	}

	private static NetworkHierachyNode filter(NetworkHierachyNode rootNode, Integer humanId) {
		Stack<NetworkHierachyNode> stack = new Stack<NetworkHierachyNode>();
		stack.add(rootNode);
		while(!stack.isEmpty()){
			NetworkHierachyNode top = stack.pop();

			if ( top.getGroupId().equals(humanId)) {
				return top;
			}
			if ( top.getChildren() != null) stack.addAll(top.getChildren());
		}
		throw new RuntimeException("Node not found");
	}
	public static Network getUnifiedNetwork(NetworkHierachyNode rootNode, QueryService service) throws Exception{
		Stack<NetworkHierachyNode> stack = new Stack<NetworkHierachyNode>();
		List<Network> networks = new ArrayList<Network>();
		stack.add(rootNode);
		while(!stack.isEmpty()){
			NetworkHierachyNode top = stack.pop();
			if (top.hasNetwork() && (top.getChildren() == null || top.getChildren().size() == 0)){
				ReadBindingNetwork reader = new ReadBindingNetwork();
				reader.setInput(ReadBindingNetwork.NetworkHierachyNode, top);
				reader.setInput(ReadBindingNetwork.QueryService, service);
				reader.setInput(ReadBindingNetwork.GlobalRepository, true);
				networks.add(reader.operate());
			}
			if ( top.getChildren() != null){
				stack.addAll(top.getChildren());
			}
		}
		CroCoLogger.getLogger().info(String.format("Reading: %d network",networks.size()));
		CroCoLogger.getLogger().info(String.format("Networks: %s",networks.toString()));
		Union union = new Union();
		union.setInputNetwork(networks);
		Network net =  union.operate();;
		
		int w = 0;
		for(int e : net.getEdgeIds()){
			List<TFBSPeak> annot = net.getAnnotation(e,EdgeOption.BindingSite,TFBSPeak.class);
			w+=annot.size();
		}
		CroCoLogger.getLogger().debug("Number of bindings:" + w);
		
		return net;
		
	}
}
