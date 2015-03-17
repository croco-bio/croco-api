package de.lmu.ifi.bio.crco.connector;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.thoughtworks.xstream.XStream;

import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.CroCoNode;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.processor.hierachy.NetworkHierachy;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;

public class BufferedService implements QueryService {
	private File baseDir;
	private QueryService service;
	
	public BufferedService(QueryService service, File baseDir) {
		//super(logger, connection);
		this.service = service;
		this.baseDir = baseDir;
		if (! baseDir.exists())
		{
		    CroCoLogger.getLogger().info(String.format("Create buffer dir: %s",baseDir.toString()));
		    baseDir.mkdirs();
		}
	}
	public void clean() throws Exception{
		for(File file : baseDir.listFiles()){
			if ( file.getName().endsWith(".croco.gz")){
				file.delete();
			}
		}
	}
	public File[] getBufferedFiles()
	{
	    File[] files = baseDir.listFiles(new FileFilter(){

            @Override
            public boolean accept(File pathname) {
                return pathname.getName().contains(".croco.gz");
            }
            
        });
	    
	    return files;
	}
	
	/*
	@Override
	public CroCoNode getNetworkOntology() throws Exception {
	    return service.getNetworkOntology();
	}
	*/
	
	@Override
	public CroCoNode getNetworkOntology() throws Exception {
	    File ontologyFile = new File(baseDir + "/ontology.croco.gz");
	    CroCoNode rootNode = null;
	    if (! ontologyFile.exists())
	    {
	        rootNode= service.getNetworkOntology();
	        if ( rootNode != null) 
	            writeOntology(ontologyFile,rootNode);
	    }else{
	        rootNode = readOntology(ontologyFile);
	    }
	    
	    return rootNode;
	}
	
	private CroCoNode readOntology(File file) throws Exception
	{
	    CroCoLogger.getLogger().info("Write ontology file:" + file);
	    XStream xstream = new XStream();
        xstream.setMode(XStream.ID_REFERENCES);
        
        
        ObjectInputStream in = xstream.createObjectInputStream(new InputStreamReader( new GZIPInputStream(new FileInputStream(file))));
        
        CroCoNode obj = (CroCoNode)  in.readObject();
	
        in.close();
        return obj;
	}
	private void writeOntology(File file, CroCoNode node) throws Exception
	{
	    CroCoLogger.getLogger().info("Read ontology from file:" + file);
	    XStream xstream = new XStream();
        xstream.setMode(XStream.ID_REFERENCES);
        Writer out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
        ObjectOutputStream out2 = xstream.createObjectOutputStream(out);
        
        out2.writeObject(node);
        
        out2.close();
        out.close();
	    
	}
	
	@Override
	public OrthologMapping getOrthologMapping(OrthologMappingInformation orthologMappingInformation) throws Exception{
		File orthologMappingFile = new File(baseDir + "/ortholog-" + orthologMappingInformation.getDatabase().ordinal() + "-" + orthologMappingInformation.getSpecies1().getTaxId() +"-" + orthologMappingInformation.getSpecies2().getTaxId() + ".croco.gz" );
		if ( !orthologMappingFile.exists()){
			OrthologMapping mapping = service.getOrthologMapping(orthologMappingInformation);
			writeOrthologMapping(orthologMappingFile, orthologMappingInformation,mapping);
		}
		CroCoLogger.getLogger().debug(String.format("Read buffered output:%s",orthologMappingFile.getAbsoluteFile().toString()));

		return readOrthologMapping(orthologMappingFile,orthologMappingInformation);
	}
	public static void writeOrthologMapping(File file,OrthologMappingInformation orthologMappingInformation, OrthologMapping orthologMapping) throws IOException{
		OutputStreamWriter writer = new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file))));
		writer.write("#" +orthologMappingInformation.getDatabase().getName() + "\t" + orthologMappingInformation.getSpecies1().getName() + "\t" + orthologMappingInformation.getSpecies2().getName());
		
        for(Entry<Entity, Set<Entity>> e : orthologMapping.getMapping().entrySet()){
            for(Entity value : e.getValue()){
                writer.write(e.getKey().getIdentifier() + "\t" + value.getIdentifier() + "\n");
            }
        }
        
		writer.flush();
		writer.close();
	}
	private OrthologMapping readOrthologMapping(File file, OrthologMappingInformation orthologMappingInformation) throws IOException{
		GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
		BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
		OrthologMapping mapping = new OrthologMapping(orthologMappingInformation);
		String line = br.readLine();
		while((line = br.readLine())!=null){
			String[] tokens = line.split("\t");
			Entity e1 = new Entity(tokens[0]);
			Entity e2 = new Entity(tokens[1]);
			mapping.addMapping(e1, e2);
		}
		br.close();
		CroCoLogger.getLogger().debug(String.format("Read ortholog mapping from file"));
		return mapping;
	}
	
	@Override
	public Network readNetwork(Integer groupId, Integer contextId, Boolean globalRepository) throws Exception{
		File networkFile = new File(baseDir + "/network-" + groupId + "_" + contextId + ".croco.gz");
		if ( !networkFile.exists()){
			Network network = service.readNetwork(groupId,contextId,globalRepository);
			NetworkHierachy.writeNetworkHierachyFile(network, networkFile);
			return network;
		}
		CroCoLogger.getLogger().debug(String.format("Read buffered output:%s",networkFile.getAbsoluteFile().toString()));
		
		return NetworkHierachy.getNetworkReader().setGloablRepository(globalRepository).setNetworkFile(networkFile).setNetworkHierachyNode(service.getNetworkHierachyNode(groupId)).readNetwork();
		
	}
	/*
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
	*/
	@Override
	public List<NetworkHierachyNode> getNetworkHierachy() throws Exception {
		return service.getNetworkHierachy();
	}



	@Override
	public NetworkHierachyNode getNetworkHierachyNode(Integer groupId) throws Exception {
		return this.service.getNetworkHierachyNode(groupId);
	}
	

	@Override
	public Integer getNumberOfEdges(Integer groupId) throws Exception {
		return service.getNumberOfEdges(groupId);
	}

	@Override
	public List<OrthologMappingInformation> getTransferTargetSpecies(Integer taxId)	throws Exception {
		return service.getTransferTargetSpecies(taxId);
	}
	
	@Override
	public List<OrthologMappingInformation> getOrthologMappingInformation(OrthologDatabaseType database, Species species1, Species species2)throws Exception {
		return service.getOrthologMappingInformation(database, species1, species2);
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
	public BufferedImage getRenderedNetwork(Integer groupId) throws Exception {
		return service.getRenderedNetwork(groupId);
	}
	
	@Override
	public List<BindingEnrichedDirectedNetwork> getBindings(String factor, String target) throws Exception {
		return service.getBindings(factor, target);
	}
	@Override
	public BindingEnrichedDirectedNetwork readBindingEnrichedNetwork(Integer groupId, Integer contextId, Boolean gloablRepository)throws Exception {
		return service.readBindingEnrichedNetwork(groupId, contextId, gloablRepository);
	}
	@Override
	public Long getVersion() {
		return version;
	}
	@Override
	public List<Gene> getGenes(Species species, Boolean onlyCoding, ContextTreeNode context) throws Exception {
		return service.getGenes(species,onlyCoding,context);
	}
 
   
}
