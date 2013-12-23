package de.lmu.ifi.bio.crco.processor.OpenChrom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.crco.intervaltree.peaks.DNaseTFBSPeak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.processor.TFBS.FIMOHandler;
import de.lmu.ifi.bio.crco.processor.hierachy.NetworkHierachy;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.FileUtil;
import de.lmu.ifi.bio.crco.util.GenomeUtil;
import de.lmu.ifi.bio.crco.util.OrderedPair;

public class DNaseTFBSExtWriter {

	public static void main(String[] args) throws Exception{
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("experimentMappingFile").withArgName("FILE").withDescription("DNase experiment description file").isRequired().hasArgs(1).create("experimentMappingFile"));
		options.addOption(OptionBuilder.withLongOpt("taxId").withDescription("Tax id").isRequired().hasArgs(1).create("taxId"));
		options.addOption(OptionBuilder.withLongOpt("tfbsFiles").withDescription("tfbsFiles").isRequired().hasArgs().create("tfbsFiles"));
		options.addOption(OptionBuilder.withLongOpt("tfbsRegion").withDescription("tfbsRegion").isRequired().hasArgs(1).create("tfbsRegion"));
		options.addOption(OptionBuilder.withLongOpt("pValueCutOf").withDescription("pValue cut-off (with promoter option)").isRequired().hasArgs(1).create("pValueCutOf"));
		options.addOption(OptionBuilder.withLongOpt("motifMappingFiles").withDescription("motifMappingFiles ").hasArgs().create("motifMappingFiles"));
		options.addOption(OptionBuilder.withLongOpt("repositoryDir").withDescription("Repository directory").isRequired().hasArgs().create("repositoryDir"));
		options.addOption(OptionBuilder.withLongOpt("compositeName").withDescription("Composite name").isRequired().hasArgs().create("compositeName"));
		options.addOption(OptionBuilder.withLongOpt("motifSetName").withDescription("Motif Set Name").isRequired().hasArgs(1).create("motifSetName"));
		options.addOption(OptionBuilder.withLongOpt("test").withDescription("Test reading").create("test"));
		options.addOption(OptionBuilder.withLongOpt("gtf").withDescription("GTFFile").hasArgs(1).isRequired().create("gtfFile"));

		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + DNaseTFBSExtWriter.class.getName(), "", options, "", true);
			System.exit(1);
		}
		Connection connection = DatabaseConnection.getConnection();
		
		Integer taxId = Integer.valueOf(line.getOptionValue("taxId"));
		List<File> tfbsFiles = new ArrayList<File>();
		for(String file : line.getOptionValues("tfbsFiles") ){
			File tfbsFile = new File(file);
			
			if (! tfbsFile.exists()){
				System.err.println("TFBS file:" + tfbsFile + " does not exist");
				System.exit(1);
			}
			
			tfbsFiles.add(tfbsFile);
		}
		File gtfFile = FileUtil.checkFile(line.getOptionValue("gtf"));
		File tfbsRegion = FileUtil.checkFile((line.getOptionValue("tfbsRegion")));
		Float pValueCutOf = Float.valueOf(line.getOptionValue("pValueCutOf"));
		File[] motifMappingFiles = new File[line.getOptionValues("motifMappingFiles").length];
		int i = 0;
		for(String file : line.getOptionValues("motifMappingFiles") ){
			File mappingFile = FileUtil.checkFile(file);
			motifMappingFiles[i++]  = mappingFile;	
		}
		
		File repositoryDir = new File(line.getOptionValue("repositoryDir"));
		if (! repositoryDir.isDirectory()){
			System.err.println(repositoryDir + " is not a directory");
			System.exit(1);
		}
		String composite = line.getOptionValue("compositeName");
		String motifSetName = line.getOptionValue("motifSetName");
		
		File dnaseExperimentDataFile = new File(line.getOptionValue("experimentMappingFile"));
		Boolean test = line.hasOption("test");
		System.out.println("Test:\t"+test);
		System.out.println("GTF file:\t" + gtfFile);
		System.out.println("TFBS file:\t" + tfbsFiles);
		System.out.println("Mapping file:\t" +motifMappingFiles );
		System.out.println("TaxId:\t" + taxId);
		System.out.println("PValue:\t" + pValueCutOf);
		System.out.println("Composite name:\t"  +composite );
		System.out.println("Repository dir:\t" + repositoryDir);
		
		File outputDir = new File(repositoryDir + "/"   + composite);
		if ( outputDir.exists()){
			System.err.println(String.format("Composite %s already in repository %s",composite,repositoryDir.toString()));
		}else{
			if  ( !outputDir.mkdirs() ) {
				System.err.println(String.format("Cannnot create composite %s in repository %s",composite,repositoryDir.toString()));
				System.exit(1);
			}
		}
		List<Gene> genes = FileUtil.getGenes(gtfFile, "protein_coding", null);
		HashMap<String, Set<String>> mapping = new FileUtil.MappingFileReader(0,2,motifMappingFiles).includeAllColumnsAfterToIndex(true).readNNMappingFile();
		HashMap<String, IntervalTree<TFBSPeak>> matchTree = new FIMOHandler(tfbsRegion,pValueCutOf,genes, mapping,5000,5000).readHits(tfbsFiles);
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
			if ( !information.containsKey("file") || !information.containsKey("antibody") || !information.containsKey("targetMapped")) 
				throw new RuntimeException(String.format("Experiment file %s not well formated. file, target,or targetMapped column is missing.",dnaseExperimentDataFile.toString()));
				
			
			String file = information.get("file");
				
			file = dnaseExperimentDataFile.getParent()+ "/" + file;
			if (! new File(file).exists()){
				throw new RuntimeException(String.format("Cannot find referenced file %s in %s",file,dnaseExperimentDataFile.toString()));
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
						if ( equals == true){
							toSkip = true; 
							break;
						}
					}
				}
				if ( toSkip) continue;
				
				File file  = new File(expNet.get("file"));
				String fileBaseName = file.getName();
				
				
				File infoFile = new File(aggreatedDir + "/" + fileBaseName + ".info");
				if ( infoFile.exists()){
					throw new RuntimeException("Duplicate experiment :" + infoFile );
				}
				
				BufferedWriter bwInfo = new BufferedWriter(new FileWriter(infoFile));
				
				bwInfo.write(String.format("%s: %s\n",Option.NetworkName, aggregations.getKey() ));
				bwInfo.write(String.format("%s: %d\n",Option.TaxId.name(),taxId));
				bwInfo.write(String.format("%s: %s\n",Option.EdgeType,"Directed"));
				bwInfo.write(String.format("%s: %s\n",Option.NetworkType.name(), NetworkType.DNase.name()));
				bwInfo.write(String.format("%s: %s\n",Option.DNaseMotifSet.name(),motifSetName.toString()));
				bwInfo.write(String.format("%s: %s\n",Option.DNaseMotifPVal.name(),pValueCutOf.toString()));

				String cell = expNet.get("Cell");
				String age= expNet.get("Age");
				//String composite= expNet.get("Composite");
				String replicate= expNet.get("Replicate");
				String strain= expNet.get("Strain");
				String treatment= expNet.get("Treatment");
				
				if (cell!= null && cell.length()> 0  ){
					bwInfo.write(String.format("%s: %s\n",Option.cellLine.name(),cell));
				}
				
				if (age != null && age.length() > 0){
					bwInfo.write(String.format("%s: %s\n",Option.age.name(),age));
				}
				
				if ( composite != null && composite.length() > 0){
					bwInfo.write(String.format("%s: %s\n",Option.composite.name(),composite));
				}
				
				if (replicate != null&& replicate.length() > 0 ){
					bwInfo.write(String.format("%s: %d\n",Option.replicate.name(),replicate));
				}
				
				if (strain != null && strain.length() > 0){
					bwInfo.write(String.format("%s: %s\n",Option.strain.name(),strain));
				}
				
				if ( treatment != null && treatment.length() > 0){
					bwInfo.write(String.format("%s: %s\n",Option.treatment.name(),treatment));
				}
				File networkFile = new File(aggreatedDir + "/" + fileBaseName + ".network.gz");
				//BufferedWriter bwNetwork = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(networkFile)) ));
				
				File annotationFile = new File(aggreatedDir + "/" + fileBaseName + ".annotation.gz");
				BufferedWriter bwAnnotation = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(annotationFile)) ));
				
				HashMap<String, IntervalTree<Peak>> peaks = GenomeUtil.createPeakIntervalTree(file,0,1,2,-1,null); 
				
				DirectedNetwork network = new DirectedNetwork(aggregations.getKey(),taxId,false);
				
				//HashMap<String, IntervalTree> enrichedNetwork = GenomeUtil.enichPeaksWithTFBS(peaks, matchTree);
				//HashMap<OrderedPair<String, String>, List<DNaseTFBSPeak>> net = GenomeUtil.createEnrichedNetwork(enrichedNetwork,motifMapping);
				int k = 0;
				HashSet<String> factors = new HashSet<String>();
				for(String c : peaks.keySet()){
					IntervalTree<Peak> openChromPeaks = peaks.get(c);
					if (!matchTree.containsKey(c) ){
						CroCoLogger.getLogger().warn(String.format("No TFBS predictiosn for chrom: %s",c));
						continue;
					}
					IntervalTree<TFBSPeak> tfbsPeaks = matchTree.get(c);
					for(Peak openChromPeak :openChromPeaks.getObjects() ){
						if ( openChromPeak == null) continue;
						List<TFBSPeak> openChromChipEnriched = tfbsPeaks.searchAll(openChromPeak);
						
						for(TFBSPeak peak :openChromChipEnriched ){
							for(Entity factor : peak.getFactors() )  {
								factors.add(factor.getIdentifier());
								network.add(factor, peak.getTarget());
								bwAnnotation.write("OpenChromTFBS\t" +
										factor + "\t" + peak.getTarget() + "\t" +
										peak.getChrom() + "\t" + (int)peak.getLow() + "\t" + (int)peak.getHigh() + "\t" + 
										peak.getpValue() + "\t" + peak.getMotifId() + "\t" + peak.getDistanceToTranscript()+
										openChromPeak.getChrom() + "\t" + (int)openChromPeak.getLow() + "\t" + (int)openChromPeak.getHigh() + "\n"
									);
							}
							
						}
					}
	
					if ( k++ % 10000 == 0){
						bwAnnotation.flush();
					}
					
				}
				NetworkHierachy.writeNetworkHierachyFile(network, networkFile);
				StringBuffer factorStr = new StringBuffer();
				for(String factor: factors){
					factorStr.append(factor + " ");
				}
				bwInfo.write(String.format("%s: %s\n",Option.FactorList,factorStr.toString().trim()));
				bwInfo.flush();
				bwInfo.close();
				
				//bwNetwork.flush();
				//bwNetwork.close();
				
				bwAnnotation.flush();
				bwAnnotation.close();
			}
			}
		}
	
		
	
		
	}

}
