package de.lmu.ifi.bio.crco.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.Transfer;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.test.IntegrationTest;

@Category(IntegrationTest.class)
public class RemoteWebServiceTest {
	//public static String url="http://services.bio.ifi.lmu.de/croco-web/services";
	public static String url="http://localhost:8080/croco-web/services";
	public static String url_local2="http://tomcluster64:1046/croco-web/services";
	
	
	@Test
	public void testConnection() throws Exception{
	    RemoteWebService remoteService = new RemoteWebService(url);
	    BufferedService service = new BufferedService(remoteService,new File("networkBufferDir/")); 
	    
	}
	
	@Test
	public void testGetVersion() throws Exception{
		Long version = RemoteWebService.getServiceVersion(url +"/getVersion");
		assertEquals(version,(Long)RemoteWebService.version);
	}
	
	@Test
	public void testGetNetworkHierachy() throws Exception{
		RemoteWebService service = new RemoteWebService(url);
		
		service.getNetworkHierachy();
	}
	
	@Test
	public void testGetOrthologMapping() throws Exception{
		RemoteWebService service = new RemoteWebService(url);
		
		List<OrthologMappingInformation> orthologMappings = service.getOrthologMappingInformation(null,Species.Human, Species.Mouse);
		OrthologMapping mapping = service.getOrthologMapping(orthologMappings.get(0));
		assertEquals(16957,mapping.getSize());
	}
	
	
	@Test
	public void testGetOrthologMappingInformation() throws Exception{
		RemoteWebService service = new RemoteWebService(url);
		List<OrthologMappingInformation> orthologMappings = service.getOrthologMappingInformation(null,Species.Human, Species.Mouse);
		assertTrue(orthologMappings.size() > 0);
	
		Transfer transferOperation = new Transfer();
        transferOperation.setInput(Transfer.OrthologMappingInformation, service.getOrthologMappingInformation(null, Species.Human, Species.Mouse));
        transferOperation.setInput(Transfer.OrthologRepository,OrthologRepository.getInstance( service));
        
	}

	@Test
	public void testReadAnnotationEnrichedNetwork() throws Exception{
		RemoteWebService service = new RemoteWebService(url);
		
		BindingEnrichedDirectedNetwork network = service.readBindingEnrichedNetwork(2075, null, false);
	}
	
	@Test
	public void testGetContext() throws Exception{
		RemoteWebService service = new RemoteWebService(url);
		ContextTreeNode node = service.getContextTreeNode("GO:0008150");
		assertEquals(node.getDescription(),"biological_process");
	}

	
	@Test
	public void testListNetwork() throws Exception {
		RemoteWebService service = new RemoteWebService(url);
		NetworkHierachyNode networks = service.getNetworkHierachy();
		assertTrue(networks.getChildren().size()>0);
		assertTrue(networks != null);
	}
	@Test
	public void testReadK562() throws Exception
	{
	    RemoteWebService remoteService = new RemoteWebService(url);
        BufferedService service = new BufferedService(remoteService,new File("networkBufferDir/")); 
        String path="/H. sapiens/Context-Specific Networks/Open Chromatin (TFBS)/" +
                "DNase I hypersensitive sites (DNase)/High Confidence/JASPAR/K562/";
        
	    List<NetworkHierachyNode> k562Networks =  service.getNetworkHierachy().getNode(path).getAllChildren();
	 
	}
	
	@Test
	public void testReadNetwork() throws Exception{
		
		RemoteWebService service = new RemoteWebService(url);
		Network networks = service.readNetwork(10761,null,true);
		
		
		assertTrue(networks.getSize() > 0);
		for(int edgeId : networks.getEdgeIds()){
			assertNotNull(networks.getAnnotation(edgeId, Network.EdgeOption.GroupId));
			assertEquals(10761,networks.getAnnotation(edgeId, Network.EdgeOption.GroupId).get(0));
		}
		
	}
	@Test
	public void testReadNetworkWithContext() throws Exception{
		
		RemoteWebService service = new RemoteWebService(url);
		ContextTreeNode contextNode = service.getContextTreeNode("GO:0032502");
		Network network = service.readNetwork(3619, contextNode.getContextId(),true);
		assertTrue(network.getSize() > 0);
	
	}
}
