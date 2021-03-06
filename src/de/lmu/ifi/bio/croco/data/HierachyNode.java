package de.lmu.ifi.bio.croco.data;

import java.util.List;

public abstract class HierachyNode {
	
	private Integer numLeafs;
	private Integer id;
	
	//added for xsteam
	public HierachyNode(){};
	
	public HierachyNode(Integer numLeafs,Integer id){
		this.numLeafs = numLeafs;
		this.id = id;
	}

	public Integer getNumLeafs() {
		return numLeafs;
	}

	protected List<HierachyNode> children;
	
	 public List<HierachyNode> getChildren() {
		 return children;
	 }
	 
	 public void setChildren(List<HierachyNode> children) {
		 this.children = children;
	}
	 
	 public Integer getId(){
		 return id;
	 }
}
