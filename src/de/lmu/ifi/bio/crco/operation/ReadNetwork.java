package de.lmu.ifi.bio.crco.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;
/**
 * Reads a network from a CroCo-Repository.
 * @author rpesch
 */
public class ReadNetwork extends GeneralOperation {
	
	public static Parameter<Boolean> GlobalRepository = new Parameter<Boolean>("GlobalRepository",false);
	public static Parameter<NetworkHierachyNode> NetworkHierachyNode = new Parameter<NetworkHierachyNode>("NetworkHierachyNode",null);
	public static Parameter<ContextTreeNode> ContextTreeNode = new Parameter<ContextTreeNode>("ContextTreeNode",null);
	
	/**
	 * Wrapper for {@link de.lmu.ifi.bio.crco.connector.NetworkHierachyNode}
	 * @param path string
	 * @return Object
	 * @throws Exception if the connection to the {@link de.lmu.ifi.bio.crco.connector.QueryService} does not work.
	 */
	@ParameterWrapper(parameter="NetworkHierachyNode",alias="NetworkPath")
	public void setNetworkPathParameter(String query) throws Exception{
		CroCoLogger.getLogger().debug("Query for network:"+query);
		QueryService service = this.getParameter(QueryService);
		if ( service == null) throw new RuntimeException("Query service not set");
		Pattern pattern = Pattern.compile("(\\w+)=(\\w+)");
		Matcher matcher = pattern.matcher(query);
		List<Pair<Option,String>> options = new ArrayList<Pair<Option,String>>();
		while ( matcher.find()){
			
			Option option = Option.getOption(matcher.group(1));
			if ( option == null) throw new Exception(String.format("Unknown option %s in query string",matcher.group(1)));
			options.add(new Pair<Option,String>(option,matcher.group(2)));
			
		}
		String path = query.substring(0,query.lastIndexOf("/"));
	
		de.lmu.ifi.bio.crco.data.NetworkHierachyNode node = service.getNetworkHierachy(path);
		if ( node.hasNetwork()) 
			this.setInput(NetworkHierachyNode, node);
		else{
			for(de.lmu.ifi.bio.crco.data.NetworkHierachyNode child  : node.getChildren()){
				List<Pair<Option, String>> infos = service.getNetworkInfo(child.getGroupId());
				int k = 0;
				for(Pair<Option, String> o : options){
					for(Pair<Option, String> info : infos){
						if ( info.getFirst().equals(o.getFirst()) && info.getSecond().equals(o.getSecond()))k++;
					}
				}
				if ( k == options.size()) this.setInput(NetworkHierachyNode, child);
			}
		}
	}
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
		
		
		NetworkHierachyNode node = this.getParameter(NetworkHierachyNode);
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
		
		NetworkHierachyNode node = this.getParameter(NetworkHierachyNode);
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
		NetworkHierachyNode node = this.getParameter(NetworkHierachyNode);
		
		if ( service == null) throw new OperationNotPossibleException("Query service is null");
		if ( node == null) throw new OperationNotPossibleException("No NetworkHierachyNode given");
		
		Boolean globalRepository = this.getParameter(GlobalRepository);
		if (globalRepository ==null ){
			CroCoLogger.getLogger().warn("No edge repository strategy defined");
		}
		
	}

	@Override
	public List<Parameter<?>> getParameters() {
		List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
		parameters.add(GlobalRepository);
		parameters.add(NetworkHierachyNode);
		parameters.add(QueryService);
		parameters.add(ContextTreeNode);
		return parameters;
	}

}
