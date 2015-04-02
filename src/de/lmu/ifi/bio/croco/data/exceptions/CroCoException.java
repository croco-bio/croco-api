package de.lmu.ifi.bio.croco.data.exceptions;

public class CroCoException extends Exception {

	public CroCoException(String message, Exception e) {
		super(message,e);
	}
	public CroCoException(String message) {
		super(message);
	}
	public CroCoException() {
		super();
	}
	
	private static final long serialVersionUID = 1L;

}
