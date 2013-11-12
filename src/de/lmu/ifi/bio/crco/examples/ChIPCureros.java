package de.lmu.ifi.bio.crco.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.intervaltree.GenomeCoordinationMapper;
import de.lmu.ifi.bio.crco.intervaltree.Interval;
import de.lmu.ifi.bio.crco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.crco.intervaltree.peaks.GeneInterval;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.GenomeUtil;

public class ChIPCureros {

	class SummaryGene extends Interval{
		public SummaryGene(String chrom,int start, int end){
			super(start,end);
			this.chrom = chrom;
		}
		String chrom = null;
		HashSet<String> genes = new HashSet<String>();
		HashSet<String> transcripts = new HashSet<String>();
	}
	class DetailedPeak extends Interval{

		protected Float score;

		public DetailedPeak(double low, double high) {
			super(low, high);
		}

		public String getChrom() {
			return null;
		}
	}
	private HashMap<String, IntervalTree> filter(HashMap<String, IntervalTree> tree, int minSupport) {
		HashMap<String, IntervalTree> ret = new HashMap<String, IntervalTree> ();
		
		for ( Entry<String, IntervalTree> e  : tree.entrySet()){
			IntervalTree t = new IntervalTree();
			for(Object o : e.getValue().getObjects()){
				if ( o == null) continue;
				Peak peak = (Peak)o;
				if ( e.getValue().searchAll(peak).size()>=minSupport)t.insert(peak);
			}
			ret.put(e.getKey(), t);
		}
		return ret;
	}
	private HashMap<String,IntervalTree>  readChipTree(List<File> files ) throws Exception{
		HashMap<String,IntervalTree> tree = new HashMap<String,IntervalTree>();
		
		for(File file : files){
			CroCoLogger.getLogger().debug(String.format("Read file: %s",file));
			HashMap<String, List<Peak>> peaks = GenomeUtil.peakReader(file, null);
			for(Entry<String, List<Peak>> e : peaks.entrySet()){
				if (! tree.containsKey(e.getKey())){
					tree.put(e.getKey(), new IntervalTree());
				}
				for(Peak p : e.getValue()){
					tree.get(e.getKey()).insert(p);
				}
			}
		}
		return tree;
		
	}
	private HashMap<Peak, Peak> mapPeaks(HashMap<String,IntervalTree> tree, File liftOverExec, File chainFile, Float minMatch ) throws Exception{
		
		Set<Peak> peaks = new HashSet<Peak>();
		int k = 0;
		for ( Entry<String, IntervalTree> e  : tree.entrySet()){
			for(Object o : e.getValue().getObjects() )  {
				if ( o == null) continue;
				Peak peak = (Peak)o;
				peaks.add(peak);
				k++;
			}
		}
		CroCoLogger.getLogger().debug(String.format("Number of peaks: %d",k));
		HashMap<Peak, Peak> mapping = GenomeCoordinationMapper.map(liftOverExec, chainFile, peaks,  minMatch);
		return mapping;
	}
	private void writeGeneDist(HashMap<String,IntervalTree> tree, File outputFile, int step, String toRep) throws Exception{
	
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
		int maxGenes = 0;
		List<SummaryGene> sg = new ArrayList<SummaryGene>();
		for(Entry<String,IntervalTree> e : tree.entrySet() ){
			if ( e.getKey().contains("_")) continue;
			if ( e.getKey().contains("chrM")) continue;
			String chrom = e.getKey().replace("chr", toRep);
	
			int min = Integer.MAX_VALUE;
			int max=Integer.MIN_VALUE;
			
			Collection<GeneInterval> genes = e.getValue().getObjects();
			for(GeneInterval gene : genes) { 
				if ( gene == null) continue;
				min = (int)Math.min(min,gene.getLow());
				max = (int)Math.max(max,gene.getHigh());
			}
			
			for(int i = 0 ; i< max-step;i+=step){
				SummaryGene g = new SummaryGene(chrom,i,i+step);
				List<GeneInterval> overlaps = e.getValue().searchAll(g);
				for(GeneInterval overlap : overlaps){
					g.genes.add(overlap.getGeneId());
					g.transcripts.add(overlap.getTranscriptId());
				}
				sg.add(g);
			}
		}
		for(SummaryGene s : sg){
			maxGenes = (int)Math.max(maxGenes, s.transcripts.size());
		}
		
		for(SummaryGene s : sg){
			double frac = (double)s.transcripts.size()/(double)maxGenes;
			if ( frac <= 0) continue;
			bw.write(s.chrom + "\t" + (int)s.getLow() + "\t" + (int)s.getHigh() + "\t" + frac + "\n");
		}
			
		bw.flush();
		

		bw.flush();
		bw.close();
	}
	private void writeTransfer(HashMap<Peak, Peak> peaks, File outputFile,int step, String toRep) throws Exception {
		HashMap<String,IntervalTree> tree = new HashMap<String,IntervalTree> ();
		for(Peak peak : peaks.values()){
			if (! tree.containsKey(peak.getChrom())){
				tree.put(peak.getChrom(), new IntervalTree());
			}
			tree.get(peak.getChrom()).insert(peak);
		}
		writeChipPeakDist(tree,outputFile,step,toRep);
	}
	
	private void writeChipPeakDist(HashMap<String,IntervalTree> tree, File outputFile,int step, String toRep) throws Exception{
		class SummaryChromatin extends Interval{
			public SummaryChromatin(String chrom,int start, int end,int count){
				super(start,end);
				this.chrom = chrom;
				this.count = count;
			}
			String chrom = null;
			int count = 0;
		}
	
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
		int stepSize = 1000;
		int maxOpenChrom = 0;
		List<SummaryChromatin> sh = new ArrayList<SummaryChromatin>();
		for(Entry<String,IntervalTree> e : tree.entrySet() ){
			String chrom = e.getKey().replace("chr",toRep);
			int min = Integer.MAX_VALUE;
			int max=Integer.MIN_VALUE;
			
			Collection<Peak> peaks = e.getValue().getObjects();
			for(Peak peak : peaks) { 
				if ( peak == null) continue;
				min = (int)Math.min(min,peak.getLow());
				max = (int)Math.max(max,peak.getHigh());
			}
			
			for(int i = 0 ; i< max-step;i+=step){
				int count = 0;
				/*for(int j = i; j< i+step;j+=stepSize){
					boolean found = e.getValue().search(new Interval(j,j+stepSize))==null?false:true;
					if ( found) count++;
				}*/
				Interval tmp = new Interval(i,i+step);
				List result = e.getValue().searchAll(tmp);
				if ( result != null) count = result.size();
				
				SummaryChromatin g = new SummaryChromatin(chrom,i,i+step, count);
				sh.add(g);
				
			}
		}
		for(SummaryChromatin s : sh){
			maxOpenChrom = (int)Math.max(maxOpenChrom, s.count);
		}
		for(SummaryChromatin s : sh){
			double frac = (double)s.count/(double)maxOpenChrom;
			if ( frac <= 0) continue;
			bw.write(s.chrom + "\t" + (int)s.getLow() + "\t" + (int)s.getHigh() + "\t" + frac + "\n");
		}
		bw.flush();
		bw.close();
	}

	
	public static void main(String[] args) throws Exception{
		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();

		options.addOption(OptionBuilder.withLongOpt("liftOverExec").withArgName("FILE").withDescription("Lift over exceutable").isRequired().hasArgs(1).create("liftOverExec"));
		options.addOption(OptionBuilder.withLongOpt("chainFile").withArgName("FILE").withDescription("Chain file").isRequired().hasArgs(1).create("chainFile"));
		options.addOption(OptionBuilder.withLongOpt("minMatch").withDescription("Min match value (liftOver default = 0.1)").isRequired().hasArgs(1).create("minMatch"));	
	
		
		options.addOption(OptionBuilder.withLongOpt("genedefHuman").withArgName("FILE").withDescription("Human gene definition").isRequired().hasArgs(1).create("genedefHuman"));
		options.addOption(OptionBuilder.withLongOpt("genedefMouse").withArgName("FILE").withDescription("Mouse gene definition").isRequired().hasArgs(1).create("genedefMouse"));
		
		options.addOption(OptionBuilder.withLongOpt("humanChipPeaks").withArgName("FILE").withDescription("Human chip peaks").isRequired().hasArgs().create("humanChipPeaks"));
		options.addOption(OptionBuilder.withLongOpt("mouseChipPeaks").withArgName("FILE").withDescription("Mouse chip peaks").isRequired().hasArgs().create("mouseChipPeaks"));
		options.addOption(OptionBuilder.withLongOpt("transferredMouseChipPeaks").withArgName("FILE").withDescription("Transferred mouse chip peaks").isRequired().hasArgs().create("transferredMouseChipPeaks"));
		
		options.addOption(OptionBuilder.withLongOpt("output").withArgName("DIR").withDescription("Output dir").isRequired().hasArgs(1).create("output"));

		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + ChIPCureros.class.getName(), "", options, "", true);
			System.exit(1);
		}
		File output = new File(line.getOptionValue("output"));
	
		File genedefHuman = new File(line.getOptionValue("genedefHuman"));
		File genedefMouse = new File(line.getOptionValue("genedefMouse"));
		File liftOver = new File(line.getOptionValue("liftOverExec") );
		if (! liftOver.canExecute()){
			System.err.println("Can not execture lift over:" + liftOver);
			System.exit(1);
		}
	
		File chainFile = new File(line.getOptionValue("chainFile"));
		if (! chainFile.isFile()){
			System.err.println(chainFile + " does not exist");
			System.exit(1);
		}
		Float minMatch = Float.valueOf(line.getOptionValue("minMatch"));
		
		ChIPCureros plotData  = new ChIPCureros();
		
		CroCoLogger.getLogger().debug("Reading gene definitions");
		HashMap<String, IntervalTree> humanGeneTree = GenomeUtil.readIntervalTree(genedefHuman, 9606);
		HashMap<String, IntervalTree> mouseGeneTree = GenomeUtil.readIntervalTree(genedefMouse, 10090);
		
		List<File> humanChipFiles = new ArrayList<File>();
		for(String humanChipFile : line.getOptionValues("humanChipPeaks")){
			humanChipFiles.add(new File(humanChipFile));
		}
		List<File> mouseChipFiles = new ArrayList<File>();
		for(String mouseChipFile : line.getOptionValues("mouseChipPeaks")){
			mouseChipFiles.add(new File(mouseChipFile));
		}
		List<File> transferredMouseChipFiles = new ArrayList<File>();
		for(String mouseChipFile : line.getOptionValues("transferredMouseChipPeaks")){
			transferredMouseChipFiles.add(new File(mouseChipFile));
		}
		
		
		plotData.writeGeneDist(humanGeneTree, new File(output + "/human.genes"), 2000000,"hs");
		plotData.writeGeneDist(mouseGeneTree, new File(output + "/mouse.genes"), 2000000,"mm");
		HashMap<String, IntervalTree> transferredMouseChipTree = plotData.readChipTree(transferredMouseChipFiles);
		//mouseChipTree = plotData.filter(mouseChipTree,2);
		plotData.writeChipPeakDist(transferredMouseChipTree, new File(output + "/human.transferred.chip"), 2000000,"hs");
		
		
		HashMap<String, IntervalTree> humanChipTree = plotData.readChipTree(humanChipFiles);
		//humanChipTree = plotData.filter(humanChipTree,2);
		plotData.writeChipPeakDist(humanChipTree, new File(output + "/human.chip"), 2000000,"hs");
		
		HashMap<String, IntervalTree> mouseChipTree = plotData.readChipTree(mouseChipFiles);
		//mouseChipTree = plotData.filter(mouseChipTree,2);
		plotData.writeChipPeakDist(mouseChipTree, new File(output + "/mouse.chip"), 2000000,"mm");
		
	
		//HashMap<Peak, Peak> mapped = plotData.mapPeaks(mouseChipTree, liftOver, chainFile,minMatch);
		//plotData.writeTransfer(mapped,new File(output + "/human.transfer.chip"),2000000,"hs");
		
		//
		//HashMap<String, IntervalTree> mouseChipTree = plotData.readChipTree(humanChipFiles);
		
		//System.out.println(humanChipFiles);
		
	}


}
