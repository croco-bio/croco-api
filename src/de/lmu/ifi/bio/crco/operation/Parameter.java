package de.lmu.ifi.bio.crco.operation;


public class Parameter<E extends Object>{

	private String name = null;
	private E defaultValue= null;


	public Parameter(String name,E defaultValue){
		this.name = name;
		this.defaultValue = defaultValue;
	}
	public Parameter(String name){
		this.name = name;
	}
	public E getDefaultValue() {
		return defaultValue;
	}

	public String getName(){
		return name;
	}


}