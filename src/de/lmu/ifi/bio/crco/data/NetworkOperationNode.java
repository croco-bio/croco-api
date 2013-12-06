package de.lmu.ifi.bio.crco.data;

import java.util.Vector;

import de.lmu.ifi.bio.crco.operation.GeneralOperation;


public class NetworkOperationNode {
	private  GeneralOperation operator = null;
	private Species species = null;
	private Vector<NetworkOperationNode> children = new Vector<NetworkOperationNode>();
	private NetworkOperationNode parent;
	
	public NetworkOperationNode(){}
	
	public NetworkOperationNode(NetworkOperationNode parent,Integer taxId,  GeneralOperation operator ){
		this.operator = operator;
		this.species = Species.getSpecies(taxId);
		this.parent = parent;
	}
	public NetworkOperationNode(NetworkOperationNode parent,Species species,  GeneralOperation operator ){
		this.operator = operator;
		this.species = species;
		this.parent = parent;
	}

	
	public Species getSpecies(){
		return species;
	}

	
	public  GeneralOperation getOperator() {
		return operator;
	}
	
	public String toString(){
		String ret = "";
		if ( operator != null) ret = operator.getDescription(); //+ " (";
		if( species != null) ret += "(" + species.getName() + ")";
		
		return ret;
	}

	public void setSpecies(Species species) {
		this.species = species;
	}

	public Vector<NetworkOperationNode> getChildren() {
		return children;
	}

	public void addChild(NetworkOperationNode operatorable) {
		operatorable.setParent(this);
		
		//this.identifiers.addAll(operatorable.getIdentifiers());
		
		this.children.add(operatorable);
		
	}


	public NetworkOperationNode getParent() {
		return parent;
	}

	public void removeChild(NetworkOperationNode operatorable) {
		this.children.remove(operatorable);
		
	}

	public void setParent(NetworkOperationNode parent) {
		this.parent = parent;
		
	}
	public Integer getTaxId() {
		if ( species == null)return null;
		return species.getTaxId();
	}
}
