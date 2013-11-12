package de.lmu.ifi.bio.crco.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import de.lmu.ifi.bio.crco.operation.GeneralOperation;


public class NetworkOperationNode {
	private  GeneralOperation operator = null;
	private Integer taxId = null;
	private Vector<NetworkOperationNode> children = new Vector<NetworkOperationNode>();
	private NetworkOperationNode parent;
	
	public NetworkOperationNode(NetworkOperationNode parent,Integer taxId,  GeneralOperation operator ){
		this.operator = operator;
		this.taxId = taxId;
		this.parent = parent;
	}

	
	public Integer getTaxId(){
		return taxId;
	}

	
	public  GeneralOperation getOperator() {
		return operator;
	}
	
	public String toString(){
		String ret = "";
		if ( operator != null) ret = operator.getDescription(); //+ " (";
		
		ret += "(" + taxId + ") - ";
		
		
		
		return ret;
	}

	public void setTaxId(Integer taxId) {
		this.taxId = taxId;
		
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
}
