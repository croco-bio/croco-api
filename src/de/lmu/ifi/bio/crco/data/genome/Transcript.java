package de.lmu.ifi.bio.crco.data.genome;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.crco.data.Entity;

public class Transcript extends Entity{
	enum TranscriptType{
		ProteinCoding, OTHER
	}
	private List<Exon> exons = null;
	private String transcriptName;
	private TranscriptType type;
	private Gene parentGene;
	private Protein protein;
	
	public Transcript(Gene parentGene, String transcriptName) {
		super(transcriptName);
		this.transcriptName = transcriptName;
		this.parentGene = parentGene;
		this.exons = new ArrayList<Exon>();
		
	}
	
	public Transcript(Gene parentGene, String transcriptName,TranscriptType type) {
		super(transcriptName);
		this.transcriptName = transcriptName;
		this.parentGene = parentGene;
		this.type = type;
		this.exons = new ArrayList<Exon>();
		
	}
	public Gene getParentGene() {
		return parentGene;
	}

	public void setProtein(Protein currentProtein) {
		this.protein = currentProtein;
	}
	public void addExon(Exon exon){
		this.exons.add(exon);
	}
	
	/**
	 * Returns the TSS position
	 * @return the TSS position or null when no exon or parent gene annotations are available
	 */
	public Integer getStart(){
		if ( exons != null && parentGene != null){
			if ( parentGene.getStrand().equals(Strand.PLUS))
				return exons.get(0).getStart();
			else
				return exons.get(0).getEnd();
		}
		return null;
	}
}
