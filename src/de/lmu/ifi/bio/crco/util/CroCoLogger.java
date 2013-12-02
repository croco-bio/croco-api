package de.lmu.ifi.bio.crco.util;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class CroCoLogger {
	
	private static Logger logger;
	private CroCoLogger(){}
	
	public static Logger getLogger()  {
		if ( logger == null){
			Properties props  = null;
			try{
				props = CroCoProperties.getInstance().getProperties();
			}catch(IOException e){
				throw new RuntimeException("Cannot read croco config file\n",e);
			}
			PropertyConfigurator.configure(props);
			logger = Logger.getRootLogger();
			
			logger.info("Logger started");
			
		}
		return logger;
	}


}
