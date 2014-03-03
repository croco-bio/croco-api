package de.lmu.ifi.bio.crco.intervaltree.peaks;


/**
 * Represents a TFBS and a general Peak (used for DNase/TFBS peaks)
 * @author pesch
 *
 */
public class DNaseTFBSPeak extends Peak {
	public TFBSPeak getTfbsPeak() {
		return tfbsPeak;
	}
	public void setTfbsPeak(TFBSPeak tfbsPeak) {
		this.tfbsPeak = tfbsPeak;
	}
	public Peak getOpenChromPeak() {
		return openChromPeak;
	}
	public void setOpenChromPeak(Peak openChromPeak) {
		this.openChromPeak = openChromPeak;
	}
	private TFBSPeak tfbsPeak;
	private Peak openChromPeak;
	public DNaseTFBSPeak(TFBSPeak tfbsPeak, Peak openChromPeak){
		super((int)tfbsPeak.getLow(),(int)tfbsPeak.getHigh());
		this.tfbsPeak = tfbsPeak;
		this.openChromPeak = openChromPeak;
	}
}
