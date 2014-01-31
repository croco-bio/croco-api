package de.lmu.ifi.bio.crco.operation.progress;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.bio.crco.operation.GeneralOperation;

public class ProgressInformation {
	public void addListener(ProgressListener listener){
		this.listeners.add(listener);
	}
	private List<ProgressListener> listeners = null;
	private Integer numberOfTasks;
	
	public Integer getNumberOfTasks() {
		return numberOfTasks;
	}
	public ProgressInformation(int numberOfTasks){
		this.numberOfTasks = numberOfTasks;
		this.listeners = new ArrayList<ProgressListener>();
	}
	public void nextStep(GeneralOperation operation){
		for(ProgressListener listener  : listeners){
			listener.update(operation);
		}
	}
	private boolean kill;

	public boolean isKill() {
		return kill;
	}
	public void setKill(boolean kill) {
		this.kill = kill;
	}

}
