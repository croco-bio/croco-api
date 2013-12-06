package de.lmu.ifi.bio.crco.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.Gene;
import de.lmu.ifi.bio.crco.data.IdentifierType;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;

public class UnionTest {

	@Test
	public void testUnionBindingSite() throws Exception{
		ReadBindingNetwork reader = new ReadBindingNetwork();
		QueryService service = new LocalService(DatabaseConnection.getConnection());
		
		reader.setInput(ReadBindingNetwork.QueryService, service);
		reader.setInput(ReadBindingNetwork.NetworkHierachyNode, new NetworkHierachyNode(3734, 10090));
		
		BindingEnrichedDirectedNetwork net1 = (BindingEnrichedDirectedNetwork)  reader.operate();
		assertTrue(net1.getSize()>0);
		
		Union union = new Union();
		union.setInputNetwork(net1,net1);
		Network ret = union.operate();
		assertTrue(ret.getSize()>0);
		assertEquals(ret.getSize(),net1.getSize());
		
		
		reader.setInput(ReadBindingNetwork.NetworkHierachyNode, new NetworkHierachyNode(3735, 10090));
		BindingEnrichedDirectedNetwork net2 = (BindingEnrichedDirectedNetwork)  reader.operate();
		
		union = new Union();
		union.setInputNetwork(net1,net2);
		ret = union.operate();
		assertTrue(ret.getSize()>=net1.getSize() && ret.getSize() >= net2.getSize());
		
		
		
	}
	
	@Test
	public void test() throws Exception {
		Union operation = new Union();
		
		Network network1 = new DirectedNetwork("test",9606);
		network1.add(new Gene("a"), new Gene("b"), 0);
		network1.add(new Gene("a"), new Gene("b"), 0);
		network1.add(new Gene("b"), new Gene("a"), 0);
		
		Network network2 = new DirectedNetwork("test",9606);
		network2.add(new Gene("a"), new Gene("b"), 1);
		
		operation.setInputNetwork(new Network[] {network1,network2});
		
	//	assertTrue(operation.accept(network2));
		Network network = operation.operate();
		
		
		
		for( int edgeId : network.getEdgeIds()){
			System.out.println(network.getEdge(edgeId));
			List<Integer> groupIds = network.getAnnotation(edgeId,EdgeOption.GroupId,Integer.class);
			System.out.println(groupIds);
			assertNotNull(network.getEdge(edgeId));
		}
		
		assertEquals(network.getSize(),2);
		
		/*
		network2.add(new Gene("c"), new Gene("b"), 0);
		network = operation.operate(Arrays.asList(new Network[] {network1,network2}));
		assertEquals(network.getSize(),2);

		network2.add(new Gene("b"), new Gene("a"), 0);
		network = operation.operate(Arrays.asList(new Network[] {network1,network2}));
		assertEquals(network.getSize(),3);
		*/
	}

}