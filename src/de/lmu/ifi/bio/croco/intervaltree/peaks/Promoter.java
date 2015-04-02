package de.lmu.ifi.bio.croco.intervaltree.peaks;

import java.util.HashSet;
import java.util.Set;

import de.lmu.ifi.bio.croco.data.genome.Transcript;
import de.lmu.ifi.bio.croco.intervaltree.Interval;

/**
 * Represents a gene promoter. A gene can have multiple transcription start sides thus multiple core promoter regions. 
 * @author pesch
 *
 */
public class Promoter extends Interval {

	private int start;
	private int end;
	private Set<Transcript> transcripts;
	
	public Promoter(int start, int end,  Set<Transcript> transcripts ) {
		super(start,end);
		this.start = start;
		this.end = end;
		this.transcripts = transcripts;
	}
	public Set<Transcript> getTranscripts(){
		return transcripts;
	}
	
	public Promoter(int start, int end,  Transcript ...transcripts ) {
		super(start,end);
		this.start = start;
		this.end = end;
		this.transcripts = new HashSet<Transcript>(transcripts.length);
		for(Transcript transcript : transcripts ){
			this.transcripts.add(transcript);
		}
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	@Override
	public int hashCode(){
		return this.toString().hashCode();
	}
}
