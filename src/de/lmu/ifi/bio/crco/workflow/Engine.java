package de.lmu.ifi.bio.crco.workflow;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.reflections.Reflections;

import de.lmu.ifi.bio.crco.data.NetworkOperationNode;
import de.lmu.ifi.bio.crco.operation.GeneralOperation;
import de.lmu.ifi.bio.crco.operation.Parameter;
import de.lmu.ifi.bio.crco.operation.ParameterWrapper;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;

public class Engine {
	private HashMap<String,Class<? extends GeneralOperation>> generalOperationLookUp = null;
	private HashMap<Pair<Class<? extends GeneralOperation>,Parameter>,List<Pair<String,Method>>> parameterAlias= null;
	
	public Engine() throws Exception{
		generalOperationLookUp = new HashMap<String,Class<? extends GeneralOperation>>();
		parameterAlias = new  HashMap<Pair<Class<? extends GeneralOperation>,Parameter>,List<Pair<String,Method>>>();
		
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
					ParameterWrapper annotations = method.getAnnotation(ParameterWrapper.class);
					Pair<Class<? extends GeneralOperation>,Parameter> parameter = new Pair<Class<? extends GeneralOperation>,Parameter(operation,annotations.annotationType());
					
					for(String alias : annotations.alias() ) {
						
					}
				}
			}
			
			CroCoLogger.getLogger().info(String.format("Register operation %s with alias %s",operation.getSimpleName(),""));
			generalOperationLookUp.put(operation.getSimpleName(), operation);
		}
		CroCoLogger.getLogger().info(String.format("Found %d possible operation",generalOperationLookUp.size()));
	}
	public void parse() throws Exception{
		SAXReader reader = new SAXReader();
		File xmlFile = new File("/home/users/pesch/workspace/croco-api/data/workflow/MEL_K562_example.xml");
		Document document = reader.read(xmlFile);
		
		Element root = document.getRootElement();
		if ( root.elements("operation").size() !=1){
			throw new RuntimeException("Only one root operation allowed");
		}
		
		Element rootOperation = (Element) root.elements("operation").get(0);
		processOperation(rootOperation);
	}
	private Object getValue(GeneralOperation generalOperation, String name, String value){
		/*
		Reflections reflections = new Reflections(generalOperation.getClass().getName());
		Set<Method> methods = reflections.getMethodsAnnotatedWith(ParemterWrapper.class);
		System.out.println(generalOperation.getClass().getName() + " " + methods);
		*/
		
	
		for(Parameter parameter : generalOperation.getParameters()){
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
		}
		
		return null;
	}
	
	public NetworkOperationNode processOperation(Element operation){
		Attribute operationName = operation.attribute("name");
		
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
		
		for(Element child : (List<Element>)operation.elements()){
			if ( child.getName().equals("inputNetworks")){
				for(Element childOperation : (List<Element>) child.elements("operation")){
					 processOperation(childOperation);
				}
			
			}else if ( child.getName().equals("parameter")){
				getValue(generalOperation,child.getName(),child.attributeValue("name"));
				System.out.println(generalOperation + " " + child.attributeValue("name"));
			}else{
				throw new RuntimeException(String.format("Unknown element %s",child));
			}
		}
		
		NetworkOperationNode node = new NetworkOperationNode(null,null,generalOperation);
		
		
		return node;
	}
	
	public static void main(String[] args) throws Exception{
		/*HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("input").withArgName("FILE").withDescription("Input XML file").isRequired().hasArgs(1).create("input"));
		options.addOption(OptionBuilder.withLongOpt("repository").withArgName("ID").withDescription("Repository location (Web service url, or SQL connection string). Default http://services.bio.ifi.lmu.de/croco").hasArgs(1).create("repository"));
		options.addOption(OptionBuilder.withLongOpt("output").withArgName("FILE").withDescription("Final network").isRequired().hasArgs(1).create("output"));

		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + Engine.class.getName(), "", options, "", true);
			System.exit(1);
		}
		*/
		Engine engine = new Engine();
		engine.parse();
	}
	
	
}
