package de.lmu.ifi.bio.crco.operation.converter;

import java.util.Random;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.util.Tuple;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import gnu.trove.list.array.TIntArrayList;

public class JUNGConverter implements Convert<DirectedSparseGraph<String,Integer>>{

	private Integer max = null;
	public JUNGConverter(int max){
		this.max = max;
	}
	public JUNGConverter(){
	}
	
	public DirectedSparseGraph<String,Integer> convert(Network network) {
		DirectedSparseGraph<String, Integer> graph = new DirectedSparseGraph<String, Integer>();
		
		if ( max != null){
			TIntArrayList list = new TIntArrayList(network.getEdgeIds().getInternalSet());
			
			list.shuffle(new Random(0));
			
			for(int i = 0 ; i < max; i++){
				if ( i >= list.size()) break;
				int edgeId = list.get(i);
				Tuple<Entity, Entity> edge = network.getEdge(edgeId);
				graph.addEdge(edgeId, edge.getFirst().getName(), edge.getSecond().getName());
	 
			}
		}else{
			for(int edgeId: network.getEdgeIds()){
				Tuple<Entity, Entity> edge = network.getEdge(edgeId);
				graph.addEdge(edgeId, edge.getFirst().getName(), edge.getSecond().getName());
			}
		}
		return graph;
	}



}
