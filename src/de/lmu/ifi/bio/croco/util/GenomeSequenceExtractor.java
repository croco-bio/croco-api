package de.lmu.ifi.bio.croco.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class GenomeSequenceExtractor {
	class FastaInfo{
		private File file;
		private String chrom;
		private Integer headerOffset;
		private Integer lineWidth;
		private RandomAccessFile reader;
		
		public FastaInfo(File file, Integer headerOffset,Integer start, Integer lineWidth, String chrom) {
			super();
			
			this.file = file;
			this.chrom = chrom;
		//	this.start = start;
		//	this.end = end;
			this.headerOffset = headerOffset;
			this.lineWidth = lineWidth;
			try{
				this.reader =  new RandomAccessFile(file,"r");
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		
		
	}
	private static Pattern fileNamePattern = Pattern.compile(".+dna.chromosome.([^\\.]+).fa");
	
//	>1 dna:chromosome chromosome:GRCh37:1:1:249250621:1
	private File baseDir;
	private HashMap<String,FastaInfo> infos;
	public GenomeSequenceExtractor(File baseDir) throws Exception{
		this.baseDir = baseDir;
		infos= new HashMap<String,FastaInfo>();
		init();
	}
	public Set<String> getChromsoms(){
		return infos.keySet();
	}
	
	public String getDNASequence(String chrom, Integer start, Integer end) throws IOException{
		String ret = null;
		
		FastaInfo fastaInfo = infos.get(chrom);
		if ( fastaInfo == null){
			throw new RuntimeException("No file for chrom:" + chrom);
		}
		if ( start > end) {
			throw new RuntimeException("start > end");
		}
		
		start=start-1;
		end=end-1;
		
		RandomAccessFile reader = fastaInfo.reader;
		
		int numberLines = start/fastaInfo.lineWidth;
		int correctedStart = fastaInfo.headerOffset+numberLines+start;
		numberLines = end/fastaInfo.lineWidth;
		int corredtedEnd = fastaInfo.headerOffset+numberLines+end;
		
		int corredtedLength = corredtedEnd-correctedStart;
		
		
		byte[] buffer = new byte[corredtedLength];
		reader.seek(correctedStart);
		int read=reader.read(buffer);
		reader.seek(correctedStart);
		
		ret = new String(buffer,0,read).replace("\n", "");
		
		return ret;
	}
	private Pattern headerPattern = Pattern.compile("chromosome:[^:]+:[^:]+:(\\d+):(\\d+):(\\d+)");
	
	private void init() throws Exception{
		for(File file : baseDir.listFiles()){
			CroCoLogger.getLogger().info(String.format("Register file: %s", file.toString()));
			Matcher matcher = fileNamePattern.matcher(file.getName());	
			if (! matcher.matches()){
				continue;
			}
			String chrom = matcher.group(1);
			
			BufferedReader br = new BufferedReader(new FileReader(file));
			String header = br.readLine();
			if ( header == null) {
				CroCoLogger.getLogger().warn("No header (file empty?");
				continue;
			}
			int headerOffset = header.length()+1; //+1 because of line break
			String firstLine = br.readLine();
			
			br.close();
			matcher = headerPattern.matcher(header);
			Integer start = 0;
			if ( matcher.find()){
				start =Integer.valueOf(matcher.group(1))-1;
				//Integer end = Integer.valueOf(matcher.group(2));
			}else{
				CroCoLogger.getLogger().warn("Strange header line:" + header);
			}
			
			FastaInfo info = new FastaInfo(file,headerOffset,start,firstLine.length(),chrom);
			infos.put(chrom, info);
		
		}
	}
}
