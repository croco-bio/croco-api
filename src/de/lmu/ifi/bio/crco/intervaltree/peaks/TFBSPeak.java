package de.lmu.ifi.bio.crco.intervaltree.peaks;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.genome.Transcript;



public class TFBSPeak extends Peak {
	
	private Entity factor;
	private Transcript target;
	
	private String chrom;
	private Integer distanceToTranscript = null;
	private float pValue;
	
	
	public float getpValue() {
		return pValue;
	}

	public String getChrom() {
		return chrom;
	}
	
	public Entity getFactor(){
		return factor;
	}
	public Entity getTarget(){
		return target;
	}
	
	public TFBSPeak(String chrom,Entity factor,Transcript target,Integer distanceToTranscript, Float pValue, int start, int end) {
		super(start, end);
		this.chrom = chrom;
		this.target = target;
		this.factor = factor;
		this.pValue = pValue;
		this.distanceToTranscript = distanceToTranscript;
	}
	

	public TFBSPeak(int start, int end){
		super(start,end);
	}
	
	@Override
	public String toString(){
		return String.format("%s\t%s\t%s\t%s\t%s\t%s\t%d\t%f\t%d\t%d\n",
				factor.getName(), factor.getIdentifier(), target.getIdentifier() , target.getParentGene().getIdentifier(),
				getChrom() ,distanceToTranscript,pValue,getStart(),getEnd() );
	}


}
