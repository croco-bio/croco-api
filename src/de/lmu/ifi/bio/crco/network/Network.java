package de.lmu.ifi.bio.crco.network;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TransferredPeak;
import de.lmu.ifi.bio.crco.util.TIntHashSetInteratorableWrapper;
import de.lmu.ifi.bio.crco.util.Tuple;

public abstract class Network {
	public enum EdgeOption{
		GroupId(Integer.class), BindingSite(TFBSPeak.class), TransferredSite(TransferredPeak.class);
		
		public Class<?> type;
		EdgeOption(Class<?> type){
			this.type = type;
		}
	}
	
	private String name;
	private NetworkSummary networkSummary;
	
	//todo
	private HashMap<Option,String> networkInfo = new HashMap<Option,String>();
	protected TIntHashSet edges;

	public HashMap<Option,String> getOptionValues(){
		return networkInfo;
	}
	public String getOptionValue(Option option){
		return networkInfo.get(option);
	}
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
	// edgeId -> EdgeOption (ordinal) -> value 
	//number of elements |n| * |options| * |values|
	protected TIntObjectHashMap<TIntObjectHashMap<List<Object>>> annotation;
	
	protected EdgeRepository repositoyInstance;
	private Integer taxId;
	

	
	public static Network getEmptyNetwork(Class<? extends Network> clazz, Network network) {
		Network ret =null;
		
		try{
			Constructor<? extends Network> c = clazz.getConstructor(Network.class);
			ret =c.newInstance(network); //creates a new network with same subtype as the network passed to the method
			
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return ret;
	}
	
	public static Network getEmptyNetwork(Class<? extends Network> network, String name, Integer taxId,boolean repository){
		Network ret =null;
		try{
			
			Constructor<? extends Network> c = network.getConstructor(String.class,Integer.class);
			ret =c.newInstance(name,taxId); //creates a new network with same subtype as the network passed to the method
			
			
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
		return this.name;
	}
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
	public List<Object> getAnnotation(int edge, EdgeOption edgeOption){
		return this.annotation.get(edge).get(edgeOption.ordinal());
	}
	
	
	public Network(Network network){
		this.edges = new TIntHashSet();
		this.annotation = new TIntObjectHashMap<TIntObjectHashMap<List<Object>>>();
		this.taxId = network.taxId;
		this.name = network.name;
		this.repositoyInstance = network.repositoyInstance;
		this.networkInfo = network.networkInfo;
		//this.networkSummary = network.networkSummary;
	}
	
	public Network(String name, Integer taxId,  boolean useEdgeRepository) {
		
		this.taxId = taxId;
		this.name = name;
		this.edges = new TIntHashSet();
		this.annotation = new TIntObjectHashMap<TIntObjectHashMap<List<Object>>>();
		if (useEdgeRepository ){
			repositoyInstance = EdgeRepository.getInstance(); //global instance
		}else{
			repositoyInstance = new EdgeRepository(); //local instance
		}
		
	}
	
	public Network(NetworkHierachyNode node,boolean useEdgeRepository) {

		this.name = node.getName();
		this.taxId = node.getTaxId();
		this.edges = new TIntHashSet();
		this.annotation = new TIntObjectHashMap<TIntObjectHashMap<List<Object>>>();
		if (useEdgeRepository ){
			repositoyInstance = EdgeRepository.getInstance(); //global instance
		}else{
			repositoyInstance = new EdgeRepository(); //local instance
		}
	}

	public int getSize() {
		return this.edges.size();
	}
	

	public void add(Network network){
		
		for(int edgeId : network.getEdgeIds()){
			
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			TIntObjectHashMap<List<Object>> annotations = network.annotation.get(edgeId);
			
			this.add(edge.getFirst(),edge.getSecond(),annotations);
			
		}
	
	}
	public void add(Entity e1, Entity e2){
		
		this.add(e1,e2,new TIntObjectHashMap<List<Object>>());
		
	}
	public void add(Entity e1, Entity e2, Integer groupId){
		
		List<Object> groupIds = new ArrayList<Object>();
		groupIds.add(groupId);
		
		TIntObjectHashMap<List<Object>> edgeAnnotation = new TIntObjectHashMap<List<Object>> ();
		edgeAnnotation.put(EdgeOption.GroupId.ordinal(), groupIds);
		
		this.add(e1,e2,edgeAnnotation);
		
	}
	public void add(Entity e1, Entity e2, List<Integer> groupIds){
		TIntObjectHashMap<List<Object>> edgeAnnotation = new TIntObjectHashMap<List<Object>> ();
		edgeAnnotation.put(EdgeOption.GroupId.ordinal(),(List) groupIds);
		
		this.add(e1,e2,edgeAnnotation);
	}
	public void add(Entity e1, Entity e2, TIntObjectHashMap<List<Object>> edgeAnnotation){
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
						annotation.get(edgeId).get(edgeOptionOrdinal).addAll(edgeAnnotation.get(edgeOptionOrdinal));
					}
				}
			}
		}
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
	public int size() {
		return this.edges.size();
	}
	public int getEdgeId(Tuple<Entity, Entity> edge) {
		return this.repositoyInstance.getId(edge, false);
	}
	public void add(Tuple<Entity, Entity> edge, TIntObjectHashMap<List<Object>> annotation) {
		this.add(edge.getFirst(),edge.getSecond(),annotation);
	}

	public boolean containsEdge(Entity entity1, Entity entity2) {
		return this.containsEdge(this.createEdgeCore(entity1, entity2));
	}

	public String printNetwork() {
		StringBuffer ret = new StringBuffer(">Network:" + this.toString() + "\n");
		for(int edgeId : this.getEdgeIds()){
			Tuple<Entity, Entity> edge = this.getEdge(edgeId);
			ret.append(edge.getFirst() + "->" + edge.getSecond() + "\n");
		}
		
		return ret.toString();
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
}
