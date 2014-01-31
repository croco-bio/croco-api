package de.lmu.ifi.bio.crco.operation;

import java.util.List;

import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.network.Network;

/**
 * Unifies networks
 * @author robert
 *
 */
public class Union extends GeneralOperation {
	
	public Network doOperation(){
		
		//assert(operatorAble(networks) == true);
		List<Network> networks = this.getNetworks(); //this.getParameter(List.class, 0);
		
		Network net = Network.getEmptyNetwork(networks.get(0).getClass(),networks.get(0)); 
		net.setName("Union");
		//Network ret = networks.get(0);
		for(int i = 0 ; i< networks.size(); i++){
			
			net.add(networks.get(i));
			
		}
		return net;
	}

	@Override
	public void accept(List<Network>  networks) throws OperationNotPossibleException{
		if ( networks.size() > 0){
			Integer taxId = networks.get(0).getTaxId();
			
			for(int i = 1 ; i< networks.size();i++){
				if ( networks.get(i).getTaxId() != null && !networks.get(i).getTaxId().equals(taxId)){
					throw new OperationNotPossibleException(String.format("Union not possible for different tax ids (%d and %d)", taxId, networks.get(i).getTaxId()) );
				}
			}
		
		}else{
			throw new OperationNotPossibleException("No network given");
		}
		
	}

	@Override
	public List<Parameter<?>> getParameters() {
		return null;
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		//do nothing
	}


}
