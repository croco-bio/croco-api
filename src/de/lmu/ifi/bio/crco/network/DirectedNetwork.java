package de.lmu.ifi.bio.crco.network;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.OrderedPair;
import de.lmu.ifi.bio.crco.util.Tuple;


public class DirectedNetwork extends Network{


	public DirectedNetwork(String name,Integer taxId, boolean globalRepository){
		super(name, taxId,globalRepository);
	}
	public DirectedNetwork(NetworkHierachyNode networkHierachyNode, boolean globalRepository){
		super(networkHierachyNode,globalRepository);
	}
	
	
	public DirectedNetwork(String name,Integer taxId){
		super(name, taxId,false);
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
