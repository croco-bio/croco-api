package de.lmu.ifi.bio.crco.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.Intersect;
import de.lmu.ifi.bio.crco.operation.ReadNetwork;
import de.lmu.ifi.bio.crco.operation.Shuffle;
import de.lmu.ifi.bio.crco.operation.SupportFilter;
import de.lmu.ifi.bio.crco.operation.Union;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.MathUtil;
import de.lmu.ifi.bio.crco.util.Tuple;

public class DegreeStat {
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
			lvFormater.printHelp(120, "java " + BindingSiteAnnotatedCoreNetwork.class.getName(), "", options, "", true);
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
				network = shuffle.operate();//Shuffle.shuffle(network,rnd);
			}
			
		//	CroCoLogger.getLogger().debug("Network size: " + network.size() + " " + shuffled.size());
			if (!networksByCellLine.containsKey(network.getName()) ){
				networksByCellLine.put(network.getName(), new ArrayList<Network>());
			}
			networksByCellLine.get(network.getName()).add(network);
		//	if ( k++ > 10) break;
		}
		CroCoLogger.getLogger().info("Create core networks");
		if ( method.equals("Core")){
			Network coreNetwork = getCore(networksByCellLine);
			HashMap<String,List<Network>> networksByCellLineNew= new HashMap<String,List<Network>> ();
			for(Entry<String,List<Network>> net : networksByCellLine.entrySet()){
				List<Network> l = new ArrayList<Network>();
				for(Network n : net.getValue()){
					Intersect intersect = new Intersect();
					intersect.setInputNetwork(n,coreNetwork);
					l.add(intersect.operate());
				}
				networksByCellLineNew.put(net.getKey(), l);
			}
			networksByCellLine = networksByCellLineNew;
		}
		
		CroCoLogger.getLogger().info("Estimating similarities");
		
		List<Entry<String,List<Network>>> list = new ArrayList<Entry<String,List<Network>>>(networksByCellLine.entrySet());
		List<String> names = new ArrayList<String>();
		float[][] matrix = new float[networksByCellLine.size()][networksByCellLine.size()];
		for(int i=0; i<list.size() ; i++ ){
			CroCoLogger.getLogger().info("Process: " + list.get(i).getKey());
			names.add(list.get(i).getKey());
			for(int j=i+1; j<list.size() ; j++ ){
				List<Float> overlapShuffle = new ArrayList<Float>();
				for(Network rep1 : list.get(i).getValue() ){
					for(Network rep2 : list.get(j).getValue() ){
						int[] stat = null;
						stat = getStat(rep1,rep2);
						float overlap = (float)stat[0]/ (float)stat[1];
						overlapShuffle.add(overlap);
					}
				}
				matrix[i][j] = MathUtil.mean(overlapShuffle);
				matrix[j][i] = MathUtil.mean(overlapShuffle);
			}
		}
		writeMatrix(new File(line.getOptionValue("output")),names,matrix);
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
	public static File writeMatrix(File file, List<String> labels,float[][] data) throws Exception{
		if ( labels.size() != data.length) throw new Exception("Labels != data length");
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		
		Locale.setDefault(Locale.US);
		for(int i = 0 ; i < labels.size() ; i++){
			if ( i > 0) bw.write("\t");
		
			bw.write(labels.get(i));
			
		}
		bw.write("\n");
		for(int i = 0 ; i<data.length;i++ ){
			for(int j = 0 ; j<data[i].length;j++ ){
				if ( j > 0) bw.write("\t");
				if ( i == j){
					bw.write(String.format("NaN") );
				}else{
					bw.write(String.format("%.2f",data[i][j]) );
				}
				
			}
			bw.write("\n");
		}
		bw.flush();
		bw.close();
		return file;
	}
	
	
	
	private static int getSize(HashMap<Entity,Integer> x){
		int ret = 0;
		for(Entity e : x.keySet()){
			ret+=x.get(e);
		}
		return ret;
	}
	public static float[] getStat(List<Integer> values, int max){
		float mean = 0;
		float variance = 0;
		
		float sum = 0;
		
		for(int value : values){
			sum+=value;
		}
		mean = sum/(float)max;
		for(int value : values){
			variance += Math.pow(value-mean, 2);
		}
		variance = variance/((float)values.size());
		
		return new float[]{mean,variance};
	}

	public static int[] getStat(Network network1, Network network2){
		int overlap = 0;
		int size = 0;
		for( int edgeId : network1.getEdgeIds()){
			size++;
			Tuple<Entity, Entity> edge = network1.getEdge(edgeId);
			
			if ( network2.containsEdge(edge)){
				overlap++;
			}
		}
		
		for( int edgeId : network2.getEdgeIds()){
			Tuple<Entity, Entity> edge = network2.getEdge(edgeId);
			
			if ( !network1.containsEdge(edge)){
				size++;
			}
		}
		/*
		if ( network1.getSize() > network2.getSize())
			size = network2.getSize();
		else
			size = network1.getSize();
		*/
		if ( size == 0) return new int[]{0,0};
		return new int[]{overlap,size};
	}
	public static  HashMap<Entity,Integer[]> degrees(Network network){
		 HashMap<Entity,Integer[]> ret = new  HashMap<Entity,Integer[]>();
		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			if (! ret.containsKey(edge.getFirst())){
				ret.put(edge.getFirst(),new Integer[]{0,0});
			}
			if (! ret.containsKey(edge.getSecond())){
				ret.put(edge.getSecond(),new Integer[]{0,0});
			}
			ret.get(edge.getFirst())[0] +=1; 
			ret.get(edge.getSecond())[1] +=1; 
		}
		
		return ret;
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
