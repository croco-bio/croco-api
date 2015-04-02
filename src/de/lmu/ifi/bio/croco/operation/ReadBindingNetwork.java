package de.lmu.ifi.bio.croco.operation;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.ContextTreeNode;
import de.lmu.ifi.bio.croco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.croco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.croco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.util.CroCoLogger;

/**
 * Reads a network with binding site annotations
 * @author rpesch
 *
 */
public class ReadBindingNetwork extends ReadNetwork {
	
	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		NetworkHierachyNode node = this.getParameter(NetworkHierachyNode );
		QueryService service = this.getParameter(QueryService);
		ContextTreeNode contextTreeNode = this.getParameter(ContextTreeNode);
		
		Boolean globalRepository = this.getParameter(GlobalRepository);
		if (globalRepository ==null ){
			globalRepository = false;
		}
		
		BindingEnrichedDirectedNetwork network =null;
		
		try{
			if ( contextTreeNode == null)
				network = service.readBindingEnrichedNetwork(node.getGroupId(),null,globalRepository);
			else
				network = service.readBindingEnrichedNetwork(node.getGroupId(),contextTreeNode.getId(),globalRepository);
		
		}catch(Exception e){
			throw new OperationNotPossibleException("Can not get binding sites for experiment:" +node.toString(),e);
		}
	
		return network;
	}

	@Override
	public void accept(List<Network> networks) throws OperationNotPossibleException {
		// TODO Auto-generated method stub
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		QueryService service = this.getParameter(QueryService);
		NetworkHierachyNode node = this.getParameter(NetworkHierachyNode);
		
		if ( service == null) throw new OperationNotPossibleException("Query service is null");
		if ( node == null) throw new OperationNotPossibleException("No node given");
		Boolean globalRepository = this.getParameter(GlobalRepository);
		if (globalRepository ==null ){
			CroCoLogger.getLogger().warn("No edge repository strategy defined");
		}
	
	}

	@Override
	public List<Parameter<?>> getParameters() {
		List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
		parameters.add(NetworkHierachyNode);
		parameters.add(GlobalRepository);
		parameters.add(QueryService);
		parameters.add(ContextTreeNode);
		return parameters;
	}


}
