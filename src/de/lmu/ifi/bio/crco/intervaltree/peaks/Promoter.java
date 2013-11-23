package de.lmu.ifi.bio.crco.intervaltree.peaks;

import de.lmu.ifi.bio.crco.data.Strand;



public class Promoter extends Gene {


	private int geneStart;
	public int getGeneStart() {
		return geneStart;
	}



	public int getGeneEnd() {
		return geneEnd;
	}



	private int geneEnd;



	public Promoter(String geneId,String transcriptId, int promoterStart, int promoterEnd, int geneStart, int geneEnd,Strand strand) {
		super(geneId,transcriptId,promoterStart, promoterEnd,strand);
		if (promoterStart >  promoterEnd && promoterStart ==promoterEnd ){
			System.err.println("Can not handle that");
			System.exit(-1);
		}

		this.geneStart = geneStart;
		this.geneEnd = geneEnd;

	}

	
	@Override
	public String toString(){
		return super.getTranscriptId();
	}
	
	@Override
	public int hashCode(){
		return this.toString().hashCode();
	}
}
