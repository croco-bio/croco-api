package de.lmu.ifi.bio.croco.connector;

import java.awt.image.BufferedImage;
import java.util.List;

import de.lmu.ifi.bio.croco.data.BindingEvidence;
import de.lmu.ifi.bio.croco.data.ContextTreeNode;
import de.lmu.ifi.bio.croco.data.CroCoNode;
import de.lmu.ifi.bio.croco.data.NetworkMetaInformation;
import de.lmu.ifi.bio.croco.data.Species;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologMappingInformation;

/**
 * Provides various queries to the croco-repo
 * @author pesch
 *
 */
public interface QueryService {
	final static long version = 11l;
	
	/**
	 * The current QueryService version
	 * @return long value
	 */
	public Long getVersion();
	
	/*
	public NetworkHierachyNode getNetworkHierachy() throws Exception;
	public List<NetworkHierachyNode> findNetwork(List<Pair<Option,String>> options) throws Exception;
	*/
	
	public NetworkMetaInformation getNetworkMetaInformation(Integer groupId) throws Exception;
	public List<NetworkMetaInformation> getNetworkMetaInformation() throws Exception;
	
	/**
	 * Returns network annotations for a specific network id
	 * @param groupId the network id
	 * @return a list of options
	 * @throws Exception
	 */
//	public List<Pair<Option,String>> getNetworkInfo(Integer groupId) throws Exception;


	/**
	 * Reads a specific network with possible context restrictions on the nodes e.g. include only nodes with certain GO annotation.
	 * @param groupId the network identifier
	 * @param contextId the context ID, or null (no context restrictions)
	 * @param globalRepository edge repository strategy
	 * @return the Network
	 * @throws Exception
	 */
	public Network readNetwork(Integer groupId, Integer contextId, Boolean globalRepository) throws Exception;	
	
	/**
	 * Returns the number of edges for a specific network id
	 * @param groupId the network id
	 * @return the number of edges
	 * @throws Exception
	 */
	public Integer getNumberOfEdges(Integer groupId) throws Exception;
	
	public BindingEnrichedDirectedNetwork readBindingEnrichedNetwork(Integer groupId, Integer contextId, Boolean gloablRepository ) throws Exception;
	public List<BindingEvidence> getBindings(String factor, String target) throws Exception;
	
	//ortolog mapping
	public List<OrthologMappingInformation> getTransferTargetSpecies(Integer taxId) throws Exception;
	public OrthologMapping getOrthologMapping(OrthologMappingInformation orthologMappingInformation) throws Exception;
	public List<OrthologMappingInformation> getOrthologMappingInformation(OrthologDatabaseType database, Species species1, Species species2) throws Exception;

	//context information
	public List<ContextTreeNode> getContextTreeNodes(String name) throws Exception;
	public List<ContextTreeNode> getChildren(ContextTreeNode node) throws Exception;
	public ContextTreeNode getContextTreeNode(String sourceId) throws Exception;
	

	/**
	 * Retrieves a rendered network image for a given network
	 * @param groupId the network groupId identifier
	 * @return the image
	 * @throws Exception
	 */
	public BufferedImage getRenderedNetwork(Integer groupId) throws Exception;
	

	/**
	 * Returns genes with specific properties
	 * @param species the species of interest, or null
	 * @param bioType the ensembl biotype e.g. protein_coding, or null
	 * @param context the node context, or null
	 * @return list of entities
	 * @throws Exception
	 */
	public List<Gene> getGenes(Species species,Boolean onlyCoding, ContextTreeNode context) throws Exception;

	/**
	 * Return the croco network ontology
	 * @return --
	 * @throws Exception
	 */
	public CroCoNode<NetworkMetaInformation> getNetworkOntology(boolean onlyPublic) throws Exception;
}
