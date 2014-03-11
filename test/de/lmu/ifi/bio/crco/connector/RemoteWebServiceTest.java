package de.lmu.ifi.bio.crco.connector;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.test.IntegrationTest;

@Category(IntegrationTest.class)
public class RemoteWebServiceTest {
	public static String url="http://services.bio.ifi.lmu.de/croco-web/services";
	public static String url_local="http://localhost:8080/croco-web/services";
	
	
	@Test
	public void testGetVersion() throws Exception{
		Long version = RemoteWebService.getServiceVersion(url +"/getVersion");
		assertEquals(version,(Long)RemoteWebService.version);
		
		
	}
	
	@Test
	public void testGetNetworkHierachy() throws Exception{
		RemoteWebService service = new RemoteWebService(url);
		
		service.getNetworkHierachy("");
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
		List<OrthologMappingInformation> orthologMappings = service.getOrthologMappingInformation(null,Species.Human, null);
		assertTrue(orthologMappings.size() > 0);
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
		RemoteWebService service = new RemoteWebService(url_local);
		NetworkHierachyNode networks = service.getNetworkHierachy("");
		assertTrue(networks != null);
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
		
		System.out.println(networks.getTaxId());
	}
	@Test
	public void testReadNetworkWithContext() throws Exception{
		
		RemoteWebService service = new RemoteWebService(url);
		ContextTreeNode contextNode = service.getContextTreeNode("GO:0032502");
		Network networks = service.readNetwork(10761, contextNode.getContextId(),true);
		assertTrue(networks.getSize() > 0);
	
	}
}
