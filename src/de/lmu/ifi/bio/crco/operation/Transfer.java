package de.lmu.ifi.bio.crco.operation;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;
import de.lmu.ifi.bio.crco.util.Tuple;

/**
 * Transfers a network using orthologs
 * @author robert
 *
 */
public class Transfer extends GeneralOperation{
	public static Parameter<OrthologRepository> OrthologRepository = new Parameter<OrthologRepository>("OrthologRepository");
	public static Parameter<List<OrthologMappingInformation>> OrthologMappingInformation = new Parameter<List<OrthologMappingInformation>>("List of OrthologMappingInformation");
	
	private Pair<Species,Species> getSourceAndTargetSpeciesRepresentation(OrthologMappingInformation orthologMappingInformation, Integer sourceTaxId ){
		Species sourceSpeciesRepresentation = null;
		Species targetSpeciesRepresentation = null;
		if ( orthologMappingInformation.getSpecies1().getTaxId().equals(sourceTaxId)){
			sourceSpeciesRepresentation = orthologMappingInformation.getSpecies1();
			targetSpeciesRepresentation = orthologMappingInformation.getSpecies2();
		}else{
			sourceSpeciesRepresentation = orthologMappingInformation.getSpecies2();
			targetSpeciesRepresentation = orthologMappingInformation.getSpecies1();
		}
		return new Pair<Species,Species>(sourceSpeciesRepresentation,targetSpeciesRepresentation);
	}
	
	@Override
	protected Network doOperation(){
		Network network = this.getNetworks().get(0);
		
		Integer taxId = network.getTaxId();
		
		List<OrthologMappingInformation> orthologMappingInformations = this.getParameter(OrthologMappingInformation);

		OrthologRepository repository = this.getParameter(OrthologRepository);

		Species targetSpecies = getSourceAndTargetSpeciesRepresentation(orthologMappingInformations.get(0),taxId).getSecond();
		
		Network ret =Network.getEmptyNetwork(network.getClass(), "Transferred", targetSpecies.getTaxId(),false );
		
		CroCoLogger.getLogger().debug(String.format("Transfer %s (%d) to %d", network.toString(),network.getTaxId(), ret.getTaxId()));
		for(int edgeId  : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			
			Set<Entity> factorOrthologs =new HashSet<Entity>(); 
			Set<Entity> targetOrthologs =new HashSet<Entity>();
			for(OrthologMappingInformation orthologMappingInformation : orthologMappingInformations ){
				Set<Entity> tmp = repository.getOrthologMapping(orthologMappingInformation).getOrthologs(edge.getFirst());
				if ( tmp != null) factorOrthologs.addAll(tmp);
				tmp = repository.getOrthologMapping(orthologMappingInformation).getOrthologs(edge.getSecond());
				if (tmp!= null)targetOrthologs.addAll(tmp);
			}
			if (factorOrthologs != null && targetOrthologs != null ){
				
				TIntObjectHashMap<List<Object>> annotation = network.getAnnotation(edgeId);
				
				for(Entity factorOrtholog : factorOrthologs ){
					for(Entity targetOrtholog : targetOrthologs ){
						
						ret.add(factorOrtholog, targetOrtholog, annotation);
					}
				}
			}
		}
		CroCoLogger.getLogger().debug(String.format("Transferred network size: %d",ret.getSize()));
		
		return ret;
	}

	@Override
	public void accept(List<Network> networks) throws OperationNotPossibleException {
		if ( networks.size() != 1){
			throw new OperationNotPossibleException("Can not add more than one network");
		}
		
		Network network = networks.get(0);
		Integer sourceTaxId = network.getTaxId();
		if ( sourceTaxId == null){
			throw new OperationNotPossibleException("No tax id given for network");
		}
	
		List<OrthologMappingInformation> orthologs = this.getParameter(OrthologMappingInformation);
		if ( orthologs == null || orthologs.size() == 0){
			throw new OperationNotPossibleException("No ortholog mapping given");
		}
		
		for(OrthologMappingInformation ortholog : orthologs){
			if ( !ortholog.getSpecies1().getTaxId().equals(sourceTaxId) && !ortholog.getSpecies2().getTaxId().equals(sourceTaxId) ){
				throw new OperationNotPossibleException("Can not map network" + network.getTaxId());
			}
			
			
		}
	
		
	}

	@Override
	public List<Parameter<?>> getParameters() {
		List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
		
		parameters.add(OrthologMappingInformation);
		parameters.add(OrthologRepository);
	//	parameters.add(Parameter.TransferTargetSpecie);
	
		
		
		return parameters;
	}

	
	@Override
	public void checkParameter() throws OperationNotPossibleException {
		//Network network = this.getNetworks().get(0);
		//Species species = this.getParameter(Parameter.Specie,Species.class);
		List<OrthologMappingInformation> orthologs = this.getParameter(OrthologMappingInformation);
		if ( orthologs == null){
			throw new OperationNotPossibleException("No mapping given");
		}
		
		OrthologRepository repository = this.getParameter(OrthologRepository);;
		if( repository == null){
			throw new OperationNotPossibleException("No ortholog repository given");
		}
	
	}

}
