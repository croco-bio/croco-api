package de.lmu.ifi.bio.croco.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.croco.network.DirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.network.Network.EdgeOption;

public class UnionTest {
    /*
    @Test
    public void testUnionK562() throws Exception{
        RemoteWebService remoteService = new RemoteWebService("http://localhost:8080/croco-web/services");
        BufferedService service = new BufferedService(remoteService,new File("networkBufferDir/")); 
        List<NetworkMetaInformation> k562Networks =  service.getNetworkHierachy().getNode("/H. sapiens/Context-Specific Networks/Open Chromatin (TFBS)/DNase I hypersensitive sites (DNase)/High Confidence/JASPAR/K562/").getAllChildren();
        
        ReadNetwork reader = new ReadNetwork();
        reader.setInput(ReadNetwork.QueryService, service);
        reader.setInput(ReadNetwork.NetworkMetaInformation, k562Networks.get(0));
            
        Network k562_rep1 = reader.operate();
        
        reader.setInput(ReadNetwork.QueryService, service);
        reader.setInput(ReadNetwork.NetworkMetaInformation, k562Networks.get(1));
            
        Network k562_rep2 = reader.operate();
        
        Union union = new Union();
        union.setInputNetwork(k562_rep1,k562_rep2);
        Network k562_rep12 = union.operate();
    }
    */
	@Test
	public void testUnionBindingSite() throws Exception{
		ReadBindingNetwork reader = new ReadBindingNetwork();
		QueryService service = new LocalService();
		
		reader.setInput(ReadBindingNetwork.QueryService, service);
		reader.setInput(ReadBindingNetwork.NetworkMetaInformation,service.getNetworkMetaInformation(3734));
		
		BindingEnrichedDirectedNetwork net1 = (BindingEnrichedDirectedNetwork)  reader.operate();
		assertTrue(net1.size()>0);
		
		Union union = new Union();
		union.setInputNetwork(net1,net1);
		Network ret = union.operate();
		assertTrue(ret.size()>0);
		assertEquals(ret.size(),net1.size());
		
		
		reader.setInput(ReadBindingNetwork.NetworkMetaInformation,service.getNetworkMetaInformation(3735) );
		BindingEnrichedDirectedNetwork net2 = (BindingEnrichedDirectedNetwork)  reader.operate();
		
		union = new Union();
		union.setInputNetwork(net1,net2);
		ret = union.operate();
		assertTrue(ret.size()>=net1.size() && ret.size() >= net2.size());
		
		
		
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
		
		assertEquals(network.size(),2);
		
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
