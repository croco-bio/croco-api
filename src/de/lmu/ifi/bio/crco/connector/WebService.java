package de.lmu.ifi.bio.crco.connector;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;

import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

public class WebService /*implements QueryService*/{

	
	
	public List<NetworkHierachyNode> listNetwork(Integer taxId) throws Exception {
		String urlString = String.format("http://localhost:8080/RegulatoryNetworkBrowser/services/getNetwork?taxId=%d", taxId);
		return listNetwork(urlString);
	}
	public Object performceOperation(String method, Object...parameters) throws Exception{
		String urlStr = String.format("http://localhost:8080/RegulatoryNetworkBrowser/services/method");
		
		CroCoLogger.getLogger().info(urlStr);
		URL url = new URL(urlStr);
		URLConnection conn = url.openConnection();
		OutputStream os = conn.getOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(os);
		for(Object parameter: parameters){
			out.writeObject(parameter);
		}
		out.close();
		InputStream is = conn.getInputStream();
		CroCoLogger.getLogger().debug("Got stream");

		XStream xstream = new XStream();
		xstream.createObjectInputStream(is);
		
		ObjectInputStream in = xstream.createObjectInputStream(is);
		List<NetworkHierachyNode> ret = new ArrayList<NetworkHierachyNode>();
		
		Object obj = null;
		int k = 0;
		CroCoLogger.getLogger().debug("Reading objects");
		while (true) {
			try {
				obj = in.readObject();
				System.out.println("Got:\t" + obj);
			} catch (java.io.EOFException e) {
				break;
			}

			if (k++ % 10000 == 0) {
				System.err.print(".");
			}

		}
		System.err.println("\n");
		CroCoLogger.getLogger().info(String.format("Read:%d objects",k));
		return ret;
	}
	
	private List<NetworkHierachyNode> listNetwork(String urlStr) throws Exception{
		CroCoLogger.getLogger().info(urlStr);
		URL url = new URL(urlStr);
		URLConnection conn = url.openConnection();

		InputStream is = conn.getInputStream();
		CroCoLogger.getLogger().debug("Got stream");

		XStream xstream = new XStream();
		xstream.createObjectInputStream(is);
		
		ObjectInputStream in = xstream.createObjectInputStream(is);
		List<NetworkHierachyNode> ret = new ArrayList<NetworkHierachyNode>();
		
		Object obj = null;
		int k = 0;
		CroCoLogger.getLogger().debug("Reading objects");
		while (true) {
			try {
				obj = in.readObject();
				if ( obj instanceof NetworkHierachyNode){
					ret.add((NetworkHierachyNode) obj);
				}else{
					throw new RuntimeException("Recieved unknown data type:\t"+ obj);
				}
			} catch (java.io.EOFException e) {
				break;
			}

			if (k++ % 10000 == 0) {
				System.err.print(".");
			}

		}
		System.err.println("\n");
		CroCoLogger.getLogger().info(String.format("Read:%d objects",k));
		return ret;
	}
	



}
