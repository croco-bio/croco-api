package de.lmu.ifi.bio.crco.data.genome;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.crco.data.Entity;

public class Transcript extends Entity{
	
	private List<Exon> exons = null;
	private String transcriptName;
	private String transcriptId;
	private String type;
	private Gene parentGene;
	private Protein protein;
	
	public Transcript(Gene parentGene, String transcriptName) {
		super(transcriptName);
		this.transcriptName = transcriptName;
		this.parentGene = parentGene;
		this.exons = new ArrayList<Exon>();
		
	}
	
	public Transcript(Gene parentGene, String transcriptName,String transcriptId, String type) {
		super(transcriptName);
		this.transcriptName = transcriptName;
		this.parentGene = parentGene;
		this.type = type;
		this.transcriptId = transcriptId;
		this.exons = new ArrayList<Exon>();
		
	}
	public Gene getParentGene() {
		return parentGene;
	}
	public String getType(){
		return type;
	}
	public void setProtein(Protein currentProtein) {
		this.protein = currentProtein;
	}
	/**
	 * Adds an exon (assume correct order)
	 * @param exon 
	 */
	public void addExon(Exon exon){
		this.exons.add(exon);
	}
	public List<Exon> getExons(){
		return exons;
	}

	
	/**
	 * @return the <b>strand corrected<b> TSS end position or null when no exon or parent gene annotations are available
	 */
	public Integer getTSSStrandCorredEnd(){
		if ( exons != null && parentGene != null){
			if ( parentGene.getStrand().equals(Strand.PLUS)) //exons are ordered
				return exons.get(exons.size()-1).getEnd();  //last exon end => TSS end
			else
				return exons.get(exons.size()-1).getStart(); //last exon start => TSS end (because of MINUS strand!)
		}
		return null;
	}
	
	/**
	 * @return the <br>strand corrected<b> TSS start position or null when no exon or parent gene annotations are available
	 */
	public Integer getTSSStrandCorredStart(){
		if ( exons != null && parentGene != null){
			if ( parentGene.getStrand().equals(Strand.PLUS)) //exons are ordered
				return exons.get(0).getStart();  //first exon start => TSS start
			else
				return exons.get(0).getEnd(); //first exon end => TSS start (because of MINUS strand!)
		}
		return null;
	}
}
