package de.lmu.ifi.bio.crco.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.MathUtil;

public class IndicatorGenes {
	public static void main(String[] args) throws Exception{
		
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("testNetworkId").withArgName("ID").isRequired().hasArgs().create("testNetworkId"));
		options.addOption(OptionBuilder.withLongOpt("rootNetworkId").withArgName("ID").isRequired().hasArgs(1).create("rootNetworkId"));
		options.addOption(OptionBuilder.withLongOpt("out").withArgName("FILE").isRequired().hasArgs(1).create("out"));

		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + CreateAnalogList.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
	/*	
		ReadNetwork reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(61,9606));
		Network humanNet = reader.operate();
		*/
		Connection connection = DatabaseConnection.getConnection();
		List<Entity> genes = service.getEntities(new Species(9606),"protein_coding", null);
		
		
		List<NetworkHierachyNode> networks = null;//service.getN(Integer.valueOf(line.getOptionValue("rootNetworkId"))); //10
		
		PreparedStatement stat = connection.prepareStatement("SELECT count(*) FROM Network where gene2 like ? and group_id = ?");
		List<Integer> toTest = new ArrayList<Integer>();
		for(String opt : line.getOptionValues("testNetworkId")){
			System.out.println(opt);
			Integer t = Integer.valueOf(opt);
			toTest.add(t);
		}
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(line.getOptionValue("out"))));
		System.out.println("Tree size:" + networks.size());
		System.out.println("Number of genes:" + genes.size());
		for(Entity gene : genes){
			List<Float> test = new ArrayList<Float>();
			for(Integer t: toTest){
				
				stat.setString(1, gene.getIdentifier());
				stat.setInt(2, t);
				stat.execute();
				ResultSet res = stat.getResultSet();
				if ( res.next()){
					test.add((float)res.getInt(1));
				}
				res.close();
			}
			if ( MathUtil.min(test) == 0) continue;
			List<Float> counts = new ArrayList<Float>();
		//	System.out.println(gene.getIdentifier());
			for(NetworkHierachyNode network : networks){
				if (! network.hasNetwork()) continue;
				
				stat.setString(1, gene.getIdentifier());
				stat.setInt(2, network.getGroupId());
				stat.execute();
				ResultSet res = stat.getResultSet();
				if ( res.next()){
					counts.add((float)res.getInt(1));
				}
				res.close();
			}
			String ret = gene.getIdentifier() + "\t"+ MathUtil.min(test) + "\t" + MathUtil.mean(counts) + "\t" + MathUtil.sd(counts);
			bw.write(ret + "\n");
			bw.flush();
			
		}
		bw.flush();
		bw.close();
	}
	
	
}
