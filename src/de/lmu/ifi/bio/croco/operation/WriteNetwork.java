package de.lmu.ifi.bio.croco.operation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.processor.hierachy.NetworkHierachy;
import de.lmu.ifi.bio.croco.util.CroCoLogger;

/**
 * Writes a network to a file.
 * @author pesch
 *
 */
public class WriteNetwork extends GeneralOperation {
	public static Parameter<File> NetworkOutputFile = new Parameter<File>("NetworkOutputFile");
	public static Parameter<File> NetworkAnnotationFile = new Parameter<File>("NetworkAnnotationFile");
	
	
	/**
	 * The OutputNetworkFile parameters
	 * @param path -- the file e.g. /tmp/network.gz
	 * @throws Exception
	 */
	@ParameterWrapper(parameter="NetworkOutputFile",alias="OutputNetworkFile")
	public void setNetworkOutputFile(String path) throws Exception{
		this.setInput(NetworkOutputFile, new File(path));
	}
	/**
	 * The OutputNetworkAnnotationFile parameter
	 * @param path -- the file e.g /tmp/network.details.gz
	 * @throws Exception
	 */
	@ParameterWrapper(parameter="NetworkAnnotationFile",alias="OutputNetworkAnnotationFile")
	public void setContextTreeNodeParameter(String path) throws Exception{
		this.setInput(NetworkAnnotationFile, new File(path));
	}
			
	protected Network doOperation() throws OperationNotPossibleException {
		File networkOutputFile = this.getParameter(NetworkOutputFile);
		Network network = this.getNetworks().get(0);
		
		try {
			CroCoLogger.getLogger().info(String.format("Write network to %s",networkOutputFile));
			NetworkHierachy.writeNetworkHierachyFile(network, networkOutputFile);
		} catch (Exception e) {
			throw new OperationNotPossibleException("Could not write network",e);
		}
		
		File annotationOutputFile = this.getParameter(NetworkAnnotationFile);
		if ( annotationOutputFile != null){
			try {
				CroCoLogger.getLogger().info(String.format("Write network annotation to %s",networkOutputFile));
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
