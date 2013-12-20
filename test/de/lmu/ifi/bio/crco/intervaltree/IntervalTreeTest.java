package de.lmu.ifi.bio.crco.intervaltree;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.test.UnitTest;

@Category(UnitTest.class)
public class IntervalTreeTest {

	@Test
	public void testGeneral() {
		IntervalTree<Interval> tree  = new IntervalTree<Interval>();
		tree.insert(new Interval(0,100));
		tree.insert(new Interval(90,200));
		tree.insert(new Interval(500,1000));
		
		assertEquals(2,tree.searchAll(new Interval(0,100)).size());
		assertEquals(1,tree.searchAll(new Interval(0,80)).size());
		assertEquals(1,tree.searchAll(new Interval(120,200)).size());
		assertEquals(1,tree.searchAll(new Interval(600,620)).size());
		assertEquals(0,tree.searchAll(new Interval(400,499)).size());
		
	}

}
