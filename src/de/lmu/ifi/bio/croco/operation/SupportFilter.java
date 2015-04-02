package de.lmu.ifi.bio.croco.operation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.croco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.network.Network.EdgeOption;
import de.lmu.ifi.bio.croco.util.CroCoLogger;

/**
 * Filters edges based on the number of times they have been observed
 * @author robert
 *
 */
public class SupportFilter extends GeneralOperation{
	public static Parameter<Integer> Support = new Parameter<Integer>("Support");
	
	/**
	 * The Support parameter
	 * @param number -- minimum number of supports for an edge e.g. 2
	 * @throws Exception
	 */
	@ParameterWrapper(parameter="Support",alias="Support")
	public void setContextTreeNodeParameter(String number) throws Exception{
		Integer sup = null;
		try{
			sup = Integer.valueOf(number);
		}catch(NumberFormatException e){
			throw new OperationNotPossibleException(String.format("%s cannot be coverted to an integer number",number),e);
		}
		this.setInput(Support,sup);
		
	}
		
	
	@Override
	public Network doOperation() {
		Network network = this.getNetworks().get(0); //this.getParameter(Network.class, 0);
		Integer minSupport = this.getParameter(Support);
		
		
		Network ret =Network.getEmptyNetwork(network.getClass(), network.getName(),network.getTaxId(), false);
		ret.setName(ret.getName() + ("SupportFilter =>" + minSupport));
		
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
		if ( this.getParameter(Support) == null) throw new OperationNotPossibleException("Value for support not set.");
	}

}
