package de.lmu.ifi.bio.croco.data;

import java.util.ArrayList;
import java.util.List;

public class ContextTreeNode extends HierachyNode  {
	public ContextTreeNode(Integer contextId, String term, String description, int numLeafs) {
		super(numLeafs,contextId);
		this.contextId = contextId;
		this.term = term;
		this.description = description;
	}

	private Integer contextId;
	private String term;
	private String description;
	private List<ContextTreeNode> children;
	
	//added for xsteam
	public ContextTreeNode(){}
	
	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		this.term = term;
	}
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getContextId() {
		return contextId;
	}
	public void setContextId(Integer contextId) {
		this.contextId = contextId;
	}
	
	public String toString(){
		return description;
	}
	public void addChild(ContextTreeNode node) {
		if ( children == null){
			children = new ArrayList<ContextTreeNode>();
		}
		children.add(node);
	}
}
