package de.lmu.ifi.bio.crco.operation;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.Gene;
import de.lmu.ifi.bio.crco.data.IdentifierType;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.test.IntegrationTest;
import de.lmu.ifi.bio.crco.util.Tuple;

@Category(IntegrationTest.class)
public class GeneAnnotationEnrichmentTest {

	@Test
	public void testNameMapping() throws Exception{
		Network network = new DirectedNetwork("test",9606);
		network.add(new Gene("ENSG00000185591"), new Gene("ENSG00000185591"), 0);
		GeneAnnotationEnrichment gae = new GeneAnnotationEnrichment();
		
		gae.setInput(GeneAnnotationEnrichment.QueryService, new LocalService(DatabaseConnection.getConnection()));
		gae.setInputNetwork(network);
		network = gae.operate();
		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> tuple = network.getEdge(edgeId);
			
			System.out.println(tuple.getFirst().getName());
			System.out.println(tuple.getSecond().getName());
			
		}
	}

}
