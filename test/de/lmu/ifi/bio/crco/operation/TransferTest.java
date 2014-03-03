package de.lmu.ifi.bio.crco.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

public class TransferTest {
	
	@Test
	public void transferFlyHuman() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());

		
		List<OrthologMappingInformation> orthologMappingInformatons = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, Species.Human, Species.Fly);
		assertEquals(1,orthologMappingInformatons.size());
		OrthologMappingInformation mapping = orthologMappingInformatons.get(0);
		List<OrthologMappingInformation> mappings = new ArrayList<OrthologMappingInformation>();
		mappings.add(mapping);
		Network flyNetwork = service.readNetwork(86,null,false);
		assertTrue(flyNetwork.getSize() > 0);
		
		Transfer transfer = new Transfer();
		transfer.setInputNetwork(flyNetwork);
		transfer.setInput(Transfer.OrthologMappingInformation, mappings);
		transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
		
		Network transferred = transfer.operate();
		assertTrue(transferred.getSize() > 0);
		System.out.println("Human ensembl network size:\t" + transferred.getSize());

	}
	
	@Test
	public void transferHumanCow() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());


		List<OrthologMappingInformation> orthologMappingInformatons = service.getOrthologMappingInformation(OrthologDatabaseType.InParanoid, Species.Human, new Species(9913));
		assertEquals(1,orthologMappingInformatons.size());
		OrthologMappingInformation mapping = orthologMappingInformatons.get(0);
		List<OrthologMappingInformation> mappings = new ArrayList<OrthologMappingInformation>();
		mappings.add(mapping);
		
		Network humanTestNetwork = service.readNetwork(106,null,false);
		assertTrue(humanTestNetwork.getSize() > 0);
		System.out.println("Human network size:\t" + humanTestNetwork.getSize());
		
		Transfer transfer = new Transfer();
		transfer.setInputNetwork(humanTestNetwork);
		transfer.setInput(Transfer.OrthologMappingInformation, mappings);
		transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
		
		Network transferred = transfer.operate();
		assertTrue(transferred.getSize() > 0);
		System.out.println("Cow ensembl network size:\t" + transferred.getSize());

	}
}
