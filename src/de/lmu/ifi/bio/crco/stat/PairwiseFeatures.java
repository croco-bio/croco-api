package de.lmu.ifi.bio.crco.stat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.reflections.Reflections;

import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.GeneSetFilter;
import de.lmu.ifi.bio.crco.operation.Transfer;
import de.lmu.ifi.bio.crco.operation.GeneSetFilter.FilterType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.processor.hierachy.NetworkHierachy;
import de.lmu.ifi.bio.crco.processor.hierachy.NetworkHierachy.CroCoRepositoryProcessor;
import de.lmu.ifi.bio.crco.stat.generator.PairwiseStatGenerator;
import de.lmu.ifi.bio.crco.stat.generator.PairwiseStatGenerator.FeatureType;
import de.lmu.ifi.bio.crco.stat.generator.PairwiseStatGenerator.Result;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;

/**
 * Generates the pair-wise statistics for one network against all networks in the croco repository.
 * @author pesch
 *
 */
public class PairwiseFeatures {
	/**
	 * Used within the NetworkHierachy to retrieve all list of all networks
	 * @author pesch
	 *
	 */
	public class NetworkCollector implements CroCoRepositoryProcessor {
		private List<Network> networks;
		
		public NetworkCollector(){
			this.networks = new ArrayList<Network>();
		}
		public List<Network> getNetworks(){
			return networks;
		}
		
		@Override
		public void init(Integer rootId) throws Exception {}

		@Override
		public void process(Integer rootId, Integer networkId, File networkFile, File infoFile, File statFile,File annotationFile) throws Exception {
			Network network = NetworkHierachy.getNetworkReader().setNetworkInfo(infoFile).setGloablRepository(false).readNetwork();
			network.addNetworkInfo(Option.networkFile, networkFile.toString());
			networks.add(network);
		
		}

		@Override
		public void finish() throws Exception {}
		
	}
	public static class Check{
		public static void main(String[] args) throws Exception{
			HelpFormatter lvFormater = new HelpFormatter();
			CommandLineParser parser = new BasicParser();
		
			Options options = new Options();
			options.addOption(OptionBuilder.withLongOpt("repositoryDir").withArgName("DIR").withDescription("Location of the croco Repository").isRequired().hasArgs(1).create("repositoryDir"));
			
			CommandLine line = null;
			try{
				line = parser.parse( options, args );
			}catch(Exception e){
				System.err.println( e.getMessage());
				lvFormater.printHelp(120, "java " + PairwiseFeatures.class.getName(), "", options, "", true);
				System.exit(1);
			}
			File repositoryDir = new File(line.getOptionValue("repositoryDir"));

			PairwiseFeatures features = new PairwiseFeatures(repositoryDir);
			
			NetworkCollector collector = features.new NetworkCollector(); 
			NetworkHierachy hierachy = new NetworkHierachy();
			hierachy.processHierachy(repositoryDir, collector, null);
			
			List<Network> networks = collector.getNetworks();
			
			
			for(Network sourceNetwork : networks){
				File networkFile = new File(sourceNetwork.getOptionValue(Option.networkFile));
				
				File statFile =new File(networkFile.toString().replace(".network.gz", ".stat"));
				//sourceNetwork.getTaxId()
				sourceNetwork.addNetworkInfo( Option.networkFile, networkFile.toString());
				if ( !isKnownSpecies(sourceNetwork.getTaxId())) continue;
				
				String sourceNetworkName =sourceNetwork.getOptionValue(Option.networkFile).replace(repositoryDir.toString(), "") ;
				
				HashMap<Pair<String, Option>, Float> computations = PairwiseFeatures.readStatFile(repositoryDir, statFile);
				for(int i = 0 ; i< networks.size() ; i++){
					Network network = networks.get(i);
					if ( !isKnownSpecies(network.getTaxId())) continue;
					
					String targetNetworkName = network.getOptionValue(Option.networkFile).replace(repositoryDir.toString(),"").replace("//", "/");
					CroCoLogger.getLogger().debug(String.format("Skip network %s, because of ASYMMETRIC (and or already computed) metric",targetNetworkName));
					List<Pair<Entity, Entity>> factorOverlap = features.overlap(sourceNetwork,network);
					
					if ( ! features.canSkip(computations,  sourceNetworkName,  targetNetworkName,factorOverlap) ) {
						System.out.printf("Missing %s in %s\n", targetNetworkName,sourceNetworkName );
						break;
					}
					
					
				}
			}
		}

	}
	/**
	 * Checks wether a given taxId is currently available in the CroCo repo (it may happen that networks are deposited in the repositroy folder without proper configurations available in the croco framework)
	 * @param taxId
	 * @return
	 */
	public static boolean isKnownSpecies(Integer taxId) {
		for( Species species: Species.knownSpecies ) {
			if ( species.getTaxId().equals(taxId)) return true;
		}
		
		return false;
	}
	private QueryService service;
	private File repositoryDir;
	private HashMap<Option, PairwiseStatGenerator> generators;
	private HashMap<Pair<Integer,Integer>,OrthologMappingInformation> possibleMappings;

	public PairwiseFeatures( File repositoryDir ) throws Exception{
		this.repositoryDir = repositoryDir;
		this.service = new LocalService();
		
		this.possibleMappings = new HashMap<Pair<Integer,Integer>,OrthologMappingInformation> ();
	
		this.generators =new HashMap<Option, PairwiseStatGenerator> ();

		//read ortholog mappings
		for(int i = 0 ; i< Species.knownSpecies.size(); i++){
			for(int j = i+1 ; j< Species.knownSpecies.size(); j++){
				List<OrthologMappingInformation> orthologMappings = service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara, Species.knownSpecies.get(i),Species.knownSpecies.get(j));
				for(OrthologMappingInformation orthologMapping: orthologMappings){
					Integer tId1 = orthologMapping.getSpecies1().getTaxId();
					Integer tId2 = orthologMapping.getSpecies2().getTaxId();
					
					CroCoLogger.getLogger().info(String.format("Added ortholog mapping: %d to %d",tId1,tId2));
					possibleMappings.put(new Pair<Integer,Integer>(tId1,tId2),orthologMapping);
				}
			}	
		}

		CroCoLogger.getLogger().info(String.format("Ortholog mappings: %d",possibleMappings.size()));
		
		registerGenerators();
	}
	
	private Network transfer(Network sourceNetwork,Network network)  throws Exception{
		
		Transfer transfer = new Transfer();
		ArrayList<OrthologMappingInformation> orthologMappings = new ArrayList<OrthologMappingInformation>();
		orthologMappings.add(possibleMappings.get(new Pair<Integer,Integer>(network.getTaxId(),sourceNetwork.getTaxId())));
		if ( orthologMappings.size() == 0) {
			CroCoLogger.getLogger().warn(String.format("No ortholog mappings given for network %s. Tried to transfer from TaxId %d to taxId %d ",network.getName(),network.getTaxId(),sourceNetwork.getTaxId()));
			return new DirectedNetwork(network);
		}
		transfer.setInput(Transfer.OrthologRepository,  OrthologRepository.getInstance(service));
		transfer.setInput(Transfer.OrthologMappingInformation,orthologMappings);
			
		transfer.setInputNetwork(network);
		Network transferred = transfer.operate();
	
		return transferred;
		
	}
	public static HashMap<Pair<String,Option>,Float>  readStatFile(File repositoryDir,File outputFile) throws Exception{
		HashMap<Pair<String,Option>,Float> computations = new HashMap<Pair<String,Option>,Float>();
		if ( outputFile.exists()){
			BufferedReader br = new BufferedReader(new FileReader(outputFile));
			String line = null;
			while ((line=br.readLine())!=null){
				if ( line.startsWith("#")) continue;
				String[] tokens = line.split("\t");
				Option option = Option.valueOf(tokens[0]) ;
				String file2 = tokens[2].replace(repositoryDir.getName(), "");
				Float value = Float.valueOf(tokens[5]);
				
				Pair<String,Option> pair = new Pair<String,Option>(file2,option);
				computations.put(pair, value);
			}
			br.close();
		}
		return computations;
	}
	/**
	 * 
	 * @throws Exception
	 */
	private void  registerGenerators() throws Exception{
		Reflections reflections = new Reflections("de.lmu.ifi.bio.crco.stat.generator");
		Set<Class<? extends PairwiseStatGenerator>> subTypes = reflections.getSubTypesOf(PairwiseStatGenerator.class);

		
		for (Class<? extends PairwiseStatGenerator> c : subTypes) {
			PairwiseStatGenerator generator = c.getConstructor().newInstance();
			if (generators.containsKey(generator.getOption())) {
				throw new RuntimeException("Two stat processors for same type:"+ generator.getOption());
			}
			if ( generator.getOption() != null){
				CroCoLogger.getLogger().debug(String.format("Register generator %s for option %s",generator.getClass().getSimpleName(),generator.getOption().name()));
				generators.put(generator.getOption(), generator);
			}
		}
	}
	



	/**
	 * Returns a list of ortholog TF factors for two networks
	 * @param sourceNetwork -- source network
	 * @param network -- target network
	 * @return paired ortholog TF factors
	 */
	public List<Pair<Entity,Entity>> overlap(Network sourceNetwork, Network network){
		 List<Pair<Entity,Entity>> ret = new ArrayList<Pair<Entity,Entity>>();
		 Set<String> factorList1 = getFactorList(sourceNetwork.getOptionValue(Option.FactorList));
		 Set<String> factorList2 = getFactorList(network.getOptionValue(Option.FactorList));
		 for(String factor1 :factorList1 ){
				
			 if (! sourceNetwork.getTaxId().equals(network.getTaxId())){
				// Set<String> transferredList2 = new HashSet<String>();
				 OrthologMappingInformation mapping = possibleMappings.get(new Pair<Integer,Integer>(sourceNetwork.getTaxId(),network.getTaxId()));
				 if ( mapping == null) throw new RuntimeException("No ortholog mapping for" + sourceNetwork.getTaxId() + " " + network.getTaxId());
				 Set<Entity> mappedTargets = OrthologRepository.getInstance(service).getOrthologMapping(mapping).getOrthologs(new Entity(factor1));
				if ( mappedTargets != null){
					 for(Entity mappedTarget :mappedTargets ){
						 if ( factorList2.contains(mappedTarget)) {
							 ret.add(new Pair<Entity,Entity>(new Entity(factor1),mappedTarget)); 
						 }
					 }
				}
			 }
			 else if(factorList2.contains(factor1) ) {
					ret.add(new Pair<Entity,Entity>(new Entity(factor1),new Entity(factor1))); //since same tax id
			}
		}
		return ret;
	}
	public boolean canSkip(HashMap<Pair<String, Option>, Float> computations,String sourceNetworkName, String targetNetworkName, List<Pair<Entity, Entity>> overlap ){
		boolean skip = true;
		if ( overlap == null || overlap.size() == 0) {
			CroCoLogger.getLogger().debug(String.format("Skip network %s, because of none factor overlap",targetNetworkName));
			
			return true;
		}
		for (Entry<Option, PairwiseStatGenerator> e : generators.entrySet()) {
	
			if ( !computations.containsKey(new Pair<String,Option>(targetNetworkName,e.getKey())))  {                    //when not yet computed
				if ( e.getValue().getFeatureType().equals(FeatureType.ASYMMETRIC) || sourceNetworkName.compareTo(targetNetworkName) <= 0) {// and metric is asymmetric, or sourceNetworkName precedes targetNetworkName lexicographically
					skip = false;																										  // than can not skip
					break;
				}
			}
		}
		if ( skip){
			CroCoLogger.getLogger().debug(String.format("Skip network %s, because of ASYMMETRIC (and or already computed) metric",targetNetworkName));
			return true;
		}

		return false;
	}
	
	public void compute(File networkFile, File networkInfo, File outputFile) throws Exception{
		
		HashMap<Pair<String, Option>, Float> computations = readStatFile(repositoryDir,outputFile);
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile,true));
			
		NetworkCollector collector = new NetworkCollector(); 
		Network sourceNetwork =  NetworkHierachy.getNetworkReader().setNetworkInfo(networkInfo).setGloablRepository(false).setNetworkFile(networkFile).readNetwork();
		String sourceNetworkName =sourceNetwork.getOptionValue(Option.networkFile).replace(repositoryDir.toString(), "") ;
		
		CroCoLogger.getLogger().info(String.format("Source network size: %d", sourceNetwork.getSize()));
		
		NetworkHierachy hierachy = new NetworkHierachy();
	
		hierachy.processHierachy(repositoryDir, collector, null);
		List<Network> networks = collector.getNetworks();

		CroCoLogger.getLogger().info("Number of networks: " + networks.size());
		for(int i = 0 ; i< networks.size() ; i++){
			
			CroCoLogger.getLogger().info(String.format("State: %d of %d",i,networks.size()));

			Network network = networks.get(i); //Network with network infos but without edges
			String targetNetworkName = network.getOptionValue(Option.networkFile).replace(repositoryDir.toString(),"").replace("//", "/");
				
			List<Pair<Entity, Entity>> factorOverlap = overlap(sourceNetwork,network);
				
			if ( canSkip( computations, sourceNetworkName,targetNetworkName,factorOverlap)) continue;
			Set<Entity> sourceNetworkFactors = new HashSet<Entity>();
			Set<Entity> targetNetworkFactors = new HashSet<Entity>();
			for(Pair<Entity, Entity> pair : factorOverlap){
				sourceNetworkFactors.add(pair.getFirst());
				targetNetworkFactors.add(pair.getSecond());
			}
				
			File targetNetworkFile = new File(networks.get(i).getOptionValue(Option.networkFile).toString());
			Network tmpNetwork = NetworkHierachy.getNetworkReader().setNetworkInfo(networks.get(i).getNetworkInfo()).setGloablRepository(false).setFactors(targetNetworkFactors).setNetworkFile(new File(networks.get(i).getOptionValue(Option.networkFile))).readNetwork();
			CroCoLogger.getLogger().info(String.format("Network size: %d", tmpNetwork.getSize()));
			
			if (! tmpNetwork.getTaxId().equals(sourceNetwork.getTaxId())){
				tmpNetwork = transfer( sourceNetwork,tmpNetwork);
				tmpNetwork.setNetworkInfo(network.getNetworkInfo());
			}
			if ( tmpNetwork.size() == 0){
				CroCoLogger.getLogger().warn("No edges in network" +"\t" + targetNetworkFile);
				continue;
			}
			network = tmpNetwork;
			
			GeneSetFilter filter = new GeneSetFilter();
			
			filter.setInput(GeneSetFilter.filterType, FilterType.FactorFilter);
			filter.setInput(GeneSetFilter.genes, sourceNetworkFactors );
			filter.setInputNetwork(sourceNetwork);
			
			Network sourceNetworkFiltered = filter.operate();
			for (Entry<Option, PairwiseStatGenerator> e : generators.entrySet()) {
				if ( !computations.containsKey(new Pair<String,Option>(targetNetworkName,e.getKey()))) {                    //when not yet computed
					if ( e.getValue().getFeatureType().equals(FeatureType.ASYMMETRIC) || sourceNetworkName.compareTo(targetNetworkName) <= 0) {// and metric is asymmetric, or sourceNetworkName precedes targetNetworkName lexicographically
						Result sim = e.getValue().compute(sourceNetworkFiltered,network);
						bw.write(e.getKey().name() + "\t" + sourceNetworkName + "\t" + targetNetworkName + "\t" + sim.numerator + "\t" + sim.denominator  + "\t" + sim.getFrac() + "\n");
						bw.flush();
					}
				}
			}
			
			
		}
		bw.flush();
		bw.close();
		
	}
	


	private Set<String> getFactorList(String factorList) {
		Set<String> ret = new HashSet<String>();
		if ( factorList != null){
			for(String factor : factorList.split("\\s+")){
				ret.add(factor.toUpperCase().trim());
			}
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();

		options.addOption(OptionBuilder.withLongOpt("repositoryDir").withArgName("DIR").withDescription("Location of the croco Repository").isRequired().hasArgs(1).create("repositoryDir"));
		options.addOption(OptionBuilder.withLongOpt("infoFile").withArgName("FILE").withDescription("Valid croco network info file used to compute metrics").isRequired().hasArgs(1).create("infoFile"));
		options.addOption(OptionBuilder.withLongOpt("networkFile").withArgName("FILE").withDescription("Valid croco network network file used to compute metrics").isRequired().hasArgs(1).create("networkFile"));
		options.addOption(OptionBuilder.withLongOpt("output").withArgName("DIR").withDescription("Output dir").isRequired().hasArgs(1).create("output"));
		
		
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + PairwiseFeatures.class.getName(), "", options, "", true);
			System.exit(1);
		}
		File repositoryDir = new File(line.getOptionValue("repositoryDir"));
		File infoFile = new File(line.getOptionValue("infoFile"));
		if (! infoFile.exists()){
			CroCoLogger.getLogger().fatal(String.format("Cannot find infoFile %s",infoFile.getName()));
			return;
		}
		File networkFile = new File(line.getOptionValue("networkFile"));
		if ( !networkFile.exists()){
			CroCoLogger.getLogger().fatal(String.format("Cannot locate network file (%s) for info file %s",networkFile.getName(),infoFile.getName()));
		}
		File output = new File(line.getOptionValue("output"));
		CroCoLogger.getLogger().info(String.format("Repository dir: %s",repositoryDir.toString()));
		CroCoLogger.getLogger().info(String.format("Network info file: %s",infoFile.toString()));
		CroCoLogger.getLogger().info(String.format("Network file: %s",networkFile.toString()));
		CroCoLogger.getLogger().info(String.format("Output file: %s",output.toString()));
		
		
		PairwiseFeatures featureGenerator = new PairwiseFeatures(repositoryDir);
		
		featureGenerator.compute(networkFile,infoFile, output);
		CroCoLogger.getLogger().info("Finished");
	}

}
