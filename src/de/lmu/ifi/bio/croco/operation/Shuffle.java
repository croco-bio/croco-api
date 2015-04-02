package de.lmu.ifi.bio.croco.operation;

import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.util.Tuple;

/**
 * Shuffles a network with same (out) degree distribution.
 * @author robert
 *
 */
public class Shuffle extends GeneralOperation{
	
	public static Parameter<Random> RandomGenerator  = new Parameter<Random>("Random generator",new Random(0));
	public static Parameter<Integer> RandomizeCount  = new Parameter<Integer>("RandomizeCountr",5);
	
	
	@Override
	protected Network doOperation() throws OperationNotPossibleException {
		Network network = this.getNetworks().get(0);

		Random rnd = this.getParameter(RandomGenerator);
		int k = this.getParameter(RandomizeCount);
		
		Network ret = Network.getEmptyNetwork(network.getClass(), network);
		//copy network
		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> edge = network.getEdge(edgeId);
			
			ret.add(edge.getFirst(),edge.getSecond());
		}
		
		for(int j = 0 ; j < k; j++){
			int[] edges = ret.getEdgeIds().getInternalSet().toArray();
			
			for(int i = 0 ; i< ret.size(); i++){
				
				int sourceEdgeId = edges[i];
				if ( !ret.containsEdgeId(sourceEdgeId)) continue;
				
				//very inefficient!
				int randomEdgeId =  ret.getEdgeIds().getInternalSet().toArray()[rnd.nextInt(ret.size())];
				
				
				if(randomEdgeId ==  sourceEdgeId) continue;
				Tuple<Entity, Entity> sourceEdge = ret.getEdge(sourceEdgeId);
				Tuple<Entity, Entity> randomEdge = ret.getEdge(randomEdgeId);
				
				if (! ret.containsEdge(sourceEdge.getFirst(),  randomEdge.getSecond()) && ! ret.containsEdge(randomEdge.getFirst(),  sourceEdge.getSecond())){
					if ( !ret.removeEdge(randomEdgeId) ) throw new OperationNotPossibleException("Could not remove edge:" +ret.getEdge(randomEdgeId) );
					if ( !ret.removeEdge(sourceEdgeId) ) throw new OperationNotPossibleException("Could not remove edge:" +ret.getEdge(sourceEdgeId) );
					
					ret.add(sourceEdge.getFirst(), randomEdge.getSecond());
					ret.add(randomEdge.getFirst(), sourceEdge.getSecond());
				}
				
				
				
			}
		}
		if ( ret.size() != network.size()) throw new OperationNotPossibleException("Shuffle went wrong (" + ret.size() + " "  + network.size());
		
	
		HashMap<Entity, Set<Entity>> n1 = network.createFactorTargetNetwork();
		HashMap<Entity, Set<Entity>> n2 = ret.createFactorTargetNetwork();
		for(Entity e : n1.keySet()){
			if ( n1.get(e).size() != n2.get(e).size()) throw new OperationNotPossibleException("Shuffle went wrong (" +e + " " + n1.get(2).size() + " "  + n2.get(e).size());;
		}
		
		return ret;
		/*
		
		Random rnd = this.getParameter(RandomGenerator);
		Network ret = Network.getEmptyNetwork(network.getClass(), network);

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
		int w = 0;
		for(Entity e : inDeg.keySet()){
			w+=inDeg.get(e);
		}
		System.out.println("--" + w);
		
		List<Entity> s = new ArrayList<Entity>(inDeg.keySet());
		Comparator<Entity> cmp = new Comparator<Entity>(){
			@Override
			public int compare(Entity o1, Entity o2) {
				return -1*inDeg.get(o1).compareTo(inDeg.get(o2));
			}
		};
		Collections.shuffle(s);
		Collections.sort(s,cmp);
		
		List<Entity> factors = new ArrayList<Entity>(outDeg.keySet());
		Collections.shuffle(factors,rnd);
		System.out.println(network.size());
		for( Entity e:factors){
			int out = outDeg.get(e);
			int z = 0;
			for(Entity te : s){
				z+=inDeg.get(te);
			}
			System.out.println(z+ " " + ret.size() + " " + (ret.size()+z));
			if ( e.getIdentifier().equals("ENSG00000167182")){
				System.out.println(out + " " + s.size() + " " + ret.size());
			}
			if ( out > s.size()) throw new OperationNotPossibleException("Shuffle went wrong!");
			int k= 0;
			for(int  i = 0 ; i < out ; i++){
	
				Entity selected = s.get(k);
				if ( ret.containsEdge(e, selected)) throw new OperationNotPossibleException("Shuffle went wrong, edge already contained");
				ret.add(e, selected);
				inDeg.put(selected, inDeg.get(selected)-1);
				
				if ( inDeg.get(selected) == 0) {
					s.remove(k);
				}else{
					k++;
				}
			}
			
			
		}
		System.out.println(inDeg);
		if ( s.size() != 0) throw new OperationNotPossibleException("Indeg not null");
		if ( ret.size() != network.size()) throw new OperationNotPossibleException("Networks have a different size");
		return ret;
		*/
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
