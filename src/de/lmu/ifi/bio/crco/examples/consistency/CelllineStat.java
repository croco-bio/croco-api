package de.lmu.ifi.bio.crco.examples.consistency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
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
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.Intersect;
import de.lmu.ifi.bio.crco.operation.ReadNetwork;
import de.lmu.ifi.bio.crco.operation.Shuffle;
import de.lmu.ifi.bio.crco.operation.SupportFilter;
import de.lmu.ifi.bio.crco.operation.Union;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

public class CelllineStat {
	public static void main(String[] args) throws Exception{
		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("networkRootId").withArgName("ID").isRequired().hasArgs(1).create("networkRootId"));
		options.addOption(OptionBuilder.withLongOpt("output").isRequired().hasArgs(1).create("output"));
		options.addOption(OptionBuilder.withLongOpt("method").withArgName("Shuffle|Core").isRequired().hasArgs(1).create("method"));
			
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + CelllineStat.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		Integer rootId = Integer.valueOf(line.getOptionValue("networkRootId"));//2658;
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());

		NetworkHierachyNode hierachy = service.getNetworkHierachy(null);
		List<NetworkHierachyNode> networks = filter(hierachy,rootId);
		Random rnd = new Random(0);
		String method = line.getOptionValue("method");
		
		HashMap<String,List<Network>> networksByCellLine= new HashMap<String,List<Network>> ();
		CroCoLogger.getLogger().info("Reading networks");
		int k = 0;
		for(NetworkHierachyNode node : networks){
			if ( !node.hasNetwork()) continue;
			ReadNetwork reader = new ReadNetwork();
			reader.setInput(ReadNetwork.NetworkHierachyNode, node);
			reader.setInput(ReadNetwork.QueryService, service);
			reader.setInput(ReadNetwork.GlobalRepository, true);
			Network network = reader.operate();
			if ( method.equals("Shuffle")){
				Shuffle shuffle = new Shuffle();
				shuffle.setInput(Shuffle.RandomGenerator, rnd);
				shuffle.setInputNetwork(network);
				network = shuffle.operate();
			}
			
			if (!networksByCellLine.containsKey(network.getName()) ){
				networksByCellLine.put(network.getName(), new ArrayList<Network>());
			}
			networksByCellLine.get(network.getName()).add(network);
		}
		CroCoLogger.getLogger().info("Create core networks");
		File file = new File(line.getOptionValue("output"));
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		
		Network coreNetwork = getCore(networksByCellLine);
		
		for(Entry<String,List<Network>> net : networksByCellLine.entrySet()){
			for(Network n : net.getValue()){
				Intersect intersect = new Intersect();
				intersect.setInputNetwork(n,coreNetwork);
				Network intersectedNetwork = intersect.operate();
				float frac = (float)n.size() / (float)intersectedNetwork.size();
				bw.write(n.getName() + "\t" + n.size() + "\t" + intersectedNetwork.size() + "\t" + frac + "\n");
			}
				
		}
		
		bw.flush();
		bw.close();
		
	}
	public static Network getCore(HashMap<String,List<Network>> networks) throws Exception{
		Union union = new Union();
		List<Network> nets = new ArrayList<Network>();
		for(Entry<String,List<Network>> net : networks.entrySet()){
			nets.addAll(net.getValue());
		}
		union.setInputNetwork(nets);
		Network unified = union.operate();
		
		SupportFilter filter = new SupportFilter();
		int sup = nets.size()/2;
		filter.setInput(SupportFilter.Support, sup);
		filter.setInputNetwork(unified);
		return filter.operate();
		
	}
	private static List<NetworkHierachyNode> filter(NetworkHierachyNode rootNode, Integer humanId) {
		Stack<NetworkHierachyNode> stack = new Stack<NetworkHierachyNode>();
		stack.add(rootNode);
		while(!stack.isEmpty()){
			NetworkHierachyNode top = stack.pop();
			if ( top.getGroupId().equals(humanId)) {
				Stack<NetworkHierachyNode> ofInterest = new Stack<NetworkHierachyNode>();
				List<NetworkHierachyNode> ret = new ArrayList<NetworkHierachyNode>();
				ofInterest.add(top);
				while(!ofInterest.isEmpty()){
					NetworkHierachyNode i = ofInterest.pop();
					ret.add(i);
					if ( i.getChildren() != null) ofInterest.addAll(i.getChildren());
				}
				return ret;
				
			}
			if ( top.getChildren() != null) stack.addAll(top.getChildren());
		}
		
		throw new RuntimeException("Node not found");
	}
}
