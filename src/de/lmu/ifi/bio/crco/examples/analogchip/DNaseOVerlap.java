package de.lmu.ifi.bio.crco.examples.analogchip;

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
import de.lmu.ifi.bio.crco.examples.BindingSiteAnnotatedCoreNetwork;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;
import de.lmu.ifi.bio.crco.operation.ReadBindingNetwork;
import de.lmu.ifi.bio.crco.operation.ReadNetwork;
import de.lmu.ifi.bio.crco.operation.Union;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

public class DNaseOVerlap {
	public static void main(String[] args) throws Exception{
		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("chipCellLineId").withArgName("ID").isRequired().hasArgs(1).create("chipCellLineId"));
		options.addOption(OptionBuilder.withLongOpt("dNaseId").withArgName("ID").isRequired().hasArgs(1).create("dNaseId"));
		options.addOption(OptionBuilder.withLongOpt("output").withArgName("output").isRequired().hasArgs(1).create("output"));
		
		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + BindingSiteAnnotatedCoreNetwork.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		Integer dnaseExpId = Integer.valueOf(line.getOptionValue("dNaseId"));
		Integer chipRootId = Integer.valueOf(line.getOptionValue("chipCellLineId"));
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
		
		ReadNetwork readNetwork =new ReadNetwork();
		readNetwork.setInput(ReadNetwork.QueryService, service);
		readNetwork.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(dnaseExpId,9606));
		CroCoLogger.getLogger().info("Read DNase experments");
		Network dNaseExp = readNetwork.operate();
		CroCoLogger.getLogger().info("DNase exp size:" + dNaseExp.size());
		NetworkHierachyNode chipNetwork = filter(service.getNetworkHierachy(null),chipRootId);
		Network chipUni = getUnifiedNetwork(chipNetwork,service);
		CroCoLogger.getLogger().info("Read ChIP experments");
		Set<Entity> factors = chipUni.getFactors();
		CroCoLogger.getLogger().info("Number of factors with chip experiment:" + factors.size());
		
		HashMap<Entity, Set<Entity>> d = dNaseExp.createFactorTargetNetwork();
		HashMap<Entity, Set<Entity>> c = chipUni.createFactorTargetNetwork();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(line.getOptionValue("output"))));
		
		Set<Entity> chipNodes = chipUni.getNodes();
		Set<Entity> dNaseNodes = dNaseExp.getNodes();
		
		bw.write("ID Name ChIP DNase overlap totalChIPNodes totalDNaseNodes\n");
		//Set<Entity> tmp2 = new HashSet<Entity>(chipUni.getNodes());
		//tmp2.retainAll(dNaseExp.getNodes());
		//bw.write(String.format("#nodes in DNase: %d nodes in ChIP: %d overlap: %d\n",dNaseExp.getNodes().size(),chipUni.getNodes().size(),tmp2.size()));
		for(Entity factor : c.keySet()){
			int totalChip = c.get(factor).size();
			int totalDnase = -1;
			int overlap = -1;
			if ( d.containsKey(factor)){
				totalDnase = d.get(factor).size();
				
				Set<Entity> tmp = new HashSet<Entity>(d.get(factor));
				tmp.retainAll(c.get(factor));
				
				overlap = tmp.size();
			}
			
			bw.write(String.format("%s %s %d %d %d %d %d\n",factor.getIdentifier(),idNameMapping.get(factor.getIdentifier()),totalChip,totalDnase,overlap,chipNodes.size(),dNaseNodes.size()));
		}
		bw.flush();
		bw.close();
	}
	public static Network getUnifiedNetwork(NetworkHierachyNode rootNode, QueryService service) throws Exception{
		Stack<NetworkHierachyNode> stack = new Stack<NetworkHierachyNode>();
		List<Network> networks = new ArrayList<Network>();
		stack.add(rootNode);
		while(!stack.isEmpty()){
			NetworkHierachyNode top = stack.pop();
			if (top.hasNetwork() && (top.getChildren() == null || top.getChildren().size() == 0)){
				ReadNetwork reader = new ReadNetwork();
				reader.setInput(ReadNetwork.NetworkHierachyNode, top);
				reader.setInput(ReadNetwork.QueryService, service);
				reader.setInput(ReadNetwork.GlobalRepository, true);
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
		
		
		return net;
		
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
}
