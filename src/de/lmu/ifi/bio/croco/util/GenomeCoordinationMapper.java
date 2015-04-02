package de.lmu.ifi.bio.croco.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;


public class GenomeCoordinationMapper {
	private static class DetailedPeak extends Peak{
		private String chrom;
		public DetailedPeak(String chrom, int start, int end, float score) {
			super(start, end, score);
			this.chrom = chrom;
		}
		public String getChrom() {
			return chrom;
		}
		public String toString(){
			return chrom +":" +(int)super.getLow()+"-"+(int)super.getHigh();
	 	}
	}
	public static void main(String[] args) throws IOException, InterruptedException{
		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("liftOverDir").withArgName("DIR").withDescription("Dir with Lift over exceutable").isRequired().hasArgs(1).create("liftOver"));
		options.addOption(OptionBuilder.withLongOpt("chainFile").withArgName("FILE").withDescription("Chain file").isRequired().hasArgs(1).create("chainFile"));
		options.addOption(OptionBuilder.withLongOpt("peakFile").withArgName("FILE").withDescription("Peak file").isRequired().hasArgs(1).create("peakFile"));
		options.addOption(OptionBuilder.withLongOpt("output").withArgName("FILE").withDescription("Output file").isRequired().hasArgs(1).create("output"));
		options.addOption(OptionBuilder.withLongOpt("mapping").withDescription("Write mapping file instead of bed file").create("mapping"));	
		options.addOption(OptionBuilder.withLongOpt("minMatch").withDescription("Min match value (liftOver default = 0.1)").isRequired().hasArgs(1).create("minMatch"));	
		
		CommandLine line = null;
		
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + GenomeCoordinationMapper.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		File liftOver = new File(line.getOptionValue("liftOverDir") + "/liftOver");
		if (! liftOver.isFile()){
			System.err.println("can not find lift over in " + liftOver);
			System.exit(1);
		}
		if (! liftOver.isFile()){
			System.err.println("liftOver executable not found (" + liftOver);
			System.exit(1);
		}
		File chainFile = new File(line.getOptionValue("chainFile"));
		if (! chainFile.isFile()){
			System.err.println(chainFile + " does not exist");
			System.exit(1);
		}
		File peakFile = new File(line.getOptionValue("peakFile"));
		if (! peakFile.exists()){
			System.err.println(peakFile + " does not exist");
			System.exit(1);
		}
		Boolean mapping = line.hasOption("mapping");
		Float minMatch = Float.valueOf(line.getOptionValue("minMatch"));
		File outputFile = new File(line.getOptionValue("output"));
		
		System.out.println("Lift over dir:\t" + liftOver);
		System.out.println("Chain file:\t" + chainFile);
		System.out.println("Input file:\t" + peakFile);
		System.out.println("Output file:\t" +outputFile ) ;
		System.out.println("Mapping:\t" + mapping);
		System.out.println("Min match value:\t" + minMatch);
		
		HashMap<String, List<Peak>> peaks = GenomeUtil.peakReader(peakFile,null);
		
		File tmpFileInput = File.createTempFile("LiftOver.", ".peaks");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFileInput));
		List<DetailedPeak> peakList = new ArrayList<DetailedPeak>();
		
		for(Entry<String, List<Peak>> entry  : peaks.entrySet()){
			for(Peak peak : entry.getValue()){
				peakList.add(new DetailedPeak(entry.getKey(),(int)peak.getLow(),(int)peak.getHigh(),peak.score));
				bw.write(entry.getKey() + "\t" +(int) peak.getLow() + "\t" + (int)peak.getHigh() + "\n");
			}
		}
		bw.flush();
		bw.close();
		File tmpFileOutput = File.createTempFile("LiftOver.", ".mapped");
		File tmpFileOutputNotMapped = File.createTempFile("LiftOver.", ".notmapped");
		
		GenomeCoordinationMapper.liftOver(liftOver,chainFile,tmpFileInput, tmpFileOutput,tmpFileOutputNotMapped,minMatch);
		List<DetailedPeak> mappedPeakList = new ArrayList<DetailedPeak>();
		BufferedReader br = new BufferedReader(new FileReader(tmpFileOutput));
		String currentLine = null;
		while( (currentLine = br.readLine())!=null){
			String[] tokens = currentLine.split("\t");
			String chrom = tokens[0];
			Integer start = Integer.valueOf(tokens[1]);
			Integer end = Integer.valueOf(tokens[2]);
			mappedPeakList.add(new DetailedPeak(chrom,start,end,-1));
		}
		br.close();
		List<DetailedPeak> notMappedPeakList = new ArrayList<DetailedPeak>();
		br = new BufferedReader(new FileReader(tmpFileOutputNotMapped));
		currentLine = null;
		while( (currentLine = br.readLine())!=null){
			if ( currentLine.startsWith("#")) continue;
			String[] tokens = currentLine.split("\t");
			String chrom = tokens[0];
			Integer start = Integer.valueOf(tokens[1]);
			Integer end = Integer.valueOf(tokens[2]);
			notMappedPeakList.add(new DetailedPeak(chrom,start,end,-1));
		}
		br.close();
		
		int j = 0, k = 0;
		
		bw = new BufferedWriter(new FileWriter(outputFile));
		
		if ( mappedPeakList.size()+notMappedPeakList.size() != peakList.size() ) {
			System.err.println("Number of mappings strange. Can not write mapping file.");
			System.out.println(mappedPeakList.size() + "\t" +notMappedPeakList.size()  + "\t" +peakList.size()  );
		}else{
			for(DetailedPeak peak : peakList){
				if (j < notMappedPeakList.size() && peak.equals(notMappedPeakList.get(j))){
					if ( mapping)
						bw.write("#Not mapped\t" + peak.getChrom() + "\t" + (int)peak.getLow() + "\t" + (int)peak.getHigh() +"\t" +  peak.score +  "\n");
					j++;
				}else{
					if ( mapping)
						bw.write(mappedPeakList.get(k).getChrom() + "\t" + (int)mappedPeakList.get(k).getLow() + "\t" + (int)mappedPeakList.get(k).getHigh()  + "\t" + peak.getChrom() + "\t" + (int)peak.getLow() + "\t" + (int)peak.getHigh() + "\t" +  peak.score + "\n" );
					else
						bw.write( mappedPeakList.get(k).getChrom() + "\t" + (int)mappedPeakList.get(k).getLow() + "\t" + (int)mappedPeakList.get(k).getHigh() + "\t" + peak.score + "\n");
					k++;
				}
			}
		}
		
		bw.flush();
		bw.close();
		
		tmpFileOutput.delete();
		tmpFileOutputNotMapped.delete();
		tmpFileInput.delete();
		
		
	}
	
	public static HashMap<Peak,Peak> map(File liftOverExec, File cainFile, Set<Peak> toTransfer, Float minMatch, String chrPrefix) throws Exception{
		
		File peakFile = File.createTempFile("liftOver.in.", ".tmp");
		BufferedWriter bw = new BufferedWriter(new FileWriter(peakFile));
		for(Peak peak  : toTransfer){
			if ( peak.getChrom() == null) {
				CroCoLogger.getLogger().warn("No chromosome name given for peak");
				continue;
			}
		
			bw.write( (chrPrefix!=null?chrPrefix:"") + peak.getChrom() + "\t" + peak.getStart() + "\t" + peak.getEnd() + "\n");
		}
		bw.flush();
		bw.close();
		
		File outputMatched = File.createTempFile("liftOver.out.matched", ".tmp");
		File outputNotMatched = File.createTempFile("liftOver.out.notmatched", ".tmp");
		CroCoLogger.getLogger().info(String.format("Start lift over with %d peaks",toTransfer.size()));
		CroCoLogger.getLogger().debug(String.format("Match tmp file: %s",outputMatched.toString()));
		CroCoLogger.getLogger().debug(String.format("No match tmp file: %s",outputNotMatched.toString()));
		
		liftOver(liftOverExec,cainFile,peakFile,outputMatched,outputNotMatched,minMatch);
		
		List<Peak> mappedPeakList = new ArrayList<Peak>();
		BufferedReader br = new BufferedReader(new FileReader(outputMatched));
		String currentLine = null;
		while( (currentLine = br.readLine())!=null){
			String[] tokens = currentLine.split("\t");
			String chrom = tokens[0];
			
			Integer start = Integer.valueOf(tokens[1]);
			Integer end = Integer.valueOf(tokens[2]);
			mappedPeakList.add(new Peak(chrom,start,end));
		}
		br.close();
		List<Peak> notMappedPeakList = new ArrayList<Peak>();
		br = new BufferedReader(new FileReader(outputNotMatched));
		currentLine = null;
		while( (currentLine = br.readLine())!=null){
			if ( currentLine.startsWith("#")) continue;
			String[] tokens = currentLine.split("\t");
			String chrom = tokens[0];
			Integer start = Integer.valueOf(tokens[1]);
			Integer end = Integer.valueOf(tokens[2]);
			notMappedPeakList.add(new Peak(chrom,start,end));
		}
		br.close();
		
		int j = 0, k = 0;
		
		HashMap<Peak,Peak> ret = new HashMap<Peak,Peak> ();

		if ( mappedPeakList.size()+notMappedPeakList.size() != toTransfer.size() ) {
			throw new RuntimeException("Number of mappings is strange. Can not create mapping.");
		}else{
			for(Peak peak : toTransfer){
				if (j < notMappedPeakList.size() && peak.equals(chrPrefix,notMappedPeakList.get(j))){
					j++;
				}else{
					//from -> to
					ret.put(peak, mappedPeakList.get(k));
					k++;
				}
			}
		}
		
	//	peakFile.delete();
	//	outputMatched.delete();
	//	outputNotMatched.delete();
		
		return ret;
	}
	
	public static void liftOver(File liftOverExec, File chainFile, File peakFile, File outputMatched, File outputNotMatched,Float minMatch) throws IOException, InterruptedException{
		CroCoLogger.getLogger().info("Start liftOver");
		ArrayList<String> command = new ArrayList<String>();
		command.add(liftOverExec.toString() );
		command.add("-minMatch="+minMatch);
		command.add(peakFile.toString());
		command.add(chainFile.toString());
		command.add(outputMatched.toString());
		command.add(outputNotMatched.toString());
		
		ProcessBuilder builder = new ProcessBuilder(command  );
		CroCoLogger.getLogger().debug(String.format("Start program: %s",command.toString()));
		
		//builder.command("-minMatch=0.1 " + peakFile.toString() +" " + chainFile.toString()   +" " + outputMatched.toString() + " " + outputNotMatched.toString() );
		builder.redirectErrorStream(true);
		Process process = builder.start();
		//BufferedReader br = new BufferedReader(process.getOutputStream());
		BufferedReader reader = new BufferedReader (new InputStreamReader(process.getInputStream()));
		String line = null;
		while ((line = reader.readLine ()) != null) {
		    CroCoLogger.getLogger().debug("\tLiftOver:\tStdout: " + line);
		}
		reader.close();
		process.waitFor();
		
		int exitValue = process.exitValue();
		if ( exitValue != 0){
			throw new RuntimeException("Lift over exit with return value != 0");
		}
	}
}
