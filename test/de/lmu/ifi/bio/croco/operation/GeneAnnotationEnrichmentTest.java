package de.lmu.ifi.bio.croco.operation;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.croco.category.IntegrationTest;
import de.lmu.ifi.bio.croco.connector.DatabaseConnection;
import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.IdentifierType;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.network.DirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.util.Tuple;

@Category(IntegrationTest.class)
public class GeneAnnotationEnrichmentTest {

	@Test
	public void testNameMapping() throws Exception{
		Network network = new DirectedNetwork("test",9606);
		network.add(new Gene("ENSG00000185591"), new Gene("ENSG00000185591"), 0);
		GeneAnnotationEnrichment gae = new GeneAnnotationEnrichment();
		
		gae.setInput(GeneAnnotationEnrichment.QueryService, new LocalService());
		gae.setInputNetwork(network);
		network = gae.operate();
		for(int edgeId : network.getEdgeIds()){
			Tuple<Entity, Entity> tuple = network.getEdge(edgeId);
			
			System.out.println(tuple.getFirst().getName());
			System.out.println(tuple.getSecond().getName());
			
		}
	}

}
