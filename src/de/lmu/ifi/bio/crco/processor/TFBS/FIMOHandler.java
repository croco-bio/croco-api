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
import de.lmu.ifi.bio.crco.data.genome.Strand;
import de.lmu.ifi.bio.crco.data.genome.Transcript;
import de.lmu.ifi.bio.crco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.crco.intervaltree.peaks.DNARegion;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Promoter;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.processor.hierachy.NetworkHierachy;
import de.lmu.ifi.bio.crco.util.FileUtil;
import de.lmu.ifi.bio.crco.util.GenomeUtil;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.StringUtil;


public class FIMOHandler extends TFBSHandler {
	private Pattern pattern = Pattern.compile(">(\\d+):(\\w+)\\s+(\\d+)-(\\d+).*");
	private File regionFile = null;
	private Float pValueThreshold = null;
	private HashMap<String, Set<String>> motifIdMapping;
	private HashMap<String, IntervalTree<Promoter>> trees;
	private Integer upstream = null;
	private Integer downstream = null;
	
	public FIMOHandler(File regionFile, Float pValueThreshold, List<Gene> genes, HashMap<String, Set<String>> motifIdMapping,Integer upstream,Integer downstream){
		this(regionFile,pValueThreshold,motifIdMapping,downstream,upstream,GenomeUtil.createPromoterIntervalTree(genes,upstream,downstream,true));
	}
	private FIMOHandler(File regionFile, Float pValueThreshold, HashMap<String, Set<String>> motifIdMapping,Integer upstream, Integer downstream, HashMap<String, IntervalTree<Promoter>> chromPromoterIntervalTree){
		this.regionFile = regionFile;
		this.pValueThreshold = pValueThreshold; 
		this.motifIdMapping = motifIdMapping;
		this.trees =chromPromoterIntervalTree;
		this.upstream = upstream;
		this.downstream= downstream;
	}

	
	private HashMap<Integer,DNARegion> readRegions() throws IOException{
		CroCoLogger.getLogger().info(String.format("Reading:\t%s" , regionFile));
		HashMap<Integer,DNARegion> regions = new HashMap<Integer,DNARegion>();
		BufferedReader br = new BufferedReader(new FileReader(regionFile));
		String line = null;
		while ( (line = br.readLine() )!= null) {
			Matcher matcher = pattern.matcher(line);
			if ( matcher.find() ) {
				Integer id = Integer.valueOf(matcher.group(1));
				String chrom = matcher.group(2);
				
				Integer start = Integer.valueOf(matcher.group(3));
				Integer end = Integer.valueOf(matcher.group(4));
				
				DNARegion dnaRegion = new DNARegion(chrom,start,end);
				regions.put(id, dnaRegion);
			}else{
				CroCoLogger.getLogger().warn("Skip region:\t" + line);
			}
		}
		
		br.close();
		return regions;
	}
	@Override
	public HashMap<String,IntervalTree<TFBSPeak>> readHits(File tfbsFile) throws IOException{
	
		HashMap<String,IntervalTree<TFBSPeak>> tfbsPeaks = new HashMap<String,IntervalTree<TFBSPeak>>();
		HashMap<Integer,DNARegion> dnaRegions = readRegions();
		BufferedReader br = new BufferedReader(new FileReader(tfbsFile));
		String line = br.readLine();
		System.out.println("Reading:\t" + tfbsFile);
		Set<String> skippedMotifs = new HashSet<String>();
		Set<String> processedMotifs = new HashSet<String>();
		
		int lineCounter=0;
		int belowThreshold = 0;
		int skipped=0;
		
		while ( (line = br.readLine() )!= null) {
			String[] tokens = line.split("\t");
			
			if ( tokens.length < 7){
				br.close();
				throw new IOException("strange line:\t" + line);
			}
			if ( lineCounter++ % 200000 == 0){
			//	System.err.print(".");
			}
			String motifId = tokens[0].toUpperCase();;
			Integer regionId = Integer.valueOf(tokens[1]);
			
			DNARegion dnaRegion = dnaRegions.get(regionId);
			
			Integer start = Integer.valueOf(tokens[2]);
			Integer end= Integer.valueOf(tokens[3]);
		//	Strand strand = Strand.valueOf(tokens[4]);
			
			Integer absolutStart= start+(int)dnaRegion.getLow();
			Integer absolutEnd= end+(int)dnaRegion.getLow();
			Integer absolutMiddle = (absolutStart+absolutEnd)/2;
			
			Float pValue = Float.valueOf(tokens[6]);
			Float score = Float.valueOf(tokens[5]);
			
			if ( pValue >pValueThreshold) continue;
			if ( !trees.containsKey(dnaRegion.getChrom()))continue;
			
			belowThreshold++;
			
			if (! motifIdMapping.containsKey(motifId)) {
				skippedMotifs.add(motifId);
				skipped++;
				continue;
			}
			processedMotifs.add(motifId);
			List<Entity> factors = new ArrayList<Entity>();
			for(String mapped : motifIdMapping.get(motifId)){
				Entity mappedFactor = new Entity(mapped,motifId);
				factors.add(mappedFactor);
			}
			List<Promoter> promoters = trees.get(dnaRegion.getChrom()).searchAll(dnaRegion);
			
			if ( promoters.size() > 0){
				if (!tfbsPeaks.containsKey(dnaRegion.getChrom()) ){
					tfbsPeaks.put(dnaRegion.getChrom(), new IntervalTree<TFBSPeak>());
				}
				for(Promoter promoter :promoters ){
					for(Transcript transcript : promoter.getTranscripts()){
						Gene gene = transcript.getParentGene();
						Integer distanceToTss=null;
						if (gene.getStrand().equals(Strand.PLUS) ){
							distanceToTss = absolutMiddle-transcript.getTSSStrandCorredStart();
						}else{
							distanceToTss = transcript.getTSSStrandCorredEnd()-absolutMiddle;
						}
						if ( distanceToTss < -upstream||  distanceToTss > downstream){
						//	CroCoLogger.getLogger().debug("skip " +distanceToTss );
							continue;
						}
						
						TFBSPeak peak = new TFBSPeak(dnaRegion.getChrom(),factors,motifId,transcript,distanceToTss,pValue,score,absolutStart,absolutEnd);
						tfbsPeaks.get(dnaRegion.getChrom()).insert(peak);
					}
				}
			}
		}
		System.err.println();
		br.close();
		CroCoLogger.getLogger().info("Number of mapped Motifs:\t" + processedMotifs.size() + "\tNumber of not mapped Motifs:\t" +skippedMotifs.size()  );
		CroCoLogger.getLogger().info("Number of TFBS predictions:\t" + lineCounter );
		CroCoLogger.getLogger().info("Number of TFBS predictions below < " +pValueThreshold + ":\t" + belowThreshold );
		CroCoLogger.getLogger().info("Number of TBFS predictions below < " + pValueThreshold + " not mappable:\t"  + skipped );
		for(String s : skippedMotifs){
			CroCoLogger.getLogger().debug(String.format("Skipped: %s",s));
		}
		
		return tfbsPeaks;
		
	}
	public static void main(String[] args) throws Exception{
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("taxId").withDescription("Tax id").isRequired().hasArgs(1).create("taxId"));
		options.addOption(OptionBuilder.withLongOpt("tfbsFiles").withDescription("tfbsFiles").isRequired().hasArgs().create("tfbsFiles"));
		options.addOption(OptionBuilder.withLongOpt("tfbsRegion").withDescription("tfbsRegion").isRequired().hasArgs(1).create("tfbsRegion"));
		options.addOption(OptionBuilder.withLongOpt("pValueCutOf").withDescription("pValue cut-off (with promoter option)").isRequired().hasArgs(1).create("pValueCutOf"));
		options.addOption(OptionBuilder.withLongOpt("motifMappingFiles").withDescription("motifMappingFiles ").hasArgs().create("motifMappingFiles"));
		options.addOption(OptionBuilder.withLongOpt("repositoryDir").withDescription("Repository directory").isRequired().hasArgs().create("repositoryDir"));
		options.addOption(OptionBuilder.withLongOpt("compositeName").withDescription("Composite name").isRequired().hasArgs().create("compositeName"));
		options.addOption(OptionBuilder.withLongOpt("motifSetName").withDescription("Motif Set Name").isRequired().hasArgs(1).create("motifSetName"));
		options.addOption(OptionBuilder.withLongOpt("upstream").withDescription("Upstream region size").isRequired().hasArgs().create("upstream"));
		options.addOption(OptionBuilder.withLongOpt("downstream").withDescription("Downstream region size").isRequired().hasArgs(1).create("downstream"));
		options.addOption(OptionBuilder.withLongOpt("gtf").withDescription("GTFFile").hasArgs(1).isRequired().create("gtfFile"));

		
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
		File gtfFile = new File(line.getOptionValue("gtf"));
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
		System.out.println("GTF file:\t" + gtfFile.toString());
		
		File outputDir = new File(repositoryDir + "/"   + composite + "/" +pValueCutOf  +"/" );
		if ( outputDir.exists()){
			System.err.println(String.format("Composite %s already in repository %s",composite,repositoryDir.toString()));
		}else {
			if  ( !outputDir.mkdirs() ) {
				System.err.println(String.format("Cannnot create composite %s in repository %s",composite,repositoryDir.toString()));
				System.exit(1);
			}
		}
		
		List<Gene> genes = FileUtil.getGenes(gtfFile, "protein_coding", null);
		HashMap<String, Set<String>> mapping = new FileUtil.MappingFileReader(0,2,motifMappingFiles).includeAllColumnsAfterToIndex(true).setColumnSeperator("\\s+").readNNMappingFile();
		
		HashMap<String, IntervalTree<TFBSPeak>> matchTree = new FIMOHandler(tfbsRegion,pValueCutOf,genes, mapping,5000,5000).readHits(tfbsFiles);
		
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
		for(Entry<String, IntervalTree<TFBSPeak>> e : matchTree.entrySet()){
			for(TFBSPeak peak : e.getValue().getObjects()){
				if (peak == null) continue;
	//			Transcript target = (Transcript) peak.getTarget();
				for(Entity factor : peak.getFactors()){
					network.add(factor,new Entity(peak.getTarget().getParentGene().getIdentifier()));
				}
				String tfbs =  String.format("%s\t%s\t%s\t%s\t%d\t%f\t%.7f\t%s\t%d\t%d",
						StringUtil.getAsString(peak.getFactors(), ','),  peak.getTarget().getParentGene().getIdentifier(),peak.getTarget().getIdentifier() ,peak.getMotifId(),
						 peak.getDistanceToTranscript(),peak.getScore(),peak.getpValue(),peak.getChrom(),peak.getStart(),peak.getEnd() );
				
				bwAnnotation.write(String.format("TBFS\t%s\n",tfbs));
			}
			bwAnnotation.flush();
		}
		
		StringBuffer factorStr = new StringBuffer();
		for(Entity factor: network.getFactors()){
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
