package de.lmu.ifi.bio.crco.intervaltree.peaks;

import de.lmu.ifi.bio.crco.data.Strand;
import de.lmu.ifi.bio.crco.intervaltree.Interval;




public class Gene extends Interval {	

	private Strand strand;
	
	private String transcriptId;
	private String geneId ;


	public Gene(String geneId,String transcriptId, int start, int end,Strand strand) {
		super(start, end);
		if (start >  end ){
			System.err.println("Can not handle that");
			System.exit(-1);
		}
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
