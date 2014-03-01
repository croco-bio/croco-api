package de.lmu.ifi.bio.crco.workflow;

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
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.reflections.Reflections;

import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.NetworkOperationNode;
import de.lmu.ifi.bio.crco.operation.GeneralOperation;
import de.lmu.ifi.bio.crco.operation.OperationUtil;
import de.lmu.ifi.bio.crco.operation.Parameter;
import de.lmu.ifi.bio.crco.operation.ParameterWrapper;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;

public class Engine {
	public File getFile(String path){
		return new File(path);
	}
	
	private HashMap<String,Class<? extends GeneralOperation>> generalOperationLookUp = null;
	private HashMap<Class<? extends GeneralOperation>,HashMap<String,List<Method>>> parameterLookUp = null;
	private HashMap<Pair<Class<? extends GeneralOperation>,String>,Method> parameterAlias= null;
	private QueryService service;
	
	public Engine() throws Exception{
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
					//annotations.parameter()
					//System.out.println(annotations.parameter());
					
				//	Pair<Class<? extends GeneralOperation>,Parameter> parameter = new Pair<Class<? extends GeneralOperation>,Parameter(operation,annotations.annotationType());
					
				//	for(String alias : annotations.alias() ) {
				//		System.out.println(alias);
				//	}
				}
			}
			
			CroCoLogger.getLogger().info(String.format("Register operation %s with alias %s",operation.getSimpleName(),""));
			generalOperationLookUp.put(operation.getSimpleName(), operation);
		}
		CroCoLogger.getLogger().info(String.format("Found %d possible operation",generalOperationLookUp.size()));
	}
	public void parse(File xmlFile) throws Exception{
		SAXReader reader = new SAXReader();
		Document document = reader.read(xmlFile);
		
		Element root = document.getRootElement();
		if ( root.elements("operation").size() !=1){
			throw new RuntimeException("Only one root operation allowed");
		}
		
		Element rootOperation = (Element) root.elements("operation").get(0);
		
		service = new LocalService();
		NetworkOperationNode rootNode = processOperation(rootOperation);
		OperationUtil.process(service, rootNode);
	}
	private Object getValue(GeneralOperation generalOperation, String name, String value){
		/*
		Reflections reflections = new Reflections(generalOperation.getClass().getName());
		Set<Method> methods = reflections.getMethodsAnnotatedWith(ParemterWrapper.class);
		System.out.println(generalOperation.getClass().getName() + " " + methods);
		*/
		
	/*
		for(Parameter<?> parameter : generalOperation.getParameters()){
			if ( parameter.getName().equals(name)){ //no wrapper is needed
				if ( parameter.getClazz().equals(Integer.class)){
					return Integer.valueOf(value);
				}else if ( parameter.getClazz().equals(Float.class)){
					return Float.valueOf(value);
				}else if ( parameter.getClazz().equals(Double.class)){
					return Double.valueOf(value);
				}else if ( parameter.getClazz().equals(String.class)){
					return value;
				}else{
					throw new RuntimeException(String.format("Can not cast value %s to %s for parameter %s. Use a parameter alias.",value,parameter.getClazz().toString()));
				}
			}

			/*
			if ( parameter.getAlias().equals(name)){
				Wrapper wrapper = null;
				try{
					parameter.getWrapper().getClass().newInstance();
				}catch(Exception e){
					throw new RuntimeException(
							String.format("Parameter %s can not be procssed by operation %d, due to initilization issues for wrapper",
									name,generalOperation.toString(),parameter.getWrapper().toString()
									),e
								);
				}
			}*/
		
		
		return null;
	}
  
	public NetworkOperationNode processOperation(Element operation) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException, IOException{
		Attribute operationName = operation.attribute("name");
		CroCoLogger.getLogger().debug(String.format("Init %s",operationName.getValue()));
		
		if( !generalOperationLookUp.containsKey(operationName.getValue())){
			throw new RuntimeException(String.format("Unknown operation %s",operationName.getName()));
		}
		Class<? extends GeneralOperation> generalOperationClass =  generalOperationLookUp.get(operationName.getValue());
		GeneralOperation generalOperation =null;
		try{
			generalOperation =generalOperationClass.newInstance();
		}catch(Exception e){
			throw new RuntimeException(String.format("Can not initialize %s",operationName.getValue()),e);
		}
		NetworkOperationNode node = new NetworkOperationNode(null,-1,generalOperation);
		
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
				method.invoke(generalOperation,value);
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

		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + Engine.class.getName(), "", options, "", true);
			System.exit(1);
		}
		File xmlFile =new File(line.getOptionValue("input"));
		if (! xmlFile.exists()){
			CroCoLogger.getLogger().fatal(String.format("input XML file %s does not exist",xmlFile.toString()));
			System.exit(1);
		}
		
		Engine engine = new Engine();
		engine.parse(xmlFile);
		
		CroCoLogger.getLogger().info("XML file processed");
	}
	
	
}
