package de.lmu.ifi.bio.croco.operation;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.croco.connector.BufferedService;
import de.lmu.ifi.bio.croco.connector.DatabaseConnection;
import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.connector.RemoteWebService;
import de.lmu.ifi.bio.croco.data.IdentifierType;
import de.lmu.ifi.bio.croco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.croco.network.DirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.operation.intersect.BindingSiteOverlapCheck;
import de.lmu.ifi.bio.croco.util.CroCoLogger;

public class IntersectTest {
    /*
    @Test
    public void testIntersectk562() throws Exception
    {
        RemoteWebService remoteService = new RemoteWebService("http://localhost:8080/croco-web/services");
        BufferedService service = new BufferedService(remoteService,new File("networkBufferDir/")); 
        List<NetworkHierachyNode> k562Networks =  service.getNetworkHierachy().getNode("/H. sapiens/Context-Specific Networks/Open Chromatin (TFBS)/DNase I hypersensitive sites (DNase)/High Confidence/JASPAR/K562/").getAllChildren();
        
        ReadNetwork reader = new ReadNetwork();
        reader.setInput(ReadNetwork.QueryService, service);
        reader.setInput(ReadNetwork.NetworkHierachyNode, k562Networks.get(0));
            
        Network k562_rep1 = reader.operate();
        
        reader.setInput(ReadNetwork.QueryService, service);
        reader.setInput(ReadNetwork.NetworkHierachyNode, k562Networks.get(1));
            
        Network k562_rep2 = reader.operate();
        
        Intersect intersect = new Intersect();
        intersect.setInputNetwork(k562_rep1,k562_rep2);
        Network k562_rep12 = intersect.operate();
        
        CroCoLogger.getLogger().info("Intersection:" +k562_rep12.getSize());
        assertTrue(k562_rep12.size()<k562_rep1.size());
        assertTrue(k562_rep12.size()<k562_rep2.size());
        
    }
    */
	@Test
	public void testBinding() throws Exception{
		ReadBindingNetwork reader = new ReadBindingNetwork();
		QueryService service = new LocalService();
		
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
		QueryService service = new LocalService();
		
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
