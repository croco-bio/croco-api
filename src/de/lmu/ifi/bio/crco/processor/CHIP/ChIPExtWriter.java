package de.lmu.ifi.bio.crco.processor.CHIP;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.data.genome.Transcript;
import de.lmu.ifi.bio.crco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Promoter;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.FileUtil;
import de.lmu.ifi.bio.crco.util.GenomeUtil;
import de.lmu.ifi.bio.crco.util.Pair;

public class ChIPExtWriter {
	public static void main(String [] args) throws Exception{
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("gtf").withDescription("GTFFile").hasArgs(1).isRequired().create("gtfFile"));
		options.addOption(OptionBuilder.withLongOpt("chromosomNameMappings").withDescription("reference=synonym").hasArgs().create("chromosomNameMappings"));
		options.addOption(OptionBuilder.withLongOpt("chromosomNamePrefix").withDescription("chromosoms names prefix").hasArgs().create("chromosomNamePrefix"));
		options.addOption(OptionBuilder.withLongOpt("chromsomIndex").withDescription("chromsom index column index in PEAK files (default 0)").hasArgs(1).create("chromsomIndex"));
		options.addOption(OptionBuilder.withLongOpt("startIndex").withDescription("chromsom start column index in PEAK files (default 1)").hasArgs(1).create("startIndex"));
		options.addOption(OptionBuilder.withLongOpt("endIndex").withDescription("chromsom end column index in PEAK files (default 2)").hasArgs(1).create("endIndex"));
		options.addOption(OptionBuilder.withLongOpt("taxId").withDescription("TAX-ID").hasArgs(1).create("taxId"));
		options.addOption(OptionBuilder.withLongOpt("repositoryDir").withDescription("Repository directory").isRequired().hasArgs().create("repositoryDir"));
		options.addOption(OptionBuilder.withLongOpt("compositeName").withDescription("Composite name").isRequired().hasArgs().create("compositeName"));
		options.addOption(OptionBuilder.withLongOpt("experimentMappingFiles").withArgName("FILE(s)").withDescription("ChiP experiment description file(s)").isRequired().hasArgs().create("experimentMappingFiles"));
		options.addOption(OptionBuilder.withLongOpt("aggregateKey").withArgName("NAME").withDescription("e.g. cell, or development").isRequired().hasArgs().create("aggregateKey"));
		options.addOption(OptionBuilder.withLongOpt("upstream").withDescription("Upstream").isRequired().hasArgs(1).create("upstream"));
		options.addOption(OptionBuilder.withLongOpt("downstream").withDescription("Downstream ").isRequired().hasArgs().create("downstream"));
		options.addOption(OptionBuilder.withLongOpt("maxSize").withDescription("maxSize of a peak").hasArgs(1).create("maxSize"));
		options.addOption(OptionBuilder.withLongOpt("type").withDescription("Transcript type").hasArgs(1).create("type"));
		
		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + ChIPExtWriter.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		String type = null;
		if ( line.hasOption("type"))type =line.getOptionValue("type");
		File gtfFile = new File(line.getOptionValue("gtfFile"));
		if (! gtfFile.exists()){
			CroCoLogger.getLogger().fatal("GTF file does not exist (" + gtfFile  + ")");
			System.exit(1);
		}
		List<File> experimentMappingFiles = new ArrayList<File>(); // File(line.getOptionValue("experimentMappingFile"));
		for(String s : line.getOptionValues("experimentMappingFiles")){
			File experimentMappingFile = new File(s);
			if (!experimentMappingFile.exists() || !experimentMappingFile.isFile() ){
				CroCoLogger.getLogger().fatal(experimentMappingFile + " does not exist/ no file");
				System.exit(1);
			}	
			experimentMappingFiles.add(experimentMappingFile);
		}
		HashMap<String,String> chromosomNameMapping = new HashMap<String,String>();
		if ( line.hasOption("chromosomNameMappings")){
			for(String mapping : line.getOptionValues("chromosomNameMappings")){
				String[] tokens = mapping.split("=");
				chromosomNameMapping.put(tokens[0], tokens[1]);
			}
		}
		String chromosomNamePrefix =null;
		if ( line.hasOption("chromosomNamePrefix")){
			chromosomNamePrefix = line.getOptionValue("chromosomNamePrefix");
		}
		/*
		HashMap<String,String> geneIdMapping = null;
		if ( line.hasOption("tssMappingFile")){
			File tssMappingFile = new File(line.getOptionValue("tssMappingFile"));
			if ( !tssMappingFile.exists()){
				throw new RuntimeException("Can not find file:" + tssMappingFile);
			}
			geneIdMapping = FileUtil.readMappingFile(tssMappingFile, "\t", 0, 1, true,false);
		}
		*/
		File repositoryDir = new File(line.getOptionValue("repositoryDir"));
		if (! repositoryDir.isDirectory()){
			throw new RuntimeException(repositoryDir + " is not a directory");
		}
		String composite = line.getOptionValue("compositeName");
	
		Integer maxSize = null;
		if ( line.hasOption("maxSize"))
			maxSize = Integer.valueOf(line.getOptionValue("maxSize"));
		Integer taxId = Integer.valueOf(line.getOptionValue("taxId"));
		Integer upstream = Integer.valueOf(line.getOptionValue("upstream"));
		Integer downstream = Integer.valueOf(line.getOptionValue("downstream"));
		String aggregateKey = line.getOptionValue("aggregateKey");
		Integer chromIndex = 0;
		Integer startIndex=1;
		Integer endIndex=2;
		if ( line.hasOption("chromsomIndex")) chromIndex = Integer.valueOf(line.getOptionValue("chromsomIndex"));
		if ( line.hasOption("startIndex")) startIndex = Integer.valueOf(line.getOptionValue("startIndex"));
		if ( line.hasOption("endIndex")) endIndex = Integer.valueOf(line.getOptionValue("endIndex"));
		
		CroCoLogger.getLogger().info("Max size:\t" + maxSize);
		CroCoLogger.getLogger().info("TaxId:\t" + taxId);
		CroCoLogger.getLogger().info("GTF file:\t" + gtfFile);
		CroCoLogger.getLogger().info("Experiment description:\t" + experimentMappingFiles);
		CroCoLogger.getLogger().info("Chromosom mapping:\t" + chromosomNameMapping);
		CroCoLogger.getLogger().info("Chromosom name perfix:\t" + chromosomNamePrefix);
		CroCoLogger.getLogger().info("Upstream distance:\t" + upstream);
		CroCoLogger.getLogger().info("Downstream distance:\t" + downstream);
		CroCoLogger.getLogger().info("Composite name:\t"  +composite );
		CroCoLogger.getLogger().info("Repository dir:\t" + repositoryDir);
		CroCoLogger.getLogger().info("Chrom index:\t" + chromIndex);
		CroCoLogger.getLogger().info("Start index:\t" + startIndex);
		CroCoLogger.getLogger().info("End index:\t" + endIndex);
		CroCoLogger.getLogger().info("Type:\t" + type);
		
		File outputDir = new File(repositoryDir + "/"   + composite);
		if ( outputDir.exists()){
			CroCoLogger.getLogger().fatal(String.format("Composite %s already in repository %s",composite,repositoryDir.toString()));
			//System.exit(1);
		}
		if  ( !outputDir.mkdirs() ) {
			CroCoLogger.getLogger().fatal(String.format("Cannnot create composite %s in repository %s",composite,repositoryDir.toString()));
			//System.exit(1);
		}
		
		List<Gene> genes = FileUtil.getGenes(gtfFile, type, null);
		HashMap<String,IntervalTree<Promoter>> promoterTrees = GenomeUtil.createPromoterIntervalTree(genes,upstream,downstream,false);

		HashMap<String,List<HashMap<String, String>>> experiments = new HashMap<String,List<HashMap<String, String>>>();
		for(File experimentMappingFile : experimentMappingFiles ){
			List<HashMap<String, String>> lookup = FileUtil.fileLookUp(experimentMappingFile,"\t").getLookUp();
			
			for(HashMap<String, String> information : lookup){
				if (! information.containsKey(aggregateKey)){
					throw new RuntimeException(String.format("Key %s defined for aggregate function is missing:",aggregateKey));
				}
				
				if (! experiments.containsKey(information.get(aggregateKey))){
					experiments.put(information.get(aggregateKey), new ArrayList<HashMap<String,String>>());
				}
				if ( !information.containsKey("file") || !information.containsKey("antibody") || !information.containsKey("targetMapped")) 
					throw new RuntimeException(String.format("Experiment file %s not well formated. file, target,or targetMapped column is missing.",experimentMappingFile.toString()));
				
			
				String file = information.get("file");
				
				file = experimentMappingFile.getParent()+ "/" + file;
				if (! new File(file).exists()){
					throw new RuntimeException(String.format("Cannot find referenced file %s in %s",file,experimentMappingFile.toString()));
				}
				information.put("file", file);
				
				experiments.get(information.get(aggregateKey)).add(information);
			}
	
		}
		
	
		int k = 0;
		for(Entry<String,List<HashMap<String,String>>> aggregations : experiments.entrySet()){
			
			CroCoLogger.getLogger().info("Start Processing:\t" + aggregations.getKey());
			String aggreatedBaseDir=aggregations.getKey().replace("/", "").replace(":","");
			
			File aggreatedDir = new File(outputDir + "/" + aggreatedBaseDir);
			if ( aggreatedDir.exists()){
			//	throw new RuntimeException("Duplicate cell line:" + cellLineDir);
			}
			aggreatedDir.mkdir();
			
			/*
			File infoFile = new File(outputDir + "/" +  cellLineBaseDir+ ".info");
			
			BufferedWriter bwInfo = new BufferedWriter(new FileWriter(infoFile));
			//create cell line node
			
			bwInfo.write(String.format("Name: %s\n", ex.getKey() ));
			bwInfo.write(String.format("TaxId: %d\n",taxId));
			bwInfo.write("Directed: true\n");
			bwInfo.write(String.format("Type: %s\n", NetworkType.ChIP.name()));
			
			bwInfo.close();

			*/
			for(HashMap<String,String> expNet: aggregations.getValue()){
				CroCoLogger.getLogger().info("Processing:\t" + aggregations.getKey() + "\t" + expNet.get("antibody"));
				File file = new File(expNet.get("file"));
				String fileBaseName = file.getName().replace(".gz", "");
				
				String antibody = expNet.get("antibody");
				String targetMapped = expNet.get("targetMapped");
				
				File infoFile = new File(aggreatedDir + "/" + fileBaseName + ".info");
				if ( infoFile.exists()){
					//throw new RuntimeException("Duplicate experiment :" + infoFile );
				}
				
				BufferedWriter bwInfo = new BufferedWriter(new FileWriter(infoFile));
				
				bwInfo.write(String.format("%s: %s\n",Option.NetworkName.name(),antibody));
				bwInfo.write(String.format("%s: %d\n",Option.TaxId.name(),taxId));
				bwInfo.write(String.format("%s: %s\n",Option.EdgeType,"Directed"));
				bwInfo.write(String.format("%s: %s\n",Option.NetworkType.name(), NetworkType.ChIP.name()));
				bwInfo.write(String.format("%s: %d\n",Option.Upstream.name(), upstream));
				bwInfo.write(String.format("%s: %d\n",Option.Downstream.name(), downstream));
			//	bwInfo.write(String.format("%s: %s\n",Option.AntibodyTarget.name(), antibody));
			//	bwInfo.write(String.format("%s: %s\n",Option.AntibodyTargetMapped.name(), targetMapped));
				bwInfo.write(String.format("%s: %s\n",Option.FactorList.name(),targetMapped));
				
				
				for(Option option : Option.values()) {
					for(String alias : option.alias){
						if ( expNet.containsKey(alias) && !expNet.get(alias).toUpperCase().equals("NA")){
							bwInfo.write(String.format("%s: %s\n",option.name(),expNet.get(alias)));
							break;
						}
					}
				}
				bwInfo.flush();
				bwInfo.close();
				
				HashMap<String, IntervalTree<Peak>> peaks = GenomeUtil.createPeakIntervalTree(file,chromIndex,startIndex,endIndex,-1,maxSize); //max size == we want to ignore very long peaks
				File networkFile = new File(aggreatedDir + "/" +  fileBaseName+ ".network.gz");
				BufferedWriter bwNetwork = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(networkFile)) ));
				
				File annotationFile = new File(aggreatedDir + "/" +  fileBaseName + ".annotation.gz");
				BufferedWriter bwAnnotation = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(annotationFile)) ));
				
				HashMap<String, List<Pair<Peak, Promoter>>> targets = createEnrichedNetworkChipNetwork(peaks,promoterTrees, chromosomNamePrefix,chromosomNameMapping);
				
				for(Entry<String, List<Pair<Peak, Promoter>>> entry  : targets.entrySet()){
					String target = entry.getKey();
					
					bwNetwork.write( targetMapped.toUpperCase() + "\t" + target.toUpperCase() + "\n");
					
					for(Pair<Peak, Promoter> peakPromoter : entry.getValue()){
						Peak peak = peakPromoter.getFirst();
						Promoter promoter = peakPromoter.getSecond();
						bwAnnotation.write("ChiPBinding\t" +
								targetMapped.toUpperCase() + "\t" +target.toUpperCase() + "\t" +
								peak.getChrom() + "\t" + (int)peak.getLow() + "\t" + (int)peak.getHigh() + "\t"  + promoter.getStart() + "\t" + promoter.getEnd() + "\t" +  promoter.getTranscripts().iterator().next().getParentGene().getName() + "\n"
							);
					}

					if ( k++ % 10000 == 0){
						bwAnnotation.flush();
						bwNetwork.flush();
					}
				}
				bwNetwork.flush();
				bwNetwork.close();
				bwAnnotation.flush();
				bwAnnotation.close();
				
			
			}

		}
	}


	
	/**
	 * Maps ChIP bindings to genes via the transcripts
	 * @param chipBindings ChIP bindings IntervalTree
	 * @param promoterTrees Promoter IntervalTree
	 * @return a map of genes to ChIP bindings
	 */
	public static HashMap<String,List<Pair<Peak,Promoter>>> createEnrichedNetworkChipNetwork(HashMap<String, IntervalTree<Peak>> chipBindings, HashMap<String, IntervalTree<Promoter>> promoterTrees,String chromosomNamePrefix, HashMap<String,String> chromosomNameMapping) {
		HashMap<String,List<Pair<Peak,Promoter>>>  ret = new HashMap<String,List<Pair<Peak,Promoter>>> ();
		
		for(Entry<String,IntervalTree<Peak>> chipBinding : chipBindings.entrySet()){
			String chrom = chipBinding.getKey();
			
			IntervalTree<Promoter> chromPromoter = promoterTrees.get(chrom);
			if ( chromPromoter == null && chromosomNameMapping.containsKey(chrom)){
				chromPromoter = promoterTrees.get(chromosomNameMapping.get(chrom));
			}
			if (chromPromoter == null && chromosomNamePrefix!= null){
				chrom = chrom.replace(chromosomNamePrefix, "");
				chromPromoter = promoterTrees.get(chrom);
			}
			if (chromPromoter == null ){
				CroCoLogger.getLogger().warn(String.format("No promoter information for chrom: %s (mapping %s)", chrom, chromosomNameMapping));
				continue;
			} 
			Collection<Peak> chipBindingsForChrom = chipBinding.getValue().getObjects();
			for(Peak peak : chipBindingsForChrom){ //for each peak in chip experiment for current chrom
				if ( peak == null) continue;
				List<Promoter> promoters =  chromPromoter.searchAll(peak); 
				if ( promoters != null && promoters.size() > 0){ //in the case any overlaps with transcript promoters
					for(Promoter promoter : promoters){
						HashSet<String> geneId = new HashSet<String>(); 
						for(Transcript transcript : promoter.getTranscripts()){ //each promoter may overlaps with several transcripts and several genes
							geneId.add(transcript.getParentGene().getIdentifier());
						}
						for(String target : geneId){
							if (! ret.containsKey(target)){
								ret.put(target, new ArrayList<Pair<Peak,Promoter>>());
							}
							ret.get(target).add(new Pair<Peak,Promoter>(peak,promoter)); //add peak to gene
						}
					}
				}
			}
		}
	
		return ret;
		
	}
}
