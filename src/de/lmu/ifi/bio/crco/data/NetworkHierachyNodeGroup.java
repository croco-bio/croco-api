package de.lmu.ifi.bio.crco.data;

import java.util.List;


public class NetworkHierachyNodeGroup {
	
	
	public NetworkHierachyNodeGroup(String name, List<NetworkHierachyNode> nodes, NetworkType type) {
		super();
		this.name = name;
		this.nodes = nodes;
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public List<NetworkHierachyNode> getNodes() {
		return nodes;
	}
	private NetworkType type;
	public NetworkType getType() {
		return type;
	}
	private String name;
	private List<NetworkHierachyNode> nodes;
	
	@Override
	public String toString(){
		return String.format("%s; Type %s; Nodes %d",name,type.name(),nodes.size());
	}
}
