package de.lmu.ifi.bio.croco.operation;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.Species;
import de.lmu.ifi.bio.croco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.Pair;
import de.lmu.ifi.bio.croco.util.Tuple;

/**
 * Transfers a network using orthologs
 * @author rpesch
 *
 */
public class Transfer extends GeneralOperation{
	public static Parameter<OrthologRepository> OrthologRepository = new Parameter<OrthologRepository>("OrthologRepository");
	public static Parameter<List<OrthologMappingInformation>> OrthologMappingInformation = new Parameter<List<OrthologMappingInformation>>("List of OrthologMappingInformation");
	
	/**
	 * The OrthologMapping parameter
	 * @param mapping -- two comma separated species names either (Human, Mouse, Fly, or Worm), or two comma separated tax ids for species in the ENSEMBL Compara. e.g. Human,Mouse, or 9606,10090 
	 * @throws Exception
	 */
	@ParameterWrapper(parameter="OrthologMappingInformation",alias="OrthologMapping")
	public void setOrthologMapping(String mapping) throws Exception{
		String[] species = mapping.split(",");
		if ( species.length != 2) throw new Exception("OrthologMapping format species1,species2 e.g. Human,Mouse");
		Species s1 = null;
		Species s2 = null;
		for(Species s : Species.knownSpecies){
			if ( s.getName().equals(species[0]) || s.getShortName().equals(species[0]) ) s1 = s;
			if ( s.getName().equals(species[1]) || s.getShortName().equals(species[1]) ) s2 = s;
		}
	
		
		if ( s1 == null || s2 == null) {
			Pattern p = Pattern.compile("\\d+");
			Matcher m1 = p.matcher(species[0]);
			Matcher m2 = p.matcher(species[1]);
			
			if ( m1.matches() && m2.matches()){
				s1  =new Species( Integer.valueOf(species[0]));
				s2 = new Species (Integer.valueOf(species[1]));
			}
		}
		
		if ( s1 == null || s2 == null) throw new Exception("No ortholog mapping for:" + mapping);
		List<de.lmu.ifi.bio.croco.operation.ortholog.OrthologMappingInformation> oMappings = this.getParameter(OrthologRepository).getService().getOrthologMappingInformation(null, s1, s2);
		this.setInput(OrthologMappingInformation, oMappings);
	}
	
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
		
		Network ret =Network.getEmptyNetwork(network.getClass(), "Transferred", targetSpecies.getTaxId(),network.getEdgeRepositoryStrategy() );
		
		
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
		CroCoLogger.getLogger().debug(String.format("Transferred network size: %d",ret.size()));
		
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
				throw new OperationNotPossibleException(String.format("Invalid ortholog mapping (%s). For network %s with taxId: %d",ortholog.toString(),network.getName(), network.getTaxId()));
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
			throw new OperationNotPossibleException("No ortholog mapping given");
		}
		
		OrthologRepository repository = this.getParameter(OrthologRepository);;
		if( repository == null){
			throw new OperationNotPossibleException("No ortholog repository given");
		}
	
	}

}
