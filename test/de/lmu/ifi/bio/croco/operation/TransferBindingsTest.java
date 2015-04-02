package de.lmu.ifi.bio.croco.operation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.croco.connector.DatabaseConnection;
import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.croco.intervaltree.peaks.TransferredPeak;
import de.lmu.ifi.bio.croco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.network.Network.EdgeOption;
import de.lmu.ifi.bio.croco.util.Tuple;


public class TransferBindingsTest {
	@Test
	public void testTransfer() throws Exception{
		ReadBindingNetwork reader = new ReadBindingNetwork();
		QueryService service = new LocalService();
		
		reader.setInput(ReadBindingNetwork.QueryService, service);
		reader.setInput(ReadBindingNetwork.NetworkHierachyNode, new NetworkHierachyNode(3720,9606));
		
		BindingEnrichedDirectedNetwork net = (BindingEnrichedDirectedNetwork)  reader.operate();
	//	System.out.println(net.getSize());
		assertTrue(net.size() > 0);
		
		for(int edgeId : net.getEdgeIds()){
			Tuple<Entity, Entity> e = net.getEdge(edgeId);
			assertNotNull(e);
			assertNotNull(net.getBindings(edgeId));
		}
		TransferBindings transfer = new TransferBindings();
		transfer.setInput(TransferBindings.ChainFileFile, new File("/mnt/raid8/bio/biosoft/ENCODE/ALIGNMENT/MM9-HG19/mm9.hg19.all.chain"));
		transfer.setInput(TransferBindings.LiftOverExec, new File("data/tools/linux.x86_64/liftOver"));
		transfer.setInput(TransferBindings.MinMatch, 0.9f);
		transfer.setInput(TransferBindings.chrPrefix, "chr");
		transfer.setInputNetwork(net);
		
		Network transferredBindings = transfer.operate();
		assertTrue(transferredBindings.size() > 0);
		int k =0;
		for(int edgeId : transferredBindings.getEdgeIds()){
			List<TransferredPeak> transferredPeaks = transferredBindings.getAnnotation(edgeId, EdgeOption.TransferredSite, TransferredPeak.class);
			List<Peak> bindings = transferredBindings.getAnnotation(edgeId, EdgeOption.BindingSite, Peak.class);
			if (transferredPeaks != null ) k++;
			assertTrue(transferredPeaks != null ||bindings != null );
		}
		assertTrue(k>0);
	}
}
