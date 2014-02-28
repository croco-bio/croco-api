package de.lmu.ifi.bio.crco.processor.TFBS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;

import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Entity;
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
import de.lmu.ifi.bio.crco.util.ConsoleParameter;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.FileUtil;
import de.lmu.ifi.bio.crco.util.GenomeUtil;
import de.lmu.ifi.bio.crco.util.GenomeUtil.TFBSGeneEnrichment;


public class FIMOHandler extends TFBSHandler {
	private Pattern pattern = Pattern.compile(">(\\d+):(\\w+)\\s+(\\d+)-(\\d+).*");
	private File regionFile = null;
	private Float pValueThreshold = null;
	
	public FIMOHandler(File regionFile, Float pValueThreshold,Integer upstream,Integer downstream){
		this.regionFile = regionFile;
		this.pValueThreshold = pValueThreshold; 
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
		CroCoLogger.getLogger().info("Reading:\t" + tfbsFile);
		
		int lineCounter=0;
		int belowThreshold = 0;
		
		while ( (line = br.readLine() )!= null) {
			String[] tokens = line.split("\t");
			lineCounter++;
		//	if ( lineCounter > 1000000) break;
			if ( tokens.length < 7){
				br.close();
				throw new IOException("strange line:\t" + line);
			}
			String motifId = tokens[0].toUpperCase();;
			Integer regionId = Integer.valueOf(tokens[1]);
			
			DNARegion dnaRegion = dnaRegions.get(regionId);
			
			Integer start = Integer.valueOf(tokens[2]);
			Integer end= Integer.valueOf(tokens[3]);
			
			Integer absolutStart= start+(int)dnaRegion.getLow();
			Integer absolutEnd= end+(int)dnaRegion.getLow();
			Float pValue = Float.valueOf(tokens[6]);
			Float score = Float.valueOf(tokens[5]);
			
			if ( pValue >pValueThreshold) continue;
			
			belowThreshold++;
			
			if (!tfbsPeaks.containsKey(dnaRegion.getChrom()) ){
				tfbsPeaks.put(dnaRegion.getChrom(), new IntervalTree<TFBSPeak>());
			}
			TFBSPeak peak = new TFBSPeak(dnaRegion.getChrom(),absolutStart,absolutEnd,motifId,pValue,score);
			
			tfbsPeaks.get(dnaRegion.getChrom()).insert(peak);

		}
		
		br.close();
		
		CroCoLogger.getLogger().info("Number of TFBS predictions:\t" + lineCounter );
		CroCoLogger.getLogger().info("Number of TFBS predictions below < " +pValueThreshold + ":\t" + belowThreshold );
	
		
		return tfbsPeaks;
		
	}
	public static void main(String[] args) throws Exception{
		Locale.setDefault(Locale.US);
		ConsoleParameter parameter = new ConsoleParameter();
		
		parameter.register(ConsoleParameter.taxId);
		parameter.register(ConsoleParameter.tfbsFiles);
		parameter.register(ConsoleParameter.tfbsRegion);
		parameter.register(ConsoleParameter.pValueCutOf);
		parameter.register(ConsoleParameter.motifMappingFiles);
		parameter.register(ConsoleParameter.repositoryDir);
		parameter.register(ConsoleParameter.compositeName);
		parameter.register(ConsoleParameter.motifSetName);
		parameter.register(ConsoleParameter.upstream);
		parameter.register(ConsoleParameter.downstream);
		parameter.register(ConsoleParameter.gtf);

		CommandLine cmdLine = parameter.parseCommandLine(args, FIMOHandler.class);
		
		parameter.printInfo();
		
		if (! ConsoleParameter.repositoryDir.getValue(cmdLine).isDirectory()){
			CroCoLogger.getLogger().fatal(ConsoleParameter.repositoryDir.getValue(cmdLine) + " is not a directory");
			System.exit(1);
		}
		
		File outputDir = new File(ConsoleParameter.repositoryDir.getValue(cmdLine) + "/"   + ConsoleParameter.compositeName.getValue(cmdLine) + "/" +ConsoleParameter.pValueCutOf.getValue(cmdLine)  +"/" );
		if ( outputDir.exists()){
			CroCoLogger.getLogger().warn(String.format("Composite %s already in repository %s",ConsoleParameter.compositeName.getValue(cmdLine),ConsoleParameter.repositoryDir.getValue(cmdLine).toString()));
		}else {
			try{
				 Files.createDirectories(outputDir.toPath());
			}catch(IOException ex){
				CroCoLogger.getLogger().fatal(String.format("Cannnot create composite %s in repository %s",ConsoleParameter.compositeName.getValue(cmdLine),ConsoleParameter.repositoryDir.getValue(cmdLine).toString()));
				ex.printStackTrace();
				System.exit(1);
			}
		}
		
		List<Gene> genes = FileUtil.getGenes(ConsoleParameter.gtf.getValue(cmdLine), "protein_coding", null);
		HashMap<String, IntervalTree<Promoter>> promoterTree = GenomeUtil.createPromoterIntervalTree(genes,ConsoleParameter.upstream.getValue(cmdLine),ConsoleParameter.downstream.getValue(cmdLine),true);
		
		
		HashMap<String, Set<String>> mapping = new FileUtil.MappingFileReader(0,2,ConsoleParameter.motifMappingFiles.getValue(cmdLine)).includeAllColumnsAfterToIndex(true).setColumnSeperator("\\s+").readNNMappingFile();
		
		
		HashMap<String, IntervalTree<TFBSPeak>> matchTree = new FIMOHandler(ConsoleParameter.tfbsRegion.getValue(cmdLine),ConsoleParameter.pValueCutOf.getValue(cmdLine),5000,5000).readHits(ConsoleParameter.tfbsFiles.getValue(cmdLine));
		
		File baseFile =  new File(outputDir + "/" + ConsoleParameter.motifSetName.getValue(cmdLine));
		
		File infoFile =  new File(baseFile + ".info");
		
		BufferedWriter bwInfo = new BufferedWriter(new FileWriter(infoFile));

		bwInfo.write(String.format("%s: %s\n",Option.NetworkName, ConsoleParameter.motifSetName.getValue(cmdLine) ));
		bwInfo.write(String.format("%s: %d\n",Option.TaxId.name(),ConsoleParameter.taxId.getValue(cmdLine)));
		bwInfo.write(String.format("%s: %s\n",Option.EdgeType,"Directed"));
		bwInfo.write(String.format("%s: %s\n",Option.NetworkType.name(), NetworkType.TFBS.name()));
		bwInfo.write(String.format("%s: %s\n",Option.MotifSet.name(),ConsoleParameter.motifSetName.getValue(cmdLine) ));
		bwInfo.write(String.format("%s: %s\n",Option.ConfidenceThreshold.name(),ConsoleParameter.pValueCutOf.getValue(cmdLine) + ""));
		bwInfo.write(String.format("%s: %s\n",Option.Upstream.name(), ConsoleParameter.upstream.getValue(cmdLine) + ""));
		bwInfo.write(String.format("%s: %s\n",Option.Downstream.name(), ConsoleParameter.downstream.getValue(cmdLine) +""));
		
		File annotationFile = new File(baseFile + ".annotation.gz");
		BufferedWriter bwAnnotation = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(annotationFile)) ));
		DirectedNetwork network = new DirectedNetwork(ConsoleParameter.motifSetName.getValue(cmdLine),ConsoleParameter.taxId.getValue(cmdLine),false);
		
		int skipped=0;
		int processed = 0;
		Set<String> skippedMotifs = new HashSet<String>();
		Set<String> processedMotifs = new HashSet<String>();
		
		for(Entry<String, IntervalTree<TFBSPeak>> e : matchTree.entrySet()){
			for(TFBSPeak peak : e.getValue().getObjects()){
		
				if (peak == null) continue;
				if ( !mapping.containsKey(peak.getMotifId())){
					skippedMotifs.add(peak.getMotifId());
					skipped++;
					continue;
				}
				processed++;
				processedMotifs.add(peak.getMotifId());
				Set<String> factorIds = mapping.get(peak.getMotifId());
				
				
				if (! promoterTree.containsKey(peak.getChrom())){
					CroCoLogger.getLogger().warn("Unknown chrom:" + peak.getChrom());
					continue;
				}
				List<Promoter> promoters = promoterTree.get(peak.getChrom()).searchAll(peak);
				Integer absolutMiddle = (peak.getStart()+peak.getEnd())/2;
				List<TFBSGeneEnrichment> geneAssoications = GenomeUtil.enrich(promoters, absolutMiddle,ConsoleParameter.upstream.getValue(cmdLine),ConsoleParameter.downstream.getValue(cmdLine));
				
				for(TFBSGeneEnrichment geneAssoication :geneAssoications ){
					
					
					for(String factorId : factorIds){
						network.add(new Entity(factorId),geneAssoication.gene);
						
						String tfbs =  String.format("%s\t%s\t%s\t%s\t%s\t%f\t%.7f\t%s\t%d\t%d",
								factorId,geneAssoication.gene.getIdentifier(),peak.getMotifId(),
								geneAssoication.closestTranscriptUpstream==null?"NaN":Transcript.getDistanceToTssStart(geneAssoication.closestTranscriptUpstream, absolutMiddle),
								geneAssoication.closestTranscriptDownstream==null?"NaN":Transcript.getDistanceToTssStart(geneAssoication.closestTranscriptDownstream, absolutMiddle),
								peak.getScore(),peak.getpValue(),peak.getChrom(),peak.getStart(),peak.getEnd() );
						
						bwAnnotation.write(String.format("TFBS\t%s\n",tfbs));
					}
				}
				
				
				

			}
			bwAnnotation.flush();
		}
		CroCoLogger.getLogger().info("Number of mapped Motifs:\t" + processedMotifs.size() + "\tNumber of not mapped Motifs:\t" +skippedMotifs.size()  );
		CroCoLogger.getLogger().info("Not mapped TFBS:" + skipped);
		CroCoLogger.getLogger().info("Processed:" + processed);
		
		StringBuffer factorStr = new StringBuffer();
		for(Entity factor: network.getFactors()){
			factorStr.append(factor.getIdentifier() + " ");
		}
		for(String s : skippedMotifs){
			CroCoLogger.getLogger().debug(String.format("Skipped: %s",s));
		}
		
		bwInfo.write(String.format("%s: %s\n",Option.FactorList,factorStr.toString().trim()));
		bwInfo.flush();
		bwInfo.close();
		
		CroCoLogger.getLogger().info(String.format("%s network size: %d",ConsoleParameter.motifSetName.getValue(cmdLine),network.getSize()));
		bwAnnotation.close();
		File networkFile = new File(baseFile + ".network.gz");
		NetworkHierachy.writeNetworkHierachyFile(network,networkFile);
	
	}
	
}
