package de.lmu.ifi.bio.croco.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.croco.category.IntegrationTest;
import de.lmu.ifi.bio.croco.data.BindingEvidence;
import de.lmu.ifi.bio.croco.data.CroCoNode;
import de.lmu.ifi.bio.croco.data.NetworkMetaInformation;
import de.lmu.ifi.bio.croco.data.Species;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.data.genome.Transcript;
import de.lmu.ifi.bio.croco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.croco.util.CroCoLogger;

@Category(IntegrationTest.class)
public class LocalServiceTest {

	@Test
	public void testInjection() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		logger.setLevel(Level.DEBUG);
		
		/*
		List<Pair<Option, String>> options = new ArrayList<Pair<Option, String>>();
		options.add(new Pair<Option,String>(Option.cellLine,";' SELECT * FROM Network LIMIT 10 ;"));
		*/
		Statement stat = connection.createStatement();
		String condition = "bla'bsfd";
		try{
		    stat.execute(String.format("SELECT * FROM NetworkOption where value  like '%s'",condition));
		    assertTrue(false);
		}catch(Exception e)
		{
		    //an exception is expected
		}
		stat.close();
		
		//service.findNetwork(options );
	}
	@Test
	public void testReadOntology() throws Exception
	{
	  
	    LocalService service = new LocalService();
        CroCoNode rootOrig = service.getNetworkOntology(true);
        
        assertTrue(rootOrig.getChildren().size()>0);
        
        System.out.println(rootOrig.getChildren());
      /*  
        System.out.println("WRITE");
        XStream xstream = new XStream();
        xstream.setMode(XStream.ID_REFERENCES);
        Writer out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(new File("bla"))));
        ObjectOutputStream out2 = xstream.createObjectOutputStream(out);
        
        out2.writeObject(rootOrig);
        
        out2.close();
        out.close();
        
        /*
	    XStream xstream = new XStream();
        xstream.setMode(XStream.ID_REFERENCES);
        
        
	    XStream xstream = new XStream();
        xstream.setMode(XStream.ID_REFERENCES);
        
	    
        System.out.println("READ");
        ObjectInputStream in = xstream.createObjectInputStream(new InputStreamReader( new GZIPInputStream(new FileInputStream(new File("bla")))));
        
        in.readObject();
        
        
        /*
        CroCoNode root = new CroCoNode(rootOrig);
        root.setNetworks(rootOrig.getNetworks());
        root.initChildren(rootOrig);
      
        CroCoNode cellLine = root.getChildren().get(0);
        cellLine.initChildren(rootOrig);
        
        CroCoNode Bcell = cellLine.getChildren().get(10);
        Bcell.initChildren(rootOrig);
      
        CroCoNode factor = Bcell.getChildren().get(0);
        factor.initChildren(rootOrig);
        
        System.out.println(factor.getChildren());
        */
        /*
        bla.initChildren(rootOrig);
        System.out.println(bla);
        System.out.println(bla.getChildren());
	*/
        
        
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
	public void testReadNetwork() throws Exception {
		Logger logger = CroCoLogger.getLogger();
		Connection connection = DatabaseConnection.getConnection();
		logger.setLevel(Level.DEBUG);
		LocalService service = new LocalService(logger);
		service.readNetwork(8, null, false);
	}
	/*
	@Test
	public void testFind() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		logger.setLevel(Level.DEBUG);
		QueryService service = new LocalService(logger);
		List<Pair<Option,String>> options = new ArrayList<Pair<Option,String>>();
		options.add(new Pair<Option,String>(Option.cellLine, "K562"));
		options.add(new Pair<Option,String>(Option.EdgeType, "Directed"));
		
		List<NetworkMetaInformation> networks = service.findNetwork(options);
		System.out.println(networks.size());
	}
	*/
	@Test
	public void getNetworkMetaInformation() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		logger.setLevel(Level.DEBUG);
		LocalService service = new LocalService(logger);
		List<NetworkMetaInformation> nodes = service.getNetworkMetaInformations();
		assertTrue(nodes.size()>0);
		
		NetworkMetaInformation single = service.getNetworkMetaInformation(32);
	
	}
	
	@Test
	public void testGetBindings() throws Exception{
	    LocalService service = new LocalService();
        //loadData("ENSG00000068305","ENSG00000111046");
        
	    List<BindingEvidence> bindings = service.getBindings("ENSG00000068305", "ENSG00000111046");
	    assertTrue(bindings.size()>0);
        
	    for(BindingEvidence b : bindings)
	    {
	        System.out.println(b.peak.getClass());
	        if ( b.peak instanceof TFBSPeak)
	        {
	            TFBSPeak tfbs = (TFBSPeak)b.peak;
	        }
	        assertNotNull(b.peak.getChrom());
	    }
	    
	}
	

	@Test
	public void testLoadHierachy() throws Exception{
		Logger logger = CroCoLogger.getLogger();
		
		QueryService service = new LocalService(logger);
		List<NetworkMetaInformation> root = service.getNetworkMetaInformations();
		System.out.println(root.size());
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
