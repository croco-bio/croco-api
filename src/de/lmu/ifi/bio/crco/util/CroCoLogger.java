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
			boolean error = false;
			try{
				Properties props = CroCoProperties.getInstance().getProperties();
				PropertyConfigurator.configure(props);
			}catch(IOException e){
				error = true;
			}
			logger = Logger.getRootLogger();
			if ( error) logger.fatal("Cannot read croco config file");
			logger.info("Logger started");
			
		}
		return logger;
	}


}
