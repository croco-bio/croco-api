package de.lmu.ifi.bio.croco.processor.CHIP;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;

import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.NetworkType;
import de.lmu.ifi.bio.croco.data.Option;
import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.data.genome.Transcript;
import de.lmu.ifi.bio.croco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Promoter;
import de.lmu.ifi.bio.croco.network.DirectedNetwork;
import de.lmu.ifi.bio.croco.network.Network;
import de.lmu.ifi.bio.croco.network.Network.EdgeRepositoryStrategy;
import de.lmu.ifi.bio.croco.util.ConsoleParameter;
import de.lmu.ifi.bio.croco.util.ConsoleParameter.CroCoOption;
import de.lmu.ifi.bio.croco.util.ConsoleParameter.IntegerValueHandler;
import de.lmu.ifi.bio.croco.util.ConsoleParameter.StringValueHandler;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.FileUtil;
import de.lmu.ifi.bio.croco.util.GenomeUtil;
import de.lmu.ifi.bio.croco.util.GenomeUtil.TFBSGeneEnrichment;

public class ChIPExtWriter {
	private static CroCoOption<Integer> chromsomIndex = new CroCoOption<Integer>("chromsomIndex",new IntegerValueHandler()).setDescription("chromsom index column index in PEAK files (default 0)").setArgs(1).setDefault(0);
	private static CroCoOption<Integer> startIndex = new CroCoOption<Integer>("startIndex",new IntegerValueHandler()).setArgs(1).setDescription("chromsom start column index in PEAK files (default 1)").setDefault(1);
	private static CroCoOption<Integer> endIndex = new CroCoOption<Integer>("endIndex",new IntegerValueHandler()).setArgs(1).setDescription("chromsom end column index in PEAK files (default 2)").setDefault(2);
	private static CroCoOption<Integer> maxSize = new CroCoOption<Integer>("maxSize",new IntegerValueHandler()).setArgs(1).setDescription("max size in bp of a ChIP-seq peak  (default 2000  base pairs)").setDefault(2000);
	private static CroCoOption<String> aggregateKey = new CroCoOption<String>("aggregateKey",new StringValueHandler()).setArgs(1).setDescription("e.g. cell, or development (default (cell)").setDefault("cell");
	
	public static void main(String [] args) throws Exception{
		Locale.setDefault(Locale.US);
		ConsoleParameter parameter = new ConsoleParameter();
		parameter.register(
				ConsoleParameter.experimentMappingFiles,
				ConsoleParameter.taxId,
				ConsoleParameter.repositoryDir,
				ConsoleParameter.gtf,
				ConsoleParameter.chromosomNamePrefix,
				ConsoleParameter.chromosomNameMappings,
				ConsoleParameter.compositeName,
				ConsoleParameter.upstream,
				ConsoleParameter.downstream,
				chromsomIndex,
				startIndex,
				endIndex,
				maxSize,
				aggregateKey
		);
		
		CommandLine cmdLine = parameter.parseCommandLine(args,ChIPExtWriter.class);
		
		Integer taxId = ConsoleParameter.taxId.getValue(cmdLine);
		String chromosomNamePrefix = ConsoleParameter.chromosomNamePrefix.getValue(cmdLine);
		HashMap<String, String> chromosomNameMapping = ConsoleParameter.chromosomNameMappings.getValue(cmdLine);
		File gtfFile = ConsoleParameter.gtf.getValue(cmdLine);
		File repositoryDir = ConsoleParameter.repositoryDir.getValue(cmdLine);
		String composite = ConsoleParameter.compositeName.getValue(cmdLine);
		List<File> experimentMappingFiles = ConsoleParameter.experimentMappingFiles.getValue(cmdLine);
		Integer downstream = ConsoleParameter.downstream.getValue(cmdLine);
		Integer upstream = ConsoleParameter.upstream.getValue(cmdLine);
		Integer chromIndex = ChIPExtWriter.chromsomIndex.getValue(cmdLine);
		Integer startIndex=ChIPExtWriter.startIndex.getValue(cmdLine);
		Integer endIndex=ChIPExtWriter.endIndex.getValue(cmdLine);
		Integer maxSize = ChIPExtWriter.maxSize.getValue(cmdLine);
		String aggregateKey = ChIPExtWriter.aggregateKey.getValue(cmdLine);
		
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
		
		File outputDir = new File(repositoryDir + "/"   + composite);
		if ( outputDir.exists()){
			CroCoLogger.getLogger().warn(String.format("Composite %s already in repository %s",composite,repositoryDir.toString()));
		}else{
			if  ( !outputDir.mkdirs() ) {
				CroCoLogger.getLogger().warn(String.format("Cannnot create composite %s in repository %s",composite,repositoryDir.toString()));
			}
		}
		
		List<Gene> genes = FileUtil.getGenes(gtfFile, "protein_coding", null);
		HashMap<String,IntervalTree<Promoter>> promoterTrees = GenomeUtil.createPromoterIntervalTree(genes,upstream,downstream,true);

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
				bwInfo.write(String.format("%s: %s\n",Option.ENCODEName.name(),file.getName()));
				
				
				for(Option option : Option.values()) {
					for(String alias : option.alias){
						if ( expNet.containsKey(alias) && !expNet.get(alias).toUpperCase().equals("NA")){
							bwInfo.write(String.format("%s: %s\n",option.name(),expNet.get(alias)));
							break;
						}
					}
				}
		
				HashMap<String, IntervalTree<Peak>> chipBindings = GenomeUtil.createPeakIntervalTree(file,chromIndex,startIndex,endIndex,7,maxSize); //max size == we want to ignore very long peaks
				bwInfo.write(String.format("%s: %s\n",Option.numberOfPeak.name(),GenomeUtil.countPeaks(chipBindings)));
				bwInfo.flush();
				bwInfo.close();
				File networkFile = new File(aggreatedDir + "/" +  fileBaseName+ ".network.gz");
				
				File annotationFile = new File(aggreatedDir + "/" +  fileBaseName + ".annotation.gz");
				BufferedWriter bwAnnotation = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(annotationFile)) ));
				//new Entity(targetMapped);
				
				//List<TFBSPeak> targets = getTFBSPeaks(peaks,promoterTrees, chromosomNamePrefix,chromosomNameMapping);
				
				DirectedNetwork network = new DirectedNetwork(aggregations.getKey(),taxId,EdgeRepositoryStrategy.LOCAL);

				for(Entry<String, IntervalTree<Peak>> chipBinding  : chipBindings.entrySet()){
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
					
					Collection<Peak> peakList = chipBinding.getValue().getObjects();
					for(Peak peak : peakList){
						if ( peak == null) continue;
						
						Integer absolutMiddle = (peak.getStart()+peak.getEnd())/2;
						List<Promoter> promoters =  chromPromoter.searchAll(peak); 
						
						List<TFBSGeneEnrichment> enrichedGenes = GenomeUtil.enrich(promoters, absolutMiddle, upstream, downstream);
						
						for(TFBSGeneEnrichment  geneAssoication : enrichedGenes){
								
							network.add(new Entity(targetMapped),geneAssoication.gene );
							
							String tfbs =  String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%d\t%d",
									targetMapped,geneAssoication.gene.getIdentifier(),antibody,
									geneAssoication.closestTranscriptUpstream==null?"NaN":Transcript.getDistanceToTssStart(geneAssoication.closestTranscriptUpstream, absolutMiddle),
									geneAssoication.closestTranscriptDownstream==null?"NaN":Transcript.getDistanceToTssStart(geneAssoication.closestTranscriptDownstream, absolutMiddle),
									peak.getScore()==null?"NaN":peak.getScore(),peak.getChrom(),peak.getStart(),peak.getEnd() );
							
							bwAnnotation.write(String.format("CHIP\t%s\n",tfbs));
							
							
						}
												
					}
					
					if ( k++ % 10000 == 0){
						bwAnnotation.flush();
						
					}
				}
				Network.writeNetwork(network, networkFile);
				bwAnnotation.flush();
				bwAnnotation.close();
				
			
			}

		}
	}

}
