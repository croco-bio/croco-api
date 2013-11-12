package de.lmu.ifi.bio.crco.cluster;

public final class Clusterable<E extends Object> {
	private E data;
	private float value;
	
	Clusterable(E data, float value){
		this.data = data;;
		this.value = value;
	}

	public E getData() {
		return data;
	}

	public float getValue() {
		return value;
	}
	
	public String toString(){
		return data.toString() + " (" + value + ")";
	}
}
