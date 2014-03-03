package de.lmu.ifi.bio.crco.operation;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TransferredPeak;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.GenomeCoordinationMapper;
import de.lmu.ifi.bio.crco.util.Tuple;

/**
 * Transfers bindings (genome positions) from one genome to another given a genome alignment (also cross-species).
 * @author robert
 *
 */
public class TransferBindings extends GeneralOperation {
	public static Parameter<File> ChainFileFile = new Parameter<File>("ChainFileFile");
	public static Parameter<File> LiftOverExec = new Parameter<File>("LiftOverExec");
	public static Parameter<Float> MinMatch = new Parameter<Float>("LiftOver Min. match score");

	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		Network network =(Network) this.getNetworks().get(0);
		
		File chainFile = this.getParameter(ChainFileFile);
		File liftOver = this.getParameter(LiftOverExec);
		Float minMatch = this.getParameter(MinMatch);
		
		Network ret = Network.getEmptyNetwork(network.getClass(), network);
		
		List<Peak> bindingSites = new ArrayList<Peak>();
		for(int edgeId : network.getEdgeIds()){
			List<TFBSPeak> sites =network.getAnnotation(edgeId, EdgeOption.BindingSite,TFBSPeak.class);
			bindingSites.addAll(sites);
		}
		CroCoLogger.getLogger().debug("Number of binding sites:\t" +bindingSites.size() );
		HashMap<Peak, Peak> mappedSites = null;
		try{
			mappedSites = GenomeCoordinationMapper.map(liftOver, chainFile, new HashSet<Peak>(bindingSites), minMatch);
		}catch(Exception e){
			throw new OperationNotPossibleException("Can not map bindings",e);
		}
		CroCoLogger.getLogger().debug(String.format("Number of mapped sites: %d",mappedSites.size()));
		for(int edgeId : network.getEdgeIds()){
			List<TFBSPeak> sites = network.getAnnotation(edgeId, EdgeOption.BindingSite,TFBSPeak.class);
			List<TransferredPeak> mappings = new ArrayList<TransferredPeak>();
			for(Peak site : sites){
				if ( mappedSites.containsKey(site)) {
					TransferredPeak transferred = new TransferredPeak(site,mappedSites.get(site));
					mappings.add(transferred);
				}
			}
			if ( mappings.size() > 0){
				TIntObjectHashMap<List<Object>> annotation = network.getAnnotation(edgeId);
				annotation.put(EdgeOption.TransferredSite.ordinal(),(List)mappings );
				Tuple<Entity, Entity> edge = network.getEdge(edgeId);
				
				ret.add(edge, annotation);
			}else{
				TIntObjectHashMap<List<Object>> annotation = network.getAnnotation(edgeId);
				Tuple<Entity, Entity> edge = network.getEdge(edgeId);
				
				ret.add(edge, annotation);
			}
		}
		
		return ret;
	}

	@Override
	public void accept(List<Network> networks) throws OperationNotPossibleException {

		
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		File chainFile = this.getParameter(ChainFileFile);
		if (! chainFile.exists()){
			throw new OperationNotPossibleException(String.format("Given cahin file %s does not exist",chainFile.toString()));
		}
		File liftOver = this.getParameter(LiftOverExec);
		if (! chainFile.exists()){
			throw new OperationNotPossibleException(String.format("Given liftover exec. %s does not exist",liftOver.toString()));
		}
	}

	@Override
	public List<Parameter<?>> getParameters() {
		List<Parameter<?>> para = new ArrayList<Parameter<?>>();
		para.add(ChainFileFile);
		para.add(LiftOverExec);
		para.add(MinMatch);
		return para;
	}

}
