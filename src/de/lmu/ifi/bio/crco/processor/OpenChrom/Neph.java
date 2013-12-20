package de.lmu.ifi.bio.crco.processor.OpenChrom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.util.FileUtil;

public class Neph {
	public static void main(String[] args) throws Exception{
		//parsing arguments
		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("NephResultDir").withArgName("DIR").withDescription("NephResultDir").isRequired().hasArgs(1).create("NephResultDir"));
		options.addOption(OptionBuilder.withLongOpt("repositoryDir").withDescription("Repository directory").isRequired().hasArgs().create("repositoryDir"));
		options.addOption(OptionBuilder.withLongOpt("compositeName").withDescription("Composite name").isRequired().hasArgs().create("compositeName"));
		options.addOption(OptionBuilder.withLongOpt("mapping").withDescription("ID mapping file").isRequired().hasArgs().create("mapping"));
		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + Neph.class.getName(), "", options, "", true);
			System.exit(1);
		}
		File repositoryDir = new File(line.getOptionValue("repositoryDir"));
		if (! repositoryDir.isDirectory()){
			throw new RuntimeException(repositoryDir + " is not a directory");
		}
		String composite = line.getOptionValue("compositeName");
		
		File outputDir = new File(repositoryDir + "/"   + composite);
		if ( outputDir.exists()){
			System.err.println(String.format("Composite %s already in repository %s",composite,repositoryDir.toString()));
		}else{
			if  ( !outputDir.mkdirs() ) {
				System.err.println(String.format("Cannnot create composite %s in repository %s",composite,repositoryDir.toString()));
				System.exit(1);
			}
		}
		HashMap<String, String> mapping = new FileUtil.MappingFileReader(0,1,new File(line.getOptionValue("mapping"))).readN1MappingFile();
		
		File NephResultDir = new File(line.getOptionValue("NephResultDir"));
		for(File file : NephResultDir.listFiles()){
			if ( file.isDirectory()){
				
				File infoFile = new File(outputDir + "/" + file.getName() + ".info");
				BufferedWriter bwInfo = new BufferedWriter(new FileWriter(infoFile));
				
				bwInfo.write(String.format("%s: %s\n",Option.NetworkName.name(),file.getName()));
				bwInfo.write(String.format("%s: %d\n",Option.TaxId.name(),9606));
				bwInfo.write(String.format("%s: %s\n",Option.EdgeType,"Directed"));
				bwInfo.write(String.format("%s: %s\n",Option.NetworkType.name(), NetworkType.DGF.name()));
				bwInfo.write(String.format("%s: %d\n",Option.Upstream.name(), 5000));
				bwInfo.write(String.format("%s: %d\n",Option.Downstream.name(), 5000));
				bwInfo.write(String.format("%s: %s\n",Option.cellLine.name(), file.getName()));
				bwInfo.write(String.format("%s: %s\n",Option.reference.name(), "Neph at el., Circuitry and Dynamics of Human Transcription Factor Regulatory Networks, Cell, 2012"));
				
			
				
				File networkFile = new File(file  + "/genes.regulate.genes");
				HashMap<String, Set<String>> network =new FileUtil.MappingFileReader(0,1,networkFile).readNNMappingFile();
			
			
				BufferedWriter bwNetwork = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(outputDir + "/" + file.getName() + ".network.gz")) ));
				HashSet<String> mappedFactors = new HashSet<String>();
				for(Entry<String, Set<String>> e : network.entrySet()){
					String factor = mapping.get(e.getKey());
					mappedFactors.add(factor);
					for(String t : e.getValue()){
						String target = mapping.get(t);
						if ( factor == null || t == null) continue;
						
						bwNetwork.write(factor + "\t" + target + "\n");
					}
				}
				StringBuffer factorStr = new StringBuffer();
				for(String factor: mappedFactors){
					factorStr.append(factor + " ");
				}
				bwInfo.write(String.format("%s: %s\n",Option.FactorList,factorStr.toString().trim()));
				bwInfo.flush();
				bwInfo.close();
				
				
				bwNetwork.flush();
				bwNetwork.close();
			}
		}
	}
}
