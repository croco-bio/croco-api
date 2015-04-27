package de.lmu.ifi.bio.croco.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Meta-information, i.e. annotations, are assigned to networks.
 * @author pesch
 */
public  class NetworkMetaInformation  implements Comparable<NetworkMetaInformation>, Identifiable {
 
    /**
     * The restriction for a network.
     * @author pesch
     */
    public enum Restriction{
        NONE, WEB
    }
    
	private Integer groupId;
	private String name;
	private NetworkType type;
	private Integer taxId;
	
	private Set<String> factors = new HashSet<String>();
	
	private Restriction restriction =Restriction.NONE ;
	private HashMap<Option,String> options = new HashMap<Option,String>();
	
	//added for xsteam
	public NetworkMetaInformation(){}

	public NetworkMetaInformation(Integer groupId, String name,  Integer taxId, NetworkType type,Restriction restriction)
	{
	    this.groupId = groupId;
	    this.restriction =restriction;
	    this.type = type;
	    this.name = name;
	    this.taxId = taxId;

	    this.options.put(Option.TaxId,taxId+"");
	    this.options.put(Option.NetworkType,type.name());
	}
	/*
	public NetworkMetaInformation(NetworkMetaInformation node){
	    this.groupId = node.getGroupId();
	    this.name = node.getName();
	    this.taxId = node.getTaxId();
	    this.selected = node.isSelected();
	    this.type = node.getType();
	    this.options = node.options;
	}
	*/
	
	public NetworkType getType() {
		return type;
	}
	
	public Restriction getRestriction() {
        return restriction;
    }

    public void setRestriction(Restriction restriction) {
        this.restriction = restriction;
    }

    public HashMap<Option,String> getOptions() {
        return options;
    }

	public String toString()
	{
	    return name;
	}
    public void setOptions(HashMap<Option,String> options) {
        this.options = options;
    }
    /**
     * Adds an annotation.
     * @param option -- the annotation 
     * @param value -- the value
     */
    public void addOption(Option option, String value)
    {
        if ( option == Option.FactorList){
            for(String gene : value.split("\\s+"))
            {
                factors.add(gene.toUpperCase());
            }
            return;
        }
        options.put(option, value);
        
    }
    
    public Set<String> getFactors() {
        return factors;
    }

	 public Integer getGroupId() {
		return groupId;
	}
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public Integer getTaxId() {
		return taxId;
	}

	public void setTaxId(Integer taxId) {
		this.taxId = taxId;
		this.options.put(Option.TaxId,taxId+"");
	}
	    
	@Override 
	public int compareTo(NetworkMetaInformation o) {
		return name.toLowerCase().compareTo(o.getName().toLowerCase());
	}

    @Override
	public boolean equals(Object o){
		if ( o instanceof NetworkMetaInformation && ((NetworkMetaInformation) o).getGroupId().equals(this.groupId))
			return true;
		else
			return false;
		
	}

    @Override
    public String getId() {
        return groupId+"";
    }



}
