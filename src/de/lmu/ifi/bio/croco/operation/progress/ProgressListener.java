package de.lmu.ifi.bio.croco.operation.progress;


import de.lmu.ifi.bio.croco.operation.GeneralOperation;

public abstract class ProgressListener {

	
	/*
	public void doUpdate(final GeneralOperation operation){
		 SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
			 	ProgressListener.this.update(operation);
			}
				 
		 });
				
		
	
	}
	*/
	
	public abstract void update(GeneralOperation operation);
}
