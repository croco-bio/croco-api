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

/**
 * Collection of commonly used methods to read files
 * @author pesch
 *
 */
public class FileUtil {
	/**
	 * Creates a column look-up for a file with a given header. Allows to access each column of each line with its defined column name.
	 * For example:
	 * <pre>
	 * FileUtil.createLookupBasedOnHeader(inFile,"\t").get(0).get("cName")
	 * </pre>
	 * returns the value for the column with the name cName for the first row in the file inFile.
	 * @param file
	 * @param the column separator e.g. \t
	 * @return list of look-ups for each line
	 * @throws IOException
	 */
	public static List<HashMap<String,String>> createLookupBasedOnHeader(File file, String seperator) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(file));
		String currentLine = br.readLine();
		br.close();
		
		return readSeperatedFile(Arrays.asList(currentLine.split(seperator)),file,seperator,true);
	}
	/**
	 *  Creates a column look-up based on the defined column names for each line in a file.
	 * @param columnNames
	 * @param file
	 * @param seperator
	 * @return
	 * @throws IOException
	 */
	public static List<HashMap<String,String>> createLookup(List<String> columnNames, File file, String seperator) throws IOException{
		return readSeperatedFile(columnNames,file,seperator,false);
	}
	
	private static List<HashMap<String,String>> readSeperatedFile(List<String> columnNames, File file, String seperator, boolean skipFirstLine) throws IOException{
		List<HashMap<String,String>> entries = new ArrayList<HashMap<String,String>> ();
		BufferedReader br = new BufferedReader(new FileReader(file));
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
	
	
	/**
	 * MappingFileReader implements the Build pattern. 
	 * @author rpesch
	 *
	 */
	public static class MappingFileReader {
		private File[] inputFiles;
		private String seperator;
		private boolean caseSensitve;
		private boolean header;
		private Integer fromIndex;
		private Integer toIndex;
		private boolean allColumns;
		
		
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
		 * Reads a mapping file and creates a look-up for the from column to the to column  
		 * @param file - mapping file
		 * @param seperator - column separator
		 * @param fromIndex - column index used as key in the look-up
		 * @param toIndex - column index used as value in the look-up (null indicates to take all indices expect the fromIndex)
		 * @param header - indicator whether the first line the mapping file is a just a header
		 * @param caseSensitve 
		 * @param allColumns - when true creates mapping between fromIndex - > toIndex; ... ; fromIndex -> <number of columns>
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
		/*
		public MappingFileReader setSeperator(String seperator){
			this.seperator = seperator;
			return this;
		}
		*/
		public MappingFileReader caseSensetive(boolean caseSensitve){
			this.caseSensitve = caseSensitve;
			return this;
		}
		
		public MappingFileReader hasHeader(boolean header){
			this.header = header;
			return this;
		}
		public MappingFileReader includeAllColumnsAfterToIndex(boolean allColumns){
			this.allColumns = allColumns;
			return this;
		}
		

		public MappingFileReader(String seperator, Integer fromIndex, Integer toIndex,File... inputFiles){
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
			this.seperator = seperator;
			this.inputFiles = inputFiles;
		}
	}

}
