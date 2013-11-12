package de.lmu.ifi.bio.crco.intervaltree;

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

import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.util.CroCoLogger;


public class GenomeCoordinationMapper {
	
	public static HashMap<Peak,Peak> map(File liftOverExec, File cainFile, Set<Peak> toTransfer, Float minMatch) throws Exception{
		
		File peakFile = File.createTempFile("liftOver.in.", ".tmp");
		BufferedWriter bw = new BufferedWriter(new FileWriter(peakFile));
		for(Peak peak  : toTransfer){
			bw.write(peak.getChrom() + "\t" + peak.getStart() + "\t" + peak.getEnd() + "\n");
		}
		bw.flush();
		bw.close();
		
		File outputMatched = File.createTempFile("liftOver.out.matched", ".tmp");
		File outputNotMatched = File.createTempFile("liftOver.out.notmatched", ".tmp");
		CroCoLogger.getLogger().info(String.format("Start lift over with %d peaks",toTransfer.size()));
		
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
	//	System.out.println(toTransfer.size());
	//	System.out.println(mappedPeakList.size());
//		System.out.println(notMappedPeakList.size());
		if ( mappedPeakList.size()+notMappedPeakList.size() != toTransfer.size() ) {
			throw new RuntimeException("Number of mappings is strange. Can not create mapping.");
		}else{
			for(Peak peak : toTransfer){
				if (j < notMappedPeakList.size() && peak.equals(notMappedPeakList.get(j))){
					j++;
				}else{
					//from -> to
					ret.put(peak, mappedPeakList.get(k));
					k++;
				}
			}
		}
		
		peakFile.delete();
		outputMatched.delete();
		outputNotMatched.delete();
		
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
