package de.lmu.ifi.bio.croco.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.croco.data.CroCoNode;
import de.lmu.ifi.bio.croco.data.NetworkMetaInformation;
import de.lmu.ifi.bio.croco.data.Species;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.croco.test.IntegrationTest;

@Category(IntegrationTest.class)
public class BufferedLocalServiceTest {
    File tmpDir = new File("/tmp/");
    
    @Test
    public void remoteServiceWrapped() throws Exception
    {
        RemoteWebService s = new RemoteWebService(RemoteWebServiceTest.url);
        BufferedService service = new BufferedService(s,tmpDir);
        
        assertNotNull(service.getVersion());
        
    }
    @Test
    public void testGetOntology() throws Exception{
        BufferedService service = new BufferedService(new LocalService(),tmpDir);
        service.clean();
        
        assertEquals(0,service.getBufferedFiles().length);    
     
        CroCoNode<NetworkMetaInformation> root =service.getNetworkOntology(false);
        int childrenSize = root.getChildren().size();
        assertEquals(1,service.getBufferedFiles().length);    
        root =service.getNetworkOntology(false);
        assertEquals(root.getChildren().size(),childrenSize);
    }
    
    @Test
    public void testReadNetwork() throws Exception
    {
        BufferedService service = new BufferedService(new LocalService(),tmpDir);
        service.clean();
        
        assertEquals(0,service.getBufferedFiles().length);    
     
        CroCoNode<NetworkMetaInformation> root =service.getNetworkOntology(false);
        assertEquals(1,service.getBufferedFiles().length);    
        
        Random rnd = new Random(0);
        
        List<NetworkMetaInformation> networks = new ArrayList<NetworkMetaInformation>(root.getData());
        
        assertTrue(networks.size()>0);
        
        Collections.sort(networks);
        Collections.shuffle(networks,rnd);
        
        Network network = service.readNetwork(networks.get(0).getGroupId(), null,false);
        assertEquals(2,service.getBufferedFiles().length);    
        
        Network networkNew = service.readNetwork(networks.get(0).getGroupId(), null,false);
        
        assertTrue(network.equals(networkNew));
    }
    
	@Test
	public void testGetOrtholog() throws Exception{
		
		BufferedService service = new BufferedService(new LocalService(),tmpDir);
		service.clean();
		
		assertEquals(0,service.getBufferedFiles().length);    
		
		OrthologMapping mapping = service.getOrthologMapping(service.getOrthologMappingInformation(null, Species.Human,Species.Mouse).get(0));
		int mappings = mapping.getSize();
		assertTrue(mappings>0);
		assertEquals(1,service.getBufferedFiles().length);    
        
		service.clean();
		assertEquals(0,service.getBufferedFiles().length);    
        
		
		service.getOrthologMapping(service.getOrthologMappingInformation(null, Species.Human,Species.Mouse).get(0));
		assertEquals(mappings,mapping.getSize());
	}
}
