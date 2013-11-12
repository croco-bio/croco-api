package de.lmu.ifi.bio.crco.data;

public class Gene extends Entity {
	public Gene(String geneId) {
		super(geneId,geneId);
	}
	
	public Gene(String geneId, String name, boolean isFactor) {
		super(geneId,name);
		this.isFactor = isFactor;
	}

	public Gene(String geneId, String name, String description, boolean isFactor) {
		super(geneId,name);
		this.isFactor = isFactor;
		this.description = description;
	}
	public Gene(String geneId, String name, String description, Integer taxId, boolean isFactor) {
		super(geneId,name);
		this.isFactor = isFactor;
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



	public boolean getFactor() {
		return isFactor;
	}

	public boolean isFactor() {
		return isFactor;
	}

	public void setFactor(boolean isFactor) {
		this.isFactor = isFactor;
	}

	private String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


	private boolean isFactor;

	@Override
	public String toString() {
		return super.getIdentifier();
	}
}