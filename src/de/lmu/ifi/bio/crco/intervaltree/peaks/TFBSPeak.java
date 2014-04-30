package de.lmu.ifi.bio.crco.intervaltree.peaks;

/**
 * A TFBS prediction peak
 * @author pesch
 *
 */
public class TFBSPeak extends Peak {
	
	private String motifId;
	private Float pValue;
	
	public Float getpValue() {
		return pValue;
	}

	public TFBSPeak(String chrom, int start, int end, String motifId, Float pValue, Float score) {
		super(chrom,start, end,score);
		this.motifId = motifId;
		this.pValue = pValue;
	}
	public String getMotifId() {
		return motifId;
	}

	public TFBSPeak(int start, int end){
		super(start,end);
	}

}
