package de.lmu.ifi.bio.crco.intervaltree.peaks;



public class TFBSPeak extends Peak {
	
	private String factor;
	private String target;
	private String chrom;
	public float getpValue() {
		return pValue;
	}

	public String getMotifName() {
		return motifName;
	}
	private float pValue;
	private String motifName;
	
	public String getChrom() {
		return chrom;
	}
	
	public String getFactor(){
		return factor;
	}
	public TFBSPeak(String chrom,String factor, String target,String motifName, Float pValue, int start, int end) {
		super(start, end);
		this.chrom = chrom;
		this.target = target;
		this.factor = factor;
		this.motifName = motifName;
		this.pValue = pValue;
	}

	
	public TFBSPeak(String chrom,String target,String motifName, Float pValue, int start, int end) {
		super(start, end);
		this.chrom = chrom;
		this.target = target;
		this.motifName = motifName;
		this.pValue = pValue;
	}

	public TFBSPeak(int start, int end){
		super(start,end);
	}
	
	public String getTarget(){
		return target;
	}


}
