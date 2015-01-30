package de.lmu.ifi.bio.crco.operation.ortholog;

import de.lmu.ifi.bio.crco.data.Species;



public class OrthologMappingInformation  {
	private OrthologDatabaseType database;
	private Species species1;
	private Species species2;
	
	
	public OrthologDatabaseType getDatabase() {
		return database;
	}

	public Species getSpecies1() {
		return species1;
	}

	public Species getSpecies2() {
		return species2;
	}
	private Integer hash = null;
	@Override
	public int hashCode()
	{
	    if ( hash == null)
	        hash =this.toString().hashCode();
	    return hash;
	}
	@Override
	public boolean equals(Object o){
	    
		if ( o instanceof OrthologMappingInformation){
			
		    OrthologMappingInformation tmp = (OrthologMappingInformation) o;
			if ( tmp.getDatabase().equals(this.getDatabase()) ){
				if ( tmp.getSpecies1().getTaxId().equals(this.getSpecies1().getTaxId())  && tmp.getSpecies2().getTaxId().equals(this.getSpecies2().getTaxId()))
					return true;
			}
			
	        
		}
		return false;
		
	}
	public OrthologMappingInformation(){}
	public OrthologMappingInformation(OrthologDatabaseType database, Species species1, Species species2) {
		super();
		if ( species1.getTaxId() > species2.getTaxId()){ //swap
			Species tmp = species1;
			species1 = species2;
			species2 = tmp;
		}
		this.database = database;
		this.species1 = species1;
		this.species2 = species2;
	}

	
	@Override
	public String toString(){
	            
		return String.format("%s-(%s)-(%s)", database.name(),species1.toString(),species2.toString());
	}
}
