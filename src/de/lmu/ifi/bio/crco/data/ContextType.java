package de.lmu.ifi.bio.crco.data;

public enum ContextType {
	GeneOntoloyBiologicalProcess("GO (biological process)");
	
	public String getName(){
		return name;
	}
	
	private String name;
	ContextType(String name){
		this.name = name;
	}
}
