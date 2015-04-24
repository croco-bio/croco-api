package de.lmu.ifi.bio.croco.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;


public class CroCoProperties {
	private Properties props;
	private static String configFileName = "croco.config";
	
	public Properties getProperties(){
		return props;
	}
	
	private CroCoProperties(InputStream stream) throws IOException{
		props = new Properties();
		props.load(stream);
	}
	
	private static CroCoProperties instance;
	
	public static void init(InputStream is) throws IOException{
		instance = new CroCoProperties(is);
	}
	public static void init(File file) throws IOException{
		instance = new CroCoProperties(new FileInputStream(file));
	}
	
	public static CroCoProperties getInstance() throws IOException{
		if ( instance == null){
		    System.out.println("Open: resources/" + configFileName);
		    InputStream stream = CroCoLogger.class.getClassLoader().getResourceAsStream("resources/" + configFileName);
			if ( stream == null){ //last try
			    System.out.println("Failed. Typ to open ./" + configFileName);
	                
				File file = new File(configFileName);
				if ( file.exists()){
					stream = new FileInputStream(file);
				}
				// stream = CroCoLogger.class.getClassLoader().getResourceAsStream("conf/connet.config");
			}
			if ( stream == null){
				throw new IOException("Can not find croco-configuration file:"+configFileName);
			}

			instance = new CroCoProperties(stream);

		}
		return instance;
	}
	
	public String getValue(String option){
		return props.getProperty(option);
	}

	public Set<String> getProperties(String prefix) {
		HashSet<String> ret = new HashSet<String>();
		for(Object o : props.keySet()){
			if ( ((String)o).startsWith(prefix)){
				ret.add((String)o);
			}
		}
		return ret;
		
	}
}
