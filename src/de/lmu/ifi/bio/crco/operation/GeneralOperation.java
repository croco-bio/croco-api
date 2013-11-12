package de.lmu.ifi.bio.crco.operation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.bio.crco.data.exceptions.CroCoException;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.data.exceptions.ParameterNotWellDefinedException;
import de.lmu.ifi.bio.crco.network.Network;
/**
 * Options can be applied to any network. Network operation do implement this abstract class.
 * @author robert
 *
 */
public abstract class GeneralOperation {

	
	HashMap<Parameter,Object> passedParameters = new HashMap<Parameter,Object>();
	
	public<E extends Object> E getParameter( Parameter parameter, Class<E> clazz){
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

	
	public final void setInput(Parameter parameter, Object value) throws CroCoException{
		if (! parameter.getClazz().isInstance(value)){
			//CroCoLogger.getLogger().fatal();
			//return false;
			throw new CroCoException("Wrong data type for " +  parameter.getName() + ".Given " + (value==null?"null":value.getClass()) + " expected:" + parameter.getClazz());
		}
		boolean in = false;
		for(Parameter p : this.getParameters()){
			if ( p.equals(parameter)) {in=true; break;}
		}
		if ( in == false) throw new CroCoException(String.format("Unknown parameter %s for %s",parameter.toString(),this.getClass().getSimpleName())); 
		passedParameters.put(parameter, value);
		
		

	}
	
	
	protected abstract  Network doOperation() throws OperationNotPossibleException ;
	
	public Network operate() throws OperationNotPossibleException, ParameterNotWellDefinedException{
		checkParameter();
		
		this.accept(this.networks) ;
		 /*
		if ( this.getParameters() != null){
			for(Parameter parameter : this.getParameters()){
				if ( this.getParameter(parameter,parameter.clazz) == null) throw new ParameterNotWellDefinedException(parameter + " not given");;
			}
		}
		*/
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
	
	public abstract List<Parameter> getParameters();

	public String getDescription() {
		return this.getClass().getSimpleName();
		
	}

	
}
