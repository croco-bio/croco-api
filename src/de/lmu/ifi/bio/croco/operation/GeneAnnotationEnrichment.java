package de.lmu.ifi.bio.croco.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.Species;
import de.lmu.ifi.bio.croco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.util.Tuple;

/**
 * Adds to a network gene names
 * @author rpesch
 *
 */
public class GeneAnnotationEnrichment extends GeneralOperation {

	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		Network network = super.getNetworks().get(0);
		QueryService service = this.getParameter(QueryService);
		Species species = new Species(network.getTaxId());
		HashMap<String,String> idNameMapping = new HashMap<String,String>();
		try{
			List<Gene> entities = service.getGenes(species,false,null);
			for(Entity entity: entities){
				idNameMapping.put(entity.getIdentifier(), entity.getName());
			}
		}catch(Exception e){
			throw new OperationNotPossibleException("Can not retrieve gene name information",e);
		}
		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			Entity factor  = edge.getFirst();
			if ( idNameMapping.containsKey(factor.getIdentifier())){
				factor.setName(idNameMapping.get(factor.getIdentifier()));
			}
			Entity target  = edge.getSecond();
			if ( idNameMapping.containsKey(target.getIdentifier())){
				target.setName(idNameMapping.get(target.getIdentifier()));
			}
		}
		
		return network;
	}

	@Override
	public void accept(List<Network> networks) throws OperationNotPossibleException {
		if ( networks.size() != 1) throw new OperationNotPossibleException("Operation not allowed on more than one network");
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		
	}

	@Override
	public List<Parameter<?>> getParameters() {
		List<Parameter<?>> parameter = new ArrayList<Parameter<?>>();
		parameter.add(QueryService);
		return parameter;
	}

}
