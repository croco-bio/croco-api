package de.lmu.ifi.bio.croco.stat.generator;

import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.Option;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.util.Tuple;

public class NetworkOverlap implements PairwiseStatGenerator {
	
	@Override
	public Result compute( Network network1, Network network2 ) throws Exception {
		if (! network1.getTaxId().equals(network2.getTaxId())) throw new RuntimeException("Cannot compare networks");
		
		int[] stat = getStat(network1,network2); //overlap of a and b

		return new Result(stat[0],stat[1]);
		
	}

	public static int[] getStat(Network network1, Network network2){
		int overlap = 0;
		int size = 0;
		for( int edgeId : network1.getEdgeIds()){
			size++;
			Tuple<Entity, Entity> edge = network1.getEdge(edgeId);
			
			if ( network2.containsEdge(edge)){
				overlap++;
			}
		}
		for( int edgeId : network2.getEdgeIds()){
			Tuple<Entity, Entity> edge = network2.getEdge(edgeId);
			
			if ( !network1.containsEdge(edge)){
				size++;
			}
		}
		
		if ( size == 0) return new int[]{0,0};
		return new int[]{overlap,size};
	}

	@Override
	public Option getOption() {
		return Option.networkOverlap;
	}

	@Override
	public FeatureType getFeatureType() {
		return FeatureType.SYMMETRIC;
	}
	
}
