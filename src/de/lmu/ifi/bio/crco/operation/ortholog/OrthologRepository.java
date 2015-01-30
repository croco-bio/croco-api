package de.lmu.ifi.bio.crco.operation.ortholog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

/**
 * Retrieves and buffers orthologs using the {@link de.lmu.ifi.bio.crco.connector.QueryService}.
 * @author pesch
 *
 */

public class OrthologRepository {

	
	private QueryService service;
	private OrthologRepository(QueryService service){
		this.service = service;
		orthologMapping = new HashMap<OrthologMappingInformation,OrthologMapping> ();
	}
	private static OrthologRepository instance;
	
	public QueryService getService() {
		return service;
	}
	private HashMap<OrthologMappingInformation,OrthologMapping> orthologMapping;
	
	/**
	 * Creates/Returns a unique {@link de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository} instance given the query service
	 * @param service
	 * @return
	 */
	public static OrthologRepository getInstance(QueryService service){
		if ( instance == null ){
			instance = new  OrthologRepository(service);
		}
		return instance;
	}
	/**
	 * Returns the mapping(s) for an identifier to a particular target species
	 * @param gene gene of interest
	 * @param fromTaxId tax id for the gene of interest
	 * @param toTaxId target tax id
	 * @return the ortholog genes
	 */
	public OrthologMapping getOrthologMapping(OrthologMappingInformation orthologMappingInformation) {
		if (! orthologMapping.containsKey(orthologMappingInformation)){
		    CroCoLogger.debug("Load orthologs for: %s", orthologMappingInformation.toString());
			try {
				OrthologMapping mapping = service.getOrthologMapping(orthologMappingInformation);
				orthologMapping.put(orthologMappingInformation, mapping);
				
			}catch(Exception e){
				CroCoLogger.getLogger().fatal("Can not get orthologs for:" + orthologMappingInformation,e);
				throw new RuntimeException(e);
			}
		}
		return orthologMapping.get(orthologMappingInformation);
	}
    public Set<OrthologMappingInformation> getOrthologMappingInformation() {
        return orthologMapping.keySet();
        
    }
    public Collection<OrthologMapping> getMappings() {
        return orthologMapping.values();
    }

}
