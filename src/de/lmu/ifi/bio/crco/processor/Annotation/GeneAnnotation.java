package de.lmu.ifi.bio.crco.processor.Annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.data.genome.Strand;
import de.lmu.ifi.bio.crco.data.genome.Transcript;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.FileUtil;

public class GeneAnnotation {
	
	private static void clean() throws Exception {
		CroCoLogger.getLogger().info("Clean database");
		Connection connection = DatabaseConnection.getConnection();
		Statement stat = connection.createStatement();
		stat.execute("DELETE FROM Gene");
		stat.execute("DELETE FROM Transcript");
		stat.execute("DELETE FROM GeneLinkout");
		
		stat.close();
	}
	private static void importLinkOut(String database,File geneLinkOutFile) throws Exception{
		CroCoLogger.getLogger().debug(String.format("Import linkout (database %s) file %s",database,geneLinkOutFile));
		Connection connection = DatabaseConnection.getConnection();
		PreparedStatement linkout = connection.prepareStatement("INSERT INTO GeneLinkout(gene,linkout,source) values(?,?,?)");
		BufferedReader br = new BufferedReader(new FileReader(geneLinkOutFile));
		String line = br.readLine(); //assume header in linkout file
		int k= 0;
		while((line=br.readLine())!=null){
			String[] tokens = line.split("\t");
			if ( tokens.length != 2) continue;
			String geneId = tokens[0];
			String reference = tokens[1];
			if ( reference.equals("NA")) continue; //NA= not available
			linkout.setString(1, geneId);
			linkout.setString(2, reference);
			linkout.setString(3, database);
			linkout.addBatch();
			if ( k++%2500==0){
				linkout.executeBatch();
			}
		}
		linkout.executeBatch();
		br.close();
		linkout.close();
		
	}
	
	private static void importGTF(File gtfFile,Integer taxId,HashMap<String,String> geneName, HashMap<String,String> geneDescriptions) throws Exception{
		CroCoLogger.getLogger().debug("Import GTF file");
		List<Gene> genes = FileUtil.getGenes(gtfFile, null,null);
		Connection connection = DatabaseConnection.getConnection();
		PreparedStatement geneStat = connection.prepareStatement("INSERT INTO Gene(gene,gene_name,description,tax_id,chrom,strand) values(?,?,?,?,?,?)");
		PreparedStatement transcriptStat = connection.prepareStatement("INSERT INTO Transcript(gene,transcript_id,tss_start,tss_end,bio_type) values(?,?,?,?,?)");
		int k = 0;
		for(Gene gene : genes){
			geneStat.setString(1, gene.getIdentifier());
			if ( geneName.containsKey(gene.getIdentifier())){
				geneStat.setString(2, geneName.get(gene.getIdentifier()));
			}else{
				geneStat.setNull(2, java.sql.Types.VARCHAR);
			}
			if ( geneDescriptions.containsKey(gene.getIdentifier())){
				geneStat.setString(3, geneDescriptions.get(gene.getIdentifier()));
			}else{
				geneStat.setNull(3, java.sql.Types.VARCHAR);
			}
			geneStat.setInt(4, taxId);
			geneStat.setString(5, gene.getChr());
			geneStat.setInt(6, gene.getStrand().equals(Strand.PLUS)?0:1);
			geneStat.addBatch();
			for(Transcript transcript : gene.getTranscripts()){
				transcriptStat.setString(1, gene.getIdentifier());
				transcriptStat.setString(2, transcript.getIdentifier());
				transcriptStat.setInt(3, transcript.getStrandCorredStart());
				transcriptStat.setInt(4, transcript.getStrandCorredEnd());
				transcriptStat.setString(5, transcript.getType());
				transcriptStat.addBatch();
			}
			if ( k++ % 1000 == 0){
				System.err.print(".");
				geneStat.executeBatch();
				transcriptStat.executeBatch();
			}
		}
		System.err.print("\n");
		
		geneStat.executeBatch();
		transcriptStat.executeBatch();
		
		geneStat.close();
		transcriptStat.close();
	}
	
	public static void main(String[] args) throws Exception{
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("taxId").withDescription("Tax id").isRequired().hasArgs(1).create("taxId"));
		options.addOption(OptionBuilder.withLongOpt("gtfFile").withDescription("GTF").isRequired().hasArgs(1).create("gtfFile"));
		options.addOption(OptionBuilder.withLongOpt("descriptionFile").withDescription("Gene description file").isRequired().hasArgs(1).create("descriptionFile"));
		options.addOption(OptionBuilder.withLongOpt("geneNameFile").withDescription("Gene name file").isRequired().hasArgs(1).create("geneNameFile"));
		options.addOption(OptionBuilder.withLongOpt("geneLinkOutFiles").withDescription("List of gene out files in the format [Database]=file").isRequired().hasArgs().create("geneLinkOutFiles"));
	//	options.addOption(OptionBuilder.withLongOpt("chromsoms").withDescription("chromsoms").hasArgs().create("chromsoms"));
		options.addOption(OptionBuilder.withLongOpt("clean").withDescription("clean").create("clean"));
		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + GeneAnnotation.class.getName(), "", options, "", true);
			System.exit(1);
		}
		/*
		List<String> chromsoms = null;
		if ( line.hasOption("chromsoms")){
			chromsoms = new ArrayList<String>();
			for(String chrom : line.getOptionValues("chromsoms")){
				chromsoms.add(chrom.trim());
			}
		}
		*/
		
		Integer taxId = Integer.valueOf(line.getOptionValue("taxId"));
		File gtfFile = new File(line.getOptionValue("gtfFile"));
		if (! gtfFile.exists()){
			CroCoLogger.getLogger().fatal(String.format("GTF file %s not found",gtfFile));
			System.exit(1);
		}
		File descriptionFile = new File(line.getOptionValue("descriptionFile"));
		if (! descriptionFile.exists()){
			CroCoLogger.getLogger().fatal(String.format("Description file %s not found",descriptionFile));
			System.exit(1);
		}
	
		File geneNameFile = new File(line.getOptionValue("geneNameFile"));
		if (! geneNameFile.exists()){
			CroCoLogger.getLogger().fatal(String.format("Gene name file  %s not found",geneNameFile));
			System.exit(1);
		}
		Boolean clean = line.hasOption("clean");
		HashMap<String,File> geneLinkOutFiles =new HashMap<String,File>();// new File(line.getOptionValue("geneLinkOutFiles"));
		for(String geneLinkOut: line.getOptionValues("geneLinkOutFiles")){
			if ( geneLinkOut.split("=").length != 2){
				CroCoLogger.getLogger().fatal(String.format("geneLinkOutFiles [Database]=file (given %s)",Arrays.asList(geneLinkOut.split("\\s+"))));
				System.exit(1);
			}
			String geneLinkOutFile =geneLinkOut.split("=")[1]; 
			String geneLinkOutType=geneLinkOut.split("=")[0]; 
			
			File linkOut = new File(geneLinkOutFile);
			if  (! linkOut.exists()) {
				CroCoLogger.getLogger().fatal(String.format("Gene linkfile %s not found (database %s)",linkOut,geneLinkOutType));
				System.exit(1);
			}
			geneLinkOutFiles.put(geneLinkOutType,linkOut);
		}
		CroCoLogger.getLogger().info(String.format("GTF file: %s",gtfFile.toString()));
		CroCoLogger.getLogger().info(String.format("Gene name file: %s",geneNameFile.toString()));
		CroCoLogger.getLogger().info(String.format("Link out files: %s",geneLinkOutFiles.toString()));
		CroCoLogger.getLogger().info(String.format("TaxId: %d",taxId));
	//	CroCoLogger.getLogger().info(String.format("Chrom: %s",chromsoms));
		CroCoLogger.getLogger().info(String.format("Clean: %s",clean.toString()));
		
		if ( clean)clean();
		
		HashMap<String, String> geneDescriptions = FileUtil.mappingFileReader( 0, 1, descriptionFile).caseSensetive(false).readMappingFile();
		HashMap<String, String> geneName = FileUtil.mappingFileReader(0, 1, geneNameFile).caseSensetive(false).readMappingFile();
		
		GeneAnnotation.importGTF(gtfFile, taxId, geneName, geneDescriptions);
		
		for(Entry<String, File> e : geneLinkOutFiles.entrySet()){
			GeneAnnotation.importLinkOut(e.getKey(),e.getValue());
		}
	}

}
