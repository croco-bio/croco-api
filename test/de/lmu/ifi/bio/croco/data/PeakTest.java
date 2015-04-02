package de.lmu.ifi.bio.croco.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.croco.intervaltree.peaks.TFBSPeak;


public class PeakTest {
	@Test
	public void testUnique() {
		Peak p1 = new Peak("chr1",10,100);
		Peak p2 = new Peak("chr2",10,100);
		
		HashSet<Peak> peaks = new HashSet<Peak>();
		peaks.add(p1);
		peaks.add(p2);
		
		assertEquals(2,peaks.size());
		Peak p3 = new Peak("chr2",10,100);
		peaks.add(p3);
		assertEquals(2,peaks.size());
	}
	@Test
	public void overlap(){
		Peak p1 = new Peak("chr1",10,100);
		Peak p2 = new Peak("chr2",101,200);
		
		assertEquals(-1,Peak.overlap(p2, p1));
	}
	
	@Test
	public void testMaxBindingSupport(){
		List<Peak> peaks = new ArrayList<Peak>();
		peaks.add(new Peak(0,5));
		peaks.add(new Peak(0,5));
		peaks.add(new Peak(0,5));
		peaks.add(new Peak(10,15));
		peaks.add(new Peak(10,15));
		peaks.add(new Peak(17,20));
		peaks.add(new Peak(17,20));
		peaks.add(new Peak(25,99));
		peaks.add(new Peak(1000,10005));
		
		assertEquals(3,(int)Peak.getMaxBindingSupport(peaks).getSecond());
		assertEquals(new Peak(0,5),Peak.getMaxBindingSupport(peaks).getFirst());
	}

	@Test
	public void testMaxBindingSupportWithShfit(){
		List<Peak> peaks1 = new ArrayList<Peak>();
		peaks1.add(new Peak(0,100));
		peaks1.add(new Peak(0,100));

		peaks1.add(new Peak(500,600));
		peaks1.add(new Peak(500,600));
		peaks1.add(new Peak(500,600));
		
		peaks1.add(new Peak(700,800));
		peaks1.add(new Peak(1000,1100));
	
		List<Peak> peaks2 = new ArrayList<Peak>();
		peaks2.add(new Peak(50,150));
		
		peaks2.add(new Peak(500,600));
		peaks2.add(new Peak(450,550));
		peaks2.add(new Peak(550,620));
		
		assertEquals((int)3,(int)Peak.getMaxBindingSupportOverlap(peaks1, peaks2, 50).getSecond());
	//	System.out.println(Peak.getMaxBindingSupportOverlap(peaks1, peaks2, 50));;
	}
	
	@Test
	public void testOverlap(){
		
		List<Peak> peaks1 = new ArrayList<Peak>();
		peaks1.add(new Peak(0,5));
		peaks1.add(new Peak(0,5));
		peaks1.add(new Peak(0,5));
		peaks1.add(new Peak(10,15));
		peaks1.add(new Peak(10,15));
		peaks1.add(new Peak(17,20));
		peaks1.add(new Peak(17,20));
		peaks1.add(new Peak(17,20));
		peaks1.add(new Peak(25,99));
		peaks1.add(new Peak(1000,10005));
		
		List<Peak> peaks2 = new ArrayList<Peak>();
		peaks2.add(new Peak(0,5));
		peaks2.add(new Peak(1,5));
		peaks2.add(new Peak(1,5));
		peaks2.add(new Peak(10,15));
		peaks2.add(new Peak(11,15));
		peaks2.add(new Peak(17,20));
		peaks2.add(new Peak(17,20));
		peaks2.add(new Peak(25,99));
		peaks2.add(new Peak(1000,10005));
	
	
		
		assertEquals(2,(int)Peak.getMaxBindingSupport(peaks1,peaks2).getSecond());
		assertEquals(new Peak(17,20),Peak.getMaxBindingSupport(peaks1,peaks2).getFirst());
		
		peaks1 = new ArrayList<Peak>();
		peaks2 = new ArrayList<Peak>();
		peaks1.add(new Peak(0,5));
		

		assertEquals(0,(int)Peak.getMaxBindingSupport(peaks1,peaks2).getSecond());
		//assertEquals(new Peak(17,20),Peak.getMaxBindingSupport(peaks1,peaks2).getFirst());
	}
	
	@Test
	public void testMatch(){
		TFBSPeak p1 = new TFBSPeak(5,10);
		TFBSPeak p2 = new TFBSPeak(0,5);

		assertFalse(Peak.matchExact(p1, p2)); //no exact match
		assertFalse(Peak.matchExact(p2, p1));
		
				
		assertTrue(Peak.matchPartial(p1, p2));
		assertTrue(Peak.matchPartial(p2, p1));
		
		TFBSPeak p3 = new TFBSPeak(0,4);
		assertFalse(Peak.matchPartial(p1, p3));
		assertFalse(Peak.matchPartial(p3, p1));
		
		TFBSPeak p4 = new TFBSPeak(7,15);
		assertTrue(Peak.matchPartial(p1, p4));
		assertTrue(Peak.matchPartial(p4, p1));
		
		TFBSPeak p5 = new TFBSPeak(0,100);
		assertTrue(Peak.matchPartial(p1, p5));
		assertTrue(Peak.matchPartial(p5, p1));
		
		TFBSPeak p6 = new TFBSPeak(6,9);
		assertTrue(Peak.matchPartial(p1, p6));
		assertTrue(Peak.matchPartial(p6, p1));
		
		TFBSPeak p7 = new TFBSPeak(10,15);
		assertTrue(Peak.matchPartial(p1, p7));
		assertTrue(Peak.matchPartial(p7, p1));
		
		TFBSPeak p8 = new TFBSPeak(11,15);
		assertFalse(Peak.matchPartial(p1, p8));
		assertFalse(Peak.matchPartial(p8, p1));
		
		TFBSPeak p9 = new TFBSPeak(5,10);
		assertTrue(Peak.matchExact(p1, p9)); //no exact match
		assertTrue(Peak.matchExact(p9, p1));
		
		TFBSPeak p10 = new TFBSPeak(0,7);
		assertTrue(Peak.matchPartial(p1, p10));
		assertTrue(Peak.matchPartial(p10, p1));
		
		
		
	}

}
