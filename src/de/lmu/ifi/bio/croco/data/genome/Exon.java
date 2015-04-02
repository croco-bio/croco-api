package de.lmu.ifi.bio.croco.data.genome;

public class Exon {
	private String exonId;
	private Integer frame;
	
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public void setCoding(int frame){
		this.frame= frame;
	}
	 
	private int start;
	private int end;
	public Exon(String exonId, int start, int end) {
		super();
		this.exonId = exonId;
		this.start = start;
		this.end = end;
	}
	@Override
	public String toString(){
		return String.format("%s; %d-%d (%s)",exonId,start,end,(frame==null?"NA":frame.toString()) );
	}
	public String getExonId() {
		return exonId;
	}
	public int getFrame() {
		return frame;
	}
}
