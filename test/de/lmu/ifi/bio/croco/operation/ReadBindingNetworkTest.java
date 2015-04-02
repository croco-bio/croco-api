package de.lmu.ifi.bio.croco.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.croco.connector.BufferedService;
import de.lmu.ifi.bio.croco.connector.DatabaseConnection;
import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.connector.RemoteWebService;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.croco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.network.Network.EdgeOption;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.Tuple;

public class ReadBindingNetworkTest {
    /*
    @Test
    public void testReadK562() throws Exception
    {
        RemoteWebService remoteService = new RemoteWebService("http://localhost:8080/croco-web/services");
        BufferedService service = new BufferedService(remoteService,new File("networkBufferDir/")); 
        
        String path="/H. sapiens/Context-Specific Networks/Open Chromatin (TFBS)/" +
                "DNase I hypersensitive sites (DNase)/High Confidence/JASPAR/K562/";
        List<NetworkHierachyNode> k562Networks =  service.getNetworkHierachy().getNode(path).getAllChildren();

        
        ReadBindingNetwork reader = new ReadBindingNetwork();
        reader.setInput(ReadBindingNetwork.QueryService, service);
        reader.setInput(ReadBindingNetwork.NetworkHierachyNode, k562Networks.get(0));
            
        BindingEnrichedDirectedNetwork net = (BindingEnrichedDirectedNetwork)  reader.operate();
    
        for(int edgeId : net.getEdgeIds())
        {
            Tuple<Entity, Entity> edge = net.getEdge(edgeId);
            List<Peak> peaks = net.getAnnotation(edgeId,Network.EdgeOption.BindingSite);
            
            for(Peak peak :peaks)
            {
              CroCoLogger.getLogger().info(String.format("%s-%s chr:%s start:%d end:%d", edge.getFirst(),edge.getSecond(),peak.getChrom(),peak.getStart(),peak.getEnd()));
            }
        }
    }
    
	@Test
	public void test() throws Exception {
		ReadBindingNetwork reader = new ReadBindingNetwork();
		QueryService service = new LocalService();
		
		NetworkHierachyNode th1 = service.getNetworkHierachy().getNode("OpenChromTFBS/Human/DNase/0.000001/JASPAR/Th1/").getChildren().get(0);
		
		reader.setInput(ReadBindingNetwork.QueryService, service);
		reader.setInput(ReadBindingNetwork.NetworkHierachyNode, th1);
		
		BindingEnrichedDirectedNetwork net = (BindingEnrichedDirectedNetwork)  reader.operate();
	//	System.out.println(net.getSize());
		assertTrue(net.size() > 0);
		
		for(int edgeId : net.getEdgeIds()){
			assertNotNull(net.getBindings(edgeId));
			
			List<Integer>groupIds = net.getAnnotation(edgeId,EdgeOption.GroupId,Integer.class);
			assertEquals(1,groupIds.size());
			
			List<Peak> bindings = net.getAnnotation(edgeId,EdgeOption.BindingSite,Peak.class);
			assertTrue(bindings.size()>0);
			
		//	System.out.println(e.getFirst() + "\t" + e.getSecond()+ "\t" +  net.getBindings(edgeId));
		}
		
		reader.setInput(ReadBindingNetwork.ContextTreeNode, service.getContextTreeNode("GO:0035556"));
		net = (BindingEnrichedDirectedNetwork)  reader.operate();
			assertTrue(net.size() > 0);
			
	}
*/
}
