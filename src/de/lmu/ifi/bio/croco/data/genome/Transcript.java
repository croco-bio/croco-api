package de.lmu.ifi.bio.croco.data.genome;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.croco.data.Entity;

public class Transcript extends Entity{
	
	private List<Exon> exons = null;
	private String type;
	private Gene parentGene;
	private Protein protein;
	private Integer tssEnd;
	private Integer tssStart;
	public Transcript(){
		
	}
	public Transcript(Gene parentGene, String transcriptId) {
		super(transcriptId);
		this.parentGene = parentGene;
		this.exons = new ArrayList<Exon>();
		
	}
	public Transcript(Gene parentGene, String transcriptId,String transcriptName,Integer tssStart,Integer tssEnd, String type) {
		super(transcriptId);
		this.parentGene = parentGene;
		this.type = type;
		this.tssStart = tssStart;
		this.tssEnd =tssEnd;
		
	}
	public Transcript(Gene parentGene, String transcriptId,String transcriptName, String type) {
		super(transcriptId);
		this.parentGene = parentGene;
		this.type = type;
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
	public Protein getProtein(){
		return protein;
	}
	
	public static Integer getDistanceToTssStart(Transcript transcript, Integer position){
		Integer distanceToTss = null;
		if (transcript.getParentGene().getStrand().equals(Strand.PLUS) ){
			distanceToTss = position-transcript.getStrandCorredStart();
		}else{
			distanceToTss = transcript.getStrandCorredStart()-position;
		}
		return distanceToTss;
	}
	/**
	 * Adds an exon (assume correct order)
	 * @param exon 
	 */
	public void addExon(Exon exon){
		if (parentGene != null && exons.size()  > 0 ){
			Exon lastExon = exons.get(exons.size()-1);
			if ( parentGene.getStrand().equals(Strand.PLUS) && lastExon.getEnd() > exon.getStart()) throw new RuntimeException("Exons not ordered (PLUS)");
			if ( parentGene.getStrand().equals(Strand.MINUS) && lastExon.getStart() < exon.getEnd()) throw new RuntimeException("Exons not ordered (MINUS)");
			
		}
		this.exons.add(exon);
	}
	public List<Exon> getExons(){
		return exons;
	}
	
	
	/**
	 * @return the <b>strand corrected<b> TSS end position or null when no exon or parent gene annotation is available
	 */
	public Integer getStrandCorredEnd(){
		if ( tssEnd != null) return tssEnd;
		if ( exons != null && parentGene != null){
			if ( parentGene.getStrand().equals(Strand.PLUS)) //exons are ordered
				return exons.get(exons.size()-1).getEnd();  //last exon end => TSS end
			else
				return exons.get(exons.size()-1).getStart(); //last exon start => TSS end (because of MINUS strand!)
		}
		return null;
	}
	
	/**
	 * @return the <br>strand corrected<b> TSS start position or null when no exon or parent gene annotation is available
	 */
	public Integer getStrandCorredStart(){
		if ( tssStart != null) return tssStart;
		if ( exons != null && parentGene != null){
			if ( parentGene.getStrand().equals(Strand.PLUS)) //exons are ordered
				return exons.get(0).getStart();  //first exon start => TSS start
			else
				return exons.get(0).getEnd(); //first exon end => TSS start (because of MINUS strand!)
		}
		return null;
	}
}
