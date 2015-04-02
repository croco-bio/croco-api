package de.lmu.ifi.bio.croco.network;

import java.util.ArrayList;
import java.util.List;

public class NetworkSummary {
	private List<NetworkSummary> children;
	
	private int nodes;
	private int edges;
	private String name;
	public String getName(){
		return name;
	}
	public int getNodes() {
		return nodes;
	}
	public int getEdges() {
		return edges;
	}

	public NetworkSummary(String name, int nodes, int edges) {
		super();
		this.name = name;
		this.nodes = nodes;
		this.edges = edges;
		this.children = new ArrayList<NetworkSummary>();
	}
	
	public void addChild(NetworkSummary child){
		children.add(child);
	}
	
	public List<NetworkSummary> getChildren(){
		return this.children;
	}
	
	@Override
	public String toString(){
		return name + "-- (Edges:" + edges + "(Nodes" + nodes + "))" ;
	}
}
