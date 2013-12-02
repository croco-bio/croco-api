package de.lmu.ifi.bio.crco.data.network;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.bio.crco.data.IdentifierType;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;
import de.lmu.ifi.bio.crco.util.OrderedPair;

public class NetworkTest {

	@Test
	public void testAnnotation() throws Exception{
		Network network1 = new DirectedNetwork("test",9606);
		network1.add(new Gene("a"), new Gene("b"), 100);
		network1.add(new Gene("a"), new Gene("b"), 101);
	
		network1.add(new Gene("a"), new Gene("c"), 101);
		
		assertEquals(2,network1.getAnnotation(network1.getEdgeId(new Gene("a"), new Gene("b")),EdgeOption.GroupId).size());
		
		assertEquals(1,network1.getAnnotation(network1.getEdgeId(new Gene("a"), new Gene("c")),EdgeOption.GroupId).size());
		
	}
	
	@Test
	public void testDirected() throws Exception {
		/*File configFile = new File("/Users/robert/Documents/workspace/CroCo-API/conf/connet.config");
		
		Logger logger = ApplicationLogger.getLogger(configFile);
		Connection con = DatabaseConnection.getConnection(configFile);
		
		QueryService service = new LocalService(logger,con);
		/*
		Network network1 = service.readNetwork(106);
		Network network2 = service.readNetwork(107);
		*/
		Network network1 = new DirectedNetwork("test",9606);
		network1.add(new Gene("a"), new Gene("b"), 0);
		network1.add(new Gene("a"), new Gene("b"), 0);
		
		assertEquals(network1.getSize(),1);
		
		Network network2 = new DirectedNetwork("test",9606);
		network2.add(new Gene("a"), new Gene("b"), 0);
		System.out.println(	network2.getEdgeIds());
		assertEquals(network2.getEdge(1),new OrderedPair<Gene,Gene>(new Gene("a"),new Gene("b")));
		
	
	
		
	}
	@Test
	public void testUndirected() throws Exception{
		Network network1 = new DirectedNetwork("test",9606);
		network1.add(new Gene("a"), new Gene("b"), 0);
		network1.add(new Gene("b"), new Gene("a"), 0);
		
		assertEquals(network1.getSize(),1);
		
		Network network2 = new DirectedNetwork("test",9606);
		network2.add(new Gene("a"), new Gene("b"), 0);
		
		
	}
	
}
