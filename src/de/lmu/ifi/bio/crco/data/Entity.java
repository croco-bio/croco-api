package de.lmu.ifi.bio.crco.data;

public class Entity {
	private String name;
	private String identifier;
	
	
	public String getName() {
		return name;
	}

	public String getIdentifier() {
		return identifier;
	}
	public Entity(){}
	
	public Entity(String identifier){
		this.identifier = identifier.toUpperCase();
		this.name = identifier;
	}
	

	public void setName(String name) {
		this.name = name;
	}
	public Entity(String identifier, String name){
		this.identifier = identifier.toUpperCase();
		this.name = name;
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
