package de.lmu.ifi.bio.crco.connector;

import java.util.List;

import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.util.Pair;


public interface QueryService {
	//find networks
	public NetworkHierachyNode getNetworkHierachy(String path) throws Exception;
	public List<NetworkHierachyNode> findNetwork(List<Pair<Option,String>> options) throws Exception;
	public NetworkHierachyNode getNetworkHierachyNode(Integer groupId) throws Exception;
	
	//get network information
	public List<Pair<Option,String>> getNetworkInfo(Integer groupId) throws Exception;
	//get entity information
	public List<Entity> getEntities(Species species,String annotation, ContextTreeNode context) throws Exception;

	//read networks
	public Network readNetwork(Integer groupId, Integer contextId, boolean globalRepository) throws Exception;	
	
	//get properties
	public Integer getNumberOfEdges(Integer groupId) throws Exception;
	public List<TFBSPeak> getTFBSBindings(int groupId, Integer contextId ) throws Exception;
	
	//ortolog mapping
	public List<OrthologMappingInformation> getTransferTargetSpecies(int taxId) throws Exception;
	public OrthologMapping getOrthologMapping(OrthologMappingInformation orthologMappingInformation) throws Exception;
	public List<OrthologMappingInformation> getOrthologMappingInformation(OrthologDatabaseType database, Species species1, Species species2) throws Exception;
	public List<Species> getPossibleTransferSpecies() throws Exception;
	
	//context information
	public List<ContextTreeNode> getContextTreeNodes(String name) throws Exception;
	public List<ContextTreeNode> getChildren(ContextTreeNode node) throws Exception;
	public ContextTreeNode getContextTreeNode(String sourceId) throws Exception;
	
	//find species
	public List<Species> getSpecies(String prefix) throws Exception;
	public Species getSpecies(Integer taxId) throws Exception;

	
}
