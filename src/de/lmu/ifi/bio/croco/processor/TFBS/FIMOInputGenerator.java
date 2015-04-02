package de.lmu.ifi.bio.croco.processor.TFBS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.croco.data.genome.Gene;
import de.lmu.ifi.bio.croco.data.genome.Strand;
import de.lmu.ifi.bio.croco.data.genome.Transcript;
import de.lmu.ifi.bio.croco.intervaltree.IntervalTree;
import de.lmu.ifi.bio.croco.intervaltree.peaks.Promoter;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.FileUtil;
import de.lmu.ifi.bio.croco.util.GenomeSequenceExtractor;
import de.lmu.ifi.bio.croco.util.GenomeUtil;

/**
 * Extracts the promoter region from a genome so that it can be used with the TFBS scanner FIMO.
 * @author rpesch
 *
 */
public class FIMOInputGenerator {
	private Integer upstream = null;
	private Integer downstream = null;
	
	public FIMOInputGenerator(Integer upstream, Integer downstream){
		this.upstream = upstream;
		this.downstream = downstream;
	}
	/**
	 * Writes the promoter region to a file. The promoter regions are written as:
	 * <pre>
	 *  >ID:chrosom start-end
	 *  DNA-Sequence
	 * <pre>
	 * to the output file.
	 * @param output -- FIMO input file
	 * @param extractor -- a GenomeSequenceExtractor used to extract the DNA sequence
	 * @param genes -- a list of genes of interest (with references to Transcript objects)
	 * @throws IOException
	 */
	public void writeFIMOInputFile(File output,GenomeSequenceExtractor extractor, List<Gene> genes) throws IOException{
		HashMap<String, IntervalTree<Promoter>> trees =GenomeUtil.createPromoterIntervalTree(genes,upstream,downstream,true);
		CroCoLogger.getLogger().debug(String.format("Writing %s",output.toString()));
		BufferedWriter bw = new BufferedWriter(new FileWriter(output ));
		int promoterId = 0;

		for(Entry<String, IntervalTree<Promoter>> e: trees.entrySet()){
			IntervalTree<Promoter> tree = e.getValue();
			Collection<Promoter> regions = tree.getObjects();
			for(Promoter region: regions){
				if ( region == null) continue;
				HashSet<Gene> g = new HashSet<Gene>();
				for(Transcript transcript : region.getTranscripts() ) {
					g.add(transcript.getParentGene());
				}
				
				String dna = extractor.getDNASequence(e.getKey(), region.getStart(), region.getEnd());
				if ( dna == null) {
					CroCoLogger.getLogger().warn(String.format("Region not found chr%s:%d-%d",e.getKey(),region.getStart(),region.getEnd()));
				}
				bw.write(String.format(">%d:%s %d-%d %s\n",promoterId++,e.getKey(),region.getStart(),region.getEnd(),g.toString()));
				bw.write(dna + "\n");
				bw.flush();
			}
			
		}
		
		bw.flush();
		bw.close();
	}

	
	public static void main(String[] args) throws Exception{
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("genomeDir").withDescription("Folde containing the genome").isRequired().hasArgs().create("genome"));
		options.addOption(OptionBuilder.withLongOpt("gtf").withDescription("GTF annotations").isRequired().hasArgs(1).create("gtf"));
		options.addOption(OptionBuilder.withLongOpt("output").withDescription("Promoter output file (base name)").isRequired().hasArgs(1).create("output"));
		options.addOption(OptionBuilder.withLongOpt("upstream").withDescription("Upstream").hasArgs(1).create("upstream"));
		options.addOption(OptionBuilder.withLongOpt("downstream").withDescription("Downstream").hasArgs(1).create("downstream"));
		options.addOption(OptionBuilder.withLongOpt("chromosoms").withDescription("List of chromosoms").hasArgs().create("chromosoms"));
		options.addOption(OptionBuilder.withLongOpt("type").withDescription("Transcript type").hasArgs(1).create("type"));
			
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + FIMOInputGenerator.class.getName(), "", options, "", true);
			System.exit(1);
		}
		List<String> chromosoms = null;
		if ( line.hasOption("chromosoms")){
			chromosoms = new ArrayList<String>();
			for(String chromosom : line.getOptionValues("chromosoms")){
				chromosoms.add(chromosom);
			}
		}
		String transcriptType = null;
		if ( line.hasOption("type")){
			transcriptType = line.getOptionValue("type");
		}
		
		Integer upstream = null;
		if ( line.hasOption("upstream")){
			upstream = Integer.valueOf(line.getOptionValue("upstream"));
		}else{
			upstream = 5000;
		}
		Integer downstream = null;
		if ( line.hasOption("downstream")){
			downstream = Integer.valueOf(line.getOptionValue("downstream"));
		}else{
			downstream = 5000;
		}
		
		File output = new File(line.getOptionValue("output"));
		File genomeDir = new File(line.getOptionValue("genome")); 
		if (!genomeDir.isDirectory() ){
			System.err.println(genomeDir + " is not a directory");
			System.exit(1);
		}
		GenomeSequenceExtractor extractor = new GenomeSequenceExtractor(genomeDir);
		
		File gtfFile = new File(line.getOptionValue("gtf"));
		if (! gtfFile.isFile()){
			System.out.println(gtfFile + " does not exist");
			System.exit(1);
		}
		
		System.out.println("GTF file:\t" + gtfFile);
		System.out.println("Genome dir:\t" + genomeDir);
		System.out.println("Output file:\t" + output);
		System.out.println("Upstream:\t" + upstream);
		System.out.println("Upstream:\t" + downstream);
		System.out.println("Chromsoms:\t" + chromosoms);
		
		List<Gene> genes = FileUtil.getGenes(gtfFile,transcriptType,chromosoms);
		
		FIMOInputGenerator generator = new FIMOInputGenerator(upstream,downstream);
		generator.writeFIMOInputFile(output, extractor, genes);
	
	}
}
