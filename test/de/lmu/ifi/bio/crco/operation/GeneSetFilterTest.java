package de.lmu.ifi.bio.crco.operation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.exceptions.CroCoException;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.test.IntegrationTest;

@Category(IntegrationTest.class)
public class GeneSetFilterTest {

	@Test
	public void testDummyNetwork() throws Exception{
		DirectedNetwork network = new DirectedNetwork(null, null);
		network.add(new Entity("SP1"), new Entity("T1"), 0);
		network.add(new Entity("SP1"), new Entity("T2"), 0);
		
		network.add(new Entity("SP1"), new Entity("CTCF"), 0);
		network.add(new Entity("CTCF"), new Entity("STAT1"), 0);
		
		network.add(new Entity("STAT1"), new Entity("T3"), 0);
		network.add(new Entity("STAT1"), new Entity("T4"), 0);
		
		network.add(new Entity("B"), new Entity("T9"), 0);
		HashSet<Entity> whiteList =new HashSet<Entity>();
		whiteList.add(new Entity("T1"));
		whiteList.add(new Entity("T2"));
		whiteList.add(new Entity("T3"));
		System.out.println(network.printNetwork());
		GeneSetFilter filter = new GeneSetFilter();
		filter.setInput(GeneSetFilter.genes, whiteList);
		filter.setInputNetwork(network);
		network = (DirectedNetwork) filter.operate();
		System.out.println(network.printNetwork());
		
	}
	@Test
	public void testOnBindingNetwork() throws Exception{
		QueryService service = new LocalService();
		ReadNetwork readNetwork = new ReadNetwork();
		
		readNetwork.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(8532,9606));
		readNetwork.setInput(ReadNetwork.QueryService, service);
		Network network = readNetwork.operate();
		System.out.println(network.getSize());
		GeneSetFilter filter = new GeneSetFilter();
		Set<Entity> geneList  = new HashSet<Entity>();
		BufferedReader br = new BufferedReader(new FileReader(new File("/home/users/pesch/Dropbox/Data/Analysis/CGC/out")));
		String line = null;
		while((line = br.readLine())!=null){
			geneList.add(new Entity(line.trim()));
		}
		
		br.close();
		
		filter.setInput(GeneSetFilter.genes, geneList);
		filter.setInputNetwork(network);
		network = filter.operate();
		System.out.println(network.getSize());
	}
	
	@Test
	public void test() throws CroCoException, SQLException, IOException {
		QueryService service = new LocalService();
		
		ReadNetwork readNetwork = new ReadNetwork();
		
		readNetwork.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(10174,9606));
		readNetwork.setInput(ReadNetwork.QueryService, service);
		
		Network network = readNetwork.operate();
		GeneSetFilter filter = new GeneSetFilter();
	//	setFilter.setInput(, value)
		HashSet<Entity> whiteList =new HashSet<Entity>();
		whiteList.add(new Entity("ENSG00000103811"));
	//	whiteList.add(new Entity("ENSG00000100092"));
		filter.setInput(GeneSetFilter.genes, whiteList);
		filter.setInputNetwork(network);
		network = filter.operate();
		//System.out.println(network.getSize());
	}

}
