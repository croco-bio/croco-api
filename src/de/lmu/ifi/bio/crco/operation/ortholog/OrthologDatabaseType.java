package de.lmu.ifi.bio.crco.operation.ortholog;


public enum OrthologDatabaseType {
	InParanoid("InParanoid","7.0"), OMA("OMA","December 2012"), EnsemblCompara("Ensembl Compara", "Ensembl Release 72 ");
	
	private String name;
	private String version;
	public String getName() {
		return name;
	}
	public String getVersion() {
		return version;
	}
	
	OrthologDatabaseType(){};
	OrthologDatabaseType(String name, String version){
		this.name = name;
		this.version = version;
	}
	
}
