package de.lmu.ifi.bio.crco.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network.EdgeOption;
import de.lmu.ifi.bio.crco.util.Tuple;

public class ReadBindingNetworkTest {

	@Test
	public void test() throws Exception {
		ReadBindingNetwork reader = new ReadBindingNetwork();
		QueryService service = new LocalService(DatabaseConnection.getConnection());
		
		NetworkHierachyNode th1 = service.getNetworkHierachy("OpenChromTFBS/Human/DNase/0.000001/JASPAR/Th1/").getChildren().get(0);
		
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

}
