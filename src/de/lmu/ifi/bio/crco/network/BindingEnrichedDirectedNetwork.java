package de.lmu.ifi.bio.crco.network;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;


public class BindingEnrichedDirectedNetwork extends DirectedNetwork{
	
	public BindingEnrichedDirectedNetwork(Network network){
		super(network);
	}
	public BindingEnrichedDirectedNetwork(NetworkHierachyNode node, boolean globalRepository){
		super(node,globalRepository);
	}
	public BindingEnrichedDirectedNetwork(String name, Integer taxId, boolean globalRepository) {
		super(name, taxId, globalRepository);
	}
	public BindingEnrichedDirectedNetwork(String name, Integer taxId) {
		super(name, taxId, false);
	}
	
	public List<TFBSPeak> getBindings(int edgeId){
		return (List)super.getAnnotation(edgeId, EdgeOption.BindingSite);
	}
	public void addEdge(Entity entity1, Entity entity2, List<Integer> groupIds, List<TFBSPeak> possibleBinding){
	
		TIntObjectHashMap<List<Object>> annotation = new TIntObjectHashMap<List<Object>>();
		if ( groupIds!= null && groupIds.size() > 0) annotation.put(EdgeOption.GroupId.ordinal(), (List)groupIds);
		if ( possibleBinding != null && groupIds.size() > 0) annotation.put(EdgeOption.BindingSite.ordinal(), (List)possibleBinding);
	
		super.add(entity1,entity2, annotation);

	}
	
	public void addEdge(Entity entity1, Entity entity2, Integer groupId, TFBSPeak possibleBinding){
		List<Integer> groupIds = new ArrayList<Integer>();
		if ( groupId != null) groupIds.add(groupId); 
		List<TFBSPeak> possibleBindings = new ArrayList<TFBSPeak>();
		if (possibleBinding != null) possibleBindings.add(possibleBinding);
		
		addEdge(entity1,entity2,groupIds,possibleBindings);
	
		
	}
	

	/*
	private IntervalTree getIntervalTree(int edgeId){
		IntervalTree tree = new IntervalTree();
		for(Object obj : bindings.get(edgeId)){
			Peak peak  = (Peak)obj;
			tree.insert(peak);
		}
		return tree;
	}
	*/


}
