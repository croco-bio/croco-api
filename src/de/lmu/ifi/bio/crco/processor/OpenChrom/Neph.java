package de.lmu.ifi.bio.crco.processor.OpenChrom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;

import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.util.ConsoleParameter;
import de.lmu.ifi.bio.crco.util.ConsoleParameter.CroCoOption;
import de.lmu.ifi.bio.crco.util.FileUtil;

public class Neph {
	private static CroCoOption<File> NephResultDir_Parameter = new CroCoOption<File>("NephResultDir",new ConsoleParameter.FolderExistHandler()).isRequired().setDescription("Flat file networks").setArgs(1);
	private static CroCoOption<File> mapping_parameter = new CroCoOption<File>("mapping",new ConsoleParameter.FileExistHandler()).isRequired().setArgs(1).setDescription("Gene name to ensembl mapping");
	
	private static String NETWORK_FILE_NAME="genes-regulate-genes.txt";
	public static void main(String[] args) throws Exception{
		Locale.setDefault(Locale.US);
		ConsoleParameter parameter = new ConsoleParameter();
		parameter.register(
				ConsoleParameter.compositeName,
				ConsoleParameter.repositoryDir,
				NephResultDir_Parameter,
				mapping_parameter
		);
		CommandLine cmdLine = parameter.parseCommandLine(args, Neph.class);
		
		File repositoryDir =ConsoleParameter.repositoryDir.getValue(cmdLine);
		String composite = ConsoleParameter.compositeName.getValue(cmdLine);
		File NephResultDir = NephResultDir_Parameter.getValue(cmdLine);
		File mappingFile = mapping_parameter.getValue(cmdLine);
		
		HashMap<String, String> mapping = new FileUtil.MappingFileReader(0,1,mappingFile).readN1MappingFile();
		
		File outputDir = new File(repositoryDir + "/"   + composite);
		if ( outputDir.exists()){
			System.err.println(String.format("Composite %s already in repository %s",composite,repositoryDir.toString()));
		}else{
			if  ( !outputDir.mkdirs() ) {
				System.err.println(String.format("Cannnot create composite %s in repository %s",composite,repositoryDir.toString()));
				System.exit(1);
			}
		}
		
		for(File file : NephResultDir.listFiles()){
			if ( file.isDirectory()){
			
				String cellLine = file.getName();//.split("-")[0];
				
				File infoFile = new File(outputDir + "/" +cellLine + ".info");
				BufferedWriter bwInfo = new BufferedWriter(new FileWriter(infoFile));
				
				bwInfo.write(String.format("%s: %s\n",Option.NetworkName.name(),cellLine));
				bwInfo.write(String.format("%s: %d\n",Option.TaxId.name(),9606));
				bwInfo.write(String.format("%s: %s\n",Option.EdgeType,"Directed"));
				bwInfo.write(String.format("%s: %s\n",Option.NetworkType.name(), NetworkType.OpenChrom.name()));
				bwInfo.write(String.format("%s: %d\n",Option.Upstream.name(), 5000));
				bwInfo.write(String.format("%s: %d\n",Option.Downstream.name(), 5000));
				bwInfo.write(String.format("%s: %s\n",Option.cellLine.name(), cellLine));
				bwInfo.write(String.format("%s: %s\n",Option.reference.name(), "Neph et al., Circuitry and Dynamics of Human Transcription Factor Regulatory Networks, Cell, 2012"));
				bwInfo.write(String.format("%s: %s\n",Option.OpenChromType.name(),"DGF"));
				
				File networkFile = new File(file.toString()  + "/" + NETWORK_FILE_NAME);
				HashMap<String, Set<String>> network =new FileUtil.MappingFileReader(1,0,networkFile).readNNMappingFile();
			
			
				BufferedWriter bwNetwork = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(outputDir + "/" +cellLine + ".network.gz")) ));
				HashSet<String> mappedFactors = new HashSet<String>();
				for(Entry<String, Set<String>> e : network.entrySet()){
					String factor = mapping.get(e.getKey());
					mappedFactors.add(factor);
					for(String t : e.getValue()){
						String target = mapping.get(t);
						if ( factor == null || target == null || factor.length() == 0 || target.length() == 0) continue;
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
