package de.lmu.ifi.bio.crco.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.crco.test.UnitTest;


@Category(UnitTest.class)
public class MathUtilTest {

	@Test 
	public void testPearson() {
		List<Pair<Integer,Integer>> pairedData = new ArrayList<Pair<Integer,Integer>>();
	
		pairedData.add(new Pair<Integer,Integer>(1,1));
		pairedData.add(new Pair<Integer,Integer>(2,2));
		pairedData.add(new Pair<Integer,Integer>(3,10));
		pairedData.add(new Pair<Integer,Integer>(4,4));
		pairedData.add(new Pair<Integer,Integer>(5,5));
		pairedData.add(new Pair<Integer,Integer>(6,6));
		
		System.out.println(MathUtil.pearson(pairedData));
		
	}

	@Test
	public void testSpearman(){
		List<Pair<Integer,Integer>> pairedData = new ArrayList<Pair<Integer,Integer>>();
		

		pairedData.add(new Pair<Integer,Integer>(1,1));
		pairedData.add(new Pair<Integer,Integer>(2,2));
		pairedData.add(new Pair<Integer,Integer>(3,6));
		pairedData.add(new Pair<Integer,Integer>(4,4));
		pairedData.add(new Pair<Integer,Integer>(5,5));
		pairedData.add(new Pair<Integer,Integer>(6,3));
		
		
		System.out.println(MathUtil.spearman(pairedData));
	}
}
