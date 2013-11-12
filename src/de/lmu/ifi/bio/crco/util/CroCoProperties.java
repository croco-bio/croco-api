package de.lmu.ifi.bio.crco.util;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class CroCoProperties {
	private Properties props;
	
	private CroCoProperties() throws Exception{
		String file = "connet.config";
		InputStream stream = CroCoLogger.class.getClassLoader().getResourceAsStream(file);
		//System.out.println(w);
		
		if ( stream == null){
			throw new RuntimeException("Can not find:\t connet.config (" + file + ")");
		}
		props = new Properties();
		try {
			props.load(stream);
		} catch (Exception e) {
			CroCoLogger.getLogger().fatal(e.getMessage());
			throw new RuntimeException(e);
		} 
			
	};
	
	private static CroCoProperties instance;
	public static CroCoProperties getInstance() {
		if ( instance == null){
			try{
				instance = new CroCoProperties();
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		return instance;
	}
	
	public String getValue(String option){
		return props.getProperty(option);
	}

	public Set<String> getProperties(){
		return (Set) props.keySet();
	}

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
