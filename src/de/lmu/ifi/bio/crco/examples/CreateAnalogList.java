package de.lmu.ifi.bio.crco.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
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
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;

public class CreateAnalogList {
	public static void main(String[] args) throws Exception{
		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("humanNetworkRootId").withArgName("ID").isRequired().hasArgs(1).create("humanNetworkRootId"));
		options.addOption(OptionBuilder.withLongOpt("mouseNetworkRotoId").withArgName("ID").isRequired().hasArgs(1).create("mouseNetworkRotoId"));

		options.addOption(OptionBuilder.withLongOpt("out").withArgName("FILE").isRequired().hasArgs(1).create("out"));

		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + CreateAnalogList.class.getName(), "", options, "", true);
			System.exit(1);
		}
		
		Integer humanId = Integer.valueOf(line.getOptionValue("humanNetworkRootId"));//5076;
		Integer mouseId =  Integer.valueOf(line.getOptionValue("mouseNetworkRotoId"));//;
		File out = new File(line.getOptionValue("out"));
		
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());

		
		NetworkHierachyNode hierachy = service.getNetworkHierachy(null);
		NetworkHierachyNode humanRoot = filter(hierachy,humanId);
		NetworkHierachyNode mouseRoot =  filter(hierachy,mouseId);
		List<OrthologMappingInformation> orthologMappings = service.getOrthologMappingInformation(OrthologDatabaseType.InParanoid, Species.Human, Species.Mouse);
		orthologMappings.addAll(service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, Species.Human,Species.Mouse));
		
		Stack<NetworkHierachyNode> stack = new Stack<NetworkHierachyNode>();
		stack.add(humanRoot);
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		
		HashMap<String,List<NetworkHierachyNode>> humanExperimentsByTarget = new HashMap<String,List<NetworkHierachyNode>>();
		while(!stack.isEmpty()){
			NetworkHierachyNode top = stack.pop();
			List<Pair<Option, String>> infos = service.getNetworkInfo(top.getGroupId());
			for(Pair<Option, String> info : infos){
				if ( info.getFirst().equals(Option.treatment) && !info.getSecond().trim().toUpperCase().equals("NONE")){
					continue;
				}
				if ( info.getFirst().equals(Option.AntibodyTargetMapped)){
					
					if (! humanExperimentsByTarget.containsKey(info.getSecond())){
						humanExperimentsByTarget.put(info.getSecond(), new ArrayList<NetworkHierachyNode>());
					}
					humanExperimentsByTarget.get(info.getSecond()).add(top);
				}
			}
			if ( top.getChildren() != null){
				for(NetworkHierachyNode child : top.getChildren()){
					stack.add(child);
				}
			}
		}
		stack.add(mouseRoot);
		
		HashMap<String,List<NetworkHierachyNode>> mouseExperimentsByTarget = new HashMap<String,List<NetworkHierachyNode>>();
		
		while(!stack.isEmpty()){
			NetworkHierachyNode top = stack.pop();
			List<Pair<Option, String>> infos = service.getNetworkInfo(top.getGroupId());
			for(Pair<Option, String> info : infos){
				if ( info.getFirst().equals(Option.treatment) && !info.getSecond().trim().toUpperCase().equals("NONE")){
					continue;
				}
				if ( info.getFirst().equals(Option.AntibodyTargetMapped)){
					
					if (! mouseExperimentsByTarget.containsKey(info.getSecond())){
						mouseExperimentsByTarget.put(info.getSecond(), new ArrayList<NetworkHierachyNode>());
					}
					mouseExperimentsByTarget.get(info.getSecond()).add(top);
				}
			}
			if ( top.getChildren() != null){
				for(NetworkHierachyNode child : top.getChildren()){
					stack.add(child);
				}
			}
		}
		OrthologMapping mappings = service.getOrthologMapping(orthologMappings.get(0));
	
		
		Set<Integer> toConsider = new HashSet<Integer>();
		for(Entry<String, List<NetworkHierachyNode>> e : humanExperimentsByTarget.entrySet()){
			Entity factor = new Entity(e.getKey());
			Set<Entity> orthologFactors = mappings.getOrthologs(factor);
			if (orthologFactors == null ) continue;
			for(Entity orthologFactor : orthologFactors){
				
				if ( mouseExperimentsByTarget.containsKey(orthologFactor.getIdentifier())){
					for(NetworkHierachyNode v  : e.getValue()){
						toConsider.add(v.getGroupId());
						bw.write("Human\t" + v.getName() + "\t" + v.getGroupId() + "\t" + e.getKey() + "\t" + orthologFactor + "\n");
					}
				}
			}
		}
		
		
		toConsider = new HashSet<Integer>();
		for(Entry<String, List<NetworkHierachyNode>> e : mouseExperimentsByTarget.entrySet()){
			Entity factor = new Entity(e.getKey());
			Set<Entity> orthologFactors = mappings.getOrthologs(factor);
			if (orthologFactors == null ) continue;
			for(Entity orthologFactor : orthologFactors){
				if ( humanExperimentsByTarget.containsKey(orthologFactor.getIdentifier())){
					for(NetworkHierachyNode v  : e.getValue()){
						toConsider.add(v.getGroupId());
						bw.write("Mouse\t" + v.getName() + "\t" + v.getGroupId() + "\t" + e.getKey() + "\t" + orthologFactor + "\n");
					}
				}
			}
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
}
