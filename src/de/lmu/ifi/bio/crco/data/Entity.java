package de.lmu.ifi.bio.crco.data;

public class Entity {
	private String name;
	private String identifier;
	private String type;
	
	public String getType() {
		return type;
	}
	public String getName() {
		return name;
	}
	public Entity(){}
	
	public Entity(String identifier){
		this.name = identifier;
		this.identifier = identifier;
	}
	
	public Entity(String identifier, String name){
		this.identifier = identifier;
		this.name = name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	public Entity(String identifier, String name, String type){
		this.identifier = identifier;
		this.name = name;
		this.type = type;
	}

	public String getIdentifier() {
		return identifier;
	}
	
	@Override
	public int hashCode() {
		return identifier.toUpperCase().hashCode();
	}
	@Override
	public String toString(){
		return identifier;
	}
	@Override
	public boolean equals(Object e){
		return e.toString().toLowerCase().equals(this.toString().toLowerCase());
	}
}
