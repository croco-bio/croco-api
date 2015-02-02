package de.lmu.ifi.bio.crco.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.util.StringUtil;


public  class NetworkHierachyNode  implements Comparable<NetworkHierachyNode> {
 
	private Integer groupId;
	private String name;
	private boolean selected = false;
	private boolean hasNetwork = false;
	private NetworkType type;
	private Integer numChildren = null;
	private Vector<NetworkHierachyNode> children;
	private Integer taxId;
	private NetworkHierachyNode parent;
	
	private Set<String> factors = new HashSet<String>();
	
	private HashMap<Option,String> options = new HashMap<Option,String>();
	
	//added for xsteam
	public NetworkHierachyNode(){}
	
	public NetworkType getType() {
		return type;
	}
	
	public NetworkHierachyNode getNode(String path) throws Exception{
	    return NetworkHierachyNode.getNode(this, path);
	}
	public static NetworkHierachyNode getNode(NetworkHierachyNode rootNode,String path) throws Exception
	{
	    String[] tokens = path.split("/");

	    for(String token : tokens){
	        if(token.length() == 0) continue;
	        NetworkHierachyNode newRoot = null;
	        for(NetworkHierachyNode child : rootNode.getChildren()){
	            if ( child.getName().equals(token)) {
	                newRoot = child;
	                break;
	            }
	        }
	        if ( newRoot == null) throw new Exception(String.format("Network not found for path %s stopped at %s (id: %d).",path,token,rootNode.getGroupId()));
	        rootNode = newRoot;
	    }
	    return rootNode;
	}
	public HashMap<Option,String> getOptions() {
        return options;
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

    public boolean hasNetwork() {
		return hasNetwork;
	}
	public String getHierachyAsString(){
		NetworkHierachyNode tmpParent = parent;
		StringBuffer ret = new StringBuffer(name );
		while(tmpParent != null ){
			ret.append("->" + parent.getName());
			
			if ( parent.parent != null && tmpParent.equals(parent.parent)) break;
			tmpParent = parent.parent;
			
		}
		return ret.toString();
	}
	
	public NetworkHierachyNode(NetworkHierachyNode node){
		this.groupId = node.getGroupId();
		this.name = node.getName();
		this.taxId = node.getTaxId();
		this.selected = node.isSelected();
		this.hasNetwork = node.hasNetwork();
		this.children = node.getChildren();
		this.type = node.getType();
		this.options = node.options;
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
/*
	public void setParent(NetworkHierachyNode parent) {
		this.parent = parent;
	}
*/
	public Integer getNumChildren() {
		 if ( numChildren == null){
			if ( this.getChildren() != null)
				return this.getChildren().size();
			return 0;
			 
		 }
		 return numChildren;
	}

	public void setNumChildren(Integer numChildren) {
		this.numChildren = numChildren;
	}


	public Integer getNumNetwork(){
		 if ( numChildren == null){
			 numChildren = 0;
			 if ( hasNetwork() )
				 numChildren++;
			 if ( children != null){
				 for(NetworkHierachyNode child : children){
					 numChildren+= ((NetworkHierachyNode) child).getNumNetwork();
				 }
			 }
			 
		 }
		 return numChildren;
	 }

	public NetworkHierachyNode(Integer groupId, Integer taxId){
		this.groupId = groupId;
		this.taxId = taxId;
		this.name = groupId +"";
	}
	private Integer parentGroupId;
	public Integer getParentGroupdId(){
		return parentGroupId;
	}
	
	public NetworkHierachyNode(Integer numChildren, Integer groupId, String name, boolean hasNetwork, Integer taxId, NetworkType type)
	{
	    this.numChildren = numChildren;
        this.groupId = groupId;
        
        this.type = type;
        this.name = name;
        this.hasNetwork = hasNetwork;
        this.taxId = taxId;
        
        this.options.put(Option.TaxId,taxId+"");
        this.options.put(Option.NetworkType,type.name());
	}
	public NetworkHierachyNode(Integer numChildren, Integer parentGroupId,Integer groupId,String name, boolean hasNetwork,Integer taxId,NetworkType type  ) {
	    this(numChildren,groupId,name,hasNetwork,taxId,type);
		
		this.parentGroupId = parentGroupId;
		this.children = null;
        
	}
	public NetworkHierachyNode(Integer numChildren, NetworkHierachyNode parent,Integer groupId,String name, boolean hasNetwork,Integer taxId,NetworkType type  ) {
		
	    this(numChildren,groupId,name,hasNetwork,taxId,type);
        
		this.parent = parent;
		if ( parent != null){
			parentGroupId = parent.groupId;
		}
		this.children = null;
	
        
	}
	public void addChild(NetworkHierachyNode child){
		if ( children == null){
			children = new Vector<NetworkHierachyNode>();
		}
		children.add(child);
	}
	 public String toString(){
		 return name;
	 }

	
	public int compareTo(NetworkHierachyNode o) {
		return name.toLowerCase().compareTo(o.getName().toLowerCase());
	}
	
	public boolean equals(Object o){
		if ( o instanceof NetworkHierachyNode && ((NetworkHierachyNode) o).getGroupId().equals(this.groupId))
			return true;
		else
			return false;
		
	}
	
	public boolean removeChild(NetworkHierachyNode node) {
		return this.children.remove(node);
		
	}

	public Vector<NetworkHierachyNode> getChildren() {
		return children;
	}

	public void setChildren(Vector<NetworkHierachyNode> children) {
		this.children = children;
	}

	public void setParent(NetworkHierachyNode parent) {
		this.parent = parent;
	}

	public NetworkHierachyNode getParent() {
		return parent;
	}
	public List<NetworkHierachyNode> getAllChildren(){
		Stack<NetworkHierachyNode> toProcess = new Stack<NetworkHierachyNode>();
		toProcess.add(this);
		List<NetworkHierachyNode> ret = new ArrayList<NetworkHierachyNode>();
		
		while(!toProcess.isEmpty()){
			NetworkHierachyNode top = toProcess.pop();
			if ( top.hasNetwork()){
				ret.add(top);
			}
			if ( top.getChildren() != null){
				for(NetworkHierachyNode child : top.getChildren()){
					toProcess.add(child);
				
				}
			}
		}
		return ret;
	}



}
