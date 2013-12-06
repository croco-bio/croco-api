package de.lmu.ifi.bio.crco.operation;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
/**
 * Reads a network from a CroCo-Repository.
 * @author rpesch
 */
public class ReadNetwork extends GeneralOperation {

	public static Parameter GlobalRepository = new Parameter("GlobalRepository",Boolean.class,false);
	/**
	 * Available Wrappers: {@link de.lmu.ifi.bio.crco.operation.ReadNetwork#getNetworkPath}
	 *@tt.wrapper bla bla
	 */
	public static Parameter NetworkHierachyNode = new Parameter("NetworkHierachyNode",NetworkHierachyNode.class,null);
	public static Parameter ContextTreeNode = new Parameter("ContextTreeNode",ContextTreeNode.class,null);
	public static Parameter QueryService = new Parameter("QueryService",de.lmu.ifi.bio.crco.connector.QueryService.class);
	
	/**
	 * @tt.wrapper Wrapper for {@link de.lmu.ifi.bio.crco.connector.NetworkHierachyNode}
	 * @param path string
	 * @return Object
	 * @throws Exception if the connection to the {@link de.lmu.ifi.bio.crco.connector.QueryService} does not work.
	 */
	@ParameterWrapper(parameter="NetworkHierachyNode",alias="networkPath")
	public Object getNetworkPath(String path) throws Exception{
		QueryService service = this.getParameter(QueryService, QueryService.class);
		return service.getNetworkHierachy(path);
	}
	@ParameterWrapper(parameter="ContextTreeNode",alias="contextTreeNode")
	public Object getContextTreeNode(String soureID) throws Exception{
		QueryService service = this.getParameter(QueryService, QueryService.class);
		return service.getContextTreeNode(soureID);
	}
					
	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		QueryService service = this.getParameter(QueryService, QueryService.class);
		ContextTreeNode contextTreeNode = this.getParameter(ContextTreeNode,ContextTreeNode.class);
		
		Boolean globalRepository = this.getParameter(GlobalRepository,Boolean.class);
		if (globalRepository ==null ){
			CroCoLogger.getLogger().warn("No edge repository strategy defined");
			globalRepository = false;
		}
		
		
		NetworkHierachyNode node = this.getParameter(NetworkHierachyNode, NetworkHierachyNode.class);
		Network network = null;
		try{
			Integer contextId = null;
			if ( contextTreeNode != null) contextId = contextTreeNode.getContextId();
			network = service.readNetwork(node.getGroupId(),contextId,globalRepository);
			
		}catch(Exception e){
			throw new OperationNotPossibleException("Could not read network",e);
		}
		return network;
	}
	@Override
	public String getDescription(){
		
		NetworkHierachyNode node = this.getParameter(NetworkHierachyNode, NetworkHierachyNode.class);
		ContextTreeNode contextTreeNode = this.getParameter(ContextTreeNode,ContextTreeNode.class);
		
		String ret = "";
		if ( node != null){
			ret+= "Read network: (" + node.getName() + ") ";
			if ( contextTreeNode != null){
				ret += "( context:" + contextTreeNode.getDescription() + ")";
			}
			return ret;
		}else{
			return super.getDescription();
		}
		
		
	}
	
	
	@Override
	public void accept(List<Network> networks)
			throws OperationNotPossibleException {
		if (networks != null && networks.size()  > 0) throw new OperationNotPossibleException("Does not accept networks as parameter") ;
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		QueryService service = this.getParameter(QueryService, QueryService.class);
		NetworkHierachyNode node = this.getParameter(NetworkHierachyNode, NetworkHierachyNode.class);
		
		if ( service == null) throw new OperationNotPossibleException("Query service is null");
		if ( node == null) throw new OperationNotPossibleException("No NetworkHierachyNode given");
		
		Boolean globalRepository = this.getParameter(GlobalRepository,Boolean.class);
		if (globalRepository ==null ){
			CroCoLogger.getLogger().warn("No edge repository strategy defined");
		}
		
	}

	@Override
	public List<Parameter> getParameters() {
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(GlobalRepository);
		parameters.add(NetworkHierachyNode);
		parameters.add(QueryService);
		parameters.add(ContextTreeNode);
		return parameters;
	}

}
