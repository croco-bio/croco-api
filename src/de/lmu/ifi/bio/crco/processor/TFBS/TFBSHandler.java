package de.lmu.ifi.bio.crco.processor.TFBS;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;


public abstract class TFBSHandler {
	//chrom -> TFBS peaks
	public  abstract HashMap<String,List<de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak>> readHits(File tfbsFile) throws Exception;
	
	public HashMap<String,List<TFBSPeak>> readHits(List<File> tfbsFiles) throws Exception{
		HashMap<String,List<TFBSPeak>> tfbsPeaks = new HashMap<String,List<TFBSPeak>>();
		
		for(File tfbsFile :tfbsFiles ){
			HashMap<String, List<TFBSPeak>> tmp = this.readHits(tfbsFile);
			for(Entry<String,List<TFBSPeak>> e : tmp.entrySet()){
				if (!tfbsPeaks.containsKey(e.getKey()) ){
					tfbsPeaks.put(e.getKey(), new ArrayList<TFBSPeak>());
				}
				tfbsPeaks.get(e.getKey()).addAll(e.getValue());
			}
		}
		
		return tfbsPeaks;
	}

}
