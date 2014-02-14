package de.lmu.ifi.bio.crco.processor.OpenChrom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;

import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.data.genome.Transcript;
import de.lmu.ifi.bio.crco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Promoter;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.processor.TFBS.FIMOHandler;
import de.lmu.ifi.bio.crco.processor.hierachy.NetworkHierachy;
import de.lmu.ifi.bio.crco.util.ConsoleParameter;
import de.lmu.ifi.bio.crco.util.ConsoleParameter.CroCoOption;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.FileUtil;
import de.lmu.ifi.bio.crco.util.GenomeUtil;
import de.lmu.ifi.bio.crco.util.GenomeUtil.TFBSGeneEnrichment;

public class DNaseTFBSExtWriter {
	private static CroCoOption<String> openChromExpType_Parameter = new CroCoOption<String>("openChromExpType",new ConsoleParameter.StringValueHandler()).isRequired().setArgs(1);
	
	public static void main(String[] args) throws Exception{
		Locale.setDefault(Locale.US);
		ConsoleParameter parameter = new ConsoleParameter();
		parameter.register(
				openChromExpType_Parameter,
				ConsoleParameter.experimentMappingFile,
				ConsoleParameter.taxId,
				ConsoleParameter.tfbsFiles,
				ConsoleParameter.tfbsRegion,
				ConsoleParameter.pValueCutOf,
				ConsoleParameter.motifMappingFiles,
				ConsoleParameter.repositoryDir,
				ConsoleParameter.compositeName,
				ConsoleParameter.motifSetName,
				ConsoleParameter.gtf,
				ConsoleParameter.chromosomNamePrefix,
				ConsoleParameter.downstream,
				ConsoleParameter.upstream,
				ConsoleParameter.test
		);
		
		CommandLine cmdLine = parameter.parseCommandLine(args,DNaseTFBSExtWriter.class);
		
		String openChromExpType = openChromExpType_Parameter.getValue(cmdLine);
		Integer taxId = ConsoleParameter.taxId.getValue(cmdLine);
		List<File> tfbsFiles = ConsoleParameter.tfbsFiles.getValue(cmdLine);
		File tfbsRegion = ConsoleParameter.tfbsRegion.getValue(cmdLine);
		String chromosomNamePrefix = ConsoleParameter.chromosomNamePrefix.getValue(cmdLine);
		File gtfFile = ConsoleParameter.gtf.getValue(cmdLine);
		Float pValueCutOf =  ConsoleParameter.pValueCutOf.getValue(cmdLine);
		List<File> motifMappingFiles = ConsoleParameter.motifMappingFiles.getValue(cmdLine);
		File repositoryDir = ConsoleParameter.repositoryDir.getValue(cmdLine);
		String composite = ConsoleParameter.compositeName.getValue(cmdLine);
		String motifSetName = ConsoleParameter.motifSetName.getValue(cmdLine);
		File dnaseExperimentDataFile = ConsoleParameter.experimentMappingFile.getValue(cmdLine);
		Integer downstream = ConsoleParameter.downstream.getValue(cmdLine);
		Integer upstream = ConsoleParameter.upstream.getValue(cmdLine);
		Boolean test = ConsoleParameter.test.getValue(cmdLine);
		
		CroCoLogger.getLogger().info("Experimental mapping file:\t" +dnaseExperimentDataFile );
		CroCoLogger.getLogger().info("GTF file:\t" + gtfFile);
		CroCoLogger.getLogger().info("TFBS file:\t" + tfbsFiles);
		CroCoLogger.getLogger().info("Mapping file:\t" +motifMappingFiles );
		CroCoLogger.getLogger().info("TaxId:\t" + taxId);
		CroCoLogger.getLogger().info("PValue:\t" + pValueCutOf);
		CroCoLogger.getLogger().info("Composite name:\t"  +composite );
		CroCoLogger.getLogger().info("Repository dir:\t" + repositoryDir);
		CroCoLogger.getLogger().info("Chromosom name perfix:\t" + chromosomNamePrefix);
		CroCoLogger.getLogger().info("Upstream:\t" + upstream);
		CroCoLogger.getLogger().info("Downstream:\t" + downstream);
		CroCoLogger.getLogger().info("Test:\t" + test);
		
		
		File outputDir = new File(repositoryDir + "/"   + composite);
		if ( outputDir.exists()){
			CroCoLogger.getLogger().warn(String.format("Composite %s already in repository %s",composite,repositoryDir.toString()));
		}else{
			if  ( !outputDir.mkdirs() ) {
				CroCoLogger.getLogger().fatal(String.format("Cannnot create composite %s in repository %s",composite,repositoryDir.toString()));
				System.exit(1);
			}
		}
		HashMap<String, Set<String>> motifIdMapping = new FileUtil.MappingFileReader(0,2,motifMappingFiles).setColumnSeperator("\\s+").includeAllColumnsAfterToIndex(true).readNNMappingFile();
		List<Gene> genes = FileUtil.getGenes(gtfFile, "protein_coding", null);
		HashMap<String, IntervalTree<Promoter>> promoterTree = GenomeUtil.createPromoterIntervalTree(genes,ConsoleParameter.upstream.getValue(cmdLine),ConsoleParameter.downstream.getValue(cmdLine),true);
		
		HashMap<String, IntervalTree<TFBSPeak>> tfbsPeaks = new FIMOHandler(tfbsRegion,pValueCutOf, upstream,downstream).readHits(tfbsFiles);
		
		HashMap<String,List<HashMap<String, String>>> experiments = new HashMap<String,List<HashMap<String, String>>>();
		
		List<HashMap<String, String>> lookup = FileUtil.fileLookUp(dnaseExperimentDataFile,"\t").getLookUp();
		String aggregateKey = "Cell";
		for(HashMap<String, String> information : lookup){
			if (! information.containsKey(aggregateKey)){
				throw new RuntimeException(String.format("Key %s defined for aggregate function is missing:",aggregateKey));
			}
				
			if (! experiments.containsKey(information.get(aggregateKey))){
				experiments.put(information.get(aggregateKey), new ArrayList<HashMap<String,String>>());
			}
				
			
			String file = information.get("file");
				
			file = dnaseExperimentDataFile.getParent()+ "/" + file;
			if (! new File(file).exists()){
				CroCoLogger.getLogger().warn(String.format("Cannot find referenced file %s in %s",file,dnaseExperimentDataFile.toString()));
				continue;
			}
			information.put("file", file);
			
			experiments.get(information.get(aggregateKey)).add(information);
		}
		
	
		
		for(Entry<String,List<HashMap<String,String>>> aggregations : experiments.entrySet()){
			
			CroCoLogger.getLogger().info("Start Processing:\t" + aggregations.getKey());
			String aggreatedBaseDir=aggregations.getKey().replace("/", "").replace(":","");
			
			File aggreatedDir = new File(outputDir + "/" + aggreatedBaseDir);
			aggreatedDir.mkdir();
			for(HashMap<String,String> expNet: aggregations.getValue()){
				//skip hotspot when Peak is available
				String type = expNet.get("Type"); //hotspot, or view
				File file  = new File(expNet.get("file"));
				
				boolean toSkip = false;
				if ( type.equals("Hotspot")){ //only for hotspots
					for(HashMap<String,String> expNetCheck: aggregations.getValue()){ //for each experiment 
						boolean equals = true;
						if ( expNetCheck.size() == expNet.size()){ //same number of annotations
							for(Entry<String, String> e  : expNet.entrySet()){ //for each annotation
								if ( !expNetCheck.containsKey(e.getKey()) || !expNetCheck.get(e.getKey()).equals(e.getValue())){ //
									equals =false;
								}
							}
						}else{
							equals = false;
						}if ( equals == true){
							toSkip = true; 
							break;
						}
					}
				}
				if ( toSkip){
					CroCoLogger.getLogger().info(String.format("Skip: %s", file.toString()));
					continue;
				}
				
				String fileBaseName = file.getName();
				
				
				File infoFile = new File(aggreatedDir + "/" + fileBaseName + ".info");
				if ( infoFile.exists()){
					CroCoLogger.getLogger().debug(String.format("Over write previous network"));
				}
				
				BufferedWriter bwInfo = new BufferedWriter(new FileWriter(infoFile));
				
				bwInfo.write(String.format("%s: %s\n",Option.NetworkName, aggregations.getKey() ));
				bwInfo.write(String.format("%s: %d\n",Option.TaxId.name(),taxId));
				bwInfo.write(String.format("%s: %s\n",Option.EdgeType,"Directed"));
				bwInfo.write(String.format("%s: %s\n",Option.NetworkType.name(), NetworkType.OpenChrom.name()));
				bwInfo.write(String.format("%s: %s\n",Option.OpenChromMotifSet.name(),motifSetName.toString()));
				bwInfo.write(String.format("%s: %s\n",Option.OpenChromMotifPVal.name(),pValueCutOf.toString()));
				bwInfo.write(String.format("%s: %s\n",Option.OpenChromType.name(),openChromExpType));
				bwInfo.write(String.format("%s: %s\n",Option.Upstream.name(), upstream + ""));
				bwInfo.write(String.format("%s: %s\n",Option.Downstream.name(), downstream +""));
				
				String cell = expNet.get("Cell");
				String age= expNet.get("Age");
				String replicate= expNet.get("Replicate");
				String strain= expNet.get("Strain");
				String treatment= expNet.get("Treatment");
				String exp_comp = expNet.get("Composite");
				
				if (cell!= null && cell.length()> 0  ){
					bwInfo.write(String.format("%s: %s\n",Option.cellLine.name(),cell));
				}
				
				if (age != null && age.length() > 0){
					bwInfo.write(String.format("%s: %s\n",Option.age.name(),age));
				}
				
				if ( composite != null && composite.length() > 0){
					bwInfo.write(String.format("%s: %s\n",Option.composite.name(),exp_comp));
				}
				
				if (replicate != null&& replicate.length() > 0 ){
					bwInfo.write(String.format("%s: %s\n",Option.replicate.name(),replicate));
				}
				
				if (strain != null && strain.length() > 0){
					bwInfo.write(String.format("%s: %s\n",Option.strain.name(),strain));
				}
				
				if ( treatment != null && treatment.length() > 0){
					bwInfo.write(String.format("%s: %s\n",Option.treatment.name(),treatment));
				}
				File networkFile = new File(aggreatedDir + "/" + fileBaseName + ".network.gz");
				if ( test) continue;
				File annotationFile = new File(aggreatedDir + "/" + fileBaseName + ".annotation.gz");
				BufferedWriter bwAnnotation = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(annotationFile)) ));
				
				HashMap<String, IntervalTree<Peak>> openChromPeaks = GenomeUtil.createPeakIntervalTree(file,0,1,2,7,null); 
				
				DirectedNetwork network = new DirectedNetwork(aggregations.getKey(),taxId,false);
				
				int k = 0;
				for(Entry<String, IntervalTree<Peak>> openChrom : openChromPeaks.entrySet()){
					String chrom = openChrom.getKey();
					IntervalTree<Peak> openChromPeakTree = openChrom.getValue();
					
					if (!tfbsPeaks.containsKey(chrom) ){
						chrom =chrom.replace( chromosomNamePrefix , "");
						if (!tfbsPeaks.containsKey(chrom) ){
							CroCoLogger.getLogger().warn(String.format("No TFBS predictions for chrom: %s",chrom));
							continue;
						}
					}
					
					IntervalTree<TFBSPeak> tfbsPeakTree =tfbsPeaks.get(chrom);
					IntervalTree<Promoter> promoter = promoterTree.get(chrom);
					
					if ( promoter == null) {
						CroCoLogger.getLogger().warn(String.format("No promoter information for chrom: %s",chrom));
						continue;
					}
					for(Peak openChromPeak :openChromPeakTree.getObjects() ){
						if ( openChromPeak == null) continue;
						
						List<TFBSPeak> openChromChipEnriched = tfbsPeakTree.searchAll(openChromPeak);
						
						
						for(TFBSPeak peak :openChromChipEnriched ){
							Set<String> factorIdMapping = motifIdMapping.get(peak.getMotifId());
							if (factorIdMapping == null) continue;
							int absolutMiddle = (peak.getStart()+peak.getEnd())/2;
							
							
							List<TFBSGeneEnrichment> enrichedGenes = GenomeUtil.enrich(promoter.searchAll(peak), absolutMiddle, upstream, downstream);
							for(TFBSGeneEnrichment  geneAssoication : enrichedGenes){
								for(String factorId :factorIdMapping )  {
									
									network.add(new Entity(factorId),geneAssoication.gene );
									
									String tfbs =  String.format("%s\t%s\t%s\t%s\t%s\t%f\t%.7f\t%s\t%d\t%d\t%d\t%d\t%.3f",
											factorId,geneAssoication.gene.getIdentifier(),peak.getMotifId(),
											geneAssoication.closestTranscriptUpstream==null?"NaN":Transcript.getDistanceToTssStart(geneAssoication.closestTranscriptUpstream, absolutMiddle),
											geneAssoication.closestTranscriptDownstream==null?"NaN":Transcript.getDistanceToTssStart(geneAssoication.closestTranscriptDownstream, absolutMiddle),
											peak.getScore(),peak.getpValue(),peak.getChrom(),peak.getStart(),peak.getEnd(),openChromPeak.getStart(),openChromPeak.getEnd(),openChromPeak.getScore());
									
									bwAnnotation.write(String.format("OpenChromTFBS\t%s\n",tfbs));
									
								}
							}
							
						}
					}
	
					if ( k++ % 10000 == 0){
						bwAnnotation.flush();
					}
					
				}
				NetworkHierachy.writeNetworkHierachyFile(network, networkFile);
				StringBuffer factorStr = new StringBuffer();
				
				for(Entity factor: network.getFactors()){
					factorStr.append(factor.getIdentifier() + " ");
				}
				bwInfo.write(String.format("%s: %s\n",Option.FactorList,factorStr.toString().trim()));
				bwInfo.flush();
				bwInfo.close();
				
				bwAnnotation.flush();
				bwAnnotation.close();
			
			}
		}
	}

}
