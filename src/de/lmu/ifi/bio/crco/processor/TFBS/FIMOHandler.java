package de.lmu.ifi.bio.crco.processor.TFBS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.data.genome.Transcript;
import de.lmu.ifi.bio.crco.intervaltree.peaks.DNARegion;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.util.FileUtil;
import de.lmu.ifi.bio.crco.util.NetworkHierachy;
import de.lmu.ifi.bio.crco.util.CroCoLogger;


public class FIMOHandler extends TFBSHandler {
	private Pattern pattern = Pattern.compile(">(\\w+):(\\w+)\\s+(\\d+)-(\\d+)\\(([-+])\\)");
	private File regionFile = null;
	private Float pValueThreshold = null;
	private HashMap<String,Gene> geneLookup;
	private HashMap<String, String> motifIdMapping;
	
	public FIMOHandler(File regionFile, Float pValueThreshold, List<Gene> genes, HashMap<String, String> motifIdMapping){
		this.regionFile = regionFile;
		this.pValueThreshold = pValueThreshold; 
		this.motifIdMapping = motifIdMapping;
		geneLookup = new HashMap<String,Gene>();
		for(Gene gene : genes){
			geneLookup.put(gene.getIdentifier(), gene);
		}
		
	}
	
	private HashMap<Entity,DNARegion> readRegions() throws IOException{
		CroCoLogger.getLogger().info(String.format("Reading:\t%s" , regionFile));
		HashMap<Entity,DNARegion> regions = new HashMap<Entity,DNARegion>();
		BufferedReader br = new BufferedReader(new FileReader(regionFile));
		String line = null;
		while ( (line = br.readLine() )!= null) {
			Matcher matcher = pattern.matcher(line);
			if ( matcher.find() ) {
				Gene geneId = geneLookup.get(matcher.group(1));
				if ( geneId == null){
					CroCoLogger.getLogger().warn(String.format("Unknown gene id %s",matcher.group(1)));
					continue;
				}
				String chrom = matcher.group(2);
				
				Integer start = Integer.valueOf(matcher.group(3));
				Integer end = Integer.valueOf(matcher.group(4));
				
				
				if (! regions.containsKey(geneId)){
					regions.put(geneId, new DNARegion(chrom,geneId,start,end));
				}else{
					br.close();
					throw new RuntimeException(geneId + " already in");
				}
			}else{
				CroCoLogger.getLogger().fatal("Unknown:\t" + line);
			}
		}
		
		br.close();
		return regions;
	}
	@Override
	public HashMap<String,List<de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak>> readHits(File tfbsFile) throws IOException{
	
		
		HashMap<String,List<TFBSPeak>> tfbsPeaks = new HashMap<String,List<TFBSPeak>>();
		HashMap<Entity,DNARegion> dnaRegions = readRegions();
		int k = 0;
		BufferedReader br = new BufferedReader(new FileReader(tfbsFile));
		String line = br.readLine();
		System.out.println("Reading:\t" + tfbsFile);
		int skipped = 0;
		Set<String> skippedMotifs = new HashSet<String>();
		Set<String> processedMotifs = new HashSet<String>();
		int m = 0;
		while ( (line = br.readLine() )!= null) {
			String[] tokens = line.split("\t");
			if ( tokens.length < 7){
				System.err.println("strange line:\t" + line);
				continue;
			}
			k++;
			String motifId = tokens[0].toUpperCase();;
			
			String targetEnsemblId = tokens[1].toUpperCase();;
			Integer start = Integer.valueOf(tokens[2]);
			Integer end= Integer.valueOf(tokens[3]);
			Float pValue = Float.valueOf(tokens[6]);
			Float score = Float.valueOf(tokens[5]);
			
			if ( pValue >pValueThreshold) continue;
			m++;
			
			processedMotifs.add(motifId);
			
			if (! dnaRegions.containsKey(targetEnsemblId)){
				br.close();
				throw new RuntimeException(targetEnsemblId +" not found");
			}
			DNARegion region = dnaRegions.get(targetEnsemblId);
			Gene gene = region.getGene();
			
			Integer absolutStart= start+(int)region.getLow();
			Integer absolutEnd= end+(int)region.getLow();
			Integer absolutMiddle = (absolutStart+absolutEnd)/2;
			
			if (! motifIdMapping.containsKey(motifId)){
				CroCoLogger.getLogger().warn(String.format("Cannot find id mapping for %s",motifId));
				continue;
			}
			Entity mappedFactor = new Entity(this.motifIdMapping.get(motifId),motifId);
			
			if (!tfbsPeaks.containsKey(region.getChrom()) ){
				tfbsPeaks.put(region.getChrom(), new ArrayList<TFBSPeak>());
			}
			Transcript bestTranscript = null;
			Integer currentMinDistance = Integer.MAX_VALUE;
			for(Transcript transcript :gene.getTranscripts()){
				Integer distance = Math.abs(absolutMiddle-transcript.getTSSStrandCorredEnd());  // Peak to TSS start
				if ( bestTranscript == null || distance < currentMinDistance ){
					bestTranscript = transcript;
					currentMinDistance = distance;
				}
			}
			
			TFBSPeak peak = new TFBSPeak(region.getChrom(),mappedFactor,(Transcript)bestTranscript,currentMinDistance,pValue,absolutStart,absolutEnd);
			tfbsPeaks.get(region.getChrom()).add(peak);
		}
		br.close();
		CroCoLogger.getLogger().info("Number of mapped Motifs:\t" + processedMotifs.size() + "\tNumber of not mapped Motifs:\t" +skippedMotifs.size()  );
		CroCoLogger.getLogger().info("Number of TFBS predictions:\t" + k );
		CroCoLogger.getLogger().info("Number of TFBS predictions below < " +pValueThreshold + ":\t" + m );
		CroCoLogger.getLogger().info("Number of TBFS predictions below < " + pValueThreshold + " not mappable:\t"  + skipped );
		
		
		return tfbsPeaks;
		
	}
	public static void main(String[] args) throws Exception{
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("taxId").withDescription("Tax id").isRequired().hasArgs(1).create("taxId"));
		options.addOption(OptionBuilder.withLongOpt("tfbsFiles").withDescription("tfbsFiles").isRequired().hasArgs().create("tfbsFilss"));
		options.addOption(OptionBuilder.withLongOpt("tfbsRegion").withDescription("tfbsRegion").isRequired().hasArgs(1).create("tfbsRegion"));
		options.addOption(OptionBuilder.withLongOpt("pValueCutOf").withDescription("pValue cut-off (with promoter option)").isRequired().hasArgs(1).create("pValueCutOf"));
		options.addOption(OptionBuilder.withLongOpt("motifMappingFiles").withDescription("motifMappingFiles ").hasArgs().create("motifMappingFiles"));
		options.addOption(OptionBuilder.withLongOpt("repositoryDir").withDescription("Repository directory").isRequired().hasArgs().create("repositoryDir"));
		options.addOption(OptionBuilder.withLongOpt("compositeName").withDescription("Composite name").isRequired().hasArgs().create("compositeName"));
		options.addOption(OptionBuilder.withLongOpt("motifSetName").withDescription("Motif Set Name").isRequired().hasArgs(1).create("motifSetName"));
		options.addOption(OptionBuilder.withLongOpt("upstream").withDescription("Upstream region size").isRequired().hasArgs().create("upstream"));
		options.addOption(OptionBuilder.withLongOpt("downstream").withDescription("Downstream region size").isRequired().hasArgs(1).create("downstream"));
		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + FIMOHandler.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		Integer taxId = Integer.valueOf(line.getOptionValue("taxId"));
		Locale.setDefault(Locale.US);
		
		List<File> tfbsFiles = new ArrayList<File>();
		for(String file : line.getOptionValues("tfbsFiles") ){
			File tfbsFile = new File(file);
			
			if (! tfbsFile.exists()){
				CroCoLogger.getLogger().fatal(String.format("Can not find tbfs region  file %s",tfbsFile.getName()));
				System.exit(1);
			}
			
			tfbsFiles.add(tfbsFile);
		}
		File tfbsRegion = new File(line.getOptionValue("tfbsRegion"));
		Float pValueCutOf = Float.valueOf(line.getOptionValue("pValueCutOf"));
		
	
		String motifSetName = line.getOptionValue("motifSetName");
		File[] motifMappingFiles = new File[line.getOptionValues("motifMappingFiles").length];
		int i = 0;
		for(String file : line.getOptionValues("motifMappingFiles") ){
			File mappingFile = new File(file);
			if (! mappingFile.exists()){
				CroCoLogger.getLogger().fatal(String.format("Can not find motif mapping file %s",mappingFile.getName()));
				System.exit(1);
			}
			
			motifMappingFiles[i++]  = mappingFile;	
		}
		
		
		Integer downstream  = Integer.valueOf(line.getOptionValue("downstream"));
		Integer upstream = Integer.valueOf(line.getOptionValue("upstream"));
		
		File repositoryDir = new File(line.getOptionValue("repositoryDir"));
		if (! repositoryDir.isDirectory()){
			System.err.println(repositoryDir + " is not a directory");
			System.exit(1);
		}
		String composite = line.getOptionValue("compositeName");
		
		System.out.println("TFBS region file:\t" + tfbsRegion);
		System.out.println("TFBS files:\t" + tfbsFiles);
		System.out.println("Mapping files:\t" +Arrays.asList(motifMappingFiles) );
		System.out.println("TaxId:\t" + taxId);
		System.out.println("PValue:\t" + pValueCutOf);
		System.out.println("Composite name:\t"  +composite );
		System.out.println("Repository dir:\t" + repositoryDir);
		
		File outputDir = new File(repositoryDir + "/"   + composite);
		if ( outputDir.exists()){
			System.err.println(String.format("Composite %s already in repository %s",composite,repositoryDir.toString()));
		}else {
			if  ( !outputDir.mkdirs() ) {
				System.err.println(String.format("Cannnot create composite %s in repository %s",composite,repositoryDir.toString()));
				System.exit(1);
			}
		}
		
		HashMap<String, String> mapping = new FileUtil.MappingFileReader(0,2,motifMappingFiles).includeAllColumnsAfterToIndex(true).readN1MappingFile();
		
		//TODO:fix
		HashMap<String, List<TFBSPeak>> matchTree = new FIMOHandler(tfbsRegion,pValueCutOf,null, null).readHits(tfbsFiles);
		
		File baseFile =  new File(outputDir + "/" + motifSetName);
		
		File infoFile =  new File(baseFile + ".info");
		
		BufferedWriter bwInfo = new BufferedWriter(new FileWriter(infoFile));
		
		bwInfo.write(String.format("%s: %s\n",Option.NetworkName, motifSetName ));
		bwInfo.write(String.format("%s: %d\n",Option.TaxId.name(),taxId));
		bwInfo.write(String.format("%s: %s\n",Option.EdgeType,"Directed"));
		bwInfo.write(String.format("%s: %s\n",Option.NetworkType.name(), NetworkType.TFBS.name()));
		bwInfo.write(String.format("%s: %s\n",Option.MotifSet.name(),motifSetName.toString()));
		bwInfo.write(String.format("%s: %s\n",Option.ConfidenceThreshold.name(),pValueCutOf.toString()));
		bwInfo.write(String.format("%s: %s\n",Option.Upstream.name(), upstream + ""));
		bwInfo.write(String.format("%s: %s\n",Option.Downstream.name(), downstream +""));
	
		File annotationFile = new File(baseFile + ".annotation.gz");
		BufferedWriter bwAnnotation = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(annotationFile)) ));
		DirectedNetwork network = new DirectedNetwork(motifSetName,taxId,false);
		for(Entry<String, List<TFBSPeak>> e : matchTree.entrySet()){
			for(TFBSPeak peak : e.getValue()){
				
				Transcript target = (Transcript) peak.getTarget();
				network.add(peak.getFactor(),peak.getTarget());
				
				bwAnnotation.write(String.format("TBFS:\t%s\n",peak.toString()));
			}
			bwAnnotation.flush();
		}
		
		StringBuffer factorStr = new StringBuffer();
		for(Entity factor: network.getTargets()){
			factorStr.append(factor.getIdentifier() + " ");
		}
		bwInfo.write(String.format("%s: %s\n",Option.FactorList,factorStr.toString().trim()));
		bwInfo.flush();
		bwInfo.close();
		
		CroCoLogger.getLogger().info(String.format("%s network size: %d",motifSetName,network.getSize()));
		bwAnnotation.close();
		File networkFile = new File(baseFile + ".network.gz");
		NetworkHierachy.writeNetworkHierachyFile(network,networkFile);
	
	}
	
}
