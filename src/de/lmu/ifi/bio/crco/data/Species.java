package de.lmu.ifi.bio.crco.data;

public class Species {
	public static Species Human = new Species(9606,"H. sapiens");
	public static Species Mouse = new Species(10090,"M. musculus");
	public static Species Worm = new Species(6239,"C. elegans");
	public static Species Fly =  new Species(7227,"D. melanogaster");
	
	
	public Integer getTaxId(){
		return taxId;
	}
	private Integer taxId;
	private String name;
	public String getName() {
		return name;
	}
	public Species( Integer taxId  ){
		this.taxId = taxId;
	}
	
	public Species( Integer taxId, String name  ){
		this.taxId = taxId;
		this.name = name;
	}
	
	@Override
	public String toString(){
		return String.format("%d-%s",taxId,name);
	}
	
	@Override
	public int hashCode(){
		return taxId;
	}
	
	@Override
	public boolean equals(Object o){
		if ( o instanceof Species)
			return ( ((Species) o).getTaxId().equals(this.getTaxId()));
		return super.equals(o);
	}
}

