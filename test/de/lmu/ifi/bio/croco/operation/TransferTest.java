package de.lmu.ifi.bio.croco.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.Species;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.croco.util.CroCoLogger;

public class TransferTest {
    /*
	@Test
	public void testK562() throws Exception
	{
	    RemoteWebService remoteService = new RemoteWebService("http://localhost:8080/croco-web/services");
        BufferedService service = new BufferedService(remoteService,new File("networkBufferDir/")); 
        List<NetworkHierachyNode> k562Networks =  service.getNetworkHierachy().getNode("/H. sapiens/Context-Specific Networks/Open Chromatin (TFBS)/DNase I hypersensitive sites (DNase)/High Confidence/JASPAR/K562/").getAllChildren();
        
        ReadNetwork reader = new ReadNetwork();
        reader.setInput(ReadNetwork.QueryService, service);
        reader.setInput(ReadNetwork.NetworkHierachyNode, k562Networks.get(0));
            
        Network k562_rep1 = reader.operate();
        
        reader.setInput(ReadNetwork.QueryService, service);
        reader.setInput(ReadNetwork.NetworkHierachyNode, k562Networks.get(1));
            
        Network k562_rep2 = reader.operate();
        
        Intersect intersect = new Intersect();
        intersect.setInputNetwork(k562_rep1,k562_rep2);
        Network k562_rep12 = intersect.operate();
        
        Transfer transfer = new Transfer();
        transfer.setInputNetwork(k562_rep12);
        transfer.setInput(Transfer.OrthologMappingInformation, service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, Species.Human, Species.Mouse));
        transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));

        Network transferred = transfer.operate();
        assertEquals((int)transferred.getTaxId(),10090);
	}
    */
   
    
	@Test
	public void transferFlyHuman() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger());

		
		List<OrthologMappingInformation> orthologMappingInformatons = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, Species.Human, Species.Fly);
		assertEquals(1,orthologMappingInformatons.size());
		OrthologMappingInformation mapping = orthologMappingInformatons.get(0);
		List<OrthologMappingInformation> mappings = new ArrayList<OrthologMappingInformation>();
		mappings.add(mapping);
		Network flyNetwork = service.readNetwork(86,null,false);
		assertTrue(flyNetwork.size() > 0);
		
		Transfer transfer = new Transfer();
		transfer.setInputNetwork(flyNetwork);
		transfer.setInput(Transfer.OrthologMappingInformation, mappings);
		transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
		
		Network transferred = transfer.operate();
		assertTrue(transferred.size() > 0);
		System.out.println("Human ensembl network size:\t" + transferred.size());

	}
	
	@Test
	public void transferHumanCow() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger());


		List<OrthologMappingInformation> orthologMappingInformatons = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, Species.Human, new Species(9913));
		assertEquals(1,orthologMappingInformatons.size());
		OrthologMappingInformation mapping = orthologMappingInformatons.get(0);
		List<OrthologMappingInformation> mappings = new ArrayList<OrthologMappingInformation>();
		mappings.add(mapping);
		
		Network humanTestNetwork = service.readNetwork(3619,null,false);
		assertTrue(humanTestNetwork.size() > 0);
		assertEquals((Integer)humanTestNetwork.getTaxId(),(Integer)9606);
		System.out.println("Human network size:\t" + humanTestNetwork.size());
		
		Transfer transfer = new Transfer();
		transfer.setInputNetwork(humanTestNetwork);
		transfer.setInput(Transfer.OrthologMappingInformation, mappings);
		transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
		
		Network transferred = transfer.operate();
		assertTrue(transferred.size() > 0);
		System.out.println("Cow ensembl network size:\t" + transferred.size());

	}
}
