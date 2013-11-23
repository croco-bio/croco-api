package de.lmu.ifi.bio.crco.operation;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

public class ReadNetworkTest {
	@Test
	public void readNetwork() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());

		
		ReadNetwork reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(2219,9606));
		
		Network network = reader.operate();
		assertTrue(network.getSize() > 0);
		
		ContextTreeNode rootNode = service.getContextTreeNode("GO:0008150");
		reader.setInput(ReadNetwork.ContextTreeNode,rootNode);
		reader.operate();
	}
}
