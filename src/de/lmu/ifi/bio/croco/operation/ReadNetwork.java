package de.lmu.ifi.bio.croco.operation;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.ContextTreeNode;
import de.lmu.ifi.bio.croco.data.NetworkMetaInformation;
import de.lmu.ifi.bio.croco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
/**
 * Reads a network from a CroCo-Repository.
 * @author rpesch
 */
public class ReadNetwork extends GeneralOperation {
	
	public static Parameter<Boolean> GlobalRepository = new Parameter<Boolean>("GlobalRepository",false);
	public static Parameter<NetworkMetaInformation> NetworkMetaInformation = new Parameter<NetworkMetaInformation>("NetworkMetaInformation",null);
	public static Parameter<ContextTreeNode> ContextTreeNode = new Parameter<ContextTreeNode>("ContextTreeNode",null);
	
	@ParameterWrapper(parameter="ContextTreeNode",alias="ContextTreeNode")
	public void setContextTreeNodeParameter(String soureID) throws Exception{
		QueryService service = this.getParameter(QueryService);
		this.setInput(ContextTreeNode, service.getContextTreeNode(soureID));
	}
					
	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		QueryService service = this.getParameter(QueryService);
		ContextTreeNode contextTreeNode = this.getParameter(ContextTreeNode);
		
		Boolean globalRepository = this.getParameter(GlobalRepository);
		if (globalRepository ==null ){
			CroCoLogger.getLogger().warn("No edge repository strategy defined");
			globalRepository = false;
		}
		
		
		NetworkMetaInformation node = this.getParameter(NetworkMetaInformation);
		if ( node.getGroupId() == null) throw new OperationNotPossibleException("No group id given");
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
		
		NetworkMetaInformation node = this.getParameter(NetworkMetaInformation);
		ContextTreeNode contextTreeNode = this.getParameter(ContextTreeNode);
		
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
	public void accept(List<Network> networks)throws OperationNotPossibleException {
		if (networks != null && networks.size()  > 0) throw new OperationNotPossibleException("Does not accept networks as parameter") ;
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		QueryService service = this.getParameter(QueryService);
		NetworkMetaInformation node = this.getParameter(NetworkMetaInformation);
		
		if ( service == null) throw new OperationNotPossibleException("Query service is null");
		if ( node == null) throw new OperationNotPossibleException("No NetworkMetaInformation given");
		
		Boolean globalRepository = this.getParameter(GlobalRepository);
		if (globalRepository ==null ){
			CroCoLogger.getLogger().warn("No edge repository strategy defined");
		}
		
	}

	@Override
	public List<Parameter<?>> getParameters() {
		List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
		parameters.add(GlobalRepository);
		parameters.add(NetworkMetaInformation);
		parameters.add(QueryService);
		parameters.add(ContextTreeNode);
		return parameters;
	}

}
