package de.lmu.ifi.bio.crco.operation.progress;

import javax.swing.SwingUtilities;

public abstract class ProgressListener {

	
	
	public void doUpdate(final String message){
		 SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
			 	ProgressListener.this.update(message);
			}
				 
		 });
				
		
	
	}
	
	
	public abstract void update(String message);
}
