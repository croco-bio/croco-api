package de.lmu.ifi.bio.crco.data;

import java.util.ArrayList;
import java.util.List;

public class Species {
	
	public static Species Human = new Species(9606,"H. sapiens");
	public static Species Mouse = new Species(10090,"M. musculus");
	public static Species Worm = new Species(6239,"C. elegans");
	public static Species Fly =  new Species(7227,"D. melanogaster");
	
	public static List<Species> knownSpecies;

	static{
		knownSpecies = new ArrayList<Species> ();
		knownSpecies.add(Human);
		knownSpecies.add(Mouse);
		knownSpecies.add(Worm);
		knownSpecies.add(Fly);
	}
	public static Species getSpecies(Integer taxId){
		for(Species species : knownSpecies ){
			if ( species.getTaxId().equals(taxId)) return species;
		}
		return new Species(taxId);
	}

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
		return String.format("%d-%s",taxId,name==null?"N.A":name);
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

