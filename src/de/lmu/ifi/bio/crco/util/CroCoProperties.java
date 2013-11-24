package de.lmu.ifi.bio.crco.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class CroCoProperties {
	private Properties props;

	public Properties getProperties(){
		return props;
	}
	
	private CroCoProperties(InputStream stream) throws Exception{
	
	
		props = new Properties();
		try {
			props.load(stream);
		} catch (Exception e) {
			CroCoLogger.getLogger().fatal(e.getMessage());
			throw new RuntimeException(e);
		} 
			
	};
	
	private static CroCoProperties instance;
	
	
	public static void init(File file){
		try{
			instance = new CroCoProperties(new FileInputStream(file));
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static CroCoProperties getInstance() {
		if ( instance == null){
			try{
				InputStream stream = CroCoLogger.class.getClassLoader().getResourceAsStream("connet.config");
				
				if ( stream == null){ //last try
					File file = new File("conf/connet.config");
					if ( file.exists()){
						stream = new FileInputStream(file);
					}
					// stream = CroCoLogger.class.getClassLoader().getResourceAsStream("conf/connet.config");
				}
				if ( stream == null){
					throw new RuntimeException("Can not find:\t connet.config");
				}
				
				instance = new CroCoProperties(stream);
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		return instance;
	}
	
	public String getValue(String option){
		return props.getProperty(option);
	}
/**
	public Set<String> getProperties(){
		return (Set) props.keySet();
	}
*/
	public Set<String> getProperties(String prefix) {
		HashSet<String> ret = new HashSet<String>();
		for(Object o : props.keySet()){
			//System.out.println(o);
			if ( ((String)o).startsWith(prefix)){
				ret.add((String)o);
			}
		}
		return ret;
		
	}
}
