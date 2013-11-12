package de.lmu.ifi.bio.crco.operation;


public class Parameter{

	private Class<?> clazz = null;
	private String name = null;
	private Object defaultValue= null;


	public Parameter(String name,Class<?> clazz,Object defaultValue){
		this.clazz = clazz;
		this.name = name;
		this.defaultValue = defaultValue;
	}
	public Parameter(String name,Class<?> clazz){
		this.clazz = clazz;
		this.name = name;
	}
	public Object getDefaultValue() {
		return defaultValue;
	}

	public String getName(){
		return name;
	}
	public Class<?> getClazz() {
		return clazz;
	}

}