package de.lmu.ifi.bio.crco.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;

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
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.GeneSetFilter;
import de.lmu.ifi.bio.crco.operation.GeneSetFilter.FilterType;
import de.lmu.ifi.bio.crco.operation.Intersect;
import de.lmu.ifi.bio.crco.operation.ReadNetwork;
import de.lmu.ifi.bio.crco.operation.Transfer;
import de.lmu.ifi.bio.crco.operation.Union;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.util.CroCoLogger;

public class TextMiningSupport {
	public static void main(String[] args) throws Exception{
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("dNaseGroupId").withArgName("ID").isRequired().hasArgs(1).create("dNaseGroupId"));
		options.addOption(OptionBuilder.withLongOpt("chipGroupId").withArgName("ID").isRequired().hasArgs(1).create("chipGroupId"));
		options.addOption(OptionBuilder.withLongOpt("textMiningGroupId").withArgName("ID").isRequired().hasArgs(1).create("textMiningGroupId"));
		options.addOption(OptionBuilder.withLongOpt("taxId").withArgName("TaxId").isRequired().hasArgs(1).create("taxId"));
		options.addOption(OptionBuilder.withLongOpt("out").withArgName("FILE").isRequired().hasArgs(1).create("out"));

		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + CreateAnalogList.class.getName(), "", options, "", true);
			System.exit(1);
		}
		File output  =new File(line.getOptionValue("out"));
		Integer dNaseGroupId = Integer.valueOf(line.getOptionValue("dNaseGroupId"));
		Integer chipGroupId = Integer.valueOf(line.getOptionValue("chipGroupId"));
		Integer textMiningGroupId = Integer.valueOf(line.getOptionValue("textMiningGroupId"));
		Integer taxId = Integer.valueOf(line.getOptionValue("taxId"));
		
		System.out.println("Output file:" + output);
		System.out.println("DNase:" + dNaseGroupId);
		System.out.println("ChiP" +chipGroupId );
		System.out.println("TextMining:" + textMiningGroupId);
		System.out.println("TaxId:" + taxId);
		
		QueryService service = new LocalService(DatabaseConnection.getConnection());
		
		Statement stat = DatabaseConnection.getConnection().createStatement();
		stat.execute("SELECT gene,gene_name FROM Gene where tax_id = 9606 or tax_id = 10090" );
		ResultSet res = stat.getResultSet();
		HashMap<String,String> idNameMapping = new HashMap<String,String>();
		while(res.next()){
			idNameMapping.put(res.getString(1),res.getString(2));
		}
		res.close();
		stat.close();
		
		NetworkHierachyNode hierachy = service.getNetworkHierachy(null);
		NetworkHierachyNode tm = filter(hierachy,textMiningGroupId);
		Transfer transfer = new Transfer();
		List<Network> tmNetworks = new ArrayList<Network>();
		
		for(NetworkHierachyNode node : tm.getAllChildren()){
			CroCoLogger.getLogger().info("Process TM:" + node.getTaxId());
			ReadNetwork readNetwork = new ReadNetwork();
			readNetwork.setInput(ReadNetwork.NetworkHierachyNode,node);
			readNetwork.setInput(ReadNetwork.QueryService, service);
			readNetwork.setInput(ReadNetwork.GlobalRepository, true);
			Network textMining = readNetwork.operate();
			if (! textMining.getTaxId().equals(taxId)){
				CroCoLogger.getLogger().info("Transfer");
				List<OrthologMappingInformation> mapping = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, new Species(taxId), new Species(textMining.getTaxId()));
				if  (mapping.size() == 0){
					CroCoLogger.getLogger().warn("No ortholog mapping for" + node.getTaxId());
					continue;
				}
				transfer.setInput(Transfer.OrthologMappingInformation, mapping);
				transfer.setInput(Transfer.OrthologRepository, OrthologRepository.getInstance(service));
				transfer.setInputNetwork(textMining);
				tmNetworks.add(transfer.operate());
			}else{
				tmNetworks.add(textMining);
			}
			
		}
		Union union = new Union();
		union.setInputNetwork(tmNetworks);
		Network textMining = union.operate();
		
		NetworkHierachyNode dnaseGroup = filter(hierachy,dNaseGroupId);
		NetworkHierachyNode chipGroup =  filter(hierachy,chipGroupId);
		
		Network dNaseNetwork = getUnifiedNetwork(dnaseGroup,service);
		Network chipNetwork = getUnifiedNetwork(chipGroup,service);
		/*
	
		*/
		
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(output));
		
		GeneSetFilter filter = new GeneSetFilter();
		filter.setInput(GeneSetFilter.filterType, FilterType.FactorFilter);
		
		Set<Entity> chipFactors = chipNetwork.getFactors() ;
		Set<Entity> dNaseFactors = dNaseNetwork.getFactors() ;
		
		chipFactors.retainAll(dNaseFactors);
		System.out.println(chipFactors);
		bw.write("Factor\tName\tDNase\tChIP\tTM\tDNaseTM\tChIPTM\n");
		for(Entity factor : chipFactors ) {
			filter.setInput(GeneSetFilter.genes,Arrays.asList(new Entity[]{factor}));
			filter.setInputNetwork(dNaseNetwork);
			Network dnaseFiltered = filter.operate();
			
			filter.setInputNetwork(chipNetwork);
			Network chipFiltered = filter.operate();
			
			filter.setInputNetwork(textMining);
			
			Network tmFiltered = filter.operate();
			
			Intersect intersect = new Intersect();
			intersect.setInputNetwork(dnaseFiltered,tmFiltered);
			Network tmDnase = intersect.operate();
			
			intersect.setInputNetwork(chipFiltered,tmFiltered);
			Network tmChIP = intersect.operate();
			
			
			bw.write(factor.getIdentifier() + "\t" + idNameMapping.get(factor.getIdentifier()) + "\t" + dnaseFiltered.size() + "\t" + chipFiltered.size() + "\t" + tmFiltered.size() + "\t" + tmDnase.size() + "\t" + tmChIP.size() + "\n");
			bw.flush();
		}
		bw.flush();
		bw.close();
	}
	private static NetworkHierachyNode filter(NetworkHierachyNode rootNode, Integer humanId) {
		Stack<NetworkHierachyNode> stack = new Stack<NetworkHierachyNode>();
		stack.add(rootNode);
		while(!stack.isEmpty()){
			NetworkHierachyNode top = stack.pop();

			if ( top.getGroupId().equals(humanId)) {
				return top;
			}
			if ( top.getChildren() != null) stack.addAll(top.getChildren());
		}
		throw new RuntimeException("Node not found");
	}
	public static Network getUnifiedNetwork(NetworkHierachyNode rootNode, QueryService service) throws Exception{
		Stack<NetworkHierachyNode> stack = new Stack<NetworkHierachyNode>();
		List<Network> networks = new ArrayList<Network>();
		stack.add(rootNode);
		while(!stack.isEmpty()){
			NetworkHierachyNode top = stack.pop();
			
			if (top.hasNetwork() && (top.getChildren() == null || top.getChildren().size() == 0)){
				ReadNetwork reader = new ReadNetwork();
				reader.setInput(ReadNetwork.NetworkHierachyNode, top);
				reader.setInput(ReadNetwork.QueryService, service);
				reader.setInput(ReadNetwork.GlobalRepository, true);
				networks.add(reader.operate());
			}
			if ( top.getChildren() != null){
				stack.addAll(top.getChildren());
			}
		}
		
		CroCoLogger.getLogger().info(String.format("Reading: %d network",networks.size()));
		CroCoLogger.getLogger().info(String.format("Networks: %s",networks.toString()));
		Union union = new Union();
		union.setInputNetwork(networks);
		Network net =  union.operate();;
		

		return net;
		
	}
}
