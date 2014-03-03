package de.lmu.ifi.bio.crco.intervaltree.peaks;



/**
 * Represents a DNA region (chromosome;start-end)
 * @author pesch
 *
 */
public class DNARegion extends Peak{

	private String chrom;
	public DNARegion(String chrom, int start, int end) {
		super(start,end);
		this.chrom = chrom;
	}
	public String getChrom() {
		return chrom;
	}
	
}
