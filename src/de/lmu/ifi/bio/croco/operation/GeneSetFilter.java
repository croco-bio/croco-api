package de.lmu.ifi.bio.croco.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.util.Tuple;

public class GeneSetFilter extends GeneralOperation {
	public static Parameter<Collection<Entity>> genes = new Parameter<Collection<Entity>>("List of genes");
	public static Parameter<FilterType> filterType = new Parameter<FilterType>("Filter type");
	
	
	public static enum FilterType{
		FactorFilter,GeneFilter,OnSideFilter
	}
	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		Network network = this.getNetworks().get(0);
		Network ret = Network.getEmptyNetwork(network.getClass(), network);
		ret.setName(ret.getName() + "(filter)");
		Set<Entity> ofInterest = new HashSet<Entity>(this.getParameter(genes));
		FilterType type = this.getParameter(filterType);
		
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
		}
		
		return ret;
	}

	@Override
	public void accept(List<Network> networks) throws OperationNotPossibleException {
		if ( networks.size() > 1) throw new OperationNotPossibleException("More than one network given");
		
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		Collection<Entity> ofInterest = this.getParameter(genes);
		if ( ofInterest == null) throw new OperationNotPossibleException("No gene list given");
		FilterType f = this.getParameter(filterType);
		if ( f == null) throw new OperationNotPossibleException("No filter type defined");
	}

	@Override
	public List<Parameter<?>> getParameters() {
		ArrayList<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
		parameters.add(genes);
		parameters.add(filterType);
		return parameters;
	}

}
