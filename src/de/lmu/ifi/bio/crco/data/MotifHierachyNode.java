package de.lmu.ifi.bio.crco.data;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.crco.util.Pair;


public class MotifHierachyNode extends HierachyNode {
	private String name;
	private String description;
	private Integer mId;
	private Integer taxid;
	public Integer getmId() {
		return mId;
	}
	public void setmId(Integer mId) {
		this.mId = mId;
	}
	private List<Pair<Gene,Gene>> edges = new ArrayList<Pair<Gene,Gene>>();
	
	
	public MotifHierachyNode(Integer mId,Integer taxId,String name, String description,  Integer numLeafs) {
		super(numLeafs,mId);
		this.mId = mId;
		this.name = name;
		this.description = description;
		this.taxid = taxId;
	}
	
	public Integer getTaxid() {
		return taxid;
	}
	public void setTaxid(Integer taxid) {
		this.taxid = taxid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public List<Pair<Gene,Gene>> getEdges(){
		return edges;
	}
	public void addEdge(Pair<Gene,Gene> edge){
		edges.add(edge);
	}
	@Override
	public String toString(){
		return name;
	}
	public void clearEdges() {
		edges = new ArrayList<Pair<Gene,Gene>>();
	}
	
}
