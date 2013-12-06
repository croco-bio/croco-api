package de.lmu.ifi.bio.crco.intervaltree.peaks;

import de.lmu.ifi.bio.crco.data.genome.Gene;



public class DNARegion extends Peak{

	public Gene getGene(){
		return gene;
	}
	
	private Gene gene;
	private String chrom;
	public DNARegion(String chrom, Gene gene, int start, int end) {
		super(start,end);
		this.chrom = chrom;
		this.gene = gene;
	}
	public String getChrom() {
		return chrom;
	}
	
}
