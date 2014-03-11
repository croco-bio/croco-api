package de.lmu.ifi.bio.crco.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.test.IntegrationTest;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

@Category(IntegrationTest.class)
public class BufferedLocalServiceTest {
	@Test
	public void testReadNetworkLocalService() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		
		
		BufferedService service = new BufferedService(new LocalService(logger,connection),new File("/tmp/"));
		NetworkHierachyNode testNetworkNode = service.getNetworkHierachy("H. Sapiens/Global Networks/Transcription Factor Binding Sites/High Confidence/JASPAR");
		
		service.clean();
		Network network1 = service.readNetwork(testNetworkNode.getGroupId(),null,false);
		for(int edgeId : network1.getEdgeIds()){
			assertEquals(testNetworkNode.getGroupId(),network1.getAnnotation(edgeId,Network.EdgeOption.GroupId).get(0));
		}
		
		Network network2 = service.readNetwork(testNetworkNode.getGroupId(),null,false);
		for(int edgeId : network2.getEdgeIds()){
			assertEquals(testNetworkNode.getGroupId(),network1.getAnnotation(edgeId,Network.EdgeOption.GroupId).get(0));
		}
		
		assertTrue(network1.getSize() > 0);
		assertEquals(network1.getSize(),network2.getSize());
		
		for(int edgeId : network1.getEdgeIds()){
			if (! network2.containsEdgeId(edgeId)){
				assertFalse(true);
			}
		}
	
	}
	@Test
	public void testReadNetworkRemoteService() throws Exception{
		
		
		BufferedService service = new BufferedService(new RemoteWebService(RemoteWebServiceTest.url_local),new File("/tmp/"));
		NetworkHierachyNode testNetworkNode = service.getNetworkHierachy("H. Sapiens/Global Networks/Transcription Factor Binding Sites/High Confidence/JASPAR");
		
		service.clean();
		Network network1 = service.readNetwork(testNetworkNode.getGroupId(),null,false);
		for(int edgeId : network1.getEdgeIds()){
			assertEquals(testNetworkNode.getGroupId(),network1.getAnnotation(edgeId,Network.EdgeOption.GroupId).get(0));
		}
		
		Network network2 = service.readNetwork(testNetworkNode.getGroupId(),null,false);
		for(int edgeId : network2.getEdgeIds()){
			assertEquals(testNetworkNode.getGroupId(),network2.getAnnotation(edgeId,Network.EdgeOption.GroupId).get(0));
		}
		
		assertTrue(network1.getSize() > 0);
		assertEquals(network1.getSize(),network2.getSize());
		
		for(int edgeId : network1.getEdgeIds()){
			if (! network2.containsEdgeId(edgeId)){
				assertFalse(true);
			}
		}
	
	}
	
	
	@Test
	public void testGetOrtholog() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		
		
		BufferedService service = new BufferedService(new LocalService(logger,connection),new File("/tmp/"));
		OrthologMapping mapping = service.getOrthologMapping(service.getOrthologMappingInformation(null, Species.Human,Species.Mouse).get(0));
		assertEquals(41910,mapping.getSize());
		service.getOrthologMapping(service.getOrthologMappingInformation(null, Species.Human,Species.Mouse).get(0));
		assertEquals(41910,mapping.getSize());
	}
}
