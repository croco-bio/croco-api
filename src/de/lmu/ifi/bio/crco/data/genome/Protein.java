package de.lmu.ifi.bio.crco.data.genome;

import java.util.ArrayList;
import java.util.List;

public class Protein {
	
	private String proteinId;
	//private String geneIdentifier;
	private Integer taxId; 
	private String refSeqIdAndVersion;
	
	private Transcript parentTranscript;
	private List<Exon> codingExons=null;
	
	public String getRefSeqIdAndVersion() {
		return refSeqIdAndVersion;
	}
	public Transcript getParentTranscript() {
		return parentTranscript;
	}

	public Protein(String proteinId, Transcript parentTranscript ){
		this.proteinId = proteinId;
		this.parentTranscript = parentTranscript;
		this.codingExons = new ArrayList<Exon>();
	}
	public Protein(String proteinId, String transcriptIdentifier,String geneIdentifier) {
		super();
		this.proteinId = proteinId;
		Gene parentGene = new Gene(geneIdentifier);
		Transcript transcript = new Transcript(parentGene,transcriptIdentifier);
		this.parentTranscript = transcript;
	}
	
	public Protein(String proteinId, String transcriptIdentifier,String geneIdentifier, Integer taxId,String refSeqIdAndVersion) {
		super();
		this.proteinId = proteinId;
		Gene parentGene = new Gene(geneIdentifier);
		Transcript transcript = new Transcript(parentGene,transcriptIdentifier);
		this.parentTranscript = transcript;
		//this.geneIdentifier = geneIdentifier;
		//this.transcriptIdentifier =	transcriptIdentifier;
		this.taxId = taxId;
		this.refSeqIdAndVersion = refSeqIdAndVersion;
	}
	
	public String getProteinId() {
		return proteinId;
	}
	public String getTranscriptIdentifier() {
		return parentTranscript.getIdentifier();
	}
	public String getGeneIdentifier() {
		return parentTranscript.getParentGene().getIdentifier();
	}
	public Integer getTaxId() {
		return taxId;
	}


	public List<Exon> getCodingExons() {
		return codingExons;
	}
	public void add(Exon exon) {
		if (parentTranscript != null && codingExons.size()  > 0 ){
			Exon lastExon = codingExons.get(codingExons.size()-1);
			Gene parentGene = parentTranscript.getParentGene();
			if ( parentGene.getStrand().equals(Strand.PLUS) && lastExon.getEnd() > exon.getStart()) throw new RuntimeException("Exons not ordered (PLUS)");
			if ( parentGene.getStrand().equals(Strand.MINUS) && lastExon.getStart() < exon.getEnd()) throw new RuntimeException("Exons not ordered (MINUS)");
			
		}
		codingExons.add(exon);
	}
	
	@Override
	public String toString(){
		if ( this.codingExons != null)
			return this.proteinId  + "  " +this.codingExons.toString() ;
		else
			return this.proteinId;
	}
	
}
