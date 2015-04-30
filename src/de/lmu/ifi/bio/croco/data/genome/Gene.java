package de.lmu.ifi.bio.croco.data.genome;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.croco.data.Entity;

public class Gene extends Entity {

	private Strand strand;
	private List<Transcript> transcripts;
	private String chr;

	
	public List<Transcript> getTranscripts() {
		return transcripts;
	}
	public Gene(String geneId) {
		super(geneId,geneId);
	}
	public Gene(){
		
	}
	public Gene( String chr, String geneId, String geneName,Strand strand) {
		super(geneId,geneName);
		this.chr = chr;
		this.strand = strand;
		this.transcripts = new ArrayList<Transcript>();
	}
	public Gene(String geneId, String name) {
		super(geneId,name);
	}
	public Gene(String geneId, String name, String description) {
		super(geneId,name);
		this.description = description;
	}
	public Gene(String geneId, String name, String description, Integer taxId) {
		super(geneId,name);
		this.description = description;
		this.taxId = taxId;
	}
	private Integer taxId;

	public void setTaxId(Integer taxId){
		this.taxId = taxId;
	}
	public Integer getTaxId() {
		return taxId;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Entity) {
			return o.toString().toLowerCase().equals(this.toString().toLowerCase());
		}
		return false;
	}

	public String getName() {
		return super.getName();
	}

	private String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return super.getIdentifier();
	}
	public Strand getStrand() {
		return strand;
	}
	public void addTranscript(Transcript transcript){
		this.transcripts.add(transcript);
	}
	public String getChr() {
		return chr;
	}
}
