package de.lmu.ifi.bio.croco.data.genome;

public enum Strand{
	PLUS,MINUS;
	
	public static Strand getStand(String strand){
		if ( strand.equals("+")) return Strand.PLUS;
		return Strand.MINUS;
	}
}

