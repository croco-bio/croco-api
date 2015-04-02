package de.lmu.ifi.bio.croco.data.exceptions;

public class OperationNotPossibleException extends CroCoException {


	private static final long serialVersionUID = 1L;

	public OperationNotPossibleException(String message){
		super(message);
	}
	public OperationNotPossibleException(String message, Exception e){
		super(message,e);
	}
}
