package de.lmu.ifi.bio.croco.intervaltree.peaks;

/**
 * A genome transferred peak
 * @author pesch
 *
 */
public class TransferredPeak extends Peak{
	private Peak from;
	private Peak to;
	public TransferredPeak(Peak from, Peak to){
		super(to);
		this.from = from;
		this.to = to;
	}
	public Peak getFrom() {
		return from;
	}
	public Peak getTo() {
		return to;
	}
	
	@Override
	public String toString(){
		return String.format("%s from %s",to.toString(),from.toString());
	}
}
