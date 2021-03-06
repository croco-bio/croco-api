package de.lmu.ifi.bio.croco.operation;

import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.junit.Test;

import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.NetworkOperationNode;
import de.lmu.ifi.bio.croco.data.Species;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.network.NetworkSummary;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.croco.operation.progress.ProgressInformation;
import de.lmu.ifi.bio.croco.util.CroCoLogger;

public class OperationUtilTest {

	@Test
	public void simpleStructure() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger());
		NetworkOperationNode root = new NetworkOperationNode(null,9695,new Union());
		ReadNetwork reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setInput(ReadNetwork.NetworkMetaInformation, service.getNetworkMetaInformation(252) );
		NetworkOperationNode child1  = new NetworkOperationNode(root, 9606, reader);
		root.addChild(child1);
		
		Network retNetwork = OperationUtil.process(service, root);
		assertTrue(retNetwork.size()>0);
	}
	
	
	@Test
	public void intersectUnion() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger());
		
		
		NetworkOperationNode root = new NetworkOperationNode(null,9695,new Difference());
		ReadNetwork reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setInput(ReadNetwork.NetworkMetaInformation, service.getNetworkMetaInformation(252));
		NetworkOperationNode child1  = new NetworkOperationNode(root, 9606, reader);
		root.addChild(child1);
		
		
		NetworkOperationNode intersect = new NetworkOperationNode(null,9606,new Intersect());

		
		ReadNetwork reader1 = new ReadNetwork();
		reader1.setInput(ReadNetwork.QueryService, service); 
		reader1.setInput(ReadNetwork.NetworkMetaInformation,service.getNetworkMetaInformation(2218) );
		
		ReadNetwork reader2 = new ReadNetwork();
		reader2.setInput(ReadNetwork.QueryService, service);
		reader2.setInput(ReadNetwork.NetworkMetaInformation,service.getNetworkMetaInformation(2219));
		
		ReadNetwork reader3 = new ReadNetwork();
		reader3.setInput(ReadNetwork.QueryService, service);
		reader3.setInput(ReadNetwork.NetworkMetaInformation,service.getNetworkMetaInformation(2220) );
		
		intersect.addChild(new NetworkOperationNode(null,9606,reader1));
		intersect.addChild(new NetworkOperationNode(null,9606,reader2));
		intersect.addChild(new NetworkOperationNode(null,9606,reader3));

		
		Transfer transferOp = new Transfer();
		
		OrthologMappingInformation mapping = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, Species.Human, Species.Mouse).get(0);
		List<OrthologMappingInformation>  mappings = new ArrayList<OrthologMappingInformation>();
		mappings.add(mapping);

		transferOp.setInput(Transfer.OrthologMappingInformation, mappings);
		transferOp.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
		NetworkOperationNode transfer = new NetworkOperationNode(null,9695,transferOp);
		transfer.addChild(intersect);
		
		root.addChild(transfer);
		
		root.print(new PrintWriter(System.out));
		
		int k =OperationUtil.getNumberOfOperations(root);
		Network retNetwork = OperationUtil.process(service, root);
		System.out.println(retNetwork.size());
	
		
		
		
	}
	
	@Test
	public void testTransferRead() throws Exception
	{
	    QueryService service = new LocalService(CroCoLogger.getLogger());
        
	    ReadNetwork read= new ReadNetwork();
	    read.setInput(ReadNetwork.NetworkMetaInformation,service.getNetworkMetaInformation(3592));
	    read.setInput(ReadNetwork.GlobalRepository, false);
	    read.setInput(ReadNetwork.QueryService, service);
	    Network network = read.operate();
	    
	    Transfer transfer = new Transfer();
	    transfer.setInput(Transfer.QueryService, service);
	    OrthologMappingInformation mapping = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, Species.Human, Species.Mouse).get(0);
	    List<OrthologMappingInformation>  mappings = new ArrayList<OrthologMappingInformation>();
	    mappings.add(mapping);

	    transfer.setInput(Transfer.OrthologMappingInformation, mappings);
	    transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
	                   
	    transfer.setInputNetwork(network);
	    
	    NetworkOperationNode root  = new NetworkOperationNode(null, 9606, transfer);
        
	    Network nTransfer = OperationUtil.process(service, root);
	    
	    System.out.println(network.getTaxId() + " " +network.size());
	    System.out.println(nTransfer.getTaxId()+ " "+ nTransfer.size());
	}
	
	@Test
	public void transferTest() throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger());
		OrthologMappingInformation mapping = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, Species.Human, Species.Mouse).get(0);

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
		QueryService service = new LocalService(CroCoLogger.getLogger());
		
		ReadNetwork reader1 = new ReadNetwork();
		reader1.setInput(ReadNetwork.QueryService, service);
		reader1.setInput(ReadNetwork.NetworkMetaInformation,service.getNetworkMetaInformation(299) );
		
		root.addChild(new NetworkOperationNode(null,9606,reader1));
		
		ReadNetwork reader2 = new ReadNetwork();
		reader2.setInput(ReadNetwork.QueryService, service);
		reader2.setInput(ReadNetwork.NetworkMetaInformation,service.getNetworkMetaInformation(399) );
		
		root.addChild(new NetworkOperationNode(null,9606,reader2));
		
		int k =OperationUtil.getNumberOfOperations(root);
		ProgressInformation pi = new ProgressInformation(k);
		
		
		Network network = OperationUtil.process(service, root,pi);

		assertTrue(network.size() > 0);
		
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
