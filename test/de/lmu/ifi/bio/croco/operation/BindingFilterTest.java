package de.lmu.ifi.bio.croco.operation;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.croco.data.exceptions.ParameterNotWellDefinedException;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.croco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;

public class BindingFilterTest {

	@Test
	public void testRealNetwork() throws Exception{
		QueryService service = new LocalService();
		BindingEnrichedDirectedNetwork network = service.readBindingEnrichedNetwork(4286, null, false);
		System.out.println(network.size());
		BindingFilter filter = new BindingFilter();
		filter.setInput(BindingFilter.Distance, 10);
		filter.setInputNetwork(network);
		filter.setInput(GeneralOperation.QueryService, new LocalService());
		Network ret = filter.operate();
		System.out.println(ret.size());
	}

	@Test
	public void testSimpleDistanceFilter() throws OperationNotPossibleException, ParameterNotWellDefinedException, SQLException, IOException {
		BindingEnrichedDirectedNetwork network = new BindingEnrichedDirectedNetwork("Bla",9606);
		List<TFBSPeak> bindings = new ArrayList<TFBSPeak>();
		bindings.add(new TFBSPeak(null,7576905 ,7576905,null,null,null));

		network.addEdge(new Gene("TF1"), new Gene("ENSG00000141510"),null,(List)bindings);
		assertEquals(network.size(),1);
		
		BindingFilter filter = new BindingFilter();
		filter.setInput(BindingFilter.Distance, 100);
		filter.setInputNetwork(network);
		filter.setInput(GeneralOperation.QueryService, new LocalService());
		
		Network ret = filter.operate();
		assertEquals(ret.size(),1);
		
		
		bindings = new ArrayList<TFBSPeak>();
		bindings.add(new TFBSPeak(null,7590957 ,7590957,null,null,null));
		network.addEdge(new Gene("TF2"), new Gene("ENSG00000141510"),null,(List)bindings);
		assertEquals(network.size(),2);
		
		filter = new BindingFilter();
		filter.setInput(BindingFilter.Distance, 100);
		filter.setInputNetwork(network);
		filter.setInput(GeneralOperation.QueryService, new LocalService());
		
		ret = filter.operate();
		assertEquals(ret.size(),1);
		
	}
	
}
