package de.lmu.ifi.bio.crco.cluster;

import java.util.ArrayList;
import java.util.List;


class Node<E>{
	public Node(int nodeId, E data){
		this.nodeId = nodeId;
		this.data = data;
	}
	int nodeId;
	E data;
	List<Link<E>> links = new ArrayList<Link<E>>();
	
	public List<E> getLeafs(){
		ArrayList<E> ret = new ArrayList<E>();
		for(Link<E> link : links){
			ret.addAll(link.toNode.getLeafs());
		}
		ret.add(data);
		return ret;
	}
	public String toString(){
		return nodeId + "\t" + data.toString() + "\t" + links;
	}
}