package de.lmu.ifi.bio.crco.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Species {
	
	public static Species Human = new Species(9606,"H. sapiens","Human");
	public static Species Mouse = new Species(10090,"M. musculus","Mouse");
	public static Species Worm = new Species(6239,"C. elegans","Worm");
	public static Species Fly =  new Species(7227,"D. melanogaster","Fly");
	
	public static List<Species> knownSpecies;
	public static HashMap<Integer,Species> taxIdMapping;
	
	static{
		knownSpecies = new ArrayList<Species> ();
		knownSpecies.add(Human);
		knownSpecies.add(Mouse);
		knownSpecies.add(Worm);
		knownSpecies.add(Fly);
		
		taxIdMapping= new HashMap<Integer,Species>();
		
		for(Species species : knownSpecies ){
		    taxIdMapping.put(species.taxId, species);
        }
	}
	
	public static Species getSpecies(Integer taxId){
	   
	    if (taxIdMapping.containsKey(taxId) )
	        return taxIdMapping.get(taxId);
	    
		return new Species(taxId);
	}

	public Integer getTaxId(){
		return taxId;
	}
	private Integer taxId;
	private String name;
	private String shortName;
	
	public String getShortName(){
		return shortName;
	}
	public String getName() {
		return name;
	}
	//added for xstream
	public Species(){}
	public Species( Integer taxId  ){
		this.taxId = taxId;
	}
	public Species( Integer taxId, String name ){
		this.taxId = taxId;
		this.name = name;
		this.shortName = name;
	}
	public Species( Integer taxId, String name, String shortName  ){
		this.taxId = taxId;
		this.name = name;
		this.shortName = shortName;
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

