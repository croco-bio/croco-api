package de.lmu.ifi.bio.crco.operation;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.intersect.IntersectionAnnotationCheck;
import de.lmu.ifi.bio.crco.util.Tuple;
/**
 * Intersect networks
 * @author rpesch
 *
 */
public class Intersect extends GeneralOperation{
	static Parameter<IntersectionAnnotationCheck> IntersectionAnnotationCheck = new Parameter<IntersectionAnnotationCheck>("IntersectionAnnotationCheck");
	
	@Override
	protected Network doOperation(){
		List<Network> networks = this.getNetworks(); 
		
		Network net0 = networks.get(0);
		for(int i = 1 ; i < networks.size(); i++){
			if ( networks.get(i).getSize() < net0.getSize()){
				net0 = networks.get(i);
			}
		}
	
		IntersectionAnnotationCheck check = this.getParameter(IntersectionAnnotationCheck);
		
		Network ret=Network.getEmptyNetwork(net0.getClass(),net0);
		ret.setName("Intersection");
		
		for(int edgeId : net0.getEdgeIds()){
			
			boolean consistent = true;
		
			Tuple<Entity, Entity> edge = net0.getEdge(edgeId);
		
			TIntObjectHashMap<List<Object>>  annotation = new TIntObjectHashMap<List<Object>> ();
			annotation = net0.getAnnotation(edgeId);
			
			for(int i = 0 ; i < networks.size(); i++){
				if ( networks.get(i) == net0) continue;
				if ( networks.get(i).containsEdge(edge) == false || (check != null && check.check(net0.getAnnotation(edgeId),networks.get(i).getAnnotation(networks.get(i).getEdgeId(edge)) ) == false) ){
					
					consistent = false;
				}else{
					TIntObjectHashMap<List<Object>> anno = networks.get(i).getAnnotation(networks.get(i).getEdgeId(edge));
					if (anno != null){
						for(int edgeOption : anno.keys()){
							if (! annotation.contains(edgeOption)){
								annotation.put(edgeOption, new ArrayList<Object>());
							}
							annotation.get(edgeOption).addAll(anno.get(edgeOption));
						}
					}
				}
			}
			if ( consistent){
				ret.add(edge,annotation);
			}
		}
		return ret;

	}

	@Override
	public void accept(List<Network>  networks) throws  OperationNotPossibleException{
		if ( networks.size() > 0){
			Integer taxId = networks.get(0).getTaxId();
			for(int i = 1 ; i< networks.size();i++){
				if ( networks.get(i).getTaxId() != null && !networks.get(i).getTaxId().equals(taxId)){
					throw new OperationNotPossibleException("Intersect not possible for different tax ids");
				}
			}
		
		}else{
			throw new OperationNotPossibleException("No network given");
		}
	}

	@Override
	public List<Parameter<?>> getParameters() {
		List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
		parameters.add(IntersectionAnnotationCheck);
		
		return parameters;
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		//do nthing
	}


	
}
