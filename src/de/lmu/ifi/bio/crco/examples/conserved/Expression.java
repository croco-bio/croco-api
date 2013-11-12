package de.lmu.ifi.bio.crco.examples.conserved;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import com.google.common.collect.BiMap;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.examples.BindingSiteAnnotatedCoreNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.ReadNetwork;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.MathUtil;
import de.lmu.ifi.bio.crco.util.Pair;

public class Expression {
	public enum ExpressionCat{
		High, Mid, Low;
	}
	public static void main(String[] args) throws Exception{

		CommandLine lvCmd = null;
		HelpFormatter lvFormater = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
	
		Options options = new Options();
		//options.addOption(OptionBuilder.withLongOpt("humanDNaseExpId").withArgName("ID").isRequired().hasArgs(1).create("humanDNaseExpId"));
		//options.addOption(OptionBuilder.withLongOpt("mouseDNaseExpId").withArgName("ID").isRequired().hasArgs(1).create("mouseDNaseExpId"));
		
		options.addOption(OptionBuilder.withLongOpt("FPKM1").withArgName("FILE").isRequired().hasArgs().create("FPKM1"));
		options.addOption(OptionBuilder.withLongOpt("FPKM2").withArgName("FILE").isRequired().hasArgs().create("FPKM2"));
		options.addOption(OptionBuilder.withLongOpt("OrthologMapping").withArgName("TaxId1,TaxId2").hasArgs(1).create("OrthologMapping"));
		
		options.addOption(OptionBuilder.withLongOpt("GeneSetFile").withArgName("FILE").withDescription("ID matching to FPKM1 file").hasArgs(1).create("GeneSetFile"));
		options.addOption(OptionBuilder.withLongOpt("GeneSetList").withArgName("GENEID").withDescription("ID matching to FPKM1 file").hasArgs().create("GeneSetList"));
		
		options.addOption(OptionBuilder.withLongOpt("output").isRequired().hasArgs(1).create("output"));
			
		CommandLine line = null;
		try{
			line = parser.parse( options, args );
		}catch(Exception e){
			System.err.println( e.getMessage());
			lvFormater.printHelp(120, "java " + BindingSiteAnnotatedCoreNetwork.class.getName(), "", options, "", true);
			System.exit(1);
		}
	
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
		OrthologMapping orthologMapping = null;
	
		
		List<File> FPKM1 = new ArrayList<File>(); 
		List<File> FPKM2 = new ArrayList<File>(); 
		for(String f :line.getOptionValues("FPKM1") ){
			File file = new File(f);
			if ( !file.exists()){
				throw new RuntimeException(String.format("File %s does not exist",file));
			}
			FPKM1.add(file);
		}
		for(String f :line.getOptionValues("FPKM2") ){
			File file = new File(f);
			if ( !file.exists()){
				throw new RuntimeException(String.format("File %s does not exist",file));
			}
			FPKM2.add(file);
		}
		CroCoLogger.getLogger().info("FPKM1:\t" + FPKM1);
		CroCoLogger.getLogger().info("FPKM2:\t"  + FPKM2);
		CroCoLogger.getLogger().info("OrthologMapping:\t" +  line.getOptionValue("OrthologMapping"));
		CroCoLogger.getLogger().info("GeneSet:\t" +  line.getOptionValue("GeneSetFile"));
		CroCoLogger.getLogger().info("GeneSetList:\t" +  line.getOptionValue("GeneSetList"));
		CroCoLogger.getLogger().info("output:\t" + line.getOptionValue("output"));
		
		if ( line.hasOption("OrthologMapping")){
			String[] tokens = line.getOptionValue("OrthologMapping").split(",");
			List<OrthologMappingInformation> mappings = service.getOrthologMappingInformation(OrthologDatabaseType.InParanoid,new Species(Integer.valueOf(tokens[0])), new Species(Integer.valueOf(tokens[1])));
			if ( mappings.size() == 0){
				CroCoLogger.getLogger().fatal("No ortholog mapping found");
				System.exit(1);
			}
			CroCoLogger.getLogger().info("Read ortholog mapping");
			OrthologRepository repo = OrthologRepository.getInstance(service);
			orthologMapping = repo.getOrthologMapping(mappings.get(0));
			CroCoLogger.getLogger().info("Number of mappings:" + orthologMapping.getSize());
		}
		HashSet<Entity> geneSet= null;
		if ( line.hasOption("GeneSetFile")){
			geneSet = getEntity(new File(line.getOptionValue("GeneSetFile")));
		}
		if ( line.hasOption("GeneSetList")){
			if ( geneSet == null) geneSet = new HashSet<Entity>();
			for(String value : line.getOptionValues("GeneSetList")){
				geneSet.add(new Entity(value));
			}
		}
		if ( geneSet != null)CroCoLogger.getLogger().info("Gene set filter size:" + geneSet.size());
		CroCoLogger.getLogger().info("Read:" + FPKM1);
		HashMap<Entity, Float> exp1 = getExpression(FPKM1);
		CroCoLogger.getLogger().info("Expression values:" + exp1.size());
		CroCoLogger.getLogger().info("Read:" + FPKM2);
		HashMap<Entity, Float> exp2 = getExpression(FPKM2);
		CroCoLogger.getLogger().info("Expression values:" + exp2.size());
		
		CroCoLogger.getLogger().info("Filter");
		exp1 = filter(exp1,exp2,orthologMapping);
		exp2 = filter(exp2,exp1,orthologMapping);
		CroCoLogger.getLogger().info("Expression values:" + exp1.size());
		CroCoLogger.getLogger().info("Expression values:" + exp2.size());

		System.out.println(getPearsonCorrelation(exp1,exp2,orthologMapping));
		System.out.println(getSpearmanCorrelation(exp1,exp2,orthologMapping));
		
		File output = new File(line.getOptionValue("output"));
		//write(exp1,exp2,orthologMapping,output);
		writeCat(exp1,exp2,geneSet,orthologMapping,output);
		
	}
	private static void write(HashMap<Entity, Float> exp1,HashMap<Entity, Float> exp2,HashSet<Entity> filter, OrthologMapping orthologMapping, File output) throws Exception{
		HashMap<Integer, HashSet<Entity>> q1 = getQuantification(exp1);
		HashMap<Integer, HashSet<Entity>> q2 = getQuantification(exp2);
		BufferedWriter bw = new BufferedWriter(new FileWriter(output));
		bw.write("Category\tS1\tCategory\tOverlap\n");
		System.out.println(q1.keySet());
		for (Integer index:  q1.keySet()){
			HashSet<Entity> s1 = q1.get(index);
			s1.retainAll(filter);
			HashSet<Entity> s2 = q2.get(index);
			for(int i = 0 ; i< 3;i++){
				if ( q2.containsKey(i+index)){
					s2.addAll(q2.get(i+index));
				}
				if ( q2.containsKey(i-index)){
					s2.addAll(q2.get(i-index));
				}
			}
		
			
		
			bw.write(index + "\t" + s1.size() + "\t"  +  countMatch(s1,s2,orthologMapping)  + "\n"  );
		}
		bw.flush();
		bw.close();
	}
	private static void writeCat(HashMap<Entity, Float> exp1,HashMap<Entity, Float> exp2,HashSet<Entity> filter,OrthologMapping orthologMapping, File output) throws Exception{
		BufferedWriter bw = new BufferedWriter(new FileWriter(output));
		
		HashMap<ExpressionCat, HashSet<Entity>> q1 = getQuantificationCat(exp1);
		HashMap<ExpressionCat, HashSet<Entity>> q2 = getQuantificationCat(exp2);
		bw.write("Category\tCount\tOverlap\tFrac\n");
		for(ExpressionCat c1 : ExpressionCat.values()){
			for(ExpressionCat c2: ExpressionCat.values()){
				HashSet<Entity> s1 = q1.get(c1);
				if ( filter != null) s1.retainAll(filter);
				HashSet<Entity> s2 = q2.get(c2);
				
				int x = countMatch(q1.get(c1),q2.get(c2),orthologMapping);
				float frac = (float)x/(float)s1.size();
				bw.write(c1.name() + "\t" + s1.size() + "\t" + c2.name()+ "\t" + x + "\t" + frac+ "\n") ;
			}
		
		}
		
		
		bw.flush();
		bw.close();
	}
	private static int countMatch(HashSet<Entity> e1, HashSet<Entity> e2, OrthologMapping orthologMapping){
		int count = 0;
		for(Entity e : e1){
			if ( orthologMapping != null){
				Set<Entity> orthologs = orthologMapping.getOrthologs(e);
				
				if ( orthologs != null && orthologs.size() > 0){
						
					for(Entity ortholog : orthologs){
						if ( e2.contains(ortholog)){
							count++;
						}
					}
				}
			}else{
				if ( e2.contains(e))count++;
			}
		}
		return count;
	}
	private static HashSet<Entity> getEntity(File network) throws Exception{
		HashSet<Entity> ret = new HashSet<Entity>();
		BufferedReader br = new BufferedReader(new FileReader(network));
		String line  = null;
		while((line=br.readLine())!=null){
			ret.add(new Entity(line.trim()));
		}
		
		br.close();
		return ret;
	}
	private static HashMap<Entity, Float> filter(HashMap<Entity, Float> exp1, HashMap<Entity, Float> exp2, OrthologMapping orthologMapping) {
		HashMap<Entity, Float> ret = new HashMap<Entity, Float> ();
		for(Entry<Entity, Float> e : exp1.entrySet()){
			
			
			if ( orthologMapping == null){
				if ( exp2.containsKey(e.getKey())){
					ret.put(e.getKey(), e.getValue());
				}
			}else{
				Set<Entity> orthologs = orthologMapping.getOrthologs(e.getKey());
				
				if ( orthologs != null ){
					Set<Entity> reverseOrthologs = orthologMapping.getOrthologs(orthologs.iterator().next());
					
					if ( orthologs.size() == 1 &&reverseOrthologs.size() == 1 )
					for(Entity ortholog : orthologs){
						if ( exp2.containsKey(ortholog)){
							ret.put(e.getKey(), e.getValue());
							break;
						}
					}
				}
			}
			
		}
		
		return ret;
	}
	
	private static double getSpearmanCorrelation(HashMap<Entity, Float> f1, HashMap<Entity, Float> f2,OrthologMapping orthologMapping){
		HashMap<Entity, Integer> f1Ranks = getRanks(f1);
		HashMap<Entity, Integer> f2Ranks = getRanks(f2);
		
		List<Pair<Integer,Integer>> pairs = new ArrayList<Pair<Integer,Integer>>();
		
		for(Entry<Entity, Integer>  h: f1Ranks.entrySet()){
			if ( orthologMapping == null){
				if ( f2Ranks.containsKey(h.getKey())){
					pairs.add(new Pair<Integer,Integer>(h.getValue(),f2Ranks.get(h.getKey())));
				}
			}else{
				Set<Entity> ortholog = orthologMapping.getOrthologs(h.getKey());
				if ( ortholog != null && ortholog.size() > 0){
					for(Entity o : ortholog){
						if ( f2Ranks.containsKey(o)){
							pairs.add(new Pair<Integer,Integer>(h.getValue(),f2Ranks.get(o)));
						}
					}
				}
			}
		}
		return MathUtil.spearman(pairs);
	}
	private static double getPearsonCorrelation(HashMap<Entity, Float> f1, HashMap<Entity, Float> f2,OrthologMapping orthologMapping){
		List<Pair<Float,Float>> pairs = new ArrayList<Pair<Float,Float>>();
		for(Entry<Entity, Float>  h: f1.entrySet()){
			if ( orthologMapping == null){
				if ( f2.containsKey(h.getKey())){
					pairs.add(new Pair<Float,Float>(h.getValue(),f2.get(h.getKey())));
				}
			}else{
				Set<Entity> ortholog = orthologMapping.getOrthologs(h.getKey());
				if ( ortholog != null && ortholog.size() > 0){
					for(Entity o : ortholog){
						if ( f2.containsKey(o)){
							
							pairs.add(new Pair<Float,Float>(h.getValue(),f2.get(o)));
						}
					}
				}
			}
		}
		return MathUtil.pearson(pairs);
	}
	
	
	private static HashMap<Entity,Integer> getRanks(HashMap<Entity,Float> expressions){
		Comparator<Entry<Entity, Float>> cmp = new Comparator<Entry<Entity, Float>>(){
			@Override
			public int compare(Entry<Entity, Float> o1, Entry<Entity, Float> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		};
		
		ArrayList<Entry<Entity, Float>> tmp = new ArrayList<Entry<Entity,Float>>(expressions.entrySet());
		Collections.sort(tmp,cmp);
		
		 HashMap<Entity,Integer> ret = new  HashMap<Entity,Integer>();
		//same ranks?
		 //float lastDifference = -1;
		 for(int i = 0 ; i < tmp.size(); i++){
			/* int sameRankCount=0;
			 for(int j = i+1 ; j < tmp.size(); j++){
				 if ( tmp.get(i).getValue() == tmp.get(j).getValue() )sameRankCount++;
				 el
			 }*/
			 ret.put(tmp.get(i).getKey(), i);
		 }
		 
		 return ret;
	}

	private static HashMap<Integer,HashSet<Entity>> getQuantification(HashMap<Entity,Float> expressions){
		
		 HashMap<Integer,HashSet<Entity>> ret = new HashMap<Integer,HashSet<Entity>>();
		 
		 HashMap<Entity, Integer> ranks = getRanks(expressions);

		 for(Entry<Entity,Integer> e : ranks.entrySet()){
			
			 int index = Math.round( ((float)e.getValue()*100) / ranks.size());
			 if (! ret.containsKey(index)){
				 ret.put(index, new HashSet<Entity>());
			 }
			 ret.get(index).add(e.getKey());
		 }
		 
		 return ret;
	}
	private static HashMap<ExpressionCat,HashSet<Entity>> getQuantificationCat(HashMap<Entity,Float> expressions){
		
		 HashMap<ExpressionCat,HashSet<Entity>> ret = new HashMap<ExpressionCat,HashSet<Entity>>();
		 for(ExpressionCat c1 : ExpressionCat.values()){
			 ret.put(c1, new HashSet<Entity>());
		 }
		 
		 
		 HashMap<Entity, Integer> ranks = getRanks(expressions);

		 int q33Index = Math.round((float)expressions.size()*0.33f); //e.g. 100 expressions q1 = 25
		 int q66Index = Math.round((float)expressions.size()*0.66f);
			
		 for(Entry<Entity,Integer> e : ranks.entrySet()){
			
			 int i = e.getValue();
			// System.out.println(i + " " + q1Index + " " + q3Index + " " + ranks.size() + "\t" + expressions.size());
			 if ( i >= q66Index){
				 ret.get(ExpressionCat.High).add(e.getKey());
			 }else if (  i < q66Index && i >= q33Index ){
				 ret.get(ExpressionCat.Mid).add(e.getKey() );
			 }else if ( i < q33Index ){
				 ret.get( ExpressionCat.Low).add(e.getKey()); 
			 }else{
				new RuntimeException("A bug");
			 }
		 }
		 
		 return ret;
	}
	private static HashMap<Entity,Float> getExpression(List<File> files) throws Exception{
		HashMap<Entity,Float> ret = new HashMap<Entity,Float>();
		HashMap<Entity,List<Float>> combined = new HashMap<Entity,List<Float>>();
		for(File file : files){
		
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();; //skip header
			while((line= br.readLine())!=null){
				
				String[] tokens = line.split("\t");
				Entity geneId = new Entity(tokens[0].split("\\.")[0]);
			
				Float fpkm = Float.valueOf(tokens[9]);
				//if ( fpkm < 1) continue;
				if (! combined.containsKey(geneId)){
					combined.put(geneId, new ArrayList<Float>());
				}
				
				combined.get(geneId).add(fpkm);
				//ret.put(geneId, fpkm);
			}
			
			br.close();
		}
		for(Entry<Entity, List<Float>>e  : combined.entrySet()){
			ret.put(e.getKey(), MathUtil.median(e.getValue()));
		}
		return ret;
	}
	private static Network readNetwork(Integer id,QueryService service) throws Exception{
		ReadNetwork readNetwork= new ReadNetwork();
		readNetwork.setInput(ReadNetwork.QueryService, service);
		readNetwork.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(id,id));
		return readNetwork.operate();
	}
}
