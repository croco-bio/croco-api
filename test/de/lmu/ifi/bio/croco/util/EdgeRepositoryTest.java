package de.lmu.ifi.bio.croco.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.network.DirectedNetwork;
import de.lmu.ifi.bio.croco.network.EdgeRepository;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.network.Network.EdgeRepositoryStrategy;
import de.lmu.ifi.bio.croco.test.UnitTest;

@Category(UnitTest.class)
public class EdgeRepositoryTest {
	@Test
	public void testEdgeRepo(){
		Network directedNetwork1 = new DirectedNetwork("bla",9606,EdgeRepositoryStrategy.GLOBAL);
		directedNetwork1.add(new Entity("bla"), new Entity("bla"), 0);
		
		Network directedNetwork2 = new DirectedNetwork("bla",9606,EdgeRepositoryStrategy.GLOBAL);
		directedNetwork2.add(new Entity("bla"), new Entity("bla"), 0);
		
		assertEquals(1,EdgeRepository.getInstance().getNumberOfEdges());
		
		Network directedNetwork3 = new DirectedNetwork("bla",9606,EdgeRepositoryStrategy.GLOBAL);
		directedNetwork3.add(new Entity("bla 2"), new Entity("bla 2"), 0);
		
		assertEquals(2,EdgeRepository.getInstance().getNumberOfEdges());
		
		Network directedNetwork4 = new DirectedNetwork("bla",9606,EdgeRepositoryStrategy.GLOBAL);
		directedNetwork4.add(new Entity("bla 2"), new Entity("bla 2"), 0);
		
		assertEquals(2,EdgeRepository.getInstance().getNumberOfEdges());
	}
}
