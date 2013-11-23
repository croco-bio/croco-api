package de.lmu.ifi.bio.crco.stat.generator;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.util.Tuple;

/**
 * Estimates the fraction of common edges from one network in another network.
 * @author Robert Pesch
 *
 */
public class Explainability implements PairwiseStatGenerator {
	
	@Override
	public float compute( Network network1, Network network2 ) throws Exception {
		if (! network1.getTaxId().equals(network2.getTaxId())) throw new RuntimeException("Cannot compare networks");
		
		int overlap = getOverlap(network1,network2); 
		return (float) overlap / (float) network1.size(); //fraction of edges in network 2 found in network 1
		
	}
	
	public static int getOverlap(Network network1, Network network2){
		int overlap = 0;
		
		for(int edgeId  : network1.getEdgeIds()){
			Tuple<Entity, Entity> edge = network1.getEdge(edgeId);
		
			if ( network2.containsEdge(edge)){
				overlap++;
			}
		}
		return overlap;
	}
	@Override
	public Option getOption() {
		return Option.explainability;
	}
	@Override
	public FeatureType getFeatureType() {
		return FeatureType.ASYMMETRIC;
	}



}
