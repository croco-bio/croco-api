package de.lmu.ifi.bio.crco.network;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.util.Tuple;

public class EdgeRepository {

	private static EdgeRepository instance;
	public static EdgeRepository getInstance() {
		if ( instance == null) {
			instance = new EdgeRepository(); 
		}
		return instance;
	}
	
	private TIntObjectHashMap<Tuple<Entity, Entity>> edgeIdToTuple = new TIntObjectHashMap<Tuple<Entity, Entity>>();
	private TObjectIntHashMap<Tuple<Entity, Entity>> tuplesToEdgeId = new TObjectIntHashMap<Tuple<Entity, Entity>>();
	int currentNewEdgeId= 1;
	
	public Tuple<Entity, Entity> getEdge(int id) {
		if (! edgeIdToTuple.contains(id)){
			return null;
		}
		return edgeIdToTuple.get(id);
	}
	
	public Integer getId(Tuple<Entity, Entity> edge, boolean createNew){
		if ( createNew == true){
		    
		    if (! tuplesToEdgeId.containsKey(edge)){
				edgeIdToTuple.put(currentNewEdgeId, edge);
				tuplesToEdgeId.put(edge, currentNewEdgeId++);
				
			}
		}
		return tuplesToEdgeId.get(edge);
	}
	public int getNumberOfEdges(){
		return edgeIdToTuple.size();
	}

    public int[] getEdgeIds() {
        return edgeIdToTuple.keys();
        
    }
}
