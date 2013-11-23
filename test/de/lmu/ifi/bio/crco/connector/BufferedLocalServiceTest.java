package de.lmu.ifi.bio.crco.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.test.IntegrationTest;
import de.lmu.ifi.bio.crco.test.UnitTest;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

@Category(IntegrationTest.class)
public class BufferedLocalServiceTest {
	@Test
	public void testBufferedReader() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		
		
		BufferedService service = new BufferedService(new LocalService(logger,connection),new File("/tmp/"));
		
		service.clean();
		Network network1 = service.readNetwork(2219,null,false);
		Network network2 = service.readNetwork(2219,null,false);
		
		assertTrue(network1.getSize() > 0);
		assertEquals(network1.getSize(),network2.getSize());
		
		
		for(int edgeId : network1.getEdgeIds()){
			if (! network2.containsEdgeId(edgeId)){
				assertFalse(true);
			}
		}
	
	}
}
