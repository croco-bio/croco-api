package de.lmu.ifi.bio.crco.processor.hierachy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.collections15.Transformer;

import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.converter.JUNGConverter;
import de.lmu.ifi.bio.crco.processor.hierachy.NetworkHierachy.CroCoRepositoryProcessor;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.decorators.AbstractVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.DirectionalEdgeArrowTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

/**
 * Processor to create images of the networks in the croco repository
 * @author pesch
 *
 */
public class NetworkRenderedHierachyProcessor implements CroCoRepositoryProcessor {
	private int sampleSize;
	private int width;
	private int height;
	private String outputFormat;
	boolean overwrite;
	
	HashMap<String,String> nameMapping= null;
	public NetworkRenderedHierachyProcessor(String outputFormat,boolean overwrite, int sampleSize, int width, int height) throws Exception{
		this.sampleSize = sampleSize;
		this.width = width;
		this.height = height;
		this.outputFormat = outputFormat;
		this.overwrite = overwrite;
	}
	
	@Override
	public void init(Integer rootId) throws Exception {
		//load gene id to gene name mapping for all species
		LocalService service = new LocalService();
		nameMapping = new HashMap<String,String>();
		List<Entity> knownEntities = service.getEntities(null, "protein_coding", null);
		System.out.println(knownEntities.size());
		for(Entity entity : knownEntities){
			nameMapping.put(entity.getIdentifier(), entity.getName());
		}
		
	}

	@Override
	public void process(Integer rootId, Integer networkId, File networkFile,File infoFile, File statFile,File annotationFile) throws Exception {
		//render network
		File networkImageFile = new File(networkFile.toString().replace(".network.gz", ".network."  +outputFormat ));
		
		if ( overwrite == false && networkImageFile.exists()) return;
		
		CroCoLogger.getLogger().debug(String.format("Process: %s",networkFile.toString()));
		Network network = NetworkHierachy.getNetwork(infoFile, networkFile, false);
		
		
		BufferedImage image = createImage(network,nameMapping, sampleSize,width, height);
		
		ImageIO.write(image, outputFormat, networkImageFile);
	}

	@Override
	public void finish() throws Exception {
		//do noting
	}
	/**
	 * Generates a network picture
	 * @param network -- croco network
	 * @param sampleSize -- number of edges to consider (randomly sampled), or null for all edges
	 * @param width -- width of network layout
	 * @param height -- height of network layout
	 * @return Layouted network as BufferedImage
	 */
	public static BufferedImage createImage(Network network,final HashMap<String,String> nameMapping, int sampleSize,int width, int height ) {
		JUNGConverter converter = new JUNGConverter(sampleSize);
		final DirectedSparseGraph<String, Integer> graph = converter.convert(network);

		FRLayout2<String, Integer> layout = new edu.uci.ics.jung.algorithms.layout.FRLayout2<String, Integer>(graph);
		layout.setMaxIterations(100); 
		layout.setSize(new Dimension(width,height));		
		
		VisualizationImageServer<String, Integer> bvs = new VisualizationImageServer<String, Integer>(layout, new Dimension(width, height));
		Transformer<String,Paint> vertexColor = new Transformer<String,Paint>() {
			public Paint transform(String i) {
				if(graph.getOutEdges(i).size() == 0) return Color.RED;
	               return Color.BLUE;
	            }
	        };
		
		VertexShapeSizeAspect<String, Integer> vssa = new VertexShapeSizeAspect<String, Integer>(graph);
		
	
		bvs.getRenderContext().setVertexShapeTransformer(vssa);
		bvs.getRenderContext().setVertexFillPaintTransformer(vertexColor);
		bvs.setBackground(new Color(255,255,255,255));
		Transformer<Integer, Paint> arrowPainter = new Transformer<Integer, Paint>(){
			@Override
			public Paint transform(Integer input) {
				return Color.DARK_GRAY;
			}
		};
		bvs.getRenderContext().setArrowFillPaintTransformer(arrowPainter );
		bvs.getRenderContext().setArrowDrawPaintTransformer(arrowPainter);
		Transformer<Context<Graph<String, Integer>, Integer>, Shape> edgeArrowTransformer  = new DirectionalEdgeArrowTransformer<String,Integer>(5, 4, 2);  
		
		bvs.getRenderContext().setEdgeArrowTransformer(edgeArrowTransformer);
		
		Transformer<Integer, Paint> edgePaint = new Transformer<Integer, Paint>() {
		    public Paint transform(Integer s) {
		        return new Color(84,84,84,84);
		    }
		};
		bvs.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		bvs.getRenderContext().setLabelOffset(0);
		bvs.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>(){
			@Override
            public String transform(String v) {
                if (  graph.getOutEdges(v).size() == 0 || !nameMapping.containsKey(v)){
                	return "";
                }else{
                	return nameMapping.get(v);
                	
                }
			}
		});
		bvs.getRenderContext().setEdgeDrawPaintTransformer(edgePaint);
		
		BufferedImage image = (BufferedImage) bvs.getImage(new Point2D.Double(width / 2, height / 2), new Dimension(width, height));
		
		return image;
	}

	private final static class VertexShapeSizeAspect<V, E> extends
			AbstractVertexShapeTransformer<V> implements Transformer<V, Shape> {


		public VertexShapeSizeAspect(final Graph<V, E> graphIn) {
			setSizeTransformer(new Transformer<V, Integer>() {

				public Integer transform(V v) {

					return (int) Math.max(Math.min(100,graphIn.getNeighborCount(v) *1),3);

				}
			});
		}

		public Shape transform(V v) {
			return factory.getEllipse(v);
		}
	}

	
	public static void main(String[] args) throws Exception {
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("repositoryDir").withDescription("Repository directory").isRequired().hasArgs(1).create("repositoryDir"));
		options.addOption(OptionBuilder.withLongOpt("outputFormat").withDescription("Output image format (e.g. png)").isRequired().hasArgs(1).create("outputFormat"));
		options.addOption(OptionBuilder.withLongOpt("width").withDescription("Image width in pixel (e.g. 300)").isRequired().hasArgs(1).create("width"));
		options.addOption(OptionBuilder.withLongOpt("height").withDescription("Image height in pixel (e.g. 300)").isRequired().hasArgs(1).create("height"));
		options.addOption(OptionBuilder.withLongOpt("overwrite").withDescription("Overwrite existing images").create("overwrite"));
		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + NetworkRenderedHierachyProcessor.class.getName(), "", options, "", true);
			System.exit(1);
		}
		String outputFormat = line.getOptionValue("outputFormat");
		if ( ImageIO.getImageWritersByFormatName(outputFormat) == null) {
			throw new RuntimeException(String.format("No image writer available for format %s",outputFormat));
		}
		Integer width = Integer.valueOf(line.getOptionValue("width"));
		Integer height = Integer.valueOf(line.getOptionValue("height"));
		File repositoryDir = new File(line.getOptionValue("repositoryDir"));
		boolean overwrite = line.hasOption("overwrite");
		NetworkHierachy hierachy = new NetworkHierachy();
		
		hierachy.processHierachy(repositoryDir, new NetworkRenderedHierachyProcessor(outputFormat,overwrite,1000,width,height), null);
		
	}
}
