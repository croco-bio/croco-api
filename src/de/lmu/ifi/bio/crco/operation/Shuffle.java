package de.lmu.ifi.bio.crco.operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.util.Tuple;

/**
 * Shuffles a network with same (out) degree distribution.
 * @author robert
 *
 */
public class Shuffle extends GeneralOperation{
	
	public static Parameter<Random> RandomGenerator  = new Parameter<Random>("Random generator",new Random(0));
	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		Network network = this.getNetworks().get(0);
		
		Random rnd = this.getParameter(RandomGenerator);
		
		Network ret = network.getEmptyNetwork(network.getClass(), network);

		final HashMap<Entity,Integer> inDeg = new HashMap<Entity,Integer>();
		HashMap<Entity,Integer> outDeg = new HashMap<Entity,Integer>();

		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			if (! outDeg.containsKey(edge.getFirst())){
				outDeg.put(edge.getFirst(),0);
			}
			if (! inDeg.containsKey(edge.getSecond())){
				inDeg.put(edge.getSecond(),0);
			}
			outDeg.put(edge.getFirst(), outDeg.get(edge.getFirst())+1);
			inDeg.put(edge.getSecond(),inDeg.get(edge.getSecond())+1);

		}

		List<Entity> s = new ArrayList<Entity>(inDeg.keySet());
		Collections.shuffle(s);
		Comparator<Entity> cmp = new Comparator<Entity>(){
			@Override
			public int compare(Entity o1, Entity o2) {
				return -1*inDeg.get(o1).compareTo(inDeg.get(o2));
			}
		};

		Collections.sort(s,cmp);
		List<Entity> factors = new ArrayList<Entity>(outDeg.keySet());
		Collections.shuffle(factors,rnd);


		for( Entity e:factors){
			int out = outDeg.get(e);

			int k = 0;
			for(int  i = 0 ; i < out ; i++){
				if ( ret.size() != network.size()) throw new RuntimeException("Networks have a different size");
				Entity selected = s.get(k);
				if ( ret.containsEdge(e, selected)) throw new RuntimeException("Shuffle went wrong, edge already contained");
				ret.add(e, selected, 1);
				inDeg.put(selected, inDeg.get(selected)-1);
				if ( inDeg.get(selected) == 0) {
					s.remove(k);
				}else{
					k++;
				}
			}
		}
		if ( ret.size() != network.size()) throw new RuntimeException("Networks have a different size");
		return ret;
		
	}

	@Override
	public void accept(List<Network> networks)throws OperationNotPossibleException {
		if ( networks.size() != 1) throw new OperationNotPossibleException("Only one network accepted as parameter");
		
	}

	@Override
	public void checkParameter() throws OperationNotPossibleException {
	
	}

	@Override
	public List<Parameter<?>> getParameters() {
		List<Parameter<?>> parameter = new ArrayList<Parameter<?>>();
		parameter.add(RandomGenerator);
		return parameter;
	}

}
