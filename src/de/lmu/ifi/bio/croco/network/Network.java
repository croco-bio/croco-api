package de.lmu.ifi.bio.croco.network;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.croco.data.Option;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.croco.intervaltree.peaks.TransferredPeak;
import de.lmu.ifi.bio.croco.network.Network.EdgeRepositoryStrategy;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.FileUtil;
import de.lmu.ifi.bio.croco.util.TIntHashSetInteratorableWrapper;
import de.lmu.ifi.bio.croco.util.Tuple;

/**
 * Represents a croco-ontology network with regulations and annotations.
 * @author pesch
 *
 */
public abstract class Network  {
	public enum EdgeOption{
		GroupId(Integer.class), BindingSite(Peak.class), TransferredSite(TransferredPeak.class);
		
		public Class<?> type;
		EdgeOption(Class<?> type){
			this.type = type;
		}
	}
	/**
	 * The scope of the edgeIds.
	 * With a local strategy the edge Ids are only unique for this network object, while with a global strategy the edge Ids are unique for all networks.
	 * @author pesch
	 *
	 */
	public enum EdgeRepositoryStrategy{
	    LOCAL,GLOBAL;
	}
	
	// edgeId -> EdgeOption (ordinal) -> value 
	//number of elements |n| * |options| * |values|
	protected TIntObjectHashMap<TIntObjectHashMap<List<Object>>> annotation;
	
	private EdgeRepository repositoyInstance;
	private Integer taxId;
	
	private EdgeRepositoryStrategy edgeRepository;
	private String name;
	private NetworkSummary networkSummary;
	private NetworkHierachyNode hierachyNode;
	
	//todo
	private HashMap<Option,String> networkInfo = new HashMap<Option,String>();
	protected TIntHashSet edges;

	public NetworkHierachyNode getHierachyNode() {
		return hierachyNode;
	}
	public void setHierachyNode(NetworkHierachyNode hierachyNode) {
		this.hierachyNode = hierachyNode;
	}

	public HashMap<Option,String> getOptionValues(){
		return networkInfo;
	}
	public String getOptionValue(Option option){
		return networkInfo.get(option);
	}
	
	public EdgeRepositoryStrategy getEdgeRepositoryStrategy() {
        return this.getEdgeRepositoryStrategy();
    }
	
	protected Network()
	{
	    
	}
	/**
	 * Adds annotations to the network
	 * @param option -- the annotation 
	 * @param value -- the value
	 */
	public void addNetworkInfo(Option option, String value){
		networkInfo.put(option, value);
	}
	public HashMap<Entity,Set<Entity>> createFactorTargetNetwork(){
		HashMap<Entity,Set<Entity>> ret = new HashMap<Entity,Set<Entity>>();
		for(int edgeId : this.getEdgeIds()){
			Tuple<Entity, Entity> edge = this.getEdge(edgeId);
			if (! ret.containsKey(edge.getFirst())){
				ret.put(edge.getFirst(), new HashSet<Entity>());
			}
			ret.get(edge.getFirst()).add(edge.getSecond());
		}
		
		return ret;
	}


	/**
	 * Use constructor instead.
	 */
	@Deprecated
	public static Network getEmptyNetwork(Class<? extends Network> clazz, Network network) {
		Network ret =null;
		try{
			Constructor<? extends Network> c = clazz.getConstructor(Network.class);
			ret =c.newInstance(network);
			
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		return ret;
	}
	/**
     * Use constructor instead.
     */
	@Deprecated
	public static Network getEmptyNetwork(Class<? extends Network> network, String name, Integer taxId,EdgeRepositoryStrategy repository){
		Network ret =null;
		try{
			
			Constructor<? extends Network> c = network.getConstructor(String.class,Integer.class,EdgeRepositoryStrategy.class);
			ret =c.newInstance(name,taxId,repository); //creates a new network with same subtype as the network passed to the method
			
			
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		return ret;
	}

	public String getName() {
		return name;
	}

	
	
	public NetworkSummary getNetworkSummary(){
		return networkSummary;
	}
	public void addNetworkSummary(NetworkSummary networkSummary){
		this.networkSummary = networkSummary;
	}
	
	protected abstract Tuple<Entity,Entity> createEdgeCore(Entity entity1, Entity entity2);
	
	
	public int createEdge(Entity entity1, Entity entity2){
		Tuple<Entity, Entity> edge = this.createEdgeCore(entity1, entity2);
		return repositoyInstance.getId(edge,true);
	}

	@Override
	public String toString(){
		String ret = this.name;
		if ( ret == null && this.networkInfo != null){
			ret = "";
			if ( networkInfo.containsKey(Option.NetworkType)){
				ret += networkInfo.get(Option.NetworkType) + "/";
			}
			if ( networkInfo.containsKey(Option.TaxId)){
				ret += networkInfo.get(Option.TaxId) + "/";
			}
			if ( networkInfo.containsKey(Option.networkFile)){
				ret += networkInfo.get(Option.networkFile).substring(networkInfo.get(Option.networkFile).lastIndexOf("/"));
			}
		}
		return ret;
	}
	

    /**
     * Printes the interactions 
     * @param pw
     */
    public void printNetwork(PrintWriter pw) {
        pw.printf(">Network:%s\n", this.toString() );
        for(int edgeId : this.getEdgeIds()){
            Tuple<Entity, Entity> edge = this.getEdge(edgeId);
            pw.printf("%s -> %s\n",edge.getFirst(),edge.getSecond() );
        }
        pw.flush();
    }
    
    /**
     * @return an iterator over the edges in this network
     */
	public TIntHashSetInteratorableWrapper getEdgeIds(){
		return new TIntHashSetInteratorableWrapper(edges);
	}

	public TIntObjectHashMap<List<Object>> getAnnotation(int edge){
		return this.annotation.get(edge);
	}
	
	public<E extends Object> List<E> getAnnotation(int edge, EdgeOption edgeOption, Class<E> type){
		if (!type.equals(edgeOption.type)) throw new RuntimeException("Incompactible types ("+ type + "," + edgeOption.type + ")");
		
		return (List<E>) this.annotation.get(edge).get(edgeOption.ordinal());
	}
	public<E extends Object> List<E> getAnnotation(int edge, EdgeOption edgeOption){
		if ( this.annotation.contains(edge) && this.annotation.get(edge).contains(edgeOption.ordinal())) return (List<E>) this.annotation.get(edge).get(edgeOption.ordinal());
		return null;
	}
	private void initEdgeRepository(){
		if (edgeRepository.equals(EdgeRepositoryStrategy.GLOBAL) ){
			CroCoLogger.getLogger().trace("Use global edge repository");
			repositoyInstance = EdgeRepository.getInstance(); //global instance
		}else if (edgeRepository.equals(EdgeRepositoryStrategy.LOCAL) ){
			CroCoLogger.getLogger().trace("Use local edge repository");
			repositoyInstance = new EdgeRepository(); //local instance
		}else{
		    throw new RuntimeException("Unknown edge repository strategy.");
		}
	}
	/**
	 * Clones a network
	 * @param network
	 */
	public Network(Network network){
		this.edges = new TIntHashSet();
		this.annotation = new TIntObjectHashMap<TIntObjectHashMap<List<Object>>>();
		this.taxId = network.taxId;
		this.name = network.name;
		this.networkInfo = network.networkInfo;
		this.edgeRepository = network.edgeRepository;
		this.initEdgeRepository();
	}
	
	/**
	 * Constructs a new network with given and species information.
	 * @param name -- the network name
	 * @param taxId -- the species
	 * @param useEdgeRepository -- the edge repository strategy (true
	 */
	public Network(String name, Integer taxId,  EdgeRepositoryStrategy edgeRepository) {
		this.taxId = taxId;
		this.name = name;
		this.edges = new TIntHashSet();
		this.annotation = new TIntObjectHashMap<TIntObjectHashMap<List<Object>>>();
		this.edgeRepository = edgeRepository;
		this.initEdgeRepository();
	}
	
	public Network(NetworkHierachyNode node,EdgeRepositoryStrategy edgeRepository) {
		this.name = node.getName();
		this.taxId = node.getTaxId();
		this.edges = new TIntHashSet();
		this.annotation = new TIntObjectHashMap<TIntObjectHashMap<List<Object>>>();
		this.edgeRepository = edgeRepository;
		this.initEdgeRepository();
	}

	
	/**
	 * Adds all edges including their annotations from a source network to this network.
	 * @param network
	 */
	public void add(Network network){
		
		for(int edgeId : network.getEdgeIds()){
			
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			TIntObjectHashMap<List<Object>> annotations = network.annotation.get(edgeId);
			
			this.add(edge.getFirst(),edge.getSecond(),annotations);
			
		}
	
	}
	
	/**
	 * Adds a TF-TG regulation to network
	 * @param tf
	 * @param tg
	 * @return the new edgeId
	 */
	public int add(Entity tf, Entity tg){
		return this.add(tf,tg,new TIntObjectHashMap<List<Object>>());
	}
	
	/**
	 * Adds a TF-TG regulation with croco-repo network id information to network
	 * @param tf
	 * @param tf
	 * @param groupId
	 * @return the new edgeId
	 */
	public int add(Entity e1, Entity e2, Integer groupId){
		
		List<Object> groupIds = new ArrayList<Object>();
		groupIds.add(groupId);
		
		TIntObjectHashMap<List<Object>> edgeAnnotation = new TIntObjectHashMap<List<Object>> ();
		edgeAnnotation.put(EdgeOption.GroupId.ordinal(), groupIds);
		
		return this.add(e1,e2,edgeAnnotation);
		
	}
	public int add(Entity e1, Entity e2, List<Integer> groupIds){
		TIntObjectHashMap<List<Object>> edgeAnnotation = new TIntObjectHashMap<List<Object>> ();
		edgeAnnotation.put(EdgeOption.GroupId.ordinal(),(List) groupIds);
		
		return this.add(e1,e2,edgeAnnotation);
	}
	public int add(Entity e1, Entity e2, TIntObjectHashMap<List<Object>> edgeAnnotation){
		int edgeId = createEdge(e1,e2);
		
		if ( !this.edges.contains(edgeId)){
			this.edges.add(edgeId);
		}
		if ( edgeAnnotation != null && edgeAnnotation.size() > 0){
			if (! annotation.contains(edgeId)){
				annotation.put(edgeId, edgeAnnotation);
			}else{
				for(int edgeOptionOrdinal : edgeAnnotation.keys()){
					if ( !annotation.get(edgeId).contains(edgeOptionOrdinal)){
						annotation.get(edgeId).put(edgeOptionOrdinal,edgeAnnotation.get(edgeOptionOrdinal));
					}else{
					    //merge
					   
					    for(Object value : edgeAnnotation.get(edgeOptionOrdinal))
					    {
					        if ( annotation.get(edgeId).get(edgeOptionOrdinal).contains(value))
					            continue;
					        annotation.get(edgeId).get(edgeOptionOrdinal).add(value);
					    }
					}
				}
			}
		}
		return edgeId;
	}

	public Set<Entity> getTargets(){
		Set<Entity> nodes = new HashSet<Entity>();
		TIntIterator it = this.edges.iterator();
		while(it.hasNext()){
			Tuple<Entity, Entity> edge = this.repositoyInstance.getEdge(it.next());
			
			nodes.add(edge.getSecond());
		}
		return nodes;
	}

	
	/**
	 * @return unique set of TFs
	 */
	public Set<Entity> getFactors(){
		Set<Entity> nodes = new HashSet<Entity>();
		TIntIterator it = this.edges.iterator();
		while(it.hasNext()){
			
			Tuple<Entity, Entity> edge = this.repositoyInstance.getEdge(it.next());
			nodes.add(edge.getFirst());
		}
		return nodes;
	}
	
	public boolean containsEdge(Tuple<Entity,Entity> edge){
		int edgeId = this.repositoyInstance.getId(edge,false);
		
		return ( this.edges.contains(edgeId));
	}
	/**
	 * @return unique set of TFs and TGs
	 */
	public Set<Entity> getNodes(){
		Set<Entity> nodes = new HashSet<Entity>();
		TIntIterator it = this.edges.iterator();
		while(it.hasNext()){
			Tuple<Entity, Entity> edge = this.repositoyInstance.getEdge(it.next());
			
			nodes.add(edge.getFirst());
			nodes.add(edge.getSecond());
		}
		return nodes;
	}

	
	public Integer getTaxId() {
		return taxId;
	}

	public Tuple<Entity,Entity> getEdge(int edgeId) {
		return this.repositoyInstance.getEdge(edgeId);
	}
	public Integer getEdgeId(Entity e1, Entity e2) {
		Tuple<Entity, Entity> edge = this.createEdgeCore(e1, e2);
		return this.repositoyInstance.getId(edge, false);
	}
	public boolean containsEdgeId(int edgeId) {
		return this.edges.contains(edgeId);
	}
	/**
	 * @return the number of interactions
	 */
	public int size() {
		return this.edges.size();
	}
	
	public int getEdgeId(Tuple<Entity, Entity> edge) {
		return this.repositoyInstance.getId(edge, false);
	}
	public void add(Tuple<Entity, Entity> edge, TIntObjectHashMap<List<Object>> annotation) {
		this.add(edge.getFirst(),edge.getSecond(),annotation);
	}
	/**
	 * Removes an edges (but keeps the edge in the edgeRepository (may cause memory leaks!)
	 * @param edgeId to remove
	 */
	public boolean removeEdge(int edgeId) {
		boolean remove = this.edges.remove(edgeId);
		this.annotation.remove(edgeId);
		return remove;
	}
	
	public boolean containsEdge(Entity entity1, Entity entity2) {
		return this.containsEdge(this.createEdgeCore(entity1, entity2));
	}


	public void setName(String name) {
		this.name = name;
		
	}
	public void setNetworkInfo(HashMap<Option, String> infos) {
		this.networkInfo = infos;
	}
	public HashMap<Option, String> getNetworkInfo() {
		return networkInfo;
	}
	
	@Override
	public boolean equals(Object o){
		if ( o instanceof Network){
			if ( this.size() != ((Network)o).size()) return false;
			for(int edgeId : this.getEdgeIds()){
				Tuple<Entity, Entity> edge = this.getEdge(edgeId);
				if ( !((Network) o).containsEdge(edge)) return false;
			}
			return true;
		}
		return false;
	}
	
    public static HashMap<Option,String> readInfoFile(File infoFile) throws IOException{
        HashMap<Option,String> ret = new HashMap<Option,String> ();
        BufferedReader br =new BufferedReader(new FileReader(infoFile));
        String line = null;
        while(( line = br.readLine())!=null){
            
            String[] tokens = line.split(":");
            Option option = Option.valueOf(tokens[0].trim());
            
            if ( option == Option.OpenChromMotifPVal)
                option = Option.ConfidenceThreshold;
            
            if ( option == Option.OpenChromMotifSet)
                option = Option.MotifSet;
            
            String value = tokens[1].trim();
            ret.put(option, value);
        }
        br.close();
        
        return ret;
    }

	
	public NetworkHierachyNode getNetworkHierachyNode() {
		return this.hierachyNode;
	}

	public static NetworkReader getNetworkReader(){
        return new NetworkReader();
    }
	/**
	 * Reader for networks using the Builder pattern.
	 * @author pesch
	 *
	 */
    public static class NetworkReader{
        
        private Network network;
        private Integer groupId;
        private Set<Entity> factors;
        private HashMap<Option,String> infos;
        private EdgeRepositoryStrategy edgeRepository = EdgeRepositoryStrategy.LOCAL;
        private File networkFile;
        private NetworkHierachyNode node;
        
        public NetworkReader setNetworkInfo(File networkInfoFile) throws IOException{
            this.infos = readInfoFile(networkInfoFile);
            return this;
        }
    
        public NetworkReader setNetworkHierachyNode(NetworkHierachyNode node){
            this.node = node;
            this.groupId = node.getGroupId();
            return this;
        }
        public NetworkReader setNetworkInfo(HashMap<Option,String> infos) throws IOException{
            this.infos = infos;
            return this;
        }
        public NetworkReader setNetwork(Network network){
            this.network = network;
            return this;
        }
        public NetworkReader setFactors(Set<Entity> factors){
            this.factors = factors;
            return this;
        }
        public NetworkReader setEdgeRepositoryStrategy(EdgeRepositoryStrategy edgeRepository){
            this.edgeRepository = edgeRepository;
            return this;
        }
        
        public NetworkReader setGroupId(Integer groupId){
            this.groupId = groupId;
            return this;
        }
        
        public NetworkReader setNetworkFile(File networkFile){
            this.networkFile = networkFile;
            return this;
        }
        
        public Network readNetwork() throws Exception {
            if ( network == null){
                if ( node != null){
                    if ( infos != null){
                        CroCoLogger.getLogger().warn("networkInfo and network hierachy node given (use hierachy node");
                    }
                    network = new DirectedNetwork(node,edgeRepository);
                }else{
                    Integer taxId = null;
                    String name = null;
                    if ( infos != null){
                        try{
                            taxId = Integer.valueOf(infos.get(Option.TaxId));
                        }catch(Exception e){
                            throw new RuntimeException("Can not get taxId for" + networkFile);
                        }
                        name = infos.get(infos.get(Option.NetworkName));
                        if ( name == null) name = infos.get(Option.networkFile);
                    }
                    network = new DirectedNetwork(name,taxId,edgeRepository);
                }
            }
            
            if ( networkFile != null){
                BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(networkFile))));
                String line = null;
            
                while((line=br.readLine())!=null){
                    String[] tokens = line.split("\t");
                    Entity factor = new Entity(tokens [0]);
                    
                    if ( factors != null && !factors.contains(factor)) continue;
                    Entity target = new Entity(tokens[1]);
                    if ( groupId != null)
                        network.add(factor, target,groupId);
                    else
                        network.add(factor,target);
                }
                br.close();
                if ( this.infos != null) network.setNetworkInfo(this.infos);
                network.getOptionValues().put(Option.networkFile, networkFile.toString());
        
            }
            if ( network == null) CroCoLogger.getLogger().warn("No network read. Neither networkFile nor networkInfo given.");
            if ( network != null) network.setNetworkInfo(this.infos);
            if ( network.getOptionValues() != null && network.getOptionValues().size() ==0) network.setNetworkInfo(this.infos);
                    
            return network;
        }
        
        
    }
    /**
     * Writes a network object to file
     * @param network -- the network object
     * @param out -- output file (.gz files are automatically compressed)
     * @throws IOException
     */
    public static void writeNetwork(Network network, File out) throws IOException{
        PrintWriter writer = FileUtil.getPrintWriter(out);
        
        for(int edgeId : network.getEdgeIds()){
            Tuple<Entity, Entity> edge = network.getEdge(edgeId);
            writer.printf(String.format("%s\t%s\n",
                    edge.getFirst().getIdentifier() ,
                    edge.getSecond().getIdentifier())
            );
        }
        
        writer.flush();
        writer.close();
    }
    /**
     * Writes the networks annotations to file
     * @param network -- the network
     * @param out -- the annotation file
     * @throws IOException
     */
    public static void writeNetworkAnnotationFile(Network network, File out) throws IOException {
        PrintWriter writer = FileUtil.getPrintWriter(out);
        
        writer.write("#Factor Target Annotation\n" );
        for(int edgeId : network.getEdgeIds()){
            Tuple<Entity, Entity> edge = network.getEdge(edgeId);
            
            
            TIntObjectHashMap<List<Object>> annotation = network.getAnnotation(edgeId);
            if ( annotation != null){
                for(int annotationId : annotation.keys()){
                    EdgeOption edgeType = Network.EdgeOption.values()[annotationId];
                    for(  Object  o: annotation.get(annotationId)){
                        writer.printf("%s\t%s\t%s\t%s\n",
                                edge.getFirst().getIdentifier(),
                                edge.getSecond().getIdentifier(),
                                edgeType.name() ,
                                o.toString() 
                        );
                    }
                    
                }
            }
            
        }
        
        writer.flush();
        writer.close();
    }
    
	
}
