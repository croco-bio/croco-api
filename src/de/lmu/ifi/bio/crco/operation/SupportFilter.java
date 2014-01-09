package de.lmu.ifi.bio.crco.operation;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;

/**
 * Filters edges based on the number of times they have been observed
 * @author robert
 *
 */
public class SupportFilter extends GeneralOperation{
	public static Parameter<Integer> Support = new Parameter<Integer>("Support");

	@Override
	public Network doOperation() {
		Network network = this.getNetworks().get(0); //this.getParameter(Network.class, 0);
		Integer minSupport = this.getParameter(Support);
		
		
		Network ret =Network.getEmptyNetwork(network.getClass(), network.getName(),network.getTaxId(), false);
		
		
		for(int edge: network.getEdgeIds()){
			int support = network.getAnnotation(edge,EdgeOption.GroupId).size();
			
			if ( support >= minSupport){
				ret.add(network.getEdge(edge), network.getAnnotation(edge));
			}
		}
		
		return ret;
	}
	@Override
	public void accept(List<Network>  networks) throws OperationNotPossibleException {
		
	}
	
	@Override
	public List<Parameter<?>> getParameters() {
		List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
		
		parameters.add(Support);
		
		return parameters;
	}
	@Override
	public void checkParameter() throws OperationNotPossibleException {
		// TODO Auto-generated method stub
	}

}
