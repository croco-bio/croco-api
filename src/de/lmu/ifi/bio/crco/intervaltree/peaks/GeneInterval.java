package de.lmu.ifi.bio.crco.intervaltree.peaks;

import de.lmu.ifi.bio.crco.data.Strand;
import de.lmu.ifi.bio.crco.intervaltree.Interval;



public class GeneInterval extends Interval {
	private Strand strand;
	
	private String transcriptId;
	private String geneId ;


	public GeneInterval(String geneId,String transcriptId, int start, int end,Strand strand) {
		super(start, end);

		this.geneId = geneId;
		this.transcriptId =transcriptId;
		this.strand = strand;
	}
	
	public String getTranscriptId() {
		return transcriptId;
	}

	public String getGeneId() {
		return geneId;
	}

	public Strand getStrand() {
		return strand;
	}
	
	@Override
	public String toString(){
		return geneId;
	}
	
	@Override
	public int hashCode(){
		return this.toString().hashCode();
	}
}
