package de.lmu.ifi.bio.croco.operation;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.croco.category.IntegrationTest;
import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.NetworkMetaInformation;
import de.lmu.ifi.bio.croco.data.exceptions.CroCoException;
import de.lmu.ifi.bio.croco.network.DirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.operation.GeneSetFilter.FilterType;

@Category(IntegrationTest.class)
public class GeneSetFilterTest {

	@Test
	public void testDummyNetwork() throws Exception{
		DirectedNetwork network = new DirectedNetwork((String)null,(Integer) null);
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
		network.printNetwork(new PrintWriter(System.out));
		GeneSetFilter filter = new GeneSetFilter();
		filter.setInput(GeneSetFilter.genes, whiteList);
		filter.setInputNetwork(network);
		network = (DirectedNetwork) filter.operate();
		network.printNetwork(new PrintWriter(System.out));
		
	}
	@Test
	public void testOnBindingNetwork() throws Exception{
		QueryService service = new LocalService();
		ReadNetwork readNetwork = new ReadNetwork();
		
		readNetwork.setInput(ReadNetwork.NetworkMetaInformation,service.getNetworkMetaInformation(8532) );
		readNetwork.setInput(ReadNetwork.QueryService, service);
		Network network = readNetwork.operate();
		System.out.println(network.size());
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
		System.out.println(network.size());
	}
	
	@Test
	public void test() throws Exception {
		QueryService service = new LocalService();
		
		ReadNetwork readNetwork = new ReadNetwork();
		
		readNetwork.setInput(ReadNetwork.NetworkMetaInformation,service.getNetworkMetaInformation(10174) );
		readNetwork.setInput(ReadNetwork.QueryService, service);
		
		Network network = readNetwork.operate();
		assertTrue(network.size()>0);
		GeneSetFilter filter = new GeneSetFilter();
	//	setFilter.setInput(, value)
		HashSet<Entity> whiteList =new HashSet<Entity>();
		whiteList.add(new Entity("ENSG00000103811"));
	//	whiteList.add(new Entity("ENSG00000100092"));
		filter.setInput(GeneSetFilter.genes, whiteList);
		filter.setInput(GeneSetFilter.filterType, FilterType.GeneFilter);
		filter.setInputNetwork(network);
		network = filter.operate();
		//System.out.println(network.getSize());
	}

}
