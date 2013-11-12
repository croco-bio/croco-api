package de.lmu.ifi.bio.crco.data;

public enum Strand{
	Plus,Minus;
	
	public static Strand getStand(String strand){
		if ( strand.equals("+")) return Strand.Plus;
		return Strand.Minus;
	}
}

