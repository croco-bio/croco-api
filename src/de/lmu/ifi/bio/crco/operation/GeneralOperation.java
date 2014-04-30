package de.lmu.ifi.bio.crco.operation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.data.exceptions.ParameterNotWellDefinedException;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
/**
 * Options can be applied to any network. Network operation do implement this abstract class.
 * @author rpesch
 *
 */
public abstract class GeneralOperation {
	public static Parameter<QueryService> QueryService = new Parameter<QueryService>("QueryService");
	
	
	HashMap<Parameter<?>,Object> passedParameters = new HashMap<Parameter<?>,Object>();
	
	public<E extends Object> E getParameter( Parameter<E> parameter){
		E ret =  (E) passedParameters.get(parameter);
		if ( ret == null) ret =(E) parameter.getDefaultValue();
		return ret;
	}
	private List<Network> networks;
	

	public List<Network> getNetworks() {
		return networks;
	}
	
	public void setInputNetwork(Network...networks){
		this.networks = Arrays.asList(networks);
	}
	
	public void setInputNetwork(List<Network> networks){
		this.networks = networks;
	}

	/**
	 * Sets a value for a parameter
	 * @param parameter -- the parameter
	 * @param value -- the value
	 * @return true when successful
	 */
	public final <E extends Object> boolean setInput(Parameter<E> parameter, E value) {

		boolean in = false;
		for(Parameter<?> p : this.getParameters()){
			if ( p.equals(parameter)) {in=true; break;}
		}
		if ( in == false) {
			CroCoLogger.getLogger().warn(String.format("Unknown parameter %s for %s",parameter.getClass().getSimpleName(),this.getClass().getSimpleName()));
			return false;
		}
		passedParameters.put(parameter, value);
		return true;
		
	}
	
	protected abstract  Network doOperation() throws OperationNotPossibleException ;
	
	public Network operate() throws OperationNotPossibleException, ParameterNotWellDefinedException{
		checkParameter();
		
		this.accept(this.networks) ;

		try{
			Network ret = this.doOperation();
			return ret;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	/**
	 * Check if operation accepts given networks
	 * @param networks
	 */
	public abstract void accept(List<Network> networks) throws OperationNotPossibleException;
	
	public void accept(Network...networks)throws OperationNotPossibleException{
		 this.accept(Arrays.asList(networks));
	}
	/**
	 * Checks if parameters are set correctly 
	 * @throws OperationNotPossibleException
	 */
	public abstract void checkParameter() throws OperationNotPossibleException;
	
	public abstract List<Parameter<?>> getParameters();

	public String getDescription() {
		return this.getClass().getSimpleName();
		
	}

	
}
