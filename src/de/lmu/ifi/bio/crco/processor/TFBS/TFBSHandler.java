package de.lmu.ifi.bio.crco.processor.TFBS;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import de.lmu.ifi.bio.crco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;


public abstract class TFBSHandler {
	//chrom -> TFBS peaks
	public  abstract HashMap<String,IntervalTree<de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak>> readHits(File tfbsFile) throws Exception;
	
	public HashMap<String,IntervalTree<TFBSPeak>> readHits(List<File> tfbsFiles) throws Exception{
		HashMap<String,IntervalTree<TFBSPeak>>  tfbsPeaks = new HashMap<String,IntervalTree<TFBSPeak>> ();
		
		for(File tfbsFile :tfbsFiles ){
			HashMap<String, IntervalTree<TFBSPeak>> tmp = this.readHits(tfbsFile);
			for(Entry<String,IntervalTree<TFBSPeak>> e : tmp.entrySet()){
				if (!tfbsPeaks.containsKey(e.getKey()) ){
					tfbsPeaks.put(e.getKey(), new IntervalTree<TFBSPeak>());
				}
				for(TFBSPeak peak : e.getValue().getObjects()){
					if ( peak == null) continue;
					tfbsPeaks.get(e.getKey()).insert(peak);
				}
			
			}
		}
		
		return tfbsPeaks;
	}

}
