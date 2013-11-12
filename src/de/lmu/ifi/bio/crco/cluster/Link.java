package de.lmu.ifi.bio.crco.cluster;

public class Link<E>{
	Double score;
	Node<E> toNode;
	public Link(Double score, Node<E> toNode) {
		super();
		this.score = score;
		this.toNode = toNode;
	}
	
	public String toString(){
		return toNode.toString();
	}
}
