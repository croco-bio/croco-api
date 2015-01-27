package de.lmu.ifi.bio.crco.connector;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.IdentifierType;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.data.genome.Transcript;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.test.IntegrationTest;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;

@Category(IntegrationTest.class)
public class LocalServiceTest {

	@Test
	public void testInjection() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		logger.setLevel(Level.DEBUG);
		QueryService service = new LocalService(logger);
		
		/*
		List<Pair<Option, String>> options = new ArrayList<Pair<Option, String>>();
		options.add(new Pair<Option,String>(Option.cellLine,";' SELECT * FROM Network LIMIT 10 ;"));
		*/
		Statement stat = connection.createStatement();
		String condition = "bla'bsfd";
		stat.execute(String.format("SELECT * FROM NetworkOption where value  like '%s'",condition));
		stat.close();
		
		//service.findNetwork(options );
	}
	@Test
	public void getGenes() throws Exception{
		QueryService service = new LocalService();
		List<Gene> genes = service.getGenes(Species.Human,true,null);
		assertEquals(genes.size(),22553);
		for(Gene gene : genes){
			if (gene.getName().equals("TP53")){
				for(Transcript transcript: gene.getTranscripts()){
					assertNotNull(transcript.getStrandCorredStart() );
				}
			}
		}
		
		
		
	}
	@Test
	public void testGetNetworkInfo() throws Exception{
		QueryService service = new LocalService();
		System.out.println(service.getNetworkInfo(10754));
	}
	
	@Test
	public void testReadNetwork() throws Exception {
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		logger.setLevel(Level.DEBUG);
		QueryService service = new LocalService(logger);
		service.readNetwork(3463, null, false);
	}
	
	@Test
	public void testFind() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		logger.setLevel(Level.DEBUG);
		QueryService service = new LocalService(logger);
		List<Pair<Option,String>> options = new ArrayList<Pair<Option,String>>();
		options.add(new Pair<Option,String>(Option.cellLine, "K562"));
		options.add(new Pair<Option,String>(Option.EdgeType, "Directed"));
		
		List<NetworkHierachyNode> networks = service.findNetwork(options);
		System.out.println(networks.size());
	}
	
	@Test
	public void testGetNetworkHierachy() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		logger.setLevel(Level.DEBUG);
		LocalService service = new LocalService(logger);
		NetworkHierachyNode rootNode = service.getNetworkHierachy(null);
		System.out.println(rootNode.getChildren());
		
		for(NetworkHierachyNode child : rootNode.getAllChildren())
		{
		    System.out.println(child.getOptions().get(Option.TaxId) + "\t" + child.getFactors().size() + " " + child.getOptions());
		}
	}
	

	@Test
	public void testLoadHierachy() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		
		QueryService service = new LocalService(logger);
		NetworkHierachyNode root = service.getNetworkHierachy(null);
		System.out.println(root.getChildren());
	}

	

	/*
	@Test
	public void readIdMapping() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		
		QueryService service = new LocalService(logger,connection);
		Species human = new Species("Human",9606);
		for(Pair<IdentifierType, IdentifierType> p  : service.getIdMapping(human)){
			
			
			IdMapping idMapping = service.getIdMapping(human, p.getFirst(),p.getSecond());
			System.out.println(idMapping.getId1() + "\t" + idMapping.getId2());
			
			
		}
		
		
	}
	*/
	/*
	@Test
	public void getIdMapping() throws Exception {
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		
		QueryService service = new LocalService(logger,connection);
		Species human = new Species("Human",9606);
		List<Pair<IdentifierType, IdentifierType>> humanMappings = service.getIdMapping(human);
		System.out.println(humanMappings);
	}
	*/
	@Test
	public void getTransferTargetSpecies() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		
		QueryService service = new LocalService(logger);
		
		List<OrthologMappingInformation> mappings = service.getTransferTargetSpecies(9606);
		assertTrue(mappings.size() > 10);
	}
}
