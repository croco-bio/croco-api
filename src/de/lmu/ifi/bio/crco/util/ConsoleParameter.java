package de.lmu.ifi.bio.crco.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.data.exceptions.CroCoException;

public class ConsoleParameter {
	private List<CroCoOption<?>> crocoOptions = new ArrayList<CroCoOption<?>> ();
	

	private CommandLine cmdLine = null;
	public CommandLine parseCommandLine(String[] args,Class<?> clazz) throws CroCoParameterException {
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		Options options = new Options();
		
		for(CroCoOption<?> crocooption : crocoOptions){
			options.addOption(crocooption.getOption());
		}
		
		try{
			cmdLine = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + clazz.getName(), "", options, "", true);
			System.exit(1);
		}
		return cmdLine;
	}
	public void register(CroCoOption<?> ... options){
		for(CroCoOption<?> option :  options){
			crocoOptions.add(option);
		}
	}
	
	public static class CroCoOption<E extends Object> {
	
		public CroCoOption<E> setName(String name) {
			this.name = name;
			return this;
		}

		public CroCoOption<E> setLongOpt(String longOpt) {
			this.longOpt = longOpt;
			return this;
		}

		public CroCoOption<E> setArgName(String argName) {
			this.argName = argName;
			return this;
		}

		public CroCoOption<E> setDescription(String description) {
			this.description = description;
			return this;
		}

		public CroCoOption<E> isRequired( ) {
			this.required = true;
			return this;
		}

		public CroCoOption<E> setArgs(int args) {
			this.args = args;
			return this;
		}
		public CroCoOption<E> setDefault(E value){
			this.defaultValue = value;
			return this;
		}

		private Option getOption(){
			Option option = new Option(name,longOpt);
			if ( required) option.setRequired(true);
			if (description != null ) option.setDescription(description);
			if ( argName != null) option.setArgName(argName);
			if ( args != null) option.setArgs(args);
			return option;
		}
		public CroCoOption(String name,Handler<E> handler){
			this.name = name;
			this.longOpt = name;
			this.handler = handler;
		}

		public E getValue(CommandLine cmdLine) throws CroCoParameterException{
			if (!cmdLine.hasOption(this.name) &&  defaultValue != null)
				return defaultValue;
			E ret = null;
			try{
				ret = handler.getValue(cmdLine,this.name);
			}catch(Exception e){
				throw new CroCoParameterException(e,String.format("Can not read parameter %s with handler %s",this.name,handler.getClass().getSimpleName()));
			}
			return ret;
		}
		
		private String name;
		private String longOpt;
		private String argName;
		private String description;
		private boolean required;
		private Integer args;
		
		private E defaultValue;
		private Handler<E> handler;

		public CroCoOption<E> hasArgs() {
			this.args = Option.UNLIMITED_VALUES;
			return this;
		}

		public CroCoOption<E> setArgs() {
			this.args = Option.UNLIMITED_VALUES;
			return this;
		}
		
		
	}
	public static class MapppingHandler extends Handler<HashMap<String,String>> {

		@Override
		public HashMap<String, String> getValue(CommandLine cmdLine, String parameterName) throws CroCoParameterException {
			HashMap<String,String> map = new HashMap<String,String>();
			if ( cmdLine.getOptionValue(parameterName) != null){
				for(String mapping : cmdLine.getOptionValues(parameterName)){
					String[] tokens = mapping.split("=");
					map.put(tokens[0], tokens[1]);
				}
			}
			return map;
		}
		
	}
	
	public static class FlagHandler extends Handler<Boolean>{

		@Override
		public Boolean getValue(CommandLine cmdLine, String parameterName) throws CroCoParameterException {
			return cmdLine.hasOption(parameterName);
		}
		
	}
	public static class FolderExistHandler extends Handler<File>{
		@Override
		public File getValue(CommandLine cmdLine,String parameterName) throws CroCoParameterException{
			File file = new File(cmdLine.getOptionValue(parameterName));
			if (! file.isDirectory()) throw new CroCoParameterException(new IOException(),String.format("%s folder does not exist",file.toString())); 
			return file;
		}
	}
	public static class FileExistHandler extends Handler<File>{
		@Override
		public File getValue(CommandLine cmdLine,String parameterName) throws CroCoParameterException{
			File file = new File(cmdLine.getOptionValue(parameterName));
			if (! file.exists()) throw new CroCoParameterException(new IOException(),String.format("%s file does not exist",file.toString())); 
			return file;
		}
	}
	public static class StringValueHandler extends Handler<String>{

		@Override
		public String getValue(CommandLine cmdLine,String parameterName) throws CroCoParameterException {
			return cmdLine.getOptionValue(parameterName);
		}
	}
	public static class IntegerValueHandler extends Handler<Integer>{

		@Override
		public Integer getValue(CommandLine cmdLine,String parameterName) throws CroCoParameterException {
			return Integer.valueOf(cmdLine.getOptionValue(parameterName));
		}
	}
	public static class FloatValueHandler extends Handler<Float>{

		@Override
		public Float getValue(CommandLine cmdLine,String parameterName) throws CroCoParameterException {
			return Float.valueOf(cmdLine.getOptionValue(parameterName));
		}
	}
	public static class FileListExistHandler extends Handler<List<File>>{

		@Override
		public List<File> getValue(CommandLine cmdLine,String parameterName) throws CroCoParameterException {
			List<File> files = new ArrayList<File>();
			for(String fileName : cmdLine.getOptionValues(parameterName)){
				File file  = new File(fileName);
				if (! file.exists()) throw new CroCoParameterException(new IOException(),String.format("%s file does not exist",file.toString())); 
				files.add(file);
			}
			return files;
		}
		
	}
	
	static class  CroCoParameterException extends CroCoException{
		private static final long serialVersionUID = 1L;
		
		public CroCoParameterException(Exception e,String msg){
			super(msg,e);
		}
		public CroCoParameterException(String msg){
			super(msg);
		}
		
	}
	abstract static class Handler<E>{
		abstract public E getValue(CommandLine cmdLine,String parameterName) throws CroCoParameterException;
	}

	//list of commonly used options
	public static CroCoOption<List<File>> experimentMappingFiles  =new CroCoOption<List<File>>("experimentMappingFiles",new FileListExistHandler()).setArgName("FILE").setDescription("Experiment description files").isRequired().setArgs();
	public static CroCoOption<File> experimentMappingFile  =new CroCoOption<File>("experimentMappingFile",new FileExistHandler()).setArgName("FILE").setDescription("Experiment description file").isRequired().setArgs(1);
	public static CroCoOption<Integer> taxId  =new CroCoOption<Integer>("taxId",new IntegerValueHandler()).isRequired().setArgs(1);
	public static CroCoOption<Boolean> test = new CroCoOption<Boolean>("test", new FlagHandler()).setDescription("Test flag");
	public static CroCoOption<List<File>> tfbsFiles = new CroCoOption<List<File>>("tfbsFiles",new FileListExistHandler()).setArgName("FILES").isRequired().hasArgs();
	public static CroCoOption<File> tfbsRegion = new CroCoOption<File>("tfbsRegion",new FileExistHandler()).setArgName("FILE").isRequired().setArgs(1);
	public static CroCoOption<Float> pValueCutOf = new CroCoOption<Float>("pValueCutOf",new FloatValueHandler()).setArgName("FLOAT").isRequired().setArgs(1);
	public static CroCoOption<List<File>> motifMappingFiles = new CroCoOption<List<File>>("motifMappingFiles",new FileListExistHandler()).setArgName("FILES").isRequired().hasArgs();
	public static CroCoOption<File> repositoryDir = new CroCoOption<File>("repositoryDir",new FolderExistHandler()).setArgName("FILE").isRequired().setArgs(1);
	public static CroCoOption<String> compositeName = new CroCoOption<String>("compositeName",new StringValueHandler()).setArgName("NAME").isRequired().setArgs(1);
	public static CroCoOption<String> motifSetName = new CroCoOption<String>("motifSetName",new StringValueHandler()).setArgName("NAME").isRequired().setArgs(1);
	public static CroCoOption<File> gtf = new CroCoOption<File>("gtf",new FileExistHandler()).setArgName("FILE").isRequired().setArgs(1);
	public static CroCoOption<HashMap<String,String>> chromosomNameMappings = new CroCoOption<HashMap<String,String>>("chromosomNameMappings",new MapppingHandler()).setArgName("MAPPING").setDescription("reference=synonym").setArgs();
	public static CroCoOption<String> chromosomNamePrefix = new CroCoOption<String>("chromosomNamePrefix",new StringValueHandler()).setArgName("NAME").setArgs(1);

	public static CroCoOption<Integer> downstream  =new CroCoOption<Integer>("downstream",new IntegerValueHandler()).isRequired().setArgs(1);
	public static CroCoOption<Integer> upstream  =new CroCoOption<Integer>("upstream",new IntegerValueHandler()).isRequired().setArgs(1);
	
}
