package de.lmu.ifi.bio.crco.intervaltree.peaks;


public class DNARegion extends Peak{

	public String gene;
	private String chrom;
	public DNARegion(String chrom, String gene, int start, int end) {
		super(start,end);
		this.chrom = chrom;
		this.gene = gene;
	}
	public String getChrom() {
		return chrom;
	}
	
}
