package de.lmu.ifi.bio.crco.cluster;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;

public class Cluster<E extends Object> {
	
	private Double score(Node<Clusterable<E>> from, Node<Clusterable<E>> to, HashMap<Pair<Clusterable<E>, Clusterable<E>>, Double> pairs) {
		int sum = 0;
		int k = 0;
		for(Clusterable<E> g1 : from.getLeafs()){
			for(Clusterable<E> g2 : to.getLeafs()){
				Pair<Clusterable<E>,Clusterable<E>> p = new Pair<Clusterable<E>,Clusterable<E>>(g1,g2);
			
				Double s = pairs.get(p);
				if ( s == null) throw new RuntimeException("No score for:\t" + p);
				sum+=s;
				k++;
			}
		}
		return (double)sum/(double)k;
	}
	public List<List<Clusterable<E>>> group( HashMap<Pair<Clusterable<E>,Clusterable<E>>,Double> pairs, List<Clusterable<E>> data, Float threshold){

		CroCoLogger.getLogger().debug("Group element");
		HashMap<Clusterable<E>,Set<Clusterable<E>>> graph = new HashMap<Clusterable<E>,Set<Clusterable<E>>> ();
		for(Clusterable<E> from : data){
			for(Clusterable<E> to : data){
				Pair<Clusterable<E>,Clusterable<E>> p = new Pair<Clusterable<E>,Clusterable<E>>(from,to);
				
				Double score = pairs.get(p);
				if ( score >=threshold){
					if (! graph.containsKey(from)){
						graph.put(from, new HashSet<Clusterable<E>>());
					}
					if (! graph.containsKey(to)){
						graph.put(to, new HashSet<Clusterable<E>>());
					}
					graph.get(from).add(to);
					graph.get(to).add(from);
					
				}
			}		
		}
		CroCoLogger.getLogger().debug("Finding clusters");
		HashSet<Clusterable<E>> procssed = new HashSet<Clusterable<E>>();
		
		List<List<Clusterable<E>>> groups = new ArrayList<List<Clusterable<E>>>();
		
		
		for(Clusterable<E> d : data){
			if ( procssed.contains(d)) continue;
			
			
			Stack<Clusterable<E>> stack = new Stack<Clusterable<E>>();
			Set<Clusterable<E>> group = new HashSet<Clusterable<E>>();
			
			stack.add(d);
			while(!stack.isEmpty()){
				Clusterable<E> top = stack.pop();
				if ( procssed.contains(top)) continue;
				group.add(top);
				procssed.add(top);
				for(Clusterable<E> child : graph.get(top)){
					if ( procssed.contains(child)) continue;
					stack.add(child);
				}
			}
			
			groups.add(new ArrayList<Clusterable<E>>(group));
		}
		
		return groups;
	}
	
	public List<Node<Clusterable<E>>> cluster( HashMap<Pair<Clusterable<E>,Clusterable<E>>,Double> pairs, List<Clusterable<E>> data, Float threshold){
		List<Node<Clusterable<E>>> clusters = new ArrayList<Node<Clusterable<E>>>();
		int clusterId = 0;
		CroCoLogger.getLogger().info("Cluster");
		for(Clusterable<E> d : data){
			Node<Clusterable<E>> node = new Node<Clusterable<E>>(clusterId++,d);
			clusters.add(node);
		}
		int k = 0;
		while(clusters.size() > 0){

			CroCoLogger.getLogger().debug(String.format("Step:%d; Number of clusters:%d", k++, clusters.size() ));

			Double mScore = null;
			Integer bestI = null;
			Integer bestJ = null;
			
			for(int i = 0 ; i< clusters.size() ; i++){
				Node<Clusterable<E>> from = clusters.get(i);

				for(int j = 0 ; j< clusters.size() ; j++){
					if ( i == j) continue;
					Node<Clusterable<E>> to = clusters.get(j);
					
					Double score = score(from,to,pairs);
					
					if ( score == null) throw new RuntimeException("Score is null");
					if (score > threshold &&  (mScore == null || score > mScore)){

						mScore = score;
						bestI = i;
						bestJ = j;
					}

				}	
				
	
				
			}
			
			if ( bestI == null ) break;
	
			Link<Clusterable<E>> link = new Link<Clusterable<E>>(mScore, clusters.get(bestJ));
			clusters.get(bestI).links.add(link);
			clusters.remove((int)bestJ);
			
		}

		return clusters;
		
	}
	public void writeClusters(File file, List<Node<E>> clusters) throws Exception{
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for(Node<E> cluster : clusters){
		
			if ( cluster.getLeafs().size() == 1){
				System.out.println("Skipping: " + cluster);
				continue;
			}
			Stack<Node<E>> nodes = new Stack<Node<E>>();
			nodes.add(cluster);
			while(!nodes.isEmpty()){
				Node<E> top = nodes.pop();
				for(Link<E> node : top.links){
					nodes.add(node.toNode);
					bw.write(cluster.nodeId  + "\t" + node + "\t" + node.score + "\n");
				}
			}
			
		}
		bw.flush();
		bw.close();
	}
	public void printCluster(Node<E> toPrint){
		Stack<String> whiteSpace = new Stack<String>();
		Stack<Node<E>> nodes = new Stack<Node<E>>();
		whiteSpace.add("--");
		nodes.add(toPrint);
		while(!nodes.isEmpty()){
			Node<E> top = nodes.pop();
			String currentWhiteScape = whiteSpace.pop();
			System.out.println(currentWhiteScape + top.getLeafs());
			for(Link<E> node : top.links){
				System.out.println(currentWhiteScape + "(" +  node + "\t" + node.toNode + "\t" + node.score);
				whiteSpace.add(currentWhiteScape + "--");
				nodes.add(node.toNode);
		
			}
		}
		
	}
	private static List<Clusterable<NetworkHierachyNode>>  readData(LocalService service) throws Exception{
		Statement stat = service.getConnection().createStatement();
		
		List<Clusterable<NetworkHierachyNode>> ret = new ArrayList<Clusterable<NetworkHierachyNode>>();
		
		String sql = String.format("SELECT nh.group_id , parent_group_id , name, has_network, tax_id,database_identifier_id, nn.nodes   FROM NetworkHierachy nh JOIN NumberNodes nn on nn.group_id = nh.group_id where has_network = 1 and tax_id=9606");
		CroCoLogger.getLogger().debug(sql);
		stat.execute(sql);
		ResultSet res = stat.getResultSet();
		while(res.next()){
			Integer groupId = res.getInt(1);
			Integer parentGroupId = res.getInt(2);
			String name = res.getString(3);
			Integer taxId = res.getInt(5);
			Integer nodes = res.getInt(7);
			
			NetworkHierachyNode nh = new NetworkHierachyNode(null,groupId,parentGroupId,name,true,taxId,null);
			ret.add(new Clusterable<NetworkHierachyNode>(nh,nodes));
		}
		

		return ret;
	}
	
	private HashMap<Pair<Clusterable<E>,Clusterable<E>>,Double>  computeSimMatrix(List<Clusterable<E>> data) throws Exception{

		
		
		HashMap<Pair<Clusterable<E>,Clusterable<E>>,Double> pairs = new  HashMap<Pair<Clusterable<E>,Clusterable<E>>,Double> ();
		for(int i = 0 ; i< data.size() ; i++){
			Clusterable<E> source = data.get(i) ;
			if ( i%5==0)
			CroCoLogger.getLogger().debug("Processing:\t" + source.getData().toString());


			for(int j =  i ; j< data.size() ; j++){
				//	if ( i == j) continue;
				Clusterable<E> target = data.get(j) ;
				double sim = 0;
				if ( target.getValue() > source.getValue()){
					sim = (float)source.getValue() /(float) target.getValue(); //Math.abs(source.getValue()-target.getValue());
				}else{
					sim = (float)target.getValue() /(float) source.getValue();
				}
				pairs.put(new Pair<Clusterable<E>,Clusterable<E>>(source,target),sim);
				pairs.put(new Pair<Clusterable<E>,Clusterable<E>>(target,source),sim);

			}	

		}
		return pairs;
	
	}

	private LocalService service;
	public Cluster( LocalService service){
		this.service  = service;
	}
	

	public static void main(String[] args) throws Exception{
	
		LocalService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
		
		Cluster<NetworkHierachyNode> cluster = new Cluster<NetworkHierachyNode>(service);
		
		List<Clusterable<NetworkHierachyNode>> data = Cluster.readData(service);
		
		HashMap<Pair<Clusterable<NetworkHierachyNode>, Clusterable<NetworkHierachyNode>>, Double> pairs = cluster.computeSimMatrix(data);
		
		List<List<Clusterable<NetworkHierachyNode>>> groups = cluster.group(pairs, data, 0.95f);
		//List<Node<Clusterable<NetworkHierachyNode>>> groups = cluster.cluster(pairs, data, 0.95f);
		//System.out.println(groups.size());
		
		for(List<Clusterable<NetworkHierachyNode>> group  : groups){
			System.out.println(group);
			Cluster<NetworkHierachyNode> groupCluster = new Cluster<NetworkHierachyNode>(service);
			HashMap<Pair<Clusterable<NetworkHierachyNode>, Clusterable<NetworkHierachyNode>>, Double> groupPairs = groupCluster.computeSimMatrix(group);
			
			List<Node<Clusterable<NetworkHierachyNode>>> c = groupCluster.cluster(groupPairs, group, 0.95f);
			
			System.out.println("\t" + c.size() + "\t" + group.size());
		}
		
		System.out.println(groups.size());
		
	}


}