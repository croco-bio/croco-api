package de.lmu.ifi.bio.crco.operation;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.crco.data.exceptions.ParameterNotWellDefinedException;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;

public class BindingFilterTest {

	@Test
	public void testRealNetwork() throws Exception{
		QueryService service = new LocalService();
		BindingEnrichedDirectedNetwork network = service.readBindingEnrichedNetwork(4286, null, false);
		System.out.println(network.getSize());
		BindingFilter filter = new BindingFilter();
		filter.setInput(BindingFilter.Distance, 10);
		filter.setInputNetwork(network);
		filter.setInput(GeneralOperation.QueryService, new LocalService());
		Network ret = filter.operate();
		System.out.println(ret.getSize());
	}

	@Test
	public void testSimpleDistanceFilter() throws OperationNotPossibleException, ParameterNotWellDefinedException, SQLException, IOException {
		BindingEnrichedDirectedNetwork network = new BindingEnrichedDirectedNetwork("Bla",9606);
		List<TFBSPeak> bindings = new ArrayList<TFBSPeak>();
		bindings.add(new TFBSPeak(null,7576905 ,7576905,null,null,null));

		network.addEdge(new Gene("TF1"), new Gene("ENSG00000141510"),null,(List)bindings);
		assertEquals(network.getSize(),1);
		
		BindingFilter filter = new BindingFilter();
		filter.setInput(BindingFilter.Distance, 100);
		filter.setInputNetwork(network);
		filter.setInput(GeneralOperation.QueryService, new LocalService());
		
		Network ret = filter.operate();
		assertEquals(ret.getSize(),1);
		
		
		bindings = new ArrayList<TFBSPeak>();
		bindings.add(new TFBSPeak(null,7590957 ,7590957,null,null,null));
		network.addEdge(new Gene("TF2"), new Gene("ENSG00000141510"),null,(List)bindings);
		assertEquals(network.getSize(),2);
		
		filter = new BindingFilter();
		filter.setInput(BindingFilter.Distance, 100);
		filter.setInputNetwork(network);
		filter.setInput(GeneralOperation.QueryService, new LocalService());
		
		ret = filter.operate();
		assertEquals(ret.getSize(),1);
		
	}
	
}