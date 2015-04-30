package de.lmu.ifi.bio.croco.operation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.croco.category.UnitTest;
import de.lmu.ifi.bio.croco.data.exceptions.OperationNotPossibleException;
import de.lmu.ifi.bio.croco.data.exceptions.ParameterNotWellDefinedException;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.network.DirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;

@Category(UnitTest.class)
public class DifferenceTest {

	@Test
	public void testDifference() throws OperationNotPossibleException, ParameterNotWellDefinedException{
		Difference operation = new Difference();
	
		Network network1 = new DirectedNetwork("test",9606);
		network1.add(new Gene("a"), new Gene("b"), 0);
		network1.add(new Gene("c"), new Gene("d"), 0);
		
		Network network2 = new DirectedNetwork("test",9606);
		network2.add(new Gene("a"), new Gene("b"), 0);
		
		operation.setInputNetwork(new Network[] {network1,network2});
		
		operation.setInputNetwork(new Network[] {network1,network2});
		
	//	assertTrue(operation.accept(network2));
		Network network = operation.operate();
		
		assertEquals(network.size(),1);
		System.out.println(network.getEdgeIds());
	}
}
