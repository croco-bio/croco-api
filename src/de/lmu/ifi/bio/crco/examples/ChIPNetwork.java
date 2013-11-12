package de.lmu.ifi.bio.crco.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;
import de.lmu.ifi.bio.crco.operation.GeneSetFilter;
import de.lmu.ifi.bio.crco.operation.ReadBindingNetwork;
import de.lmu.ifi.bio.crco.operation.ReadNetwork;
import de.lmu.ifi.bio.crco.operation.Transfer;
import de.lmu.ifi.bio.crco.operation.Union;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Tuple;

public class ChIPNetwork {
	public static void main(String[] args) throws Exception{
		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("humanNetworkGroupIds").withArgName("ID").isRequired().hasArgs().create("humanNetworkGroupIds"));
		options.addOption(OptionBuilder.withLongOpt("mouseNetworkGroupIds").withArgName("ID").isRequired().hasArgs().create("mouseNetworkGroupIds"));
		
		options.addOption(OptionBuilder.withLongOpt("liftOverExec").withArgName("FILE").withDescription("Lift over exceutable").isRequired().hasArgs(1).create("liftOverExec"));
		options.addOption(OptionBuilder.withLongOpt("chainFile").withArgName("FILE").withDescription("Chain file").isRequired().hasArgs(1).create("chainFile"));
		options.addOption(OptionBuilder.withLongOpt("minMatch").withDescription("Min match value (liftOver default = 0.1)").isRequired().hasArgs(1).create("minMatch"));	
		
		options.addOption(OptionBuilder.withLongOpt("outputHuman").isRequired().hasArgs(1).create("outputHuman"));
		options.addOption(OptionBuilder.withLongOpt("outputMouse").isRequired().hasArgs(1).create("outputMouse"));
		options.addOption(OptionBuilder.withLongOpt("outputConservation").isRequired().hasArgs(1).create("outputConservation"));

		options.addOption(OptionBuilder.withLongOpt("humanGeneList").hasArgs().create("humanGeneList"));
		options.addOption(OptionBuilder.withLongOpt("mouseGeneList").hasArgs().create("mouseGeneList"));

		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + ChIPNetwork.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		CroCoLogger.getLogger().info("Create ChIP network");
		Integer humanTextMiningNetworkGroupId = 1356;
		Integer mouseTextMiningNetworkGroupId = 1321;
		
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
		
		List<Integer> humanIds = new ArrayList<Integer>();
		for(String humanId : line.getOptionValues("humanNetworkGroupIds")){
			humanIds.add(Integer.valueOf(humanId));
		}
		System.out.println("Human network ids:\t" + humanIds);
		List<Integer> mouseIds = new ArrayList<Integer>();
		for(String mouseId : line.getOptionValues("mouseNetworkGroupIds")){
			mouseIds.add(Integer.valueOf(mouseId));
		}
		System.out.println("Mouse network ids:\t" + mouseIds);
		
		File conservationOut = new File(line.getOptionValue("outputConservation"));
		File humanOut = new File(line.getOptionValue("outputHuman"));
		File mouseOut = new File(line.getOptionValue("outputMouse"));
		Set<Entity> humanGeneList  = new HashSet<Entity>();
		if ( line.hasOption("humanGeneList")){
			for(String gene : line.getOptionValues("humanGeneList")){
				humanGeneList.add(new Entity(gene));
			}
		}
		Set<Entity> mouseGeneList  = new HashSet<Entity>();
		if ( line.hasOption("mouseGeneList")){
			for(String gene : line.getOptionValues("mouseGeneList")){
				mouseGeneList.add(new Entity(gene));
			}
		}
		System.out.println("Gene list (human):" + humanGeneList);
		System.out.println("Gene list (mouse):" + mouseGeneList);
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());

		ContextTreeNode leukocyteActivation = service.getContextTreeNode("GO:0008219");
		
		
		List<Entity> entities = service.getEntities(new Species(9606),null,null);
		HashMap<String,String> idNameMapping = new HashMap<String,String>();
		for(Entity entity : entities){
			idNameMapping.put(entity.getIdentifier(),entity.getName());
		}
		entities = service.getEntities(new Species(10090),null,null);
		for(Entity entity : entities){
			idNameMapping.put(entity.getIdentifier(),entity.getName());
		}
	//	System.out.println(textMiningNetwork.containsEdge(new Entity("ENSG00000102974"), new Entity("ENSG00000163346")));
		
		List<OrthologMappingInformation> orthologMappings = service.getOrthologMappingInformation(OrthologDatabaseType.InParanoid, new Species("Human",9606), new Species("Mouse",10090));
		//orthologMappings.addAll(service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, new Species("Human",9606), new Species("Mouse",10090)));
		
		CroCoLogger.getLogger().info(String.format("Read human networks (root group id: %s)" , humanIds.toString()));
		Network humanNetwork = getUnifiedNetwork(humanIds,9606,leukocyteActivation,service);
		CroCoLogger.getLogger().info(String.format("Unified human ChIP network: %d edges",humanNetwork.size()));
		
		CroCoLogger.getLogger().info(String.format("Read mouse networks (root group id: %s)" , mouseIds.toString()));
		Network mouseNetwork = getUnifiedNetwork(mouseIds,10090,leukocyteActivation,service);
		CroCoLogger.getLogger().info(String.format("Unified mouse ChIP network: %d edges",mouseNetwork.size()));

		//read human text mining
		ReadNetwork reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setInput(ReadNetwork.GlobalRepository,true);
		reader.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(humanTextMiningNetworkGroupId,9606));
		Network humanTextMiningNetwork = reader.operate();
		CroCoLogger.getLogger().info(String.format("Human Text-Mining network: %d edges",humanTextMiningNetwork.size()));
		//read mouse text mining
		reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setInput(ReadNetwork.GlobalRepository,true);
		reader.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(mouseTextMiningNetworkGroupId,10090));
		Network mouseTextMiningNetwork = reader.operate();
		CroCoLogger.getLogger().info(String.format("Mouse Text-Mining network: %d edges",mouseTextMiningNetwork.size()));
		//transfer mouse text mining to human
		Transfer transfer = new Transfer();
		transfer.setInput(Transfer.OrthologMappingInformation, orthologMappings);
		transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
		transfer.setInputNetwork(mouseTextMiningNetwork);
		Network mouseTransferredTextMiningNetwork = transfer.operate();

		//transfer  human text mining to mouse
		transfer.setInputNetwork(humanTextMiningNetwork);
		Network humanTransferredTextMiningNetwork = transfer.operate();
		
		Union union = new Union();
		union.setInputNetwork(mouseTransferredTextMiningNetwork,humanTextMiningNetwork);
		humanTextMiningNetwork = union.operate();
		
		union.setInputNetwork(humanTransferredTextMiningNetwork,mouseTextMiningNetwork);
		mouseTextMiningNetwork = union.operate();
		
		Set<Entity> factors = new HashSet<Entity>();
		for(int edgeId: humanNetwork.getEdgeIds()){
			Tuple<Entity, Entity> edge = humanNetwork.getEdge(edgeId);
			factors.add(edge.getFirst());
		}
		
		GeneSetFilter filter = new GeneSetFilter();
		filter.setInput(GeneSetFilter.genes, factors);
		filter.setInput(GeneSetFilter.filterType, GeneSetFilter.FilterType.FactorFilter);
		filter.setInputNetwork(humanTextMiningNetwork);
		humanTextMiningNetwork = filter.operate();
		
		factors = new HashSet<Entity>();
		for(int edgeId: mouseNetwork.getEdgeIds()){
			Tuple<Entity, Entity> edge = mouseNetwork.getEdge(edgeId);
			factors.add(edge.getFirst());
		}
		
		
		filter = new GeneSetFilter();
		filter.setInput(GeneSetFilter.filterType, GeneSetFilter.FilterType.FactorFilter);
		filter.setInput(GeneSetFilter.genes, factors);
		filter.setInputNetwork(mouseTextMiningNetwork);
		mouseTextMiningNetwork = filter.operate();
		
		CroCoLogger.getLogger().info(String.format("Human Text-Mining (filtered) network: %d edges",humanTextMiningNetwork.size()));
		
		
		union.setInputNetwork(humanNetwork,humanTextMiningNetwork);
		Network humanTMChiPNetwork = union.operate();
		CroCoLogger.getLogger().info(String.format("Intersect TM/ChIP human: %d edges",humanTMChiPNetwork.size()));
	
		writeFile(humanOut, humanTMChiPNetwork, humanTextMiningNetwork, humanNetwork, idNameMapping,humanGeneList);
			


		
		CroCoLogger.getLogger().info(String.format("Mouse Text-Mining (filtered) network: %d edges",mouseTextMiningNetwork.size()));
		
		union = new Union();
		union.setInputNetwork(mouseNetwork,mouseTextMiningNetwork);
		Network mouseTMChiPNetwork = union.operate();
		CroCoLogger.getLogger().info(String.format("Intersect TM/ChIP mouse: %d edges",mouseTMChiPNetwork.size()));
		
		
		transfer.setInput(Transfer.OrthologMappingInformation, orthologMappings);
		transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
	
		
		writeFile(mouseOut, mouseTMChiPNetwork, mouseTextMiningNetwork, mouseNetwork, idNameMapping,mouseGeneList);

		transfer.setInputNetwork(mouseNetwork);
		Network transferredMouseNetwork = transfer.operate();
		/*
		Intersect intersect = new Intersect();
		intersect.setInputNetwork(humanNetwork,humanTextMiningNetwork);
		Network humanCHIPTextMiningNetwork = intersect.operate();
	
		intersect.setInputNetwork(humanCHIPTextMiningNetwork,transferredMouseNetwork);
		*/
		
		union.setInputNetwork(transferredMouseNetwork,humanNetwork,humanTextMiningNetwork);
		Network commonNetwork = union.operate();
		
		//ContextTreeNode apoptotic = service.getContextTreeNode("GO:0042981");
		//List<Entity> g = service.getEntities(new Species(9606), apoptotic);
		
		if ( humanGeneList!= null && humanGeneList.size() > 0){
			filter.setInput(GeneSetFilter.genes, new HashSet<Entity>(humanGeneList));
			filter.setInput(GeneSetFilter.filterType, GeneSetFilter.FilterType.OnSideFilter);
			filter.setInputNetwork(commonNetwork);
			commonNetwork = filter.operate();
		}
		
		
		
		BufferedWriter bwCommon = new BufferedWriter(new FileWriter(conservationOut));
		bwCommon.write("Factor\tFactorName\tTarget\tTargetName\tTextMining\tHumanChiPSupport\tMouseChiPSupport\n");
		for(int  edgeId : commonNetwork.getEdgeIds()){
			Tuple<Entity, Entity> edge = commonNetwork.getEdge(edgeId);
			Entity factor = edge.getFirst();
			Entity target = edge.getSecond();
			
			boolean textMining = humanTextMiningNetwork.containsEdge(edge);
			boolean humanChip = humanNetwork.containsEdge(edge);
			boolean mouseChip = transferredMouseNetwork.containsEdge(edge);
			
			String str = String.format("%s\t%s\t%s\t%s\t%d\t%d\t%d",
					factor.getName() , idNameMapping.get(factor.getName()) ,
					target.getName(),  idNameMapping.get(target.getName()) , 
					textMining==true?1:0,humanChip==true?1:0,mouseChip==true?1:0
			);
					
			bwCommon.write(str+ "\n");
		}
		bwCommon.flush();
		bwCommon.close();
	}
	
	private static void writeFile(File out,Network withTMChiPNetwork, Network tmNetwork, Network chipNetwork,HashMap<String,String> idNameMapping, Set<Entity> geneSet) throws Exception{
		if ( geneSet!= null && geneSet.size() > 0){
			GeneSetFilter filter = new GeneSetFilter();
			filter.setInput(GeneSetFilter.genes, geneSet);
			filter.setInput(GeneSetFilter.filterType, GeneSetFilter.FilterType.OnSideFilter);
			filter.setInputNetwork(withTMChiPNetwork);
			withTMChiPNetwork = filter.operate();
		}
		
		BufferedWriter bwHumanNetwork = new BufferedWriter(new FileWriter(out));
		bwHumanNetwork.write("Factor\tFactorName\tTarget\tTargetName\tTextMining\tChiPSupport\tTFBSSupport\n");
		for(int  edgeId : withTMChiPNetwork.getEdgeIds()){
		
			Tuple<Entity, Entity> edge = withTMChiPNetwork.getEdge(edgeId);
		
			Entity factor = edge.getFirst();
			Entity target = edge.getSecond();
			
			List<Integer> groupIds = withTMChiPNetwork.getAnnotation(edgeId, EdgeOption.GroupId,Integer.class);
			boolean textMining = tmNetwork.containsEdge(edge);
			boolean chipSupport = chipNetwork.containsEdge(edge);
			
			Integer maxBindingSuppport  = 0;
			Integer chipSupportCount = 0;
			if ( chipSupport){
				int chipEdgeId = chipNetwork.getEdgeId(edge);
				
				chipSupportCount = chipNetwork.getAnnotation(chipEdgeId,EdgeOption.GroupId,Integer.class).size();
				
				List<TFBSPeak> bindingSites = chipNetwork.getAnnotation(chipEdgeId,EdgeOption.BindingSite,TFBSPeak.class);
				
				if ( bindingSites != null && bindingSites.size() > 0){
					maxBindingSuppport = Peak.getMaxBindingSupportPartial((List)bindingSites).getSecond();
				}
			}
			String str = String.format("%s\t%s\t%s\t%s\t%d\t%d\t%d",
					factor.getName() , idNameMapping.get(factor.getName()) ,
					target.getName(),  idNameMapping.get(target.getName()) , 
					textMining==true?1:0,chipSupportCount,maxBindingSuppport
			);
					
			bwHumanNetwork.write(str+ "\n");
			
		}
		bwHumanNetwork.flush();
		bwHumanNetwork.close();
	
	}
	
	public static Network getUnifiedNetwork(List<Integer> groupIds,Integer taxId,ContextTreeNode contextNode, QueryService service) throws Exception{
		//GeneSetFilter filter = new GeneSetFilter();
		//filter.setInput(filter.whiteList, whiteList);
		
		List<Network> networks = new ArrayList<Network>();
		
		for(Integer groupId : groupIds){
			
			ReadBindingNetwork reader = new ReadBindingNetwork();
		//	System.out.println(groupId);
		//	reader.setInput(ReadBindingNetwork.ContextTreeNode, contextNode);
			reader.setInput(ReadBindingNetwork.NetworkHierachyNode, new NetworkHierachyNode(groupId,taxId) );
			reader.setInput(ReadBindingNetwork.QueryService, service);
			reader.setInput(ReadBindingNetwork.GlobalRepository, true);
			Network network = reader.operate();
			//	CroCoLogger.getLogger().info(String.format("%s - %d edges",network.getName(),network.getSize()));
			//	filter.setInputNetwork(network);
			//	network = filter.operate();
			//	CroCoLogger.getLogger().info(String.format("%s - %d edges",network.getName(),network.getSize()));
			networks.add(network);
			
		}
		CroCoLogger.getLogger().info(String.format("Read: %d network",networks.size()));
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
