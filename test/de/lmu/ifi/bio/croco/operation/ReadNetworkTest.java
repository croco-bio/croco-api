package de.lmu.ifi.bio.croco.operation;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.bio.croco.connector.DatabaseConnection;
import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.ContextTreeNode;
import de.lmu.ifi.bio.croco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.util.CroCoLogger;

public class ReadNetworkTest {
	/*
    @Test
	public void testReadNetworkWrapper() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger());
		
		ReadNetwork reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setNetworkPathParameter("/OpenChromTFBS/Human/DNase/0.000001/JASPAR/K562/[replicate=2]");
		System.out.println(reader.getParameter(ReadNetwork.NetworkHierachyNode).getGroupId());
	}
	*/
	@Test
	public void readNetwork() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger());

		
		ReadNetwork reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(2219,9606));
		
		Network network = reader.operate();
		assertTrue(network.size() > 0);
		
		ContextTreeNode rootNode = service.getContextTreeNode("GO:0008150");
		reader.setInput(ReadNetwork.ContextTreeNode,rootNode);
		reader.operate();
	}
}
