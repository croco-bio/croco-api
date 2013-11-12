package de.lmu.ifi.bio.crco.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.util.Tuple;

public class GeneSetFilter extends GeneralOperation {
	public static Parameter genes = new Parameter("List of genes",Collection.class);
	public static Parameter filterType = new Parameter("Filter type",FilterType.class);
	
	
	public static enum FilterType{
		FactorFilter,GeneFilter,OnSideFilter
	}
	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		Network network = this.getNetworks().get(0);
		Network ret = Network.getEmptyNetwork(network.getClass(), network);
		Set ofInterest = new HashSet<Entity>(this.getParameter(genes, Set.class));
		
		FilterType type = this.getParameter(filterType, FilterType.class);
		
		HashMap<Entity,Set<Entity>> neighbours = new HashMap<Entity,Set<Entity>>();
		
		
		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			if ( !neighbours.containsKey(edge.getFirst())){
				neighbours.put(edge.getFirst(), new HashSet<Entity>());
			}
			if ( !neighbours.containsKey(edge.getSecond())){
				neighbours.put(edge.getSecond(), new HashSet<Entity>());
			}
			neighbours.get(edge.getSecond()).add(edge.getFirst());
			neighbours.get(edge.getFirst()).add(edge.getSecond());
			if ( type.equals(FilterType.FactorFilter)){
				if ( !ret.containsEdge(edge) && ofInterest.contains(edge.getFirst())){
					ret.add(edge, network.getAnnotation(edgeId));
				}
			}else if ( type.equals(FilterType.GeneFilter)){
				if ( !ret.containsEdge(edge) && ofInterest.contains(edge.getFirst()) && ofInterest.contains(edge.getSecond()) ){
					ret.add(edge, network.getAnnotation(edgeId));
				}
			}else if ( type.equals(FilterType.OnSideFilter)){
				if ( 
						!ret.containsEdge(edge) && 
						(
								(
										ofInterest.contains(edge.getFirst()) && ofInterest.contains(edge.getSecond()) 
								)
								|| 
								ofInterest.contains(edge.getSecond())
						) 
					)
				{
					ret.add(edge, network.getAnnotation(edgeId));
				}
			}
		}/*
		List<Set<Entity>> conneted = new ArrayList<Set<Entity>>();
		
		HashSet<Entity> processed = new HashSet<Entity>();
		
		for(Entity e : neighbours.keySet()){
			if ( processed.contains(e)) continue;
			
			Set<Entity> component = new HashSet<Entity>();
			Stack<Entity> toProcess = new Stack<Entity>();
			toProcess.add(e);
			while(!toProcess.isEmpty()){
				Entity top = toProcess.pop();
				component.add(top);
				processed.add(top);
				for(Entity child : neighbours.get(top)){
					if ( processed.contains(child)) continue;
					toProcess.add(child);
				}
			}
			
			conneted.add(component);
		}
		
		
		
		Set<Entity> factors = network.getFactors();
		
		for(Entity factor : factors){
			for(Set<Entity> comp : conneted){
				if ( comp.contains(factor)){
					comp.retainAll(factors);
					
					for(Entity f1 : comp){
						for(Entity f2 : comp){
							if ( network.containsEdge(f1,f2)){
								Integer d = network.getEdgeId(f1, f2);
								ret.add(f1, f2, network.getAnnotation(d));
							}
						}	
					}
					
					//factors.removeAll(comp);
				}
			}
		}
		

	*/
			
		
		return ret;
	}

	@Override
	public void accept(List<Network> networks) throws OperationNotPossibleException {
		if ( networks.size() > 1) throw new OperationNotPossibleException("More than one network given");
		
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		Collection ofInterest = this.getParameter(genes, Set.class);
		if ( ofInterest == null) throw new OperationNotPossibleException("No gene list given");
		FilterType f = this.getParameter(filterType, FilterType.class);
		if ( f == null) throw new OperationNotPossibleException("No filter type defined");
	}

	@Override
	public List<Parameter> getParameters() {
		ArrayList<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(genes);
		parameters.add(filterType);
		return parameters;
	}

}
