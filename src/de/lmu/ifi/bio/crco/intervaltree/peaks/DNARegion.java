package de.lmu.ifi.bio.crco.intervaltree.peaks;




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
