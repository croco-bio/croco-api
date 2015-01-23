package de.lmu.ifi.bio.crco.connector;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.CroCoProperties;

/**
 * The database connection. The class provides only the static method <code>getConnection<code> which
 * hold a global  <code>java.sql.Connection</code> object.
 * 
 * @author pesch
 *
 */
public class DatabaseConnection {
	private static Connection connection;
	
	private static long MAX_SQL_TIMEOUT=60*60*1000;
	private static long last_connection_retrieved = 0;
	
	private DatabaseConnection(){}
	
	/**
	 * Returns the connection using the default connet.config config
	 * @return the connection
	 * @throws SQLException when the connection can not be established.
	 * @throws IOException when the connet.config can not be read
	 */
	public static Connection getConnection() throws SQLException, IOException {
		return getConnection(CroCoProperties.getInstance().getProperties());
	}

	public static Connection getConnection(Properties props)  throws SQLException{
		
		long current = (new java.util.Date()).getTime();
		
		if (connection!= null &&  current >=last_connection_retrieved+MAX_SQL_TIMEOUT ){
			CroCoLogger.getLogger().info("Closing out-timed connection");
			connection.close();
		}

		if ( connection == null || connection.isClosed()  ){


			props.put("driver", props.get("util.DatabaseConnection.dbdriver"));
			props.put("user", props.get("util.DatabaseConnection.user"));
			props.put("password", props.get("util.DatabaseConnection.password"));
			props.put("autoReconnect", props.get("util.DatabaseConnection.autoReconnect"));
			props.put("connectionStr", props.get("util.DatabaseConnection.dbconstr"));

			try{
				CroCoLogger.getLogger().debug(String.format("Loading driver:%s",props.getProperty("driver")));
				Class.forName(props.getProperty("driver"));

			}catch(ClassNotFoundException e){
				throw new RuntimeException("Can not load mysql driver:" + props.getProperty("driver") + ")");
			}

			Properties tmp = props;
			//tmp.put("user", "anonymous");
			CroCoLogger.getLogger().info("Create new database connection to:\t"  + props.getProperty("connectionStr"));
			try{
			    connection = DriverManager.getConnection((String) props.get("connectionStr"),tmp);
			}catch(SQLException conErr)
			{
			    CroCoLogger.getLogger().error("Could not connect", conErr);
			    throw new RuntimeException(conErr);
			}
			last_connection_retrieved = current;
		}
		
		
		return connection;
	}

}
