package de.lmu.ifi.bio.croco.network;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.NetworkMetaInformation;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;


public class BindingEnrichedDirectedNetwork extends DirectedNetwork{
	public BindingEnrichedDirectedNetwork()
	{
	    
	}
	public BindingEnrichedDirectedNetwork(Network network){
		super(network);
	}
	public BindingEnrichedDirectedNetwork(NetworkMetaInformation node, EdgeRepositoryStrategy globalRepository){
		super(node,globalRepository);
	}
	public BindingEnrichedDirectedNetwork(String name, Integer taxId, EdgeRepositoryStrategy globalRepository) {
		super(name, taxId, globalRepository);
	}
	public BindingEnrichedDirectedNetwork(String name, Integer taxId) {
		super(name, taxId, EdgeRepositoryStrategy.LOCAL);
	}

	public List<Peak> getBindings(int edgeId){
		return (List)super.getAnnotation(edgeId, EdgeOption.BindingSite);
	}
	public void addEdge(Entity entity1, Entity entity2, List<Integer> groupIds, List<Peak> possibleBinding){
	
		TIntObjectHashMap<List<Object>> annotation = new TIntObjectHashMap<List<Object>>();
		if ( groupIds!= null && groupIds.size() > 0) annotation.put(EdgeOption.GroupId.ordinal(), (List)groupIds);
		if ( possibleBinding != null) annotation.put(EdgeOption.BindingSite.ordinal(), (List)possibleBinding);
	
		super.add(entity1,entity2, annotation);

	}
	
	public void addEdge(Entity entity1, Entity entity2, Integer groupId, Peak possibleBinding){
		List<Integer> groupIds = null;
		if ( groupId != null){
			groupIds = new ArrayList<Integer>();
			groupIds.add(groupId); 
		}
		
		List<Peak> possibleBindings = new ArrayList<Peak>();
		possibleBindings.add(possibleBinding);
		
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
