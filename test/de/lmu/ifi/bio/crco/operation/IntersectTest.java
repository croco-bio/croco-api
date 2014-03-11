package de.lmu.ifi.bio.crco.operation;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.IdentifierType;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.intersect.BindingSiteOverlapCheck;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

public class IntersectTest {
	@Test
	public void testBinding() throws Exception{
		ReadBindingNetwork reader = new ReadBindingNetwork();
		QueryService service = new LocalService(DatabaseConnection.getConnection());
		
		reader.setInput(ReadBindingNetwork.QueryService, service);
		reader.setInput(ReadBindingNetwork.NetworkHierachyNode, new NetworkHierachyNode(3734, 10090));
		
		BindingEnrichedDirectedNetwork net1 = (BindingEnrichedDirectedNetwork)  reader.operate();
		
		Intersect intersect = new Intersect();
		intersect.setInput(Intersect.IntersectionAnnotationCheck, new BindingSiteOverlapCheck() );
		intersect.setInputNetwork(net1,net1);
		Network ret = intersect.operate();
		System.out.println(ret.getSize() + " " + net1.getSize() );
		assertEquals(ret.getSize(),net1.getSize());
		
		
		reader.setInput(ReadBindingNetwork.NetworkHierachyNode, new NetworkHierachyNode(3735,10090));
		BindingEnrichedDirectedNetwork net2 = (BindingEnrichedDirectedNetwork)  reader.operate();
		
		intersect = new Intersect();
		intersect.setInput(Intersect.IntersectionAnnotationCheck, new BindingSiteOverlapCheck() );
		intersect.setInputNetwork(net1,net2);
		ret = intersect.operate();
		System.out.println(ret.getSize() + " " + net1.getSize() + " " + net2.getSize());
		
		Intersect realIntersect = new Intersect();
		realIntersect.setInputNetwork(net1,net2);
		Network withoutCondition = realIntersect.operate();
		System.out.println(withoutCondition.getSize() + " " + net1.getSize() + " " + net2.getSize());
		assertTrue(withoutCondition.size() > ret.size());
		
		
	}
	
	@Test
	public void testSimpleIntersectionLocalRepo() throws Exception{
		//local repository
		Network network1 = new DirectedNetwork("test",9606);
		network1.add(new Gene("a"), new Gene("b"), 0);
		network1.add(new Gene("c"), new Gene("d"), 0);
		
		
		Network network2 = new DirectedNetwork("test",9606);
		network2.add(new Gene("a"), new Gene("b"), 0);
		
		Intersect intersect = new Intersect();
		intersect.setInputNetwork(network1,network2);
		Network intersected = intersect.doOperation();
		assertEquals(1,intersected.getSize());
		System.out.println(intersected.getEdge(1));
		
	}
	@Test
	public void testSimpleIntersectionGlobalRepo() throws Exception{
		//local repository
		Network network1 = new DirectedNetwork("test",9606,true);
		network1.add(new Gene("a"), new Gene("b"), 0);
		network1.add(new Gene("c"), new Gene("d"), 0);
		
		
		Network network2 = new DirectedNetwork("test",9606,true);
		network2.add(new Gene("a"), new Gene("b"), 1);
		
		Intersect intersect = new Intersect();
		intersect.setInputNetwork(network1,network2);
		Network intersected = intersect.doOperation();
		assertEquals(1,intersected.getSize());
		for(int edgeId : intersected.getEdgeIds()){
			assertEquals(2,intersected.getAnnotation(edgeId,Network.EdgeOption.GroupId).size());
		}
	}
	
	@Test
	public void testIntersectOnNetwork() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
		
		Network net1 = service.readNetwork(2269,null,false);
		Network net2 = service.readNetwork(2270,null,false);
		
		Intersect intersect = new Intersect();
		intersect.setInputNetwork(net1,net2);
		Network intersected = intersect.doOperation();
		
		Union union = new Union();
		union.setInputNetwork(net1,net2);
		Network unified = union.doOperation();
		
		System.out.println(net1.getSize() + " " + net2.getSize() +  " " + unified.getSize() + " " + intersected.getSize());
		float frac = (float)intersected.getSize()/(float)unified.getSize();
		System.out.println(frac);
	}
}
