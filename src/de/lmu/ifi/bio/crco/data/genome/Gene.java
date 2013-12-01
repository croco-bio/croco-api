package de.lmu.ifi.bio.crco.data.genome;

import java.util.List;

import de.lmu.ifi.bio.crco.data.Entity;

public class Gene extends Entity {

	private Strand strand;
	private List<Transcript> transcripts;
	private Integer start;
	private Integer end;
	private String chr;

	
	public List<Transcript> getTranscripts() {
		return transcripts;
	}
	public Gene(String geneId) {
		super(geneId,geneId);
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
}