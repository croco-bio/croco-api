package de.lmu.ifi.bio.croco.network;

import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.OrderedPair;
import de.lmu.ifi.bio.croco.util.Tuple;


public class DirectedNetwork extends Network{


	public DirectedNetwork(String name,Integer taxId, EdgeRepositoryStrategy edgeRepository){
		super(name, taxId,edgeRepository);
	}
	public DirectedNetwork(NetworkHierachyNode networkHierachyNode, EdgeRepositoryStrategy edgeRepository){
		super(networkHierachyNode,edgeRepository);
	}
	
	
	public DirectedNetwork(String name,Integer taxId){
		super(name, taxId,EdgeRepositoryStrategy.LOCAL);
	}
	
	public DirectedNetwork(Network network) {
		super(network);
	}

	public DirectedNetwork()
	{
	}



	@Override
	public Tuple<Entity,Entity> createEdgeCore(Entity e1, Entity e2) {
		OrderedPair<Entity,Entity> edge = new OrderedPair<Entity,Entity>(e1,e2);
		return edge;
	}

}
