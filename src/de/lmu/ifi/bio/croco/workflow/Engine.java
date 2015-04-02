package de.lmu.ifi.bio.croco.workflow;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.reflections.Reflections;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.lmu.ifi.bio.croco.connector.BufferedService;
import de.lmu.ifi.bio.croco.connector.QueryService;
import de.lmu.ifi.bio.croco.connector.RemoteWebService;
import de.lmu.ifi.bio.croco.data.NetworkOperationNode;
import de.lmu.ifi.bio.croco.operation.GeneralOperation;
import de.lmu.ifi.bio.croco.operation.OperationUtil;
import de.lmu.ifi.bio.croco.operation.Parameter;
import de.lmu.ifi.bio.croco.operation.ParameterWrapper;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.Pair;

/**
 * A workflow engine prototype.
 * @author pesch
 *
 */
public class Engine {
	private static String url ="http://services.bio.ifi.lmu.de/croco/service";
	private QueryService service = null;
	
	public File getFile(String path){
		return new File(path);
	}
	
	private HashMap<String,Class<? extends GeneralOperation>> generalOperationLookUp = null;
	private HashMap<Pair<Class<? extends GeneralOperation>,String>,Method> parameterAlias= null;
	
	public Engine(QueryService service) throws Exception{
		this.service = service;
		generalOperationLookUp = new HashMap<String,Class<? extends GeneralOperation>>();
		parameterAlias = new  HashMap<Pair<Class<? extends GeneralOperation>,String>,Method> ();
		
		Reflections reflections = new Reflections("");
		
		Set<Class<? extends GeneralOperation>> subTypes = 
	               reflections.getSubTypesOf(GeneralOperation.class);
	     
		for(Class<? extends GeneralOperation> operation : subTypes){
			boolean isAbstrat = Modifier.isAbstract(operation.getModifiers());
			if ( isAbstrat){
				CroCoLogger.getLogger().debug(String.format("Skip %s (is abstract)", operation));
				continue;
			}
			try {
				operation.getDeclaredConstructor() ;
			}catch(NoSuchMethodException exception){
				CroCoLogger.getLogger().debug(String.format("Skip %s (no none-parameter constructor)", operation));
				continue;
			}
			
			
			for(Method method : operation.getMethods()){
				if ( method.isAnnotationPresent(ParameterWrapper.class))  {
					ParameterWrapper annotation = method.getAnnotation(ParameterWrapper.class);
					parameterAlias.put(new Pair<Class<? extends GeneralOperation>,String>(operation,annotation.alias()), method);
				}
			}
			
			CroCoLogger.getLogger().info(String.format("Register operation %s with alias %s",operation.getSimpleName(),""));
			generalOperationLookUp.put(operation.getSimpleName(), operation);
		}
		CroCoLogger.getLogger().info(String.format("Found %d possible operation",generalOperationLookUp.size()));
	}
	public void parse(File xmlFile,boolean validation) throws Exception{
		SAXReader reader = new SAXReader();
		reader.setValidation(validation);
		Document document = null;
		try{
			document = reader.read(xmlFile);
		}catch(DocumentException e){
			CroCoLogger.getLogger().fatal(e.getMessage());
			CroCoLogger.getLogger().debug("Stacktrace:",e);
			throw new RuntimeException(e.getMessage());
		}
		Element root = document.getRootElement();
		if ( root.elements("operation").size() !=1){
			throw new RuntimeException("Only one root operation allowed");
		}
		
		Element rootOperation = (Element) root.elements("operation").get(0);
		
		NetworkOperationNode rootNode = processOperation(rootOperation);
		OperationUtil.process(service, rootNode);
	}

	public NetworkOperationNode processOperation(Element operation) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,  SQLException, IOException{
		Attribute operationName = operation.attribute("name");
		CroCoLogger.getLogger().debug(String.format("Init %s",operationName.getValue()));
		
		if( !generalOperationLookUp.containsKey(operationName.getValue())){
			throw new RuntimeException(String.format("Unknown operation %s:",operationName.getValue()));
		}
		Class<? extends GeneralOperation> generalOperationClass =  generalOperationLookUp.get(operationName.getValue());
		GeneralOperation generalOperation =null;
		try{
			generalOperation =generalOperationClass.newInstance();
		}catch(Exception e){
			throw new RuntimeException(String.format("Can not initialize %s",operationName.getValue()),e);
		}
		NetworkOperationNode node = new NetworkOperationNode(null,-1,generalOperation);
		if  ( generalOperation.getParameters() != null){
			for(Parameter<?> paremter : generalOperation.getParameters()){
		
				if ( paremter.getName().equals("QueryService")) {
					CroCoLogger.getLogger().debug("Set query service for"  + generalOperation);
					Parameter<QueryService> p =(Parameter<QueryService>) paremter;
					generalOperation.setInput(p,service);
				}
				if ( paremter.getName().equals("OrthologRepository")) {
					CroCoLogger.getLogger().debug("Set query service for"  + generalOperation);
					Parameter<OrthologRepository> p =(Parameter<OrthologRepository>) paremter;
					generalOperation.setInput(p,OrthologRepository.getInstance(service));
				}
				
			}
		}
		for(Element child : (List<Element>)operation.elements()){
			if ( child.getName().equals("inputNetworks")){
				for(Element childOperation : (List<Element>) child.elements("operation")){
					 node.addChild(processOperation(childOperation));
				}
			
			}else if ( child.getName().equals("parameter")){
				//getValue(generalOperation,child.getName(),child.attributeValue("name"));
				String methodName=child.attributeValue("name");
				String value = child.getData().toString();
				Method method = parameterAlias.get(new Pair<Class<? extends GeneralOperation>,String>(generalOperationClass,methodName));
				if ( method == null){
				
					throw new RuntimeException("Unknown parameter name "+ methodName);
				}
				CroCoLogger.getLogger().debug(String.format("Set %s with %s on %s",method,value,generalOperation.getClass().getSimpleName()));
				try{
					method.invoke(generalOperation,value);
				}catch(InvocationTargetException e){
					CroCoLogger.getLogger().fatal("Could not perform operation because of " + e.getCause().getMessage()) ;
					CroCoLogger.getLogger().debug("Stacktrace", e);
					throw new RuntimeException(e.getCause().getMessage());
				}
			//	System.out.println(generalOperation + " " + child.attributeValue("name"));
			}else{
				throw new RuntimeException(String.format("Unknown element %s",child));
			}
		}
		
		
		
		return node;
	}
	
	public static void main(String[] args) throws Exception{
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("input").withArgName("FILE").withDescription("Input XML file").isRequired().hasArgs(1).create("input"));
		options.addOption(OptionBuilder.withLongOpt("url").withArgName("URL").withDescription("Service URL (default http://services.bio.ifi.lmu.de/croco/services)").hasArgs(1).create("url"));
		options.addOption(OptionBuilder.withLongOpt("bufferDir").withArgName("DIR").withDescription("Buffer dir (default ./croco_data)").hasArgs(1).create("bufferDir"));
		options.addOption(OptionBuilder.withLongOpt("noDTDValidation").withDescription("Distable workflow DTD validation").create("noDTDValidation"));
		
		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + Engine.class.getName(), "", options, "", true);
			System.exit(1);
		}
		String remoteUrl = url;
		if ( line.hasOption("url")){
			remoteUrl = line.getOptionValue("url");
		}
		File tmpDir = new File("croco_data");
		if ( line.hasOption("tmpDir")){
			tmpDir = new File(line.getOptionValue("tmpDir"));
		}
		Boolean validate = !line.hasOption("noDTDValidation");
		if ( !tmpDir.mkdir() || ! tmpDir.isDirectory()){
			CroCoLogger.getLogger().info("Cannot create buffer dir (" + tmpDir + ")");
		}
		CroCoLogger.getLogger().info("Temp dir:" + tmpDir);
		try{
			Long version = RemoteWebService.getServiceVersion(remoteUrl);
			if( !version.equals(QueryService.version)){
				CroCoLogger.getLogger().fatal(String.format("Local and remote API are incompactible. Update your API (local version %f; remote version %f.",QueryService.version,version));
				System.exit(1);
			}
		}catch(IOException e){
			CroCoLogger.getLogger().fatal(String.format("Cannot connect to %s croco-repo",remoteUrl));
			CroCoLogger.getLogger().debug("Stacktrace:",e);
			System.exit(1);	
		}
		
		RemoteWebService service = new RemoteWebService(remoteUrl);
		BufferedService bwService = new BufferedService(service,tmpDir);
		
		File xmlFile =new File(line.getOptionValue("input"));
		if (! xmlFile.exists()){
			CroCoLogger.getLogger().fatal(String.format("input XML file %s does not exist",xmlFile.toString()));
			System.exit(1);
		}
		
		Engine engine = new Engine(bwService);
		engine.parse(xmlFile,validate);
		
		CroCoLogger.getLogger().info("Workflow finished.");
	}
	
	
}
