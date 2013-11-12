package de.lmu.ifi.bio.crco.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
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
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;

public class ChiPSeqMatrix {
	public static void main(String[] args) throws Exception{
		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("humanNetworkRootId").withArgName("ID").isRequired().hasArgs(1).create("humanNetworkRootId"));
		options.addOption(OptionBuilder.withLongOpt("mouseNetworkRotoId").withArgName("ID").isRequired().hasArgs(1).create("mouseNetworkRotoId"));

		options.addOption(OptionBuilder.withLongOpt("out").withArgName("FILE").isRequired().hasArgs(1).create("out"));

		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + CreateAnalogList.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		Integer humanId = Integer.valueOf(line.getOptionValue("humanNetworkRootId"));//5076;
		Integer mouseId =  Integer.valueOf(line.getOptionValue("mouseNetworkRotoId"));//;
		File out = new File(line.getOptionValue("out"));
		
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
		
		NetworkHierachyNode hierachy = service.getNetworkHierachy(null);
		List<NetworkHierachyNode> humanNetworks = filter(hierachy,humanId);
		List<NetworkHierachyNode> mouseNetworks =  filter(hierachy,mouseId); 
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
	
		writeData(bw,service,humanNetworks,"Human");
		writeData(bw,service,mouseNetworks,"Mouse");

		bw.close();
	}
	private static void writeData(BufferedWriter bw, QueryService service, List<NetworkHierachyNode> networks, String species) throws Exception{
		for(NetworkHierachyNode network : networks ){
			System.out.println(network);
			if (! network.hasNetwork()) continue;
			String target = null;
			String cellLine = null;
			List<Pair<Option, String>> infos = service.getNetworkInfo(network.getGroupId());
			for(Pair<Option, String> info : infos){
	
				if ( info.getFirst().equals(Option.AntibodyTargetMapped)){
					target = info.getSecond();
				}

				if ( info.getFirst().equals(Option.cellLine)){
					cellLine = info.getSecond();
				}
			}
			if ( target == null) throw new RuntimeException("No target mapping for:" + network);
			if ( cellLine == null) throw new RuntimeException("No target mapping for:" + network);
			bw.write(target + "\t" + species +  "\t" + cellLine  + "\n");
		}
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
