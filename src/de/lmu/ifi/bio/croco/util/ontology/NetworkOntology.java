package de.lmu.ifi.bio.croco.util.ontology;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.cli.CommandLine;

import de.lmu.ifi.bio.croco.connector.DatabaseConnection;
import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.data.CroCoNode;
import de.lmu.ifi.bio.croco.data.CroCoNode.FactorFilter;
import de.lmu.ifi.bio.croco.data.CroCoNode.Filter;
import de.lmu.ifi.bio.croco.data.CroCoNode.GeneralFilter;
import de.lmu.ifi.bio.croco.data.CroCoNode.NameFilter;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.Identifiable;
import de.lmu.ifi.bio.croco.data.NetworkMetaInformation;
import de.lmu.ifi.bio.croco.data.NetworkType;
import de.lmu.ifi.bio.croco.data.Option;
import de.lmu.ifi.bio.croco.data.Species;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.croco.processor.ontology.NetworkOntologyWriter;
import de.lmu.ifi.bio.croco.util.ConsoleParameter;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.CroCoProperties;
import de.lmu.ifi.bio.croco.util.FileUtil;
import de.lmu.ifi.bio.croco.util.Pair;
import de.lmu.ifi.bio.croco.util.ontology.Obo.OboElement;

public class NetworkOntology {         
    private static String KHERADPOUR ="Kheradpour et al., Reliable prediction of regulator targets using 12 Drosophila genomes, Genome Res., 2007";
    private static String NEPH ="Neph et al., Circuitry and Dynamics of Human Transcription Factor Regulatory Networks, Cell, 2012";
    private static File taxObo= new File("data/obo/croco-sp.obo");
    private static File brendaOBO = new File("data/obo/BrendaTissue.obo");
    private static File brendaMapping = new File("data/obo/BrendaMapping");
    
    private static String all ="croco:all";
    private static String root= "croco:root";
    
    private int idCounter=0;
    private LocalService service;
    
    public NetworkOntology()
    {
        this.service = new LocalService();
       
    }
    
    public String getId()
    {
        return "croco:" + idCounter++;
    }
    
    private void addConfidence(CroCoNode<NetworkMetaInformation> root){
        CroCoNode<NetworkMetaInformation> highConf = new CroCoNode<NetworkMetaInformation>(getId(),"High confidence",root,new GeneralFilter(Option.ConfidenceThreshold,"1.0E-6"),root.getData());
        CroCoNode<NetworkMetaInformation> midConf = new CroCoNode<NetworkMetaInformation>(getId(),"Mid. confidence",root,new GeneralFilter(Option.ConfidenceThreshold,"1.0E-5"),root.getData());
        CroCoNode<NetworkMetaInformation> neph = new CroCoNode<NetworkMetaInformation>(getId(),"Neph et. al",root,new GeneralFilter(Option.reference,NEPH),root.getData());
    }
   
    List<CroCoNode<NetworkMetaInformation>> categorie = new ArrayList<CroCoNode<NetworkMetaInformation>>();
    
    public static class LeafNode<E extends Identifiable> extends CroCoNode<E>
    {
        private E data;
        public LeafNode(String id, String name, E data) {
            super(id,name);
            super.setData( new HashSet<E>());
            super.getData().add(data);
        }
        public E getDataPoint()
        {
            return data;
        }
    }

    public  List<CroCoNode<NetworkMetaInformation>> getFlatNodes(Option option,Set<NetworkMetaInformation> data)  throws Exception
    {
        
        if ( option == Option.AntibodyTargetMapped)
        {
            return factoraddOntologyNodes(data);
        }
        List<CroCoNode<NetworkMetaInformation>> ret = new ArrayList<CroCoNode<NetworkMetaInformation>>();
        
        Statement stat = DatabaseConnection.getConnection().createStatement();
        String sql = null;
        if ( option == Option.TaxId )
        {
            sql = String.format("SELECT distinct(tax_id) FROM NetworkHierachy");
        }else if ( option == Option.NetworkType )
        {
            sql = String.format("SELECT distinct(network_type) FROM NetworkHierachy");
        }else{
            sql = String.format("SELECT distinct(value) from NetworkOption where option_id = %d ORDER BY value",option.ordinal());
        }
        CroCoLogger.getLogger().debug(sql);
        stat.execute(sql);
        ResultSet res = stat.getResultSet();
        while(res.next())
        {
            String value = res.getString(1);
            String name = value;
            

            if ( option == Option.TaxId )
            {
               name = Species.getSpecies(Integer.valueOf(value)).getName();
            }
            if ( option == Option.NetworkType )
            {
               name = NetworkType.values()[Integer.valueOf(value)].niceName;
               value =  NetworkType.values()[Integer.valueOf(value)].name();
            }
            
            Filter<NetworkMetaInformation> filter = new GeneralFilter(option,value);
            
            
            CroCoNode<NetworkMetaInformation> node = new CroCoNode<NetworkMetaInformation>(getId(),name,null,filter,data);
            
            ret.add(node);
            
            if ( option == Option.NetworkType && value.equals(NetworkType.TextMining.name()))
            {
                CroCoNode<NetworkMetaInformation> directed = new CroCoNode<NetworkMetaInformation>(getId(),"Directed",node,new GeneralFilter(Option.EdgeType,"Directed"),node.getData());
                CroCoNode<NetworkMetaInformation> undirected = new CroCoNode<NetworkMetaInformation>(getId(),"Undirected",node,new GeneralFilter(Option.EdgeType,"Undirected"),node.getData());
                
                for(CroCoNode<NetworkMetaInformation> d : new CroCoNode[]{directed,undirected})
                {
                    d.setChildren( new ArrayList<CroCoNode<NetworkMetaInformation>>());
                    CroCoNode<NetworkMetaInformation> context = new CroCoNode<NetworkMetaInformation>(getId(),"Filtered",d,new GeneralFilter(Option.TextMiningSpeciesContext,"Yes"),d.getData());
                    CroCoNode<NetworkMetaInformation> nocontext = new CroCoNode<NetworkMetaInformation>(getId(),"Unfiltered",d,new GeneralFilter(Option.TextMiningSpeciesContext,"No"),d.getData());
                    
                    
                }
            }
            
            if ( option == Option.NetworkType && value.equals(NetworkType.Database.name()))
            {
                
                CroCoNode<NetworkMetaInformation> transfac = new CroCoNode<NetworkMetaInformation>(getId(),"Transfac",node,new NameFilter("Transfac9.3"),node.getData());
                CroCoNode<NetworkMetaInformation> redfly = new CroCoNode<NetworkMetaInformation>(getId(),"REDfly",node,new NameFilter("REDfly3.2"),node.getData());
                CroCoNode<NetworkMetaInformation> orgenanno = new CroCoNode<NetworkMetaInformation>(getId(),"ORegAnno",node,new NameFilter("ORegAnno"),node.getData());
                CroCoNode<NetworkMetaInformation> ncipathway = new CroCoNode<NetworkMetaInformation>(getId(),"NCI-Pathway",node,new NameFilter("NCI-Pathway"),node.getData());
                CroCoNode<NetworkMetaInformation> biocarta = new CroCoNode<NetworkMetaInformation>(getId(),"Biocarta",node,new NameFilter("Biocarta"),node.getData());
          
            }
            if ( option == Option.NetworkType && value.equals(NetworkType.TFBS.name()))
            {
                addConfidence(node);
                
                for(CroCoNode<NetworkMetaInformation> child : node.getChildren())
                {
                    addMotifSet(child);
                    
                }   
                
                CroCoNode<NetworkMetaInformation> kherapour = new CroCoNode<NetworkMetaInformation>(getId(),"Kheradpour et al.",node,new GeneralFilter(Option.reference,KHERADPOUR),node.getData());
                CroCoNode<NetworkMetaInformation> kherapour_500 = new CroCoNode<NetworkMetaInformation>(getId(),"Promoter 500.",kherapour,new GeneralFilter(Option.Upstream,"500"),kherapour.getData());
                CroCoNode<NetworkMetaInformation> kherapour_2000 = new CroCoNode<NetworkMetaInformation>(getId(),"Promoter 2000.",kherapour,new GeneralFilter(Option.Downstream,"2000"),kherapour.getData());
                
                for(int i = 0 ; i<= 10 ; i++)
                {
                    float c = (float)i/(float)10;
                    CroCoNode<NetworkMetaInformation> conf_1 = new CroCoNode<NetworkMetaInformation>(getId(),String.format("Confidence %.2f",c),kherapour_500,new GeneralFilter(Option.ConfidenceThreshold,String.format("%.1f",c)),kherapour_500.getData());
                    CroCoNode<NetworkMetaInformation> conf_2 = new CroCoNode<NetworkMetaInformation>(getId(),String.format("Confidence %.2f",c),kherapour_2000,new GeneralFilter(Option.ConfidenceThreshold,String.format("%.1f",c)),kherapour_2000.getData());
                    
                }
                
                
                
            }
            
            if ( option == Option.NetworkType && value.equals(NetworkType.OpenChrom.name()))
            {
                CroCoNode<NetworkMetaInformation> dgf = new CroCoNode<NetworkMetaInformation>(getId(),"Digital Genomic Footprinting (DGF)",node,new GeneralFilter(Option.OpenChromType,"DGF"),node.getData());
                CroCoNode<NetworkMetaInformation> dnase = new CroCoNode<NetworkMetaInformation>(getId(),"DNase I hypersensitive sites (DNase)",node,new GeneralFilter(Option.OpenChromType,"DNase"),node.getData());
                CroCoNode<NetworkMetaInformation> fair = new CroCoNode<NetworkMetaInformation>(getId(),"Formaldehyde-Assisted Isolation of Regulatory Elements (FAIRE)",node,new GeneralFilter(Option.OpenChromType,"Faire"),node.getData());
                
                for(CroCoNode<NetworkMetaInformation> openChromNode : new CroCoNode[]{dgf,dnase,fair})
                {
                    addConfidence(openChromNode);
                    
                    for(CroCoNode<NetworkMetaInformation> child : openChromNode.getChildren())
                    {
                        addMotifSet(child);
                        
                    }
                }
                
                
                //CroCoNode neph = new CroCoNode("Neph et. al",node,new GeneralFilter(Option.reference,"Neph at el., Circuitry and Dynamics of Human Transcription Factor Regulatory Networks, Cell, 2012"),false,node.networks);
                
            }
            
        }
        
        stat.close();
        return ret;
    
    }
    private void addMotifSet (CroCoNode<NetworkMetaInformation> root)
    {
        root.setChildren (new ArrayList<CroCoNode<NetworkMetaInformation>>());
        
        for(String motifSetName : getMotifSet(root.getData()))
        {
            CroCoNode<NetworkMetaInformation> node = new CroCoNode<NetworkMetaInformation>(getId(),motifSetName,root,new GeneralFilter(Option.MotifSet,motifSetName),root.getData());
        }
    }
    private List<String> getMotifSet(Set<NetworkMetaInformation> networks)
    {
        Set<String> ret = new HashSet<String>();
        
        for(NetworkMetaInformation network : networks)
        {
            if ( network.getOptions().containsKey(Option.MotifSet)){
                ret.add(network.getOptions().get(Option.MotifSet));
            }
        }
        List<String> sorted = new ArrayList<String>(ret);
        Collections.sort(sorted);
        return sorted;
    }
    public Set<String> getFactors(Set<NetworkMetaInformation> nodes, NetworkType ... types)
    {
        Set<String> factors = new HashSet<String>();
        for(NetworkMetaInformation node : nodes)
        {
            for(NetworkType type : types)
            {
                if ( node.getOptions().get(Option.NetworkType).equals(type.name()))
                {
                    factors.addAll(node.getFactors());
                }    
            }
            
        }
        return factors;
    }

 
    
    private List<CroCoNode<NetworkMetaInformation>> factoraddOntologyNodes(Set<NetworkMetaInformation> data) throws Exception
    {
        List<CroCoNode<NetworkMetaInformation>> ret = new ArrayList<CroCoNode<NetworkMetaInformation>>();
        
        Statement stat = DatabaseConnection.getConnection().createStatement();
        
        String sql =String.format("SELECT distinct(gene.gene_name) as name,value,tax_id  FROM NetworkOption op JOIN Gene gene on gene.gene = op.value where option_id = %d ORDER BY name",Option.AntibodyTargetMapped.ordinal());
        
        CroCoLogger.getLogger().debug(sql);
        stat.execute(sql);
        ResultSet res = stat.getResultSet();
        HashMap<String,List<Pair<String,Integer>>> names = new HashMap<String,List<Pair<String,Integer>>>();
       
        OrthologRepository orepo = OrthologRepository.getInstance(service);
        
        while(res.next())
        {
            String geneName = res.getString(1);
            String geneId = res.getString(2);
            Integer taxId = res.getInt(3);
            
            if ( !names.containsKey(geneName))
            {
                names.put(geneName, new ArrayList<Pair<String,Integer>>());
            }
            names.get(geneName).add(new Pair<String,Integer>(geneId,taxId));
        }
        for(Entry<String, List<Pair<String, Integer>>> geneName : names.entrySet())
        {
            Set<String> genesOfInterest = new HashSet<String>();
            for(Pair<String, Integer> v : geneName.getValue())
            {
                genesOfInterest.add(v.getFirst().toUpperCase());
                
                for(OrthologMappingInformation omep : orepo.getOrthologMappingInformation())
                {
                    if ( omep.getSpecies1().getTaxId().equals(v.getSecond()) || omep.getSpecies2().getTaxId().equals(v.getSecond()) )
                    {
                        Set<Entity> mappings = orepo.getOrthologMapping(omep).getOrthologs(new Entity(v.getFirst()));
                        
                        if(  mappings == null) continue;
                        
                        for(Entity e : mappings)
                            genesOfInterest.add(e.getIdentifier());
                    }
                }
            }
            
            CroCoNode<NetworkMetaInformation> factorNode = new CroCoNode<NetworkMetaInformation>(getId(),geneName.getKey(),null,new FactorFilter(genesOfInterest),data);
            ret.add(factorNode);
            //parent.getChildren().add(factorNode);
        }
        return ret;
    }
    private void printOntology(PrintWriter pw,CroCoNode<NetworkMetaInformation> root)
    {
        Stack<CroCoNode<NetworkMetaInformation>> stack = new Stack<CroCoNode<NetworkMetaInformation>>();
        Stack<Integer> tabs = new Stack<Integer>();
        
        stack.add(root);
        tabs.add(0);
        
        while(!stack.isEmpty())
        {
            CroCoNode<NetworkMetaInformation> top = stack.pop();
            Integer t = tabs.pop();
            
            for(int i = 0 ; i< t; i++)
            {
                pw.print(" ");
            }
            pw.println(top + " " + (top.getChildren()!=null?top.getChildren().size():"0"));
            
            if ( top.getChildren() != null)
            {
                for ( CroCoNode<NetworkMetaInformation> c : top.getChildren())
                {
                    stack.add(c);
                    tabs.add(new Integer(t+1));
                }
            }
        }
        pw.flush();
    }
    public CroCoNode<NetworkMetaInformation> createNetworkOntology() throws Exception
    {
        
        List<Species> sp = new ArrayList<Species>(Species.knownSpecies);
        List<OrthologMappingInformation> mappings = new ArrayList<OrthologMappingInformation>();
        for(int  i = 0 ; i< sp.size(); i++)
        {
            for(int j = i+1; j< sp.size(); j++)
            {
                mappings.addAll(service.getOrthologMappingInformation(OrthologDatabaseType.EnsemblCompara,sp.get(i),sp.get(j)));
                
            }
        }
        
        for(OrthologMappingInformation mapping : mappings)
        {
            CroCoLogger.debug("Load ortholog mapping: %s", mapping);
            OrthologRepository.getInstance(service).getOrthologMapping(mapping);
        }
        
        List<NetworkMetaInformation> networks = service.getNetworkMetaInformations();
        readFactors(networks);
        
        CroCoNode<NetworkMetaInformation> root = new CroCoNode<NetworkMetaInformation>(NetworkOntology.root,NetworkOntology.root,null,new HashSet<NetworkMetaInformation>(networks));
    
        CroCoNode<NetworkMetaInformation> cellLine = new CroCoNode<NetworkMetaInformation>(getId(),"Tissue/Cell-line",root,null);
        CroCoNode.addOntologyNodes(cellLine, getId(),"Tissue/cell-lines list", getFlatNodes(Option.cellLine, root.getData()));
        CroCoNode.addOntologyNodes(cellLine, getId(),"Tissue ontology", getBrendaOntology(root.getData()));
    
        CroCoNode<NetworkMetaInformation> specie = new CroCoNode<NetworkMetaInformation>(getId(),"Species",root,null);
        CroCoNode.addOntologyNodes(specie,getId(),"Species list", getFlatNodes(Option.TaxId, root.getData()));
        CroCoNode.addOntologyNodes(specie,getId(),"Species taxonomy", getSpeciesObo(root.getData()));
        
        CroCoNode<NetworkMetaInformation> compendium = new CroCoNode<NetworkMetaInformation>(getId(),"Compendium",root, null);
        CroCoNode.addOntologyNodes(compendium,null,null,  getFlatNodes(Option.Compendium,root.getData()));
        
        CroCoNode<NetworkMetaInformation> factor = new CroCoNode<NetworkMetaInformation>(getId(),"ENCODE Gene name",root,null);
        CroCoNode.addOntologyNodes(factor,null,null,  getFlatNodes(Option.AntibodyTargetMapped,root.getData()));
        
        CroCoNode<NetworkMetaInformation> technique = new CroCoNode<NetworkMetaInformation>(getId(),"Experimental technique",root, new GeneralFilter(Option.NetworkType),root.getData());
        CroCoNode.addOntologyNodes(technique,null, null, getFlatNodes(Option.NetworkType,root.getData()));
        Ontology.makeSlim(technique);
        
        CroCoNode<NetworkMetaInformation> devstage = new CroCoNode<NetworkMetaInformation>(getId(),"Development stage",root, new GeneralFilter(Option.developmentStage),root.getData());
        CroCoNode.addOntologyNodes(devstage,null,null,  getFlatNodes(Option.developmentStage,root.getData()));
        
        CroCoNode<NetworkMetaInformation> treatment = new CroCoNode<NetworkMetaInformation>(getId(),"Treatment",root, new GeneralFilter(Option.treatment),root.getData());
        CroCoNode.addOntologyNodes(treatment,null, null, getFlatNodes(Option.treatment,root.getData()));
        
        //this.printOntology(new PrintWriter(System.out), root);
        //System.exit(1);
        
        return root;
    }
    private List<CroCoNode<NetworkMetaInformation>> getBrendaOntology(Set<NetworkMetaInformation> data) throws Exception
    {
       // String oboRootElement = "BTO:0000000";
        
        Obo obo = new Obo(brendaOBO);
        
        //OboElement rootElement = obo.getElement(oboRootElement);
        
        //CroCoNode<NetworkMetaInformation> brenda = new CroCoNode<NetworkMetaInformation>(getId(),"Tissues",null, data);
        
        HashMap<String, String> mapping = FileUtil.mappingFileReader(0, 1, brendaMapping).readMappingFile();
        
        HashSet<String> notFound = new HashSet<String>();

        HashMap<OboElement,HashSet<NetworkMetaInformation>> elementsToNetwork  =new HashMap<OboElement,HashSet<NetworkMetaInformation>>();
        
        //init mapping
        for(NetworkMetaInformation nh : data)
        {
            String cellLine = nh.getOptions().get(Option.cellLine) ;
            if ( cellLine == null)
                continue;
            String map = mapping.get(cellLine);
            if ( map == null){
                notFound.add(cellLine);
                continue;
            }

            map = map.replace("(non-specific)", "").trim();
            OboElement element = obo.getElement(map);
            if (! elementsToNetwork.containsKey(element))
            {
                elementsToNetwork.put(element, new HashSet<NetworkMetaInformation>());
            }
            elementsToNetwork.get(element).add(nh);
        }
          
        CroCoLogger.getLogger().warn("Not mapped:" + notFound);
        
        List<CroCoNode<NetworkMetaInformation>> ret = new ArrayList<CroCoNode<NetworkMetaInformation>>();
        for(OboElement root :obo.getRoots(false) )
        {
           // CroCoNode<NetworkMetaInformation> rootNode = new CroCoNode<NetworkMetaInformation>(root.id,root.name,null, data);
            
            CroCoNode<NetworkMetaInformation> node = Ontology.toOntology(elementsToNetwork,root,obo,true);
            

            if ( node.getData().size() >0)
                ret.add(node);
            
        }

        return ret;
    }
    private List<CroCoNode<NetworkMetaInformation>> getSpeciesObo(Set<NetworkMetaInformation> data) throws Exception{
        Obo obo = new Obo(taxObo);
        
        HashMap<OboElement,HashSet<NetworkMetaInformation>> elementsToNetwork  =new HashMap<OboElement,HashSet<NetworkMetaInformation>>();
        
        for(OboElement element : obo.elements.values() )
        {
            GeneralFilter filter = new GeneralFilter(Option.TaxId,element.id.replaceAll("NCBITaxon:", ""));
            HashSet<NetworkMetaInformation> networks = new HashSet<NetworkMetaInformation>();
            
            for(NetworkMetaInformation nh : data)
            {
                if ( filter.accept(nh))
                    networks.add(nh);  
            }
            elementsToNetwork.put(element, networks);
        }
        List<CroCoNode<NetworkMetaInformation>> ret = new ArrayList<CroCoNode<NetworkMetaInformation>>();
        
        for(OboElement root :obo.getRoots(false) )
        {
           // CroCoNode<NetworkMetaInformation> rootNode = new CroCoNode<NetworkMetaInformation>(root.id,root.name,null, data);
            
            CroCoNode<NetworkMetaInformation> node = Ontology.toOntology(elementsToNetwork,root,obo,true);
            if ( node.getData().size() >0)
                ret.add(node);
            
        }
        return ret;
    }

    
    private void readFactors(List<NetworkMetaInformation> nodes) throws IOException
    {
        HashMap<Integer,NetworkMetaInformation> groupIdToNetwork = new HashMap<Integer,NetworkMetaInformation>();
        for(NetworkMetaInformation nh : nodes ) 
        {
            groupIdToNetwork.put(nh.getGroupId(), nh);
        }
        
        File file = CroCoProperties.getInstance().getAsFile("service.FactorFile");
        CroCoLogger.debug("Read: %s", file);
        
        if ( !file.exists())
            throw new IOException("Cannot find factor file:" + file);

        
        Iterator<String> it = FileUtil.getLineIterator(file);

        while(it.hasNext())
        {
            String[] tokens = it.next().split("\t");
            Integer groupId = Integer.valueOf(tokens[0]);
            if (! groupIdToNetwork.containsKey(groupId))
            {
                CroCoLogger.getLogger().warn("Cannot find network with id:" + groupId );
                continue;
            }
            groupIdToNetwork.get(groupId).addOption(Option.FactorList, tokens[1]);
        }

    }

    public static void main(String[] args) throws Exception
    {
        
        ConsoleParameter parameter = new ConsoleParameter();
        parameter.register(
                NetworkOntologyWriter.ONTOLOGY_OUT,
                NetworkOntologyWriter.ONTOLOGY_MAPPING_OUT
        );
        CommandLine cmdLine = parameter.parseCommandLine(args, NetworkOntology.class);
        NetworkOntology onto = new NetworkOntology();
        
        CroCoNode<NetworkMetaInformation> root = onto.createNetworkOntology();
        
        CroCoLogger.getLogger().info("Write ontology");
        Obo.writeOntology(root,NetworkOntologyWriter.ONTOLOGY_OUT.getValue(cmdLine),NetworkOntologyWriter.ONTOLOGY_MAPPING_OUT.getValue(cmdLine));
        
        LocalService service = new LocalService();
        
        System.out.println("Read ontoloy");
        service.getNetworkOntology(false);
        
    }
}
