package de.lmu.ifi.bio.crco.operation;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Tuple;

/**
 * Reads a network with binding site annotations
 * @author robert
 *
 */
public class ReadBindingNetwork extends GeneralOperation {
	public static Parameter<NetworkHierachyNode> NetworkHierachyNode  = new Parameter<NetworkHierachyNode>("NetworkHierachyNode");
	public static Parameter<QueryService> QueryService = new Parameter<QueryService>("QueryService");
	public static Parameter<Boolean> GlobalRepository = new Parameter<Boolean>("GlobalRepository");
	public static Parameter<ContextTreeNode> ContextTreeNode = new Parameter<ContextTreeNode>("ContextTreeNode");

	
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
