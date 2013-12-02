package de.lmu.ifi.bio.crco.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.bio.crco.data.genome.Strand;
import de.lmu.ifi.bio.crco.intervaltree.Interval;
import de.lmu.ifi.bio.crco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.crco.intervaltree.peaks.GeneInterval;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;



public class GenomeUtil {
	
	/**
	 * Reads a genome mapping and creates  gene interval trees.
	 * @param connection
	 * @param geneMappingFile
	 * @param taxId
	 * @param chromosomNameMapping
	 * @param map
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static HashMap<String,IntervalTree> readIntervalTree(File geneMappingFile, int taxId,  boolean promoter, int spanUpstream, int spanDownstream ) throws IOException, SQLException{
	
		CroCoLogger.getLogger().debug(String.format("Read: %s", geneMappingFile));
		HashMap<String,IntervalTree> trees = new HashMap<String,IntervalTree>();
		BufferedReader br = new BufferedReader(new FileReader(geneMappingFile));
		String line = br.readLine();
		while ( (line = br.readLine()) != null){
			if ( line.startsWith("#")) continue;
			String[] tokens = line.split("\\s+");
			String transcript = tokens[1];
			String chrom =  tokens[2];
			String strand = tokens[3];
			Integer start = Integer.valueOf(tokens[4]);
			Integer end = Integer.valueOf(tokens[5]);
			String id = tokens[12].trim().toUpperCase();
			
			if ( id == null) continue;
		
			Interval interval = null;
			if ( promoter){
				
				if ( strand.equals("+")){
					interval = new GeneInterval(id,transcript,start-spanUpstream,start+spanDownstream,Strand.PLUS);
				}else{
					interval = new GeneInterval(id,transcript,end-spanDownstream,end+spanUpstream,Strand.MINUS);
				}
			}else{
				if ( strand.equals("+")){
					interval = new GeneInterval(id,transcript,start,end,Strand.PLUS);
				}else{
					interval = new GeneInterval(id,transcript,start,end,Strand.MINUS);
				}
			}
			if (! trees.containsKey(chrom)){
				trees.put(chrom,new IntervalTree());
			}
			trees.get(chrom).insert(interval);
		}
		
		br.close();
		return trees;
	}
	public static HashMap<String,IntervalTree> readIntervalTree(File geneMappingFile, int taxId ) throws IOException, SQLException{
		return readIntervalTree(geneMappingFile,taxId,false,-1,-1);
	}
		
	public static HashMap<String,List<Peak>> peakReader(File peakFile, HashMap<String,String> chromosomNameMapping) throws IOException{
		HashMap<String,List<Peak>> peaks = new HashMap<String,List<Peak>>();
		
		BufferedReader data = null;
		if ( peakFile.getName().endsWith(".gz"))
			data= new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(peakFile))));
		else
			data = new BufferedReader(new FileReader(peakFile));
		String dataLine = null;
		while (( dataLine = data.readLine())!=null){
			if ( dataLine.startsWith("#")) continue;
			String[] tokens = dataLine.split("\t");
			String chromosom = tokens[0];
			if (chromosomNameMapping != null && chromosomNameMapping.containsKey(chromosom)){
				chromosom = chromosomNameMapping.get(chromosom);
			}
			Integer start = Integer.valueOf(tokens[1]);
			Integer end = Integer.valueOf(tokens[2]);
			
			Float score = -1f;
			if ( tokens.length >6){
				score = Float.valueOf(tokens[6]);
			}
			if ( start > end){
				System.err.println("Start position can not be greater than end position (" + dataLine  + ")");
				continue;
			}
			
			Peak peak = new Peak(chromosom,start,end,score);
			
			if (!peaks.containsKey(chromosom)){
				peaks.put(chromosom, new ArrayList<Peak>());
			}
			peaks.get(chromosom).add(peak);
		}
		
		data.close();
		
		return peaks;
	}
	

}