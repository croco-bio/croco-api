package de.lmu.ifi.bio.crco.processor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
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
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.Intersect;
import de.lmu.ifi.bio.crco.operation.Parameter;
import de.lmu.ifi.bio.crco.operation.ReadNetwork;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Tuple;

public class CreateInnerNetworks {
	public static void main(String[] args) throws Exception{

		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("tmpFile").withArgName("FILE").isRequired().hasArgs(1).create("tmpFile"));
		options.addOption(OptionBuilder.withLongOpt("new").withDescription("Remove already created networks on inner nodes").create("new"));
	
			
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + CreateInnerNetworks.class.getName(), "", options, "", true);
			System.exit(1);
		}
		File tmpFile = new File(line.getOptionValue("tmpFile"));
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile));
		Boolean newInnerNodes = line.hasOption("new");
		CroCoLogger.getLogger().info("Temp file:\t" + tmpFile);
		CroCoLogger.getLogger().info("New inner nodes:\t" + newInnerNodes);
		
		LocalService service = new LocalService();
		
		NetworkHierachyNode root = service.getNetworkHierachy(null);
		
		CreateInnerNetworks network = new CreateInnerNetworks(service,bw,newInnerNodes);
		
		network.createNetwork(root);
		
		bw.flush();
		bw.close();
		
		System.out.println("Sleeping for 5sec");
		Thread.sleep(5000);
		
		System.out.println("Loading network into database");
		Statement stat = DatabaseConnection.getConnection().createStatement();
		String sql = String.format("LOAD DATA INFILE '%s' INTO TABLE Network (group_id,gene1,gene2)",tmpFile.toString());
		CroCoLogger.getLogger().info(sql);
		stat.execute(sql );
		stat.close();
		
		
		/*
		Stack<NetworkHierachyNode> stack = new Stack<NetworkHierachyNode>();
		stack.add(root);
	
	
		while(!stack.isEmpty()){
			NetworkHierachyNode top = stack.pop();
			Set<Integer> taxIds = getTaxIds(top);
			if ( taxIds.size() == 1 && top.getNumNetwork() >1){
				createNetwork(top);
			}
			if (top.getChildren() != null){
				for(NetworkHierachyNode child : top.getChildren()){
					stack.add(child);
				}
			}
		}*/
	}
	private LocalService service;
	private BufferedWriter bw ;
	private boolean newInnerNodes;
	private PreparedStatement updataStat;
	
	
	public CreateInnerNetworks(LocalService service, BufferedWriter bw, boolean newInnerNodes) throws Exception{
		this.service = service;
		this.bw = bw;
		this.newInnerNodes = newInnerNodes;
		
		updataStat = DatabaseConnection.getConnection().prepareStatement("UPDATE NetworkHierachy set has_network=1 where group_id = ?");
	}
	
	public Set<Integer> getTaxIds(NetworkHierachyNode root){
		HashSet<Integer> taxIds = new HashSet<Integer>();
		
		Stack<NetworkHierachyNode> stack = new Stack<NetworkHierachyNode>();
		stack.add(root);
		while(!stack.isEmpty()){
			NetworkHierachyNode top = stack.pop();
			if ( top.getTaxId() != null) taxIds.add(top.getTaxId());
			if (top.getChildren() != null){
				for(NetworkHierachyNode child : top.getChildren()){
					stack.add(child);
				}
			}
		}
		return taxIds;
	}
	public boolean uniqueTaxId(List<Network> networks){
		Integer taxId = null;
		for(Network network :networks){
			if ( taxId != null && !taxId.equals(network.getTaxId())){
				return false;
			}
			taxId = network.getTaxId();
		}
		return true;
	}
	public int netNetworks = 0;
	public Network createNetwork(NetworkHierachyNode root) throws Exception{
		if ( root.hasNetwork() && ( (root.getChildren() == null || root.getChildren().size() == 0) || newInnerNodes )) {
			
			ReadNetwork reader =new ReadNetwork();
			reader.setInput(ReadNetwork.NetworkHierachyNode, root);
			reader.setInput(ReadNetwork.GlobalRepository, false);
			reader.setInput(ReadNetwork.QueryService, service);
			CroCoLogger.getLogger().info("Read network:\t" + root.getName());
			return reader.operate();
		}else if ( root.getChildren() != null){
			List<Network> networks = new ArrayList<Network>();
			
			for(NetworkHierachyNode child : root.getChildren()){
				Network network = createNetwork(child);
				if ( network != null)
					networks.add(network);
			}
			
			if ( networks.size() == 0 || !uniqueTaxId(networks)) {
				updataStat.setInt(0,root.getGroupId());
				updataStat.execute();
				return null;
			}
			CroCoLogger.getLogger().info("Create networks:\t" + networks);
			Intersect intersect = new Intersect();
			intersect.setInputNetwork(networks);
			Network network = intersect.operate();
	
			for(int edgeId  : network.getEdgeIds()){
				Tuple<Entity, Entity> edge = network.getEdge(edgeId);
				bw.write(root.getGroupId() + "\t" + edge.getFirst().getIdentifier() + "\t"  +edge.getSecond().getIdentifier() + "\n" );
			
			}

			bw.flush();
			updataStat.setInt(1,root.getGroupId());
			updataStat.execute();
			
			
			return network;
		}else{
			CroCoLogger.getLogger().warn("Reached:\t" + root.getHierachyAsString());
			updataStat.setInt(0,root.getGroupId());
			updataStat.execute();
			return null;
		}
		
	//	System.out.println(root.getHierachyAsString() + "\t" + netNetworks++);
	}
}
