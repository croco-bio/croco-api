package de.lmu.ifi.bio.crco.data.genome;

import de.lmu.ifi.bio.crco.data.Entity;

public class Transcript extends Entity{
	enum TranscriptType{
		ProteinCoding, OTHER
	}
	public int getStart(){
		return start;
	}
	public int getEnd(){
		return end;
	}
	
	private int start;
	private int end;
	private String transcriptName;
	private TranscriptType type;
	private Gene parentGene;
	
	
	public Transcript(Gene parentGene,int start, int end, String transcriptName,TranscriptType type) {
		super(transcriptName);
		this.transcriptName = transcriptName;
		this.parentGene = parentGene;
		this.start = start;
		this.end = end;
		this.type = type;
	}
	public Gene getParentGene() {
		return parentGene;
	}
	
}
