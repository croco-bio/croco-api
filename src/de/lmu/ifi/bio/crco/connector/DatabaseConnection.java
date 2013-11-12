package de.lmu.ifi.bio.crco.connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import de.lmu.ifi.bio.crco.util.CroCoLogger;


public class DatabaseConnection {
	private static Connection connection;
	
	private static long MAX_SQL_TIMEOUT=60*60*1000;
	private static long last_connection_retrieved = 0;
	
	private DatabaseConnection(){}
	
	public static Connection getConnection(String mysqlDriver, String username, String password, String connectionString) {
		Properties props = new Properties();
		
		props.put("driver", mysqlDriver);
		props.put("user", username);
		props.put("password", password);
		props.put("autoReconnect", "true");
		props.put("connectionStr",connectionString);
		
		return getConnection(props);
	}
	public static Connection getConnection(Properties props) {
		
		long current = (new java.util.Date()).getTime();
		try{
			if (connection!= null &&  current >=last_connection_retrieved+MAX_SQL_TIMEOUT ){
				CroCoLogger.getLogger().info("Closing out-timed connection");
				connection.close();
			}
		
			if ( connection == null || connection.isClosed()  ){
				
				
				try{
					CroCoLogger.getLogger().debug(String.format("Loading driver:%s",props.getProperty("driver")));
					Class.forName(props.getProperty("driver"));
					
				}catch(ClassNotFoundException e){
					throw new RuntimeException("Can not load mysql driver:" + props.getProperty("driver") + ")");
				}
			
				Properties tmp = props;
				//tmp.put("user", "anonymous");
				CroCoLogger.getLogger().info("Create new database connection to:\t"  + props.getProperty("connectionStr"));
				connection = DriverManager.getConnection((String) props.get("connectionStr"),tmp);
				last_connection_retrieved = current;
			}
		}catch(Exception e){
			e.printStackTrace();
			CroCoLogger.getLogger().fatal(e.getMessage());
			throw new RuntimeException(e);
		}
		
		return connection;
	}
	
	public static Connection getConnection(InputStream configFile) {
		Properties props = new Properties();
		try {
			props.load(configFile);
		} catch (Exception e) {
			CroCoLogger.getLogger().fatal(e.getMessage());
			throw new RuntimeException(e);
		} 
		PropertyConfigurator.configure(props);
		
		props.put("driver", props.get("util.DatabaseConnection.dbdriver"));
		props.put("user", props.get("util.DatabaseConnection.user"));
		props.put("password", props.get("util.DatabaseConnection.password"));
		props.put("autoReconnect", props.get("util.DatabaseConnection.autoReconnect"));
		props.put("connectionStr", props.get("util.DatabaseConnection.dbconstr"));
		
		
		return getConnection(props);
		
	}
	/**
	 * Tries to connect using the connet.config file
	 * @return a database connection
	 * @throws SQLException
	 */
	public static Connection getConnection() {
		String file = "connet.config";
		InputStream stream = CroCoLogger.class.getClassLoader().getResourceAsStream(file);
		//System.out.println(w);
		
		if ( stream == null){
			throw new RuntimeException("Can not find:\t connet.config (" + file + ")");
		}
			
		return getConnection(stream);
		
	}
}
