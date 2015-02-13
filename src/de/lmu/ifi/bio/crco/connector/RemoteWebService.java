package de.lmu.ifi.bio.crco.connector;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.CroCoNode;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.data.exceptions.CroCoException;
import de.lmu.ifi.bio.crco.data.genome.Gene;
import de.lmu.ifi.bio.crco.intervaltree.peaks.DNaseTFBSPeak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.Peak;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.BindingEnrichedDirectedNetwork;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;

/**
 * Uses a web services to query the network repository.
 * @author pesch
 *
 */
public class RemoteWebService implements QueryService{
	private String baseUrl;
	private boolean debug = false;
	public RemoteWebService(String baseUrl){
		this.baseUrl = baseUrl;
	
	}
	public void enableDebugging(){
		this.debug = true;
	}
	public static Long getServiceVersion(String baseUrl) throws IOException{
		return (Long) performeOperation(false,baseUrl,"getVersion");
		
	}
	
	/**
	 * Dummy object for null values;
	 * @author pesch
	 *
	 */
	public static class NullObject{}
	
	public static InputStream getStreamedData(String baseUrl, String method, Object...parameters) throws Exception{
		URL url = new URL(String.format("%s/%s",baseUrl  , method));
		
		CroCoLogger.getLogger().debug(String.format("Query remote method %s",method));
		
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		
		XStream xstream = new XStream();
	    xstream.setMode(XStream.ID_REFERENCES);
	        
		
		ObjectOutputStream out = xstream.createObjectOutputStream(new OutputStreamWriter(conn.getOutputStream()));
		for(Object parameter: parameters){
			out.writeObject(parameter);
		}
		out.close();
		
		return conn.getInputStream();
	}
	
	private static Object performeOperation(String baseUrl, String method, Object...parameters) throws IOException{
		return performeOperation(false,baseUrl,method,parameters);
	}
	private static Object performeOperation(Boolean debug, String baseUrl, String method, Object...parameters) throws IOException{
		URL url = new URL(String.format("%s/%s",baseUrl  , method));
		
		CroCoLogger.getLogger().debug(String.format("Query remote url %s",url));
		
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		
		XStream xstream = new XStream();
        xstream.setMode(XStream.ID_REFERENCES);
        
		
		
		ObjectOutputStream out = xstream.createObjectOutputStream(new OutputStreamWriter(conn.getOutputStream()));
	
		for(Object parameter: parameters){
			out.writeObject(parameter);
		}
		
		
		out.close();
		GZIPInputStream conInput = new GZIPInputStream(conn.getInputStream());
	//	InputStream conInput = conn.getInputStream();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte[] buffer = new byte[1024];
		int len;
		StringBuffer content = new StringBuffer();
		while ((len = conInput.read(buffer)) > -1 ) {
		    String c = new String(buffer,0,len);
			content.append(c.replaceAll("\\n",""));
		    baos.write(buffer, 0, len);
		}
		baos.flush();
		CroCoLogger.getLogger().trace(content);
		String debugContent = content.substring(0, Math.min(2048,content.length())).replace(" ", "");
		
        ObjectInputStream in = null;
		try{
			 in = xstream.createObjectInputStream( new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
		}catch(StreamException e){
		    CroCoLogger.getLogger().fatal(String.format("Cannnot create object. Error: %s. Message from server: %s",e.getMessage(),debugContent));
	        return null;
		}
		
		Object object = null;
		CroCoLogger.getLogger().debug(String.format("Reading results"));

		try {
			object =  in.readObject() ;
		} catch (Exception e) {
			CroCoLogger.getLogger().fatal(String.format("Cannnot create object. Error: %s. Message from server: %s",e.getMessage(),debugContent));
		}
		return object;
	
	}
	@Override
	public OrthologMapping getOrthologMapping(OrthologMappingInformation orthologMappingInformation)throws Exception {
		
		InputStream is = getStreamedData(baseUrl,"getOrthologMapping",orthologMappingInformation);
	
		BufferedReader br = null;
		try{
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(is)));
		}catch(Exception e){
			 byte[] tmp = new byte[1014];
			is.read(tmp);
			String errorMessage =new String(tmp).trim(); 
			throw new CroCoException(String.format("Can not read ortholog mapping. Message from server: %s",errorMessage));
		}
		String line = null;
		OrthologMapping ret = new OrthologMapping(orthologMappingInformation);
		while (( line = br.readLine())!=null){
			if ( line.startsWith("#")) continue;
			String[] tokens = line.split("\t");
			Entity factor = new Entity(tokens[0]);
			Entity target = new Entity(tokens[1]);
			ret.addMapping(factor, target);
		}
		br.close();
		
		is.close();
		
		
		return  ret;//(OrthologMapping)performeOperation(baseUrl,"getOrthologMapping",orthologMappingInformation);
	}
	//TODO: merge with NetworkHierachy readAnnotation
	private Float getValue(String value){
		if ( value.trim().equals("NaN"))
			return null;
		if ( value.trim().equals("-"))
			return null;
		if( value.trim().equals(".") )
			return null;
		if( value.trim().length() == 0)
			return null;
		if ( Float.valueOf(value).intValue() == -1){
			return null;
		}
		return Float.valueOf(value);
	}
	//TODO: merge with NetworkHierachy readAnnotation
	@Override
	public BindingEnrichedDirectedNetwork readBindingEnrichedNetwork(Integer groupId, Integer contextId, Boolean gloablRepository) throws Exception {

		if ( contextId != null){
			return (BindingEnrichedDirectedNetwork)performeOperation(baseUrl,"readBindingEnrichedNetwork",groupId,contextId,gloablRepository);
		}
		NetworkHierachyNode networkNode = this.getNetworkHierachyNode(groupId);
		BindingEnrichedDirectedNetwork network = new BindingEnrichedDirectedNetwork(networkNode.getName(),networkNode.getTaxId(),gloablRepository);
		InputStream is = getStreamedData(baseUrl,"readBindingEnrichedNetwork",groupId,contextId,gloablRepository);
		BufferedReader br = null;
		try{
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(is)));
		}catch(Exception e){
			 byte[] tmp = new byte[1014];
			is.read(tmp);
			String errorMessage =new String(tmp).trim(); 
			throw new CroCoException(String.format("Can not read network %d. Message from server: %s",groupId,errorMessage));
		}
		String line = null;
		while((line=br.readLine())!=null){
			String[] tokens = line.split("\t");
			String type = tokens[0];
			
			String factor = tokens[1];
			String target = tokens[2];
			String bindingPartner = null;
			Float bindingPValue = null;
			String chrom = null;
			Integer bindingStart = null;
			Integer bindingEnd = null;
	
			Integer openChromStart = null;
			Integer openChromEnd = null;
			
			Float openChromPValue = null;
			Peak peak = null;
			if ( type.equals("TFBS") || type.equals("TBFS") ){
				bindingPartner = tokens[3];
				bindingPValue = Float.valueOf(tokens[7]);
				chrom = tokens[8];
				bindingStart = Integer.valueOf(tokens[9]);
				bindingEnd = Integer.valueOf(tokens[10]);
				peak = new TFBSPeak(chrom,bindingStart,bindingEnd,bindingPartner,bindingPValue,null);
			}else if ( type.equals("CHIP")){
				bindingPartner = tokens[3];
				bindingPValue = getValue(tokens[6]);
				if ( bindingPValue != null){
					bindingPValue = (float) Math.pow(bindingPValue, -10);
				}
				chrom = tokens[7];
				bindingStart = Integer.valueOf(tokens[8]);
				bindingEnd = Integer.valueOf(tokens[9]);	
				peak = new TFBSPeak(chrom,bindingStart,bindingEnd,bindingPartner,bindingPValue,null);
			}else if ( type.equals("OpenChromTFBS")){
				bindingPartner = tokens[3];
				bindingPValue = getValue(tokens[7]);
				chrom = tokens[8];
				bindingStart = Integer.valueOf(tokens[9]);
				bindingEnd = Integer.valueOf(tokens[10]);
				
				openChromStart = Integer.valueOf(tokens[11]);
				openChromEnd = Integer.valueOf(tokens[12]);
			
				openChromPValue = null;
				TFBSPeak tfbs = new TFBSPeak(chrom,bindingStart,bindingEnd,bindingPartner,bindingPValue,null);
				peak = new DNaseTFBSPeak(tfbs,new Peak(chrom,openChromStart,openChromEnd));
			}else{
				br.close();
				throw new IOException("Unknown type:" + type);
			}
			network.addEdge(new Entity(factor), new Entity(target), groupId, peak);
		}
		
		br.close();
		
		is.close();
		
		return network;
	}
	
	@Override
	public Network readNetwork(Integer groupId, Integer contextId,Boolean globalRepository) throws Exception {
	
		if ( contextId != null){
			return 	 (Network)performeOperation(baseUrl,"readNetwork",groupId,contextId,false);
		}
		NetworkHierachyNode networkNode = this.getNetworkHierachyNode(groupId);

		Network network = new DirectedNetwork(networkNode.getName(),networkNode.getTaxId(),globalRepository);
		
		
		InputStream is = getStreamedData(baseUrl,"readNetwork",groupId,contextId,globalRepository);
	
		
		BufferedReader br = null;
		try{
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(is)));
		}catch(Exception e){
			 byte[] tmp = new byte[1014];
			is.read(tmp);
			String errorMessage =new String(tmp).trim(); 
			throw new CroCoException(String.format("Can not read network %d. Message from server: %s",groupId,errorMessage));
		}
		String line = null;
		while (( line = br.readLine())!=null){
			String[] tokens = line.split("\t");
			Entity factor = new Entity(tokens[0]);
			Entity target = new Entity(tokens[1]);
			network.add(factor, target, groupId);
		}
		br.close();
		
		is.close();

		return network;
	}
	
	@Override
	public BufferedImage getRenderedNetwork(Integer groupId) throws Exception {
		InputStream is = getStreamedData(baseUrl,"getRenderedNetwork",groupId);
		BufferedImage image = ImageIO.read(is);
		is.close();
		return image;
	}
	
	@Override
	public NetworkHierachyNode getNetworkHierachyNode(Integer groupId) throws Exception {
		return (NetworkHierachyNode)performeOperation(baseUrl,"getNetworkHierachyNode",groupId);
	}

	@Override
	public Integer getNumberOfEdges(Integer groupId) throws Exception {
		return (Integer)performeOperation(baseUrl,"getNumberOfEdges",groupId);
	}


	@Override
	public List<OrthologMappingInformation> getTransferTargetSpecies(Integer taxId)throws Exception {
		return (List) performeOperation(baseUrl,"getTransferTargetSpecies",taxId);
		
	}

	@Override
	public List<OrthologMappingInformation> getOrthologMappingInformation(OrthologDatabaseType database, Species species1, Species species2)throws Exception {
		
		return (List) performeOperation(baseUrl,"getOrthologMappingInformation",database,species1,species2);
	}

	@Override
	public List<ContextTreeNode> getContextTreeNodes(String name) throws Exception {
		return (List)performeOperation(baseUrl,"getContextTreeNodes",name);
	}

	@Override
	public List<ContextTreeNode> getChildren(ContextTreeNode node)throws Exception {
		return (List)performeOperation(baseUrl,"getChildren",node);
	}


	public ContextTreeNode getContextTreeNode(String sourceId) throws Exception {
		return (ContextTreeNode)performeOperation(baseUrl,"getContextTreeNode",sourceId);
	}


	@Override
	public List<BindingEnrichedDirectedNetwork> getBindings(String factor,String target) throws Exception {
		return (List<BindingEnrichedDirectedNetwork>)performeOperation(baseUrl,"getBindings",factor,target);
	}



	@Override
	public Long getVersion() {
		return version;
	}
	@Override
	public List<Gene> getGenes(Species species, Boolean onlyCoding, ContextTreeNode context) throws Exception {
		return (List<Gene>)performeOperation(baseUrl,"getGenes",species,onlyCoding,context);
	}
    @Override
    public CroCoNode getNetworkOntology() throws Exception {
        return (CroCoNode)performeOperation(baseUrl,"getNetworkOntology");
    }
    @Override
    public List<NetworkHierachyNode> getNetworkHierachy() throws Exception {
        return (List<NetworkHierachyNode>)performeOperation(baseUrl,"getNetworkHierachy");
    }


}
