package de.lmu.ifi.bio.crco.connector;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.test.IntegrationTest;

@Category(IntegrationTest.class)
public class RemoteWebServiceTest {

	@Test
	public void testListNetwork() throws Exception {
		RemoteWebService service = new RemoteWebService("http://localhost:8080/croco-web/services/");
		NetworkHierachyNode networks = service.getNetworkHierachy(null);
		System.out.println(networks);
	}

	@Test
	public void testReadNetwork() throws Exception{
		RemoteWebService service = new RemoteWebService("http://localhost:8080/croco-web/services/");
		Network networks = service.readNetwork(1384, null,false);
		System.out.println(networks.getSize());
	
	}
}
