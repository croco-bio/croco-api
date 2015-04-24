package de.lmu.ifi.bio.croco.operation;


public class ReadBindingNetworkTest {
    /*
    @Test
    public void testReadK562() throws Exception
    {
        RemoteWebService remoteService = new RemoteWebService("http://localhost:8080/croco-web/services");
        BufferedService service = new BufferedService(remoteService,new File("networkBufferDir/")); 
        
        String path="/H. sapiens/Context-Specific Networks/Open Chromatin (TFBS)/" +
                "DNase I hypersensitive sites (DNase)/High Confidence/JASPAR/K562/";
        List<NetworkMetaInformation> k562Networks =  service.getNetworkHierachy().getNode(path).getAllChildren();

        
        ReadBindingNetwork reader = new ReadBindingNetwork();
        reader.setInput(ReadBindingNetwork.QueryService, service);
        reader.setInput(ReadBindingNetwork.NetworkMetaInformation, k562Networks.get(0));
            
        BindingEnrichedDirectedNetwork net = (BindingEnrichedDirectedNetwork)  reader.operate();
    
        for(int edgeId : net.getEdgeIds())
        {
            Tuple<Entity, Entity> edge = net.getEdge(edgeId);
            List<Peak> peaks = net.getAnnotation(edgeId,Network.EdgeOption.BindingSite);
            
            for(Peak peak :peaks)
            {
              CroCoLogger.getLogger().info(String.format("%s-%s chr:%s start:%d end:%d", edge.getFirst(),edge.getSecond(),peak.getChrom(),peak.getStart(),peak.getEnd()));
            }
        }
    }
    
	@Test
	public void test() throws Exception {
		ReadBindingNetwork reader = new ReadBindingNetwork();
		QueryService service = new LocalService();
		
		NetworkMetaInformation th1 = service.getNetworkHierachy().getNode("OpenChromTFBS/Human/DNase/0.000001/JASPAR/Th1/").getChildren().get(0);
		
		reader.setInput(ReadBindingNetwork.QueryService, service);
		reader.setInput(ReadBindingNetwork.NetworkMetaInformation, th1);
		
		BindingEnrichedDirectedNetwork net = (BindingEnrichedDirectedNetwork)  reader.operate();
	//	System.out.println(net.getSize());
		assertTrue(net.size() > 0);
		
		for(int edgeId : net.getEdgeIds()){
			assertNotNull(net.getBindings(edgeId));
			
			List<Integer>groupIds = net.getAnnotation(edgeId,EdgeOption.GroupId,Integer.class);
			assertEquals(1,groupIds.size());
			
			List<Peak> bindings = net.getAnnotation(edgeId,EdgeOption.BindingSite,Peak.class);
			assertTrue(bindings.size()>0);
			
		//	System.out.println(e.getFirst() + "\t" + e.getSecond()+ "\t" +  net.getBindings(edgeId));
		}
		
		reader.setInput(ReadBindingNetwork.ContextTreeNode, service.getContextTreeNode("GO:0035556"));
		net = (BindingEnrichedDirectedNetwork)  reader.operate();
			assertTrue(net.size() > 0);
			
	}
*/
}
