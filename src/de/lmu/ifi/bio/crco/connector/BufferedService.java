package de.lmu.ifi.bio.crco.connector;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;

import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;
import de.lmu.ifi.bio.crco.util.Tuple;

public class BufferedService implements QueryService {
	private File baseDir;
	private QueryService service;
	
	public BufferedService(QueryService service, File baseDir) {
		//super(logger, connection);
		this.service = service;
		this.baseDir = baseDir;
	}
	public void clean() throws Exception{
		for(File file : baseDir.listFiles()){
			if ( file.getName().endsWith(".croco.gz")){
				file.delete();
			}
		}
	}
	
	public OrthologMapping getOrthologMapping(OrthologMappingInformation orthologMappingInformation) throws Exception{
		File networkFile = new File(baseDir + "/ortholog-" + orthologMappingInformation.getDatabase().ordinal() + "-" + orthologMappingInformation.getSpecies1().getTaxId() +"-" + orthologMappingInformation.getSpecies2().getTaxId() );
		if ( !networkFile.exists()){
			OrthologMapping obj = service.getOrthologMapping(orthologMappingInformation);
			write(obj, networkFile);
		}
		CroCoLogger.getLogger().debug(String.format("Read buffered output:%s",networkFile.getAbsoluteFile().toString()));

		return read(networkFile);
	}

	@Override
	public Network readNetwork(Integer groupId, Integer contextId, boolean globalRepository) throws Exception{
		File networkFile = new File(baseDir + "/network-" + groupId + ".croco.gz");
		if ( !networkFile.exists()){
			Network network = service.readNetwork(groupId,contextId,globalRepository);
			writeNetwork(networkFile,network);
			//Helper.write(network, networkFile);
			return network;
		}
		CroCoLogger.getLogger().debug(String.format("Read buffered output:%s",networkFile.getAbsoluteFile().toString()));
		return readNetwork(networkFile,globalRepository);
	}
	private Network readNetwork(File file,boolean globalRepository) throws Exception{
		GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
		BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
		String header = br.readLine().substring(1);
		String[] tokens = header.split("\t");
		String clazz = tokens[0];
		String name = tokens[1];
		Integer taxId = Integer.valueOf(tokens[2]);
		Class<Network> networkClass = (Class<Network>)Class.forName(clazz);
		Network ret = Network.getEmptyNetwork(networkClass, name, taxId,globalRepository);
		String line = null;
		int k = 0;
		while((line = br.readLine())!=null){
			tokens = line.split("\t");
			Entity e1 = new Entity(tokens[0]);
			Entity e2 = new Entity(tokens[1]);
			List<Integer> groupIds = new ArrayList<Integer>();
			for(int i = 2 ; i< tokens.length; i++){
				groupIds.add(Integer.valueOf(tokens[i]));
			}
		
			ret.add(e1, e2,groupIds);
		
			k++;
			
		}
		br.close();
		CroCoLogger.getLogger().debug(String.format("Read network has %d edges",ret.getSize()));
		return ret;
		
	}
	
	private void writeNetwork(File file, Network network) throws Exception{
		OutputStreamWriter writer = new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file))));
		 
		writer.write(String.format("#%s\t%s\t%d\n", network.getClass().getCanonicalName(), network.getName(), network.getTaxId() ));
	
		for(int edgeId  : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			
			writer.write(edge.getFirst().getIdentifier() + "\t"  +edge.getSecond().getIdentifier() );
			List<Integer> groupId = network.getAnnotation(edgeId,EdgeOption.GroupId,Integer.class);
			
			for(Integer v : groupId){
				writer.write("\t" + v);
			}
			writer.write("\n");
		}
		
		writer.flush();
		writer.close();
	
	}
	public static<E extends Object> void write(E e, File file) throws IOException{
		XStream xstream = new XStream();;
		FileWriter fw = new FileWriter(file);
		xstream.toXML(e, fw);
		fw.close();
	}
	
	public static<E extends Object> E read(File file) throws IOException{
		XStream xstream = new XStream();;
		Object o = xstream.fromXML(file);
		return (E) o;
	}
	@Override
	public NetworkHierachyNode getNetworkHierachy(String path) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<NetworkHierachyNode> findNetwork(
			List<Pair<Option, String>> options) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public NetworkHierachyNode getNetworkHierachyNode(Integer groupId)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<Pair<Option, String>> getNetworkInfo(Integer groupId)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<Entity> getEntities(Species species, String annotation,
			ContextTreeNode context) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getNumberOfEdges(Integer groupId) throws Exception {
		return service.getNumberOfEdges(groupId);
	}
	@Override
	public List<TFBSPeak> getTFBSBindings(int groupId, Integer contextId)
			throws Exception {
		return service.getTFBSBindings(groupId, contextId);
	}

	@Override
	public List<OrthologMappingInformation> getTransferTargetSpecies(int taxId)
			throws Exception {
		return service.getTransferTargetSpecies(taxId);
	}
	
	@Override
	public List<OrthologMappingInformation> getOrthologMappingInformation(OrthologDatabaseType database, Species species1, Species species2)throws Exception {
		return service.getOrthologMappingInformation(database, species1, species2);
	}
	@Override
	public List<Species> getPossibleTransferSpecies() throws Exception {
		return service.getPossibleTransferSpecies();
	}

	@Override
	public List<ContextTreeNode> getContextTreeNodes(String name) throws Exception {
		return service.getContextTreeNodes(name);
	}
	@Override
	public List<ContextTreeNode> getChildren(ContextTreeNode node) throws Exception {
		return service.getChildren(node);
	}

	@Override
	public ContextTreeNode getContextTreeNode(String sourceId) throws Exception {
		return service.getContextTreeNode(sourceId);
	}
	@Override
	public List<Species> getSpecies(String prefix) throws Exception {
		return service.getSpecies( prefix);
	}
	@Override
	public Species getSpecies(Integer taxId) throws Exception {
		return service.getSpecies(taxId);
	}

}
