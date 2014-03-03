package de.lmu.ifi.bio.crco.operation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.processor.hierachy.NetworkHierachy;

public class WriteNetwork extends GeneralOperation {
	public static Parameter<File> NetworkOutputFile = new Parameter<File>("NetworkOutputFile");
	public static Parameter<File> NetworkAnnotationFile = new Parameter<File>("NetworkAnnotationFile");
	
	
	
	@ParameterWrapper(parameter="NetworkOutputFile",alias="OutputNetworkFile")
	public void setNetworkOutputFile(String path) throws Exception{
		this.setInput(NetworkOutputFile, new File(path));
	}
	@ParameterWrapper(parameter="NetworkAnnotationFile",alias="OutputNetworkAnnotationFile")
	public void setContextTreeNodeParameter(String path) throws Exception{
		this.setInput(NetworkAnnotationFile, new File(path));
	}
			
	protected Network doOperation() throws OperationNotPossibleException {
		File networkOutputFile = this.getParameter(NetworkOutputFile);
		Network network = this.getNetworks().get(0);
		
		try {
			NetworkHierachy.writeNetworkHierachyFile(network, networkOutputFile);
		} catch (Exception e) {
			throw new OperationNotPossibleException("Could not write network",e);
		}
		
		File annotationOutputFile = this.getParameter(NetworkAnnotationFile);
		if ( annotationOutputFile != null){
			try {
				NetworkHierachy.writeNetworkHierachyAnnotationFile(network, annotationOutputFile);
			} catch (IOException e) {
				throw new OperationNotPossibleException("Could not write network annotation",e);
			}
		}
		
		return network;
	}

	@Override
	public void accept(List<Network> networks) throws OperationNotPossibleException {
		if ( networks.size() != 1) throw new OperationNotPossibleException("Only one network can be written. given " + networks.size());
		
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		if ( this.getParameter(NetworkOutputFile) == null) throw new OperationNotPossibleException("No network output file is given");
	}

	@Override
	public List<Parameter<?>> getParameters() {

		List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
		
		parameters.add(NetworkOutputFile);
		parameters.add(NetworkAnnotationFile);
		
		return parameters;
	}

}
