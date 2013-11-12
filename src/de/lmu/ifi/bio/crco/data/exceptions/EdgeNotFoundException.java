package de.lmu.ifi.bio.crco.data.exceptions;

import de.lmu.ifi.bio.crco.util.Tuple;

public class EdgeNotFoundException extends CroCoException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EdgeNotFoundException(int id){
		super(String.format("Edge %d not found",id));
	}
}
