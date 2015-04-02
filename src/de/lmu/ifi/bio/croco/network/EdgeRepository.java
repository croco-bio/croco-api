package de.lmu.ifi.bio.croco.network;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.util.Tuple;

/**
 * The mapping of (TF,TG) tuples to unique ids.
 * @author pesch
 *
 */
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
	
	
	//the edge index counter
	private int currentNewEdgeId= 1;
	
	/**
	 * Returns the (TF,TG) tuple for an edge id.
	 * @param edgeId -- the edge id
	 * @return the (TF,TG) tuple
	 */
	public Tuple<Entity, Entity> getEdge(int edgeId) {
		if (! edgeIdToTuple.contains(edgeId)){
			return null;
		}
		return edgeIdToTuple.get(edgeId);
	}
	
	
	/**
	 * Returns an edgeId for a given TF,TG tuple
	 * @param edge -- the edges
	 * @param createNew -- create new edge when not contained
	 * @return the edge id
	 */
	public Integer getId(Tuple<Entity, Entity> edge, boolean createNew){
		if ( createNew == true){
		    
		    if (! tuplesToEdgeId.containsKey(edge)){
				edgeIdToTuple.put(currentNewEdgeId, edge);
				tuplesToEdgeId.put(edge, currentNewEdgeId++);
				
			}
		}
		return tuplesToEdgeId.get(edge);
	}
	/**
	 * @return number of unique edges in the repository.
	 */
	public int getNumberOfEdges(){
		return edgeIdToTuple.size();
	}

    public int[] getEdgeIds() {
        return edgeIdToTuple.keys();
        
    }
}
