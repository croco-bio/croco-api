package de.lmu.ifi.bio.crco.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.data.genome.Transcript;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Tuple;

public class BindingFilter extends GeneralOperation{
	public static Parameter<Integer> Distance = new Parameter<Integer>("Max distance to TSS");

	@ParameterWrapper(parameter="Distance",alias="Distance")
	public void setDistance(String number) throws Exception{
		Integer sup = null;
		try{
			sup = Integer.valueOf(number);
		}catch(NumberFormatException e){
			throw new OperationNotPossibleException(String.format("%s cannot be coverted to an integer number",number),e);
		}
		this.setInput(Distance,sup);
		
	}
		
	
	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		Network network = this.getNetworks().get(0);
		
		Species species = Species.getSpecies(network.getTaxId());
		List<Gene> genes = null;
		try {
			genes = this.getParameter(QueryService).getGenes(species,true,null);
		} catch (Exception e) {
			throw new OperationNotPossibleException("Could not fetch genes",e);
		}
		Integer minDistance = this.getParameter(Distance);
		
		HashMap<String,List<Integer>> geneIdToTssStartSites = new HashMap<String,List<Integer>>();
		if ( minDistance != null){
			for(Gene gene : genes){
				List<Integer> startPositions = new ArrayList<Integer>();
				for(Transcript transcript : gene.getTranscripts()){
					startPositions.add(transcript.getStrandCorredStart());
				}
				geneIdToTssStartSites.put(gene.getIdentifier(),startPositions);
			}
		}
		
		BindingEnrichedDirectedNetwork bNetwork = (BindingEnrichedDirectedNetwork) network;
		BindingEnrichedDirectedNetwork ret = new BindingEnrichedDirectedNetwork(bNetwork);
		
		for(int edgeId : bNetwork.getEdgeIds()){
			List<Integer> startPositions = null;
			Tuple<Entity, Entity> edge = bNetwork.getEdge(edgeId);
			List<Integer> groupIds =(List) bNetwork.getAnnotation(edgeId, EdgeOption.GroupId);
			
			if ( minDistance != null){
				startPositions = geneIdToTssStartSites.get(edge.getSecond().getIdentifier());
			}
			List<Peak> bindings = bNetwork.getBindings(edgeId);
			List<Peak> notFilteredBindings = new ArrayList<Peak>();
			for(Peak binding : bindings){
				for(Integer startPosition : startPositions){
					Integer distance = Math.abs(Peak.getMiddle(binding)-startPosition);
					if ( distance <= minDistance){
						notFilteredBindings.add(binding);
					}
				}
			}
			if ( notFilteredBindings.size() > 0) ret.addEdge(edge.getFirst(), edge.getSecond(), groupIds, (List)notFilteredBindings);
		}
		return ret;
	}

	@Override
	public void accept(List<Network> networks) throws OperationNotPossibleException {
		if ( networks.size() == 1 &&  !BindingEnrichedDirectedNetwork.class.isInstance(networks.get(0)) ) 
			throw new OperationNotPossibleException(String.format("Given network is a not binding annotaiton (use operation ReadBindingNetwork)"));
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
		if (this.getParameter(Distance) == null) throw new OperationNotPossibleException("No distance threshold given");
	}

	@Override
	public List<Parameter<?>> getParameters() {
		List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
		parameters.add(Distance);
		parameters.add(QueryService);
		
		return parameters;
	}

}
