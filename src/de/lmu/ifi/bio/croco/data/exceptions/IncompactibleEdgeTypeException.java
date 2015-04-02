package de.lmu.ifi.bio.croco.data.exceptions;

public class IncompactibleEdgeTypeException extends CroCoException{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IncompactibleEdgeTypeException(Exception e){
		super("Probably directed and undirected networks were combinded",e);
	}
}
