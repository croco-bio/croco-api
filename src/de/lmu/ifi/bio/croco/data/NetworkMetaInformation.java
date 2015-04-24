package de.lmu.ifi.bio.croco.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public  class NetworkMetaInformation  implements Comparable<NetworkMetaInformation>, Identifiable {
 
	private Integer groupId;
	private String name;
	private boolean selected = false;
	//private boolean hasNetwork = false;
	private NetworkType type;
	//private Integer numChildren = null;
	//private Vector<NetworkHierachyNode> children;
	private Integer taxId;
	//private NetworkHierachyNode parent;
	
	private Set<String> factors = new HashSet<String>();
	
	private HashMap<Option,String> options = new HashMap<Option,String>();
	
	//added for xsteam
	public NetworkMetaInformation(){}

	public NetworkMetaInformation(Integer groupId, String name,  Integer taxId, NetworkType type)
	{
	    this.groupId = groupId;

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

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public Integer getTaxId() {
		return taxId;
	}

	public void setTaxId(Integer taxId) {
		this.taxId = taxId;
		this.options.put(Option.TaxId,taxId+"");
	}


	private Integer parentGroupId;
	public Integer getParentGroupdId(){
		return parentGroupId;
	}
	
	    
	   
	public int compareTo(NetworkMetaInformation o) {
		return name.toLowerCase().compareTo(o.getName().toLowerCase());
	}
	
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
