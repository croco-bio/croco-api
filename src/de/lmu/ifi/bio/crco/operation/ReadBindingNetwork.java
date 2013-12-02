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
	public static Parameter NetworkHierachyNode  = new Parameter("NetworkHierachyNode",de.lmu.ifi.bio.crco.data.NetworkHierachyNode.class);
	public static Parameter QueryService = new Parameter("QueryService",de.lmu.ifi.bio.crco.connector.QueryService.class);
	public static Parameter GlobalRepository = new Parameter("GlobalRepository",Boolean.class);
	public static Parameter ContextTreeNode = new Parameter("ContextTreeNode",ContextTreeNode.class);

	
	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		NetworkHierachyNode node = this.getParameter(NetworkHierachyNode, NetworkHierachyNode.class);
		QueryService service = this.getParameter(QueryService, QueryService.class);
		ContextTreeNode contextTreeNode = this.getParameter(ContextTreeNode,ContextTreeNode.class);
		
		Boolean globalRepository = this.getParameter(GlobalRepository,Boolean.class);
		if (globalRepository ==null ){
			globalRepository = false;
		}
		
		
		BindingEnrichedDirectedNetwork net =new BindingEnrichedDirectedNetwork(node,globalRepository);
		
		int k = 0 ;
		try{
			 List<TFBSPeak> bindings = null;
			if ( contextTreeNode == null)
				bindings = service.getTFBSBindings(node.getGroupId(),null);
			else
				bindings = service.getTFBSBindings(node.getGroupId(),contextTreeNode.getId());
			 for(TFBSPeak binding : bindings){
				 Entity factor = binding.getFactor(); 
				 Entity target = binding.getTarget();
				 Tuple<Entity, Entity> edge = net.createEdgeCore(factor,  target);
				 if ( net.containsEdge(edge)){
					 net.addEdge(factor,target, null, binding);
				 }else
					 net.addEdge(factor,target, node.getGroupId(), binding);
			 	
			 	}
			 
			 k+=bindings.size();
		}catch(Exception e){
			throw new OperationNotPossibleException("Can not get binding sites for experiment:" +node.toString(),e);
		}
	
		CroCoLogger.getLogger().debug(String.format("Number of bindings: %d; Network size: %d",k,net.size()));
		
		return net;
	}

	@Override
	public void accept(List<Network> networks) throws OperationNotPossibleException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		QueryService service = this.getParameter(QueryService, QueryService.class);
		NetworkHierachyNode node = this.getParameter(NetworkHierachyNode, NetworkHierachyNode.class);
		
		if ( service == null) throw new OperationNotPossibleException("Query service is null");
		if ( node == null) throw new OperationNotPossibleException("No node given");
		Boolean globalRepository = this.getParameter(GlobalRepository,Boolean.class);
		if (globalRepository ==null ){
			CroCoLogger.getLogger().warn("No edge repository strategy defined");
		}
	
	}

	@Override
	public List<Parameter> getParameters() {
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(NetworkHierachyNode);
		parameters.add(GlobalRepository);
		parameters.add(QueryService);
		parameters.add(ContextTreeNode);
		return parameters;
	}


}
