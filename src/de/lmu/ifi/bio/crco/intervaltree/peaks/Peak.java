package de.lmu.ifi.bio.crco.intervaltree.peaks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.bio.crco.intervaltree.Interval;
import de.lmu.ifi.bio.crco.util.Pair;

public class Peak extends Interval {
	public Float score = null;
	public Integer groupId = null;
	public Float getScore() {
		return score;
	}
	private String chrom;
	public String getChrom() {
		return chrom;
	}
	
	public Peak(String chrom, int start, int end) {
		this(start, end);
		this.chrom = chrom;
	}
	public Peak(String chrom, int start, int end, Float score) {
		this(start, end,score);
		this.chrom = chrom;
	}
	public Peak(int start, int end) {
		super(start, end);
	}
	public Peak(int start, int end, Float score) {
		super(start, end);
		this.score = score;
	}
	public Peak(Peak peak) {
		super(peak.getLow(),peak.getHigh());
		this.chrom = peak.chrom;
		this.score = peak.score;
	}
	public int getStart(){
		return (int)super.getLow();
	}
	public int getEnd(){
		return (int)super.getHigh();
	}
	public int getLength(){
		return (int)super.getHigh()-(int)super.getLow();
	}
	@Override
	public String toString(){
		return String.format("%s:%d-%d",this.getChrom(), this.getStart() ,this.getEnd());
	}
	
	@Override
	public boolean equals(Object o){
		if ( o instanceof Peak){
			Peak p2 = (Peak)o;
			if ( p2.getChrom() != null || this.chrom != null){
				if ( p2.getChrom() == null) return false;
				if ( this.getChrom() == null) return false;
				if (! p2.getChrom().equals(this.getChrom())) return false;
			}
			
			if (  p2.getStart() == this.getStart() && p2.getEnd() == this.getEnd()) 
				return true;
		}
		return false;
		
	}
	
	public static Pair<Peak,Integer> getMaxBindingSupportOverlap(List<Peak> peaks1, List<Peak> peaks2,int shift){
		
		Set<Pair<Peak, Integer>> c1 = count(peaks1);
		Set<Pair<Peak, Integer>> c2 = count(peaks2);
		
		Peak p = null;
		Integer max = 0;
		for( Pair<Peak, Integer> e1 : c1){
			int c =0;
			for( Pair<Peak, Integer> e2 : c2){
				
				Peak peak2WithShift = new Peak(e2.getFirst().getStart()-shift,e2.getFirst().getEnd()+shift);
				
				boolean overlap = matchPartial(e1.getFirst(),peak2WithShift);
				if ( overlap) {
					c+=e2.getSecond();
				}

			}
			if ( max == 0 || Math.min(e1.getSecond(), c) > max){
				max = Math.min(e1.getSecond(), c);
				p = e1.getFirst();
			}
		}
		
		return new Pair<Peak,Integer>(p,max);
	}
	
	public static Pair<Peak,Integer> getMaxBindingSupport(List<Peak> peaks1, List<Peak> peaks2){
		
		Set<Pair<Peak, Integer>> c1 = count(peaks1);
		Set<Pair<Peak, Integer>> c2 = count(peaks2);
		
		Peak p = null;
		Integer max = 0;
		for( Pair<Peak, Integer> e1 : c1){
			for( Pair<Peak, Integer> e2 : c2){
				boolean overlap = matchExact(e1.getFirst(),e2.getFirst());
				if ( overlap) {
					if ( max == 0 || Math.min(e1.getSecond(), e2.getSecond()) > max){
						max = Math.min(e1.getSecond(), e2.getSecond());
						p = e1.getFirst();
						break; //there can not be another one with exact math
					}
				}

			}
			
		}
		
		return new Pair<Peak,Integer>(p,max);
	}
	public static Set<Pair<Peak,Integer>> countPartial(List<Peak> peaks){
		Set<Pair<Peak,Integer>> c = new HashSet<Pair<Peak,Integer>> ();
		
		for(int i = 0 ; i< peaks.size() ; i++){
			int matches = 0;
			for(int j = 0 ; j< peaks.size() ; j++){
				boolean overlap = matchPartial(peaks.get(i),peaks.get(j));
				if ( overlap) matches++;
			}	
			c.add(new Pair<Peak,Integer>(peaks.get(i),matches) );
		}

		
		return c;
	}
	public static Set<Pair<Peak,Integer>> count(List<Peak> peaks){
		Set<Pair<Peak,Integer>> c = new HashSet<Pair<Peak,Integer>> ();
		
		for(int i = 0 ; i< peaks.size() ; i++){
			int matches = 0;
			for(int j = 0 ; j< peaks.size() ; j++){
				boolean overlap = matchExact(peaks.get(i),peaks.get(j));
				if ( overlap) matches++;
			}	
			c.add(new Pair<Peak,Integer>(peaks.get(i),matches) );
		}

		
		return c;
	}
	public static Pair<Peak,Integer> getMaxBindingSupportPartial(List<Peak> peaks){
		
		Set<Pair<Peak, Integer>> c = countPartial(peaks);
		Pair<Peak, Integer>  ret = null;
		for ( Pair<Peak, Integer>  e : c){
			if ( ret == null || e.getSecond() > ret.getSecond()){
				ret = e;
			}
		}
		
		return ret;
	}
	public static Pair<Peak,Integer> getMaxBindingSupport(List<Peak> peaks){
		
		Set<Pair<Peak, Integer>> c = count(peaks);
		Pair<Peak, Integer>  ret = null;
		for ( Pair<Peak, Integer>  e : c){
			if ( ret == null || e.getSecond() > ret.getSecond()){
				ret = e;
			}
		}
		
		return ret;
	}

	public static List<TFBSPeak> intersections(List<TFBSPeak> ...bindings){
		if ( bindings.length == 0) return null;
		if ( bindings.length == 1) return bindings[0];
		
		List<TFBSPeak> bindings0 =bindings[0];
		for(int i = 1 ; i< bindings.length; i++ ){
			if ( bindings[i].size() < bindings0.size()) {
				bindings0 = bindings[i];
			}
		}
		List<TFBSPeak> ret = new ArrayList<TFBSPeak>();
		for(TFBSPeak binding : bindings0){
			boolean consistent = true;
			
			for(List<TFBSPeak> toCheckBindings : bindings){
				boolean inExpConsistent = false;
				for(TFBSPeak toCheckBinding :toCheckBindings ){
					if ( matchPartial(binding,toCheckBinding)) {
						inExpConsistent = true;
						break;
					}
				}
			
				if ( !inExpConsistent){
					
					consistent = false;
					break;
				}
			}
			if ( consistent )ret.add(binding);
		}
		return ret;
	}
	public static boolean matchExact(Peak p1, Peak p2){
		return ( p1.getStart() == p2.getStart() && p1.getEnd() == p2.getEnd() ) ;
	}
	public static int overlap(Peak p1, Peak p2){
		return Math.min(p1.getEnd(), p2.getEnd())-Math.max(p1.getStart(),p2.getStart());
	}
	public static boolean matchPartial(Peak p1, Peak p2){
		if ( p1.getStart() >= p2.getStart() && p1.getStart() <= p2.getEnd()   ){
			return true;
		}else if ( p1.getEnd() >= p2.getStart() && p1.getEnd() <= p2.getEnd()   ){
			return true;
		}else if ( p1.getStart() >=p2.getStart() && p1.getEnd() <= p2.getStart()  ){
			return true;
		}else if ( p1.getStart() <= p2.getStart() && p1.getEnd() >= p2.getStart()  ){
			return true;
		}
		return false;
	}
}
