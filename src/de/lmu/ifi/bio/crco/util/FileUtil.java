package de.lmu.ifi.bio.crco.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.bio.crco.data.genome.Exon;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.data.genome.Protein;
import de.lmu.ifi.bio.crco.data.genome.Strand;
import de.lmu.ifi.bio.crco.data.genome.Transcript;


/**
 * Collection of commonly used methods to read files (including mapping files)
 * @author rpesch
 *
 */
public class FileUtil {
	
	public static ColumnLookUp fileLookUp(File inputFile, String seperator,List<String> columnNames) {
		return new ColumnLookUp(inputFile,seperator,columnNames);
	}
	public static ColumnLookUp fileLookUp(File inputFile, String seperator) throws IOException{
		return new ColumnLookUp(inputFile,seperator);
	}
	public static MappingFileReader mappingFileReader( Integer fromIndex, Integer toIndex,File... inputFiles){
		return new MappingFileReader(fromIndex,toIndex,inputFiles);
	}
	public static File checkFile(String fileStr) throws IOException{
		File file = new File(fileStr);
		if (file.exists()) throw new IOException(String.format("File %s not found",fileStr));
		return file;
	}
	
	public static List<Gene> getGenes(File gtfFile,String transcriptType, List<String> chrosoms) throws Exception{
		CroCoLogger.getLogger().info(String.format("Reading GTF:\t%s",gtfFile.toString()));
		if ( !gtfFile.exists()){
			throw new IOException(gtfFile.getAbsoluteFile().toString() + " does not exist");
		}
		Pattern pattern = Pattern.compile("\t");
		Pattern annotation = Pattern.compile("([^\\s]+)\\s+\"([^;]+)\";");
		
		Set<String> foundChrosoms = new HashSet<String>();
		Set<String> toConsider = null;
		if (chrosoms != null) toConsider = new HashSet<String>(chrosoms);
		BufferedReader br = new BufferedReader(new FileReader(gtfFile));
		String line = null;
		List<Gene> genes = new ArrayList<Gene>();
		Gene currentGene = null;
		Transcript currentTranscript = null;
		Protein currentProtein = null;
		
		while((line=br.readLine())!=null){
			String[] tokens = pattern.split(line);
			String chrom = tokens[0];
			foundChrosoms.add(chrom);
			if ( chrosoms != null && !toConsider.contains(chrom)) continue;
			String frame = tokens[7];
			String tType = tokens[1]; //e.g. protein coding
			//if (! geneType.equals("protein_coding")) continue;
			if ( transcriptType != null && !tType.equals(transcriptType)) continue;
			String annotationType = tokens[2]; // CDS, exon, start_codon, stop_codon
			Integer start = Integer.valueOf(tokens[3]);
			Integer end = Integer.valueOf(tokens[4]);
			Strand strand = null;
			if ( tokens[6].equals("+")){
				strand = Strand.PLUS;
			}else{
				strand = Strand.MINUS;
			}
			
			String geneId = null;
			String transcriptId = null;
			String transcriptName = null;
			String geneName = null;
			String proteinId = null;
			Matcher matcher = annotation.matcher(tokens[8]);
			String exonId = null;;
			
			while(matcher.find()){
				String type = matcher.group(1);
				if ( type.equals("gene_id")) geneId = matcher.group(2).toUpperCase(); //default for entity class (otherwise equals fails)
				else if ( type.equals("transcript_name")) transcriptId = matcher.group(2);
				else if ( type.equals("transcript_id")) transcriptId = matcher.group(2).toUpperCase(); 
				else if ( type.equals("gene_name")) geneName = matcher.group(2);
				else if ( type.equals("protein_id")) proteinId = matcher.group(2).toUpperCase();
				else if ( type.equals("exon_number")) exonId = "EXON" + Integer.valueOf(matcher.group(2));
			}
			if ( currentGene == null || !currentGene.getIdentifier().equals(geneId)){
				if (currentGene != null ) genes.add(currentGene);
				currentGene = new Gene(chrom,geneId,geneName,strand,start,end);
			}
			if ( currentTranscript == null || !currentTranscript.equals(transcriptId)){
				currentTranscript = new Transcript(currentGene,transcriptId,transcriptName,tType);
				currentGene.addTranscript(currentTranscript);
			}
			if ( proteinId != null && (currentProtein == null || !currentProtein.getProteinId().equals(proteinId))){
				currentProtein = new Protein(proteinId,currentTranscript);
				currentTranscript.setProtein(currentProtein);
			}
			
			if ( annotationType.equals("exon")){
				Exon currentExon = new Exon(exonId,start,end);
				currentTranscript.addExon(currentExon);
			}
			if ( annotationType.equals("CDS")){
				Exon currentExon = new Exon(exonId,start,end); 
				currentExon.setCoding(Integer.valueOf(frame));
				currentProtein.add(currentExon);
			}
			
		}
		if ( currentGene != null) genes.add(currentGene);
		br.close();
		CroCoLogger.getLogger().debug(String.format("Number of genes in GTF: %d" ,genes.size()));
		CroCoLogger.getLogger().debug(String.format("Found chroms:\t\t %s",foundChrosoms.toString()));
		if ( toConsider != null){
			CroCoLogger.getLogger().debug(String.format("Considered chroms:\t\t %s",toConsider.toString()));
			foundChrosoms.retainAll(toConsider);
			CroCoLogger.getLogger().debug(String.format("Found & Considered chroms: %s",foundChrosoms.toString()));
		}
		
		
		return genes;
	}
	
	/**
	 * Creates a column look-up for a file with a given header. Allows to access each column of each line with its defined column name.
	 * For example:
	 * <pre>
	 * FileUtil.createLookupBasedOnHeader(inFile,"\t").get(0).get("cName")
	 * </pre>
	*/
	public static class ColumnLookUp{
		private File inputFile;
		private String seperator;
		private boolean skipFirstLine = true;
		private List<String> columnNames;
		
	
		private static List<String> headerLookUp(File file, String seperator) throws IOException{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String currentLine = br.readLine();
			br.close();
			
			return Arrays.asList(currentLine.split(seperator));
		}

		
		public List<HashMap<String,String>> getLookUp() throws IOException{
			List<HashMap<String,String>> entries = new ArrayList<HashMap<String,String>> ();
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String currentLine = null;
			if (skipFirstLine ) currentLine = br.readLine();
			while (( currentLine = br.readLine())!=null){
				if ( currentLine.startsWith("#")) continue;
				String[] tokens = currentLine.split(seperator);
				if ( tokens.length != columnNames.size()) {
					br.close();
					throw new IOException("Unexpected number of columns");
				}
				
				HashMap<String,String> information = new HashMap<String,String>();
				for(int i = 0 ; i< columnNames.size();i++){
					information.put(columnNames.get(i), tokens[i]);
				}
				entries.add(information);
			}
			br.close();
			
			return entries;
		}
		public ColumnLookUp headerColumns(List<String> columnNames){
			this.columnNames = columnNames;
			return this;
		}
		public ColumnLookUp skipFirstLine(boolean skipFirstLine){
			this.skipFirstLine = skipFirstLine;
			return this;
		}
		public ColumnLookUp(File inputFile,String seperator) throws IOException{
			this(inputFile,seperator,headerLookUp(inputFile,seperator));
		}
		public ColumnLookUp(File inputFile,String seperator,List<String> columnNames){
			this.inputFile = inputFile;
			this.seperator = seperator;
			this.columnNames = columnNames;
		}
		
	}


	/**
	 * MappingFileReader implements the Build pattern. 
	 * @author rpesch
	 *
	 */
	public static class MappingFileReader {
		private File[] inputFiles;
		private String seperator = "\t";
		private boolean caseSensitve = true;
		private boolean header = false;
		private Integer fromIndex = 0;
		private Integer toIndex = 1;
		private boolean allColumns = false;
		
		
		/**
		 * Reads a mapping file and creates a look-up for the from column to the to column. 
		 * Ambiguous mappings are <b>included<b> e.g. i->j and i->k.
		 * @return n:n mapping
		 * @throws IOException
		 */
		public HashMap<String,Set<String>> readNNMappingFile() throws IOException{
			HashMap<String,Set<String>> ret = new HashMap<String,Set<String>>();
			for(File inputFile : inputFiles){
				BufferedReader br = new BufferedReader(new FileReader(inputFile));
				String line = null;
				if ( header) line = br.readLine();
				while (( line = br.readLine())!=null){
					String[] tokens = line.split(seperator);
					if ( tokens.length <= Math.max(fromIndex,toIndex)) continue;
					String from = tokens[fromIndex];
					String to = tokens[toIndex];
					if ( !caseSensitve) {
						from =  from.toUpperCase();
						to = to.toUpperCase();
					}
					
					if ( !ret.containsKey(from)) {
						ret.put(from, new HashSet<String>());
					}
					ret.get(from).add(to);	
				}
				br.close();
			}
			return ret;
		}
		/**
		 * Reads a mapping file and creates a look-up for the from column to the to column. 
		 * Ambiguous mapping are <b>not included</b> e.g. i->j and i->k.  
		 * @return n:1 mapping
		 * @throws IOException
		 */
		public HashMap<String,String> readN1MappingFile(  ) throws IOException{
			HashMap<String,String> ret = new HashMap<String,String>();
			Set<String> ambigious = new HashSet<String>();
			for(File inputFile: inputFiles){
				BufferedReader br = new BufferedReader(new FileReader(inputFile));
				String line = null;
				if ( header) line = br.readLine();
				while (( line = br.readLine())!=null){
					String[] tokens = line.split(seperator);
					
					
					if ( tokens.length <= Math.max(fromIndex,toIndex)) continue;
					String from = tokens[fromIndex];
					
					
					for(int i =  toIndex ; i< tokens.length  ; i++){
						if ( i !=fromIndex){
							String to = tokens[toIndex];
							if ( !caseSensitve) {
								from =  from.toUpperCase();
								to = to.toUpperCase();
							}
								
							if ( ret.containsKey(from)) ambigious.add(from);
							ret.put(from,to);	
						}
						if ( !allColumns) break;
					}
					
				}
				br.close();
			}
			for(String a: ambigious){
				ret.remove(a);
			}
			
			return ret;
		}

		/**
		 * Do a case sensitive mapping (converts entries to upper case)
		 * @param caseSensitve
		 * @return
		 */
		public MappingFileReader caseSensetive(boolean caseSensitve){
			this.caseSensitve = caseSensitve;
			return this;
		}
		
		/**
		 * Skip header
		 * @param header
		 * @return MappingFileReader
		 */
		public MappingFileReader hasHeader(boolean header){
			this.header = header;
			return this;
		}
		/**
		 * Includes also all columns after the toIndex into the mapping
		 * @param allColumns
		 * @return
		 */
		public MappingFileReader includeAllColumnsAfterToIndex(boolean allColumns){
			this.allColumns = allColumns;
			return this;
		}
		
		public MappingFileReader setColumnSeperator(String seperator){
			this.seperator= seperator;
			return this;
		}
		
		/**
		 * Constructs a MappingFileReader for a list of input files (Default: tab separated and with no header information).  
		 * @param seperator -- the separator between columns
		 * @param fromIndex -- the from column
		 * @param toIndex -- the two column
		 * @param inputFiles -- a list of input files.
		 */
		public MappingFileReader(Integer fromIndex, Integer toIndex,File... inputFiles){
			if ( inputFiles.length == 0) throw new IllegalArgumentException("At least one input mapping file is required");
			
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
			this.inputFiles = inputFiles;
		}
	}

}
