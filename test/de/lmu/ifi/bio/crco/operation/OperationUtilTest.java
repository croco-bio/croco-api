package de.lmu.ifi.bio.crco.operation;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.junit.Test;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.NetworkOperationNode;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.network.NetworkSummary;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.operation.progress.ProgressInformation;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

public class OperationUtilTest {

	@Test
	public void simpleStructure() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
		NetworkOperationNode root = new NetworkOperationNode(null,9695,new Union());
		ReadNetwork reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(252,9606));
		NetworkOperationNode child1  = new NetworkOperationNode(root, 9606, reader);
		root.addChild(child1);
		
		Network retNetwork = OperationUtil.process(service, root);
		assertTrue(retNetwork.getSize()>0);
		
	}
	
	@Test
	public void intersectUnion() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
		
		
		NetworkOperationNode root = new NetworkOperationNode(null,9695,new Difference());
		ReadNetwork reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(252,9606));
		NetworkOperationNode child1  = new NetworkOperationNode(root, 9606, reader);
		root.addChild(child1);
		
		
		NetworkOperationNode intersect = new NetworkOperationNode(null,9695,new Intersect());
		
		
		ReadNetwork reader1 = new ReadNetwork();
		reader1.setInput(ReadNetwork.QueryService, service); 
		reader1.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(2218,9606));
		
		ReadNetwork reader2 = new ReadNetwork();
		reader2.setInput(ReadNetwork.QueryService, service);
		reader2.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(2219,9606));
		
		ReadNetwork reader3 = new ReadNetwork();
		reader3.setInput(ReadNetwork.QueryService, service);
		reader3.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(2220,9606));
		
		intersect.addChild(new NetworkOperationNode(null,9606,reader1));
		intersect.addChild(new NetworkOperationNode(null,9606,reader2));
		intersect.addChild(new NetworkOperationNode(null,9606,reader3));

		
		Transfer transferOp = new Transfer();
		
		OrthologMappingInformation mapping = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, service.getSpecies(9606), service.getSpecies(10090)).get(0);
		List<OrthologMappingInformation>  mappings = new ArrayList<OrthologMappingInformation>();
		mappings.add(mapping);
		
		transferOp.setInput(Transfer.OrthologMappingInformation, mappings);
		transferOp.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
		NetworkOperationNode transfer = new NetworkOperationNode(null,9695,transferOp);
		transfer.addChild(intersect);
		
		root.addChild(transfer);
		
		int k =OperationUtil.getNumberOfOperations(root);
		Network retNetwork = OperationUtil.process(service, root);
		System.out.println(retNetwork.getSize());
	
		
		
		
	}
	
	@Test
	public void transferTest() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
		OrthologMappingInformation mapping = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, service.getSpecies(9606), service.getSpecies(10090)).get(0);

		OrthologMapping orthologs = service.getOrthologMapping(mapping);
		
		Network humanNet1 = service.readNetwork(252,null,false);
		Network mouseNet1 = service.readNetwork(2218,null,false);
		int k  = 0;
		int w = 0;
	
		
		Set<Entity> humanFactors = humanNet1.getFactors();
		Set<Entity> mouseFactors = mouseNet1.getFactors();
		System.out.println("Human factors:\t" + humanFactors.size());
		System.out.println("Mouse factors:\t" + mouseFactors.size());
		
		HashSet<String> unique = new HashSet<String>();
		HashSet<Entity> tranferredMouse =new HashSet<Entity>();
		for(Entity mouseFactor : mouseFactors){
			Set<Entity> os1 = orthologs.getOrthologs(mouseFactor);
			if ( os1 == null) continue;
			tranferredMouse.addAll(os1);
		}
		System.out.println("Transferrd mouse factors:\t" + tranferredMouse.size());
		tranferredMouse.retainAll(humanFactors);
		System.out.println("Transferrd mouse factors:\t" + tranferredMouse.size());
	
		 System.out.println(unique);
		 System.out.println(unique.size());
		 
	

	}
	
	@Test
	public void process() throws Exception {
		NetworkOperationNode root = new NetworkOperationNode(null,9695,new Union());
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
		
		ReadNetwork reader1 = new ReadNetwork();
		reader1.setInput(ReadNetwork.QueryService, service);
		reader1.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(299,9606));
		
		root.addChild(new NetworkOperationNode(null,9606,reader1));
		
		ReadNetwork reader2 = new ReadNetwork();
		reader2.setInput(ReadNetwork.QueryService, service);
		reader2.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(399,9606));
		
		root.addChild(new NetworkOperationNode(null,9606,reader2));
		
		int k =OperationUtil.getNumberOfOperations(root);
		ProgressInformation pi = new ProgressInformation(k);
		
		
		Network network = OperationUtil.process(service, root,pi);

		assertTrue(network.getSize() > 0);
		
		//System.out.println(network.getNetworkSummary());
		
		Stack<NetworkSummary> stack = new Stack<NetworkSummary>();
		stack.add(network.getNetworkSummary());
		while(!stack.isEmpty()){
			NetworkSummary top = stack.pop();
			System.out.println(top);
			for(NetworkSummary child : top.getChildren()){
				stack.add(child);
			}
		}
		
		//System.out.println(network.getNetworkSummary().getChildren());
	}

}
