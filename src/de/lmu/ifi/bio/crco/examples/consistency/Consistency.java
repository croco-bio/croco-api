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
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.examples.CreateAnalogList;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.GeneralOperation;
import de.lmu.ifi.bio.crco.operation.Intersect;
import de.lmu.ifi.bio.crco.operation.ReadNetwork;
import de.lmu.ifi.bio.crco.operation.Shuffle;
import de.lmu.ifi.bio.crco.operation.SupportFilter;
import de.lmu.ifi.bio.crco.operation.Transfer;
import de.lmu.ifi.bio.crco.operation.Union;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.MathUtil;
import de.lmu.ifi.bio.crco.util.Tuple;

public class Consistency {
	public static void main(String[] args) throws Exception{
		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("networkID1").withArgName("ID").isRequired().hasArgs(1).create("networkID1"));
		options.addOption(OptionBuilder.withLongOpt("networkID1").withArgName("ID").isRequired().hasArgs(1).create("networkID2"));

		options.addOption(OptionBuilder.withLongOpt("shuffle").create("shuffle"));
		options.addOption(OptionBuilder.withLongOpt("intersect").create("intersect"));

		options.addOption(OptionBuilder.withLongOpt("minNorm").withDescription("Norm on smaller network (default: min on combined network)").create("minNorm"));

		options.addOption(OptionBuilder.withLongOpt("out").withArgName("FILE").isRequired().hasArgs(1).create("out"));

		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + CreateAnalogList.class.getName(), "", options, "", true);
			System.exit(1);
		}
		Boolean intersect = line.hasOption("intersect");
		Boolean shuffle =line.hasOption("shuffle");
		Integer networkId1 = Integer.valueOf(line.getOptionValue("networkID1"));//5076;
		Integer networkId2 =  Integer.valueOf(line.getOptionValue("networkID2"));//;
		Boolean minNorm =line.hasOption("minNorm");
		File out = new File(line.getOptionValue("out"));
		CroCoLogger.getLogger().info("Shuffle:" + shuffle);
		CroCoLogger.getLogger().info("Output:" + out);
		CroCoLogger.getLogger().info("Min norm:" + minNorm);
		CroCoLogger.getLogger().info("Intersect:" + intersect);
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
		
		CroCoLogger.getLogger().info("Collect networks");
		NetworkHierachyNode hierachy = service.getNetworkHierachy(null);
		List<NetworkHierachyNode> hierachyNodes1 = filter(hierachy,networkId1);
		List<NetworkHierachyNode> hierachyNodes2 =  filter(hierachy,networkId2); 
		
		Transfer transfer = new Transfer();
		transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
		List<OrthologMappingInformation> mappings = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, new Species(9606), new Species(10090));
		transfer.setInput(Transfer.OrthologMappingInformation, mappings);
		Random rnd = new Random(0);
		
		List<Entry<String, List<Network>>> network1 = new ArrayList<Entry<String, List<Network>>>(getNetworksByCellLine(hierachyNodes1,service,null,shuffle,minNorm, rnd).entrySet());
		CroCoLogger.getLogger().info("Cell lines (1):" + network1.size());
		
		List<Entry<String, List<Network>>> network2;
		
		if (networkId1.equals(networkId2) ){
			network2 = network1;
		}else{
			network2= new ArrayList<Entry<String, List<Network>>>(getNetworksByCellLine(hierachyNodes2,service,transfer,shuffle,minNorm,rnd).entrySet());
		}
		
		CroCoLogger.getLogger().info("Cell line (2):" + network2.size());
		
		float[][] matrix =new float[network1.size()][network2.size()];
		
		CroCoLogger.getLogger().info("Computing matrix");
		for(int i =  0 ; i< network1.size();i++){
			CroCoLogger.getLogger().info("Process:" + network1.get(i).getKey());
			int start = 0;
			if (networkId1.equals(networkId2) ){
				start = i+1;
				matrix[i][i] = Float.NaN;
			}
			for(int j =  start ; j< network2.size();j++){
				List<Float> fracs = new ArrayList<Float>();
				for(Network h : network1.get(i).getValue()){
					for(Network m : network2.get(j).getValue()){
						int[] stat = getStat(h,m,minNorm);
						float frac = (float)stat[0]/(float)stat[1];
						fracs.add(frac);
						
					}
				}
				matrix[i][j] = MathUtil.mean(fracs);
				if (networkId1.equals(networkId2) ){
					matrix[j][i] = MathUtil.mean(fracs);
				}
			}
		}
		CroCoLogger.getLogger().info("Writing matrix");
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		CroCoLogger.getLogger().info("Write matrix");
		for(int i = 0 ; i < network2.size(); i++){
			if ( i > 0) bw.write("\t");
			bw.write(network2.get(i).getKey())  ;
		}
		bw.write("\n");
		for(int i = 0 ; i < matrix.length; i++){
			bw.write(network1.get(i).getKey() + "\t");
			for(int j = 0 ; j < matrix[i].length; j++){
				if ( j > 0) bw.write("\t");
				bw.write(matrix[i][j] +"");
			}	
			bw.write("\n");
		}
		
		bw.flush();
		bw.close();
	}
	public static int[] getStat(Network network1, Network network2, boolean minNorm){
		int overlap = 0;
		int size = 0;
		if ( network1.size() > network2.size()){
			Network tmp = network1;
			network1 = network2;
			network2 = tmp;
		}
		
		for( int edgeId : network1.getEdgeIds()){
			size++;
			Tuple<Entity, Entity> edge = network1.getEdge(edgeId);
			
			if ( network2.containsEdge(edge)){
				overlap++;
			}
		}
		if ( !minNorm){
			for( int edgeId : network2.getEdgeIds()){
				Tuple<Entity, Entity> edge = network2.getEdge(edgeId);
				
				if ( !network1.containsEdge(edge)){
					size++;
				}
			}
		}
		if ( size == 0) return new int[]{0,0};
		return new int[]{overlap,size};
	}
	private static HashMap<String,List<Network>> getNetworksByCellLine( List<NetworkHierachyNode>  networks,QueryService service,GeneralOperation operation, boolean shuffle,boolean intersect, Random rnd) throws Exception{
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
			//if ( k++ > 7) break;
			k++;
			if (shuffle && !intersect){ //when intersect shuffle after intersection
				Shuffle shuffleOperation = new Shuffle();
				shuffleOperation.setInput(Shuffle.RandomGenerator, rnd);
				shuffleOperation.setInputNetwork(network);
				network = shuffleOperation.operate();
			}
			if ( operation != null){
				String name = network.getName();
				operation.setInputNetwork(network);
				network = operation.operate();
				network.setName(name);
			}
		
			if (!networksByCellLine.containsKey(network.getName()) ){
				networksByCellLine.put(network.getName(), new ArrayList<Network>());
			}
			networksByCellLine.get(network.getName()).add(network);
		}
		if ( intersect){
			HashMap<String,List<Network>> n= new HashMap<String,List<Network>> ();
			for(Entry<String, List<Network>>e  : networksByCellLine.entrySet()){ //for each cell line
				List<Network> tmp = new ArrayList<Network>(); 
				if ( e.getValue().size() > 2){ //only if more than 2 experiments for cell line
					Union union = new Union(); //first unifiy
					union.setInputNetwork(e.getValue());
					Network unified = union.operate();
					SupportFilter filter = new SupportFilter(); //take intersections supported by 3/4 
					filter.setInput(SupportFilter.Support, Math.round((float)e.getValue().size()*0.75f));
					filter.setInputNetwork(unified);
					Network ret =filter.operate();
					if ( shuffle){ //shuffle unified/filterd network if needed
						Shuffle shuffleOperation = new Shuffle();
						shuffleOperation.setInput(Shuffle.RandomGenerator, rnd);
						shuffleOperation.setInputNetwork(ret);
						ret = shuffleOperation.operate();
					}
					tmp.add(ret);
				}else{
					for(Network network : e.getValue()){
						Network ret = network;
						if ( shuffle){ //shulfe if needed
							Shuffle shuffleOperation = new Shuffle();
							shuffleOperation.setInput(Shuffle.RandomGenerator, rnd);
							shuffleOperation.setInputNetwork(ret);
							ret = shuffleOperation.operate();
						}
						tmp.add(ret);
					}
				}
			
				n.put(e.getKey(),tmp);
			}
			networksByCellLine = n; //replace 
		}
		CroCoLogger.getLogger().info("Number of networks:" + k);
		return networksByCellLine;
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
