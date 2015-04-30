package de.lmu.ifi.bio.croco.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.data.genome.Strand;
import de.lmu.ifi.bio.croco.data.genome.Transcript;
import de.lmu.ifi.bio.croco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Promoter;



public class GenomeUtil {
	/**
	 * @return IntervalTree with enriched regions for each chromosome 
	 * @throws IOException when the peakFile is not readable
	 */
	public static HashMap<String,IntervalTree<Peak>> createPeakIntervalTree(File peakFile,Integer chromIndex, Integer startIndex, Integer endIndex,Integer scoreIndex,Integer maxSize) throws IOException{
		HashMap<String,IntervalTree<Peak>> peaks = new HashMap<String,IntervalTree<Peak>>();
		boolean gff = false;
		BufferedReader data = null;
		if ( peakFile.getName().endsWith(".gz"))
			data= new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(peakFile))));
		else
			data = new BufferedReader(new FileReader(peakFile));
		String dataLine = null;
		int k = 0;
		while (( dataLine = data.readLine())!=null){
			if ( dataLine.startsWith("##gff-version")) gff = true;
			if ( dataLine.startsWith("#")) continue;
			String[] tokens = dataLine.split("\t");
			String chromosom = tokens[chromIndex];
			Integer start = Integer.valueOf(tokens[startIndex]);
			Integer end = Integer.valueOf(tokens[endIndex]);
			Integer size = end-start;
			if ( maxSize != null && maxSize >= 0 && size > maxSize) continue;
			Float score = null;
			if ( gff == false && scoreIndex != null && scoreIndex > 0 &&  tokens.length >= scoreIndex) score = Float.valueOf(tokens[scoreIndex]);
			
			if ( start > end){
				CroCoLogger.getLogger().warn("Start position can not be greater than end position (" + dataLine  + ")");
				continue;
			}
			Peak peak = new Peak(chromosom,start,end,score);
			if (!peaks.containsKey(chromosom)){
				peaks.put(chromosom, new IntervalTree<Peak>());
			}
			peaks.get(chromosom).insert(peak);
			k++;
		}
		//sanity check
		if ( countPeaks(peaks) != k) throw new RuntimeException("Number of peaks differs");
		data.close();
		return peaks;
	}
	public static int countPeaks(HashMap<String,IntervalTree<Peak>> peaks){ 
		int k = 0;
		for(Entry<String, IntervalTree<Peak>> e : peaks.entrySet()){
			for(Peak p : e.getValue().getObjects()){
				if ( p == null) continue;
				k++;
			}
		}
		return k;
	}
	
	public static class TFBSGeneEnrichment{
		public Gene gene;
		public Transcript closestTranscriptUpstream;
		public Transcript closestTranscriptDownstream;
		public TFBSGeneEnrichment(Gene gene,Transcript closestTranscriptUpstream,Transcript closestTranscriptDownstream) {
			super();
			this.gene = gene;
			this.closestTranscriptUpstream = closestTranscriptUpstream;
			this.closestTranscriptDownstream = closestTranscriptDownstream;
		}
		
	}
	
	public static List<TFBSGeneEnrichment> enrich(List<Promoter> promoters,Integer position,Integer upstream, Integer downstream){
		HashSet<Gene> releventGenes = new HashSet<Gene>();
		
		for(Promoter promoter : promoters){
			for(Transcript transcript : promoter.getTranscripts()){
		
				releventGenes.add(transcript.getParentGene());;
			}
		}
		 List<TFBSGeneEnrichment> ret = new  ArrayList<TFBSGeneEnrichment>();
		for(Gene relevantGene :releventGenes){
			Transcript closestUpstream = null;
			Transcript cloestDownstream = null;
		
			for(Transcript transcript : relevantGene.getTranscripts()){
				Integer distanceToTss =Transcript.getDistanceToTssStart(transcript,position);
			
				if (distanceToTss<0  && Math.abs(distanceToTss)<=upstream  &&  (closestUpstream == null ||distanceToTss>Transcript.getDistanceToTssStart(closestUpstream, position)) ){
					closestUpstream = transcript;
				}else if (distanceToTss>=0 &&  distanceToTss<=downstream&&  (cloestDownstream == null ||distanceToTss<Transcript.getDistanceToTssStart(cloestDownstream, position)) ){
					cloestDownstream = transcript;
				}
			}
			if ( closestUpstream == null && cloestDownstream == null) continue;
			ret.add(new TFBSGeneEnrichment(relevantGene, closestUpstream, cloestDownstream));
		}
		
		
		return ret;
	}
	
	/**
	 * Generates  promoter regions. 
	 * @param genes list of genes
	 * @return IntervalTree with promoter regions for each chromosome 
	 */
	public static HashMap<String,IntervalTree<Promoter>> createPromoterIntervalTree(List<Gene> genes, int tssUpstreamSpan, int tssDownstreamSpan, boolean noneOverlapping){
		HashMap<String,IntervalTree<Promoter>> intervalsTmp = new HashMap<String,IntervalTree<Promoter>> ();
		int g = 0;
		int t = 0;
		for(Gene gene : genes){
			if (! intervalsTmp.containsKey(gene.getChr())){
				intervalsTmp.put(gene.getChr(), new IntervalTree<Promoter>());
			}
			g++;
			IntervalTree<Promoter> chrTree = intervalsTmp.get(gene.getChr());
			
			for(Transcript transcript  : gene.getTranscripts()){
				t++;
				/*
				Promoter promoter = null;
				int start =transcript.getTSSStrandCorredStart();
				int end = transcript.getTSSStrandCorredEnd();
				if ( start > end){
					int tmp = start;
					start = end;
					end = tmp;
				}
			
				promoter = new Promoter(start,end,transcript);
				*/
				
				Promoter promoter = null;
				
				if ( gene.getStrand().equals(Strand.PLUS))
					promoter = new Promoter(Math.max(transcript.getStrandCorredStart()-tssUpstreamSpan,0),transcript.getStrandCorredStart()+tssDownstreamSpan,transcript);
				else
					promoter = new Promoter(Math.max(transcript.getStrandCorredStart()-tssDownstreamSpan,0),transcript.getStrandCorredStart()+tssUpstreamSpan,transcript);
				
				chrTree.insert(promoter);
			}
		}
		CroCoLogger.getLogger().debug(String.format("Number of genes: %d",g));
		CroCoLogger.getLogger().debug(String.format("Number of transcript: %d",t));
		
		if ( noneOverlapping){
			HashMap<String,IntervalTree<Promoter>> intervals = new HashMap<String,IntervalTree<Promoter>> ();
			for(Entry<String, IntervalTree<Promoter>>  e: intervalsTmp.entrySet()){
				
				IntervalTree<Promoter> newTree = new IntervalTree<Promoter>();
				Iterator<Promoter> it = e.getValue().getObjects().iterator();
				while(it.hasNext()){
					Promoter currentPromoter = it.next();
					
					if ( currentPromoter == null || newTree.search(currentPromoter) != null){ //skip when already contained
						continue;
					}
					Set<Transcript> transcripts = new HashSet<Transcript>();
					transcripts.addAll(currentPromoter.getTranscripts());
					while(true){
						List<Promoter> promoters = e.getValue().searchAll(currentPromoter);
					
						int min= currentPromoter.getStart();
						int max = currentPromoter.getEnd();
						if ( promoters != null && promoters.size() > 0){
							for(Promoter promoter : promoters){ 
								currentPromoter.getTranscripts().addAll(promoter.getTranscripts());
								transcripts.addAll(promoter.getTranscripts());
								min = Math.min(min, promoter.getStart());
								max = Math.max(max, promoter.getEnd());
							}
						}
						
						if ( min == currentPromoter.getStart() && max == currentPromoter.getEnd()) break; //no more changes
						currentPromoter = new Promoter(min,max,transcripts);
					}
					
					newTree.insert(currentPromoter);
				}
				intervals.put(e.getKey(),newTree);
			}
			return intervals;
		}
		return intervalsTmp;
	}
	
	public static String reverse(String dna){
		StringBuffer ret = new StringBuffer();
		for(int i = dna.length()-1 ; i >= 0 ; i--){
			char c = dna.charAt(i);
			if ( c == 'T'){
				ret.append('A');
			}else if ( c == 'A'){
				ret.append('T');
			}else if ( c == 'G'){
				ret.append('C');
			}else if ( c == 'C'){
				ret.append('G');
			}else if ( c=='N'){
				ret.append('N');
			}else{ //TODO: change to proper exception!
				throw new RuntimeException("Unknown letter (" + c);
			}
		}
		
        return ret.toString();
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