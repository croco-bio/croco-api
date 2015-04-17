package de.lmu.ifi.bio.croco.util.ontology;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import com.google.common.base.Joiner;

import de.lmu.ifi.bio.croco.connector.DatabaseConnection;
import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.data.CroCoNode;
import de.lmu.ifi.bio.croco.data.CroCoNode.FactorFilter;
import de.lmu.ifi.bio.croco.data.CroCoNode.Filter;
import de.lmu.ifi.bio.croco.data.CroCoNode.GeneralFilter;
import de.lmu.ifi.bio.croco.data.CroCoNode.NameFilter;
import de.lmu.ifi.bio.croco.data.Entity;
import de.lmu.ifi.bio.croco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.croco.data.NetworkType;
import de.lmu.ifi.bio.croco.data.Option;
import de.lmu.ifi.bio.croco.data.Species;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.croco.operation.ortholog.OrthologRepository;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.CroCoProperties;
import de.lmu.ifi.bio.croco.util.FileUtil;
import de.lmu.ifi.bio.croco.util.Pair;
import de.lmu.ifi.bio.croco.util.ontology.OboReader.OboElement;

public class NetworkOntology {
    private static String KHERAPOUR ="Kherapour et al., Reliable prediction of regulator targets using 12 Drosophila genomes, Genome Res., 2007";
    private static String NEPH ="Neph et al., Circuitry and Dynamics of Human Transcription Factor Regulatory Networks, Cell, 2012";
    private static File taxObo= new File("data/croco-sp.obo");
    private static File brendaOBO = new File("data/BrendaTissue.obo");
    private static File brendaMapping = new File("data/BrendaMapping");
    
    
    private LocalService service;
    
    public NetworkOntology()
    {
        this.service = new LocalService();
       
    }
    
    private void addConfidence(CroCoNode<NetworkHierachyNode> root){
        root.setChildren( new ArrayList<CroCoNode<NetworkHierachyNode>>());
        CroCoNode<NetworkHierachyNode> highConf = new CroCoNode<NetworkHierachyNode>("High confidence",root,new GeneralFilter(Option.ConfidenceThreshold,"1.0E-6"),false,root.getData());
        CroCoNode<NetworkHierachyNode> midConf = new CroCoNode<NetworkHierachyNode>("Mid. confidence",root,new GeneralFilter(Option.ConfidenceThreshold,"1.0E-5"),false,root.getData());
        
        CroCoNode<NetworkHierachyNode> neph = new CroCoNode<NetworkHierachyNode>("Neph et. al",root,new GeneralFilter(Option.reference,NEPH),true,root.getData());
        
        root.getChildren().add(highConf);
        
        root.getChildren().add(midConf);
        root.getChildren().add(neph);
       
    }
   
    List<CroCoNode<NetworkHierachyNode>> categorie = new ArrayList<CroCoNode<NetworkHierachyNode>>();
    
    public static class LeafNode<E> extends CroCoNode<E>
    {
        public LeafNode(String name, E data) {
            super(name);
            super.setData( new HashSet<E>());
            super.getData().add(data);
        }
        
    }
    

    public  void addOntologyNodes(Option option,CroCoNode<NetworkHierachyNode> parent)  throws Exception
    {
        parent.setChildren(new ArrayList<CroCoNode<NetworkHierachyNode>>());
        
        if ( option == Option.AntibodyTargetMapped)
        {
            factoraddOntologyNodes(parent);
            return;
        }
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
            
            Filter<NetworkHierachyNode> filter = new GeneralFilter(option,value);
            
            
            CroCoNode<NetworkHierachyNode> node = new CroCoNode<NetworkHierachyNode>(name,parent,filter,true,parent.getData());
            
            parent.getChildren().add(node);
            
         
            if ( option == Option.NetworkType && value.equals(NetworkType.TextMining.name()))
            {
                node.setChildShowRootChildren(false);
                
                CroCoNode<NetworkHierachyNode> directed = new CroCoNode<NetworkHierachyNode>("Directed",node,new GeneralFilter(Option.EdgeType,"Directed"),false,node.getData());
                CroCoNode<NetworkHierachyNode> undirected = new CroCoNode<NetworkHierachyNode>("Undirected",node,new GeneralFilter(Option.EdgeType,"Undirected"),false,node.getData());
                
                node.setChildren(new ArrayList<CroCoNode<NetworkHierachyNode>>());
                node.getChildren().add(directed);
                node.getChildren().add(undirected);
                
                
                for(CroCoNode<NetworkHierachyNode> d : new CroCoNode[]{directed,undirected})
                {
                    d.setChildren( new ArrayList<CroCoNode<NetworkHierachyNode>>());
                    CroCoNode<NetworkHierachyNode> context = new CroCoNode<NetworkHierachyNode>("Species filtered",d,new GeneralFilter(Option.TextMiningSpeciesContext,"Yes"),true,d.getData());
                    CroCoNode<NetworkHierachyNode> nocontext = new CroCoNode<NetworkHierachyNode>("Species unfiltered",d,new GeneralFilter(Option.TextMiningSpeciesContext,"No"),true,d.getData());
                    
                    d.getChildren().add(context);
                    d.getChildren().add(nocontext);
                    
                }
            }
            
            if ( option == Option.NetworkType && value.equals(NetworkType.Database.name()))
            {
                node.setChildShowRootChildren(false);
                
                CroCoNode<NetworkHierachyNode> transfac = new CroCoNode<NetworkHierachyNode>("Transfac",node,new NameFilter("Transfac9.3"),true,node.getData());
                CroCoNode<NetworkHierachyNode> redfly = new CroCoNode<NetworkHierachyNode>("REDfly",node,new NameFilter("REDfly3.2"),true,node.getData());
                CroCoNode<NetworkHierachyNode> orgenanno = new CroCoNode<NetworkHierachyNode>("ORegAnno",node,new NameFilter("ORegAnno"),true,node.getData());
                CroCoNode<NetworkHierachyNode> ncipathway = new CroCoNode<NetworkHierachyNode>("NCI-Pathway",node,new NameFilter("NCI-Pathway"),true,node.getData());
                CroCoNode<NetworkHierachyNode> biocarta = new CroCoNode<NetworkHierachyNode>("Biocarta",node,new NameFilter("Biocarta"),true,node.getData());
                
                node.setChildren( new ArrayList<CroCoNode<NetworkHierachyNode>>());
                node.getChildren().add(transfac);
                node.getChildren().add(redfly);
                node.getChildren().add(orgenanno);
                node.getChildren().add(ncipathway);
                node.getChildren().add(biocarta);
                
                
                
            }
            if ( option == Option.NetworkType && value.equals(NetworkType.TFBS.name()))
            {
                node.setChildShowRootChildren(false);
                addConfidence(node);
                
                for(CroCoNode<NetworkHierachyNode> child : node.getChildren())
                {
                    addMotifSet(child);
                    
                }   
                
                CroCoNode<NetworkHierachyNode> kherapour = new CroCoNode<NetworkHierachyNode>("Kherapour et al.",node,new GeneralFilter(Option.reference,KHERAPOUR),false,node.getData());
                CroCoNode<NetworkHierachyNode> kherapour_500 = new CroCoNode<NetworkHierachyNode>("Promoter 500.",kherapour,new GeneralFilter(Option.Upstream,"500"),false,kherapour.getData());
                CroCoNode<NetworkHierachyNode> kherapour_2000 = new CroCoNode<NetworkHierachyNode>("Promoter 2000.",kherapour,new GeneralFilter(Option.Downstream,"2000"),false,kherapour.getData());
                
                
                kherapour.setChildren( new ArrayList<CroCoNode<NetworkHierachyNode>>());
                kherapour.getChildren().add(kherapour_500);
                kherapour.getChildren().add(kherapour_2000);
                
                kherapour_500.setChildren( new ArrayList<CroCoNode<NetworkHierachyNode>>());
                kherapour_2000.setChildren ( new ArrayList<CroCoNode<NetworkHierachyNode>>());
                
                for(int i = 0 ; i<= 10 ; i++)
                {
                    float c = (float)i/(float)10;
                    CroCoNode<NetworkHierachyNode> conf_1 = new CroCoNode<NetworkHierachyNode>(String.format("Kherapour conf. %.2f",c),kherapour_500,new GeneralFilter(Option.ConfidenceThreshold,String.format("%.1f",c)),true,kherapour_500.getData());
                    CroCoNode<NetworkHierachyNode> conf_2 = new CroCoNode<NetworkHierachyNode>(String.format("Kherapour conf. %.2f",c),kherapour_2000,new GeneralFilter(Option.ConfidenceThreshold,String.format("%.1f",c)),true,kherapour_2000.getData());
                    
                    kherapour_500.getChildren().add(conf_1);
                    kherapour_2000.getChildren().add(conf_2);
                    
                    //CroCoNode conf_2 = new CroCoNode(String.format("Kherapour conf. %.2f",c),kherapour_2000,new GeneralFilter(Option.reference,KHERAPOUR),false,node.networks);
                    
                         
                }
                
                node.getChildren().add(kherapour);
            }
            
            if ( option == Option.NetworkType && value.equals(NetworkType.OpenChrom.name()))
            {
                node.setChildShowRootChildren(false);
                
                CroCoNode<NetworkHierachyNode> dgf = new CroCoNode<NetworkHierachyNode>("Digital Genomic Footprinting (DGF)",node,new GeneralFilter(Option.OpenChromType,"DGF"),false,node.getData());
                CroCoNode<NetworkHierachyNode> dnase = new CroCoNode<NetworkHierachyNode>("DNase I hypersensitive sites (DNase)",node,new GeneralFilter(Option.OpenChromType,"DNase"),false,node.getData());
                CroCoNode<NetworkHierachyNode> fair = new CroCoNode<NetworkHierachyNode>("Formaldehyde-Assisted Isolation of Regulatory Elements (FAIRE)",node,new GeneralFilter(Option.OpenChromType,"Faire"),false,node.getData());
                node.setChildren ( new ArrayList<CroCoNode<NetworkHierachyNode>>());
                node.getChildren().add(dgf);
                node.getChildren().add(dnase);
                node.getChildren().add(fair);
                
                for(CroCoNode<NetworkHierachyNode> openChromNode : new CroCoNode[]{dgf,dnase,fair})
                {
                    addConfidence(openChromNode);
                    
                    for(CroCoNode<NetworkHierachyNode> child : openChromNode.getChildren())
                    {
                        addMotifSet(child);
                        
                    }
                }
                
                
                //CroCoNode neph = new CroCoNode("Neph et. al",node,new GeneralFilter(Option.reference,"Neph at el., Circuitry and Dynamics of Human Transcription Factor Regulatory Networks, Cell, 2012"),false,node.networks);
                
            }
            
        }
        
        
        stat.close();
    
    }
    private void addMotifSet (CroCoNode<NetworkHierachyNode> root)
    {
        root.setChildren (new ArrayList<CroCoNode<NetworkHierachyNode>>());
        
        for(String motifSetName : getMotifSet(root.getData()))
        {
            CroCoNode<NetworkHierachyNode> node = new CroCoNode<NetworkHierachyNode>(motifSetName,root,new GeneralFilter(Option.MotifSet,motifSetName),true,root.getData());
            root.getChildren().add(node);
        }
        
    }
    private List<String> getMotifSet(Set<NetworkHierachyNode> networks)
    {
        Set<String> ret = new HashSet<String>();
        
        for(NetworkHierachyNode network : networks)
        {
            if ( network.getOptions().containsKey(Option.MotifSet)){
                ret.add(network.getOptions().get(Option.MotifSet));
            }
        }
        List<String> sorted = new ArrayList<String>(ret);
        Collections.sort(sorted);
        return sorted;
    }
    public Set<String> getFactors(Set<NetworkHierachyNode> nodes, NetworkType ... types)
    {
        Set<String> factors = new HashSet<String>();
        for(NetworkHierachyNode node : nodes)
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
    public void persistNetworkOntology(CroCoNode<NetworkHierachyNode> root) throws Exception
    {
        PreparedStatement stat = DatabaseConnection.getConnection().prepareStatement("INSERT INTO NetworkOntology(node_id,parent_node_id,name,network_ids) values(?,?,?,?)");

        Stack<CroCoNode<NetworkHierachyNode>> stack = new Stack<CroCoNode<NetworkHierachyNode>>();
        stack.add(root);
        int id = 0;
        HashMap<CroCoNode<NetworkHierachyNode>,Integer> nodeToId = new HashMap<CroCoNode<NetworkHierachyNode>,Integer>();
        
        while(!stack.isEmpty())
        {
            CroCoNode<NetworkHierachyNode> top = stack.pop();
            if ( top.getData().size() == 0) continue;
            Integer topId = nodeToId.containsKey(top)?nodeToId.get(top):id++;
            
            Integer parentId = null;
            
            if ( top.getParent() == null)
            {
                parentId = -1;
            }else
            {
                parentId = nodeToId.get(top.getParent());
            }
            nodeToId.put(top,topId);
            List<Integer> ids = new ArrayList<Integer>();
            for(NetworkHierachyNode nh : top.getData())
            {
                ids.add(nh.getGroupId());
            }
            
            stat.setInt(1, topId);
            stat.setInt(2,parentId);
            
            stat.setString(3, top.getName());
            stat.setString(4,Joiner.on(" ").join(ids));

            stat.addBatch();
            
            if ( top.getChildren() != null)
            {
                for(CroCoNode<NetworkHierachyNode> child : top.getChildren())
                {
                    stack.add(child);
                }
            }
        }
        stat.executeBatch();
    }

   
    private void factoraddOntologyNodes(CroCoNode<NetworkHierachyNode> parent) throws Exception
    {
        Statement stat = DatabaseConnection.getConnection().createStatement();
        
        String sql ="SELECT distinct(gene.gene_name) as name,value,tax_id  FROM NetworkOption op JOIN Gene gene on gene.gene = op.value where option_id = 15 ORDER BY name";
        
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
            
            CroCoNode<NetworkHierachyNode> factorNode = new CroCoNode<NetworkHierachyNode>(geneName.getKey(),parent,new FactorFilter(genesOfInterest),true,parent.getData());
            parent.getChildren().add(factorNode);
        }
    }
    
    public CroCoNode<NetworkHierachyNode> createNetworkOntology() throws Exception
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
        
        List<NetworkHierachyNode> networks = service.getNetworkHierachy();
        readFactors(networks);
        
        CroCoNode<NetworkHierachyNode> root = new CroCoNode<NetworkHierachyNode>("Root",null,true,new HashSet<NetworkHierachyNode>(networks));
      
        CroCoNode<NetworkHierachyNode> cellLine = new CroCoNode<NetworkHierachyNode>("Tissue/Cell-line",root, new GeneralFilter(Option.cellLine),false,root.getData());
        CroCoNode<NetworkHierachyNode> cellLine_flat = new CroCoNode<NetworkHierachyNode>("All tissue/cell-lines",cellLine,false,cellLine.getData());
        cellLine.setChildren(new ArrayList<CroCoNode<NetworkHierachyNode>>());
        cellLine.getChildren().add(cellLine_flat);
        addOntologyNodes(Option.cellLine,cellLine_flat);
        addBrenda(cellLine);
        
        CroCoNode<NetworkHierachyNode> specie = new CroCoNode<NetworkHierachyNode>("Species",root, new GeneralFilter(Option.TaxId),false,root.getData());
        CroCoNode<NetworkHierachyNode> specie_flat = new CroCoNode<NetworkHierachyNode>("All species",specie,false,specie.getData());
        specie.setChildren(new ArrayList<CroCoNode<NetworkHierachyNode>>());
        specie.getChildren().add(specie_flat);
        addOntologyNodes(Option.TaxId,specie_flat);
        addSpeciesObo(specie,taxObo,"NCBITaxon:33213");
        
        
        CroCoNode<NetworkHierachyNode> compendium = new CroCoNode<NetworkHierachyNode>("Compendium",root, new GeneralFilter(Option.Compendium),false,root.getData());
        addOntologyNodes(Option.Compendium,compendium);
       
      
        CroCoNode<NetworkHierachyNode> factor = new CroCoNode<NetworkHierachyNode>("ENCODE (ChIP)-Factor",root, false,root.getData());
        addOntologyNodes(Option.AntibodyTargetMapped,factor);
        CroCoNode<NetworkHierachyNode> allFactors = new CroCoNode<NetworkHierachyNode>("All factors",factor,true,factor.getData());
        factor.getChildren().add(allFactors);
        
        
        CroCoNode<NetworkHierachyNode> technique = new CroCoNode<NetworkHierachyNode>("Experimental technique",root, new GeneralFilter(Option.NetworkType),false,root.getData());
        addOntologyNodes(Option.NetworkType,technique);
    
        CroCoNode<NetworkHierachyNode> devstage = new CroCoNode<NetworkHierachyNode>("Development stage",root, new GeneralFilter(Option.developmentStage),false,root.getData());
        addOntologyNodes(Option.developmentStage,devstage);
    
        CroCoNode<NetworkHierachyNode> treatment = new CroCoNode<NetworkHierachyNode>("Treatment",root, new GeneralFilter(Option.treatment),false,root.getData());
        addOntologyNodes(Option.treatment,treatment);
    
        root.setChildren (new ArrayList<CroCoNode<NetworkHierachyNode>>());
        
        root.getChildren().add(compendium);
        root.getChildren().add(cellLine);
        root.getChildren().add(factor);
        root.getChildren().add(specie);
        root.getChildren().add(technique);
        root.getChildren().add(devstage);
        root.getChildren().add(treatment); 
        
        return root;
    }
    private void addBrenda(CroCoNode <NetworkHierachyNode>root) throws Exception
    {
        String oboRootElement = "BTO:0000000";
        
        OboReader reader = new OboReader(brendaOBO);
        
        OboElement rootElement = reader.getElement(oboRootElement);
        
        CroCoNode<NetworkHierachyNode> brenda = new CroCoNode<NetworkHierachyNode>("Tissues",root, false,root.getData());
        if ( root.getChildren() == null)
            root.setChildren(new ArrayList<CroCoNode<NetworkHierachyNode>>());
        
        root.getChildren().add(brenda);
        
        HashMap<String, String> mapping = FileUtil.mappingFileReader(0, 1, brendaMapping).readMappingFile();
        
        HashSet<String> notFound = new HashSet<String>();

        HashMap<OboElement,HashSet<NetworkHierachyNode>> elementsToNetwork  =new HashMap<OboElement,HashSet<NetworkHierachyNode>>();
        
        //init mapping
        for(NetworkHierachyNode nh : root.getData())
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
            OboElement element = reader.getElement(map);
            if (! elementsToNetwork.containsKey(element))
            {
                elementsToNetwork.put(element, new HashSet<NetworkHierachyNode>());
            }
            elementsToNetwork.get(element).add(nh);
        }
          
        CroCoLogger.getLogger().warn("Not mapped:" + notFound);
        
        Ontology.addObo(elementsToNetwork,root,rootElement);
        

    }
    private void addSpeciesObo(CroCoNode<NetworkHierachyNode> root, File obo, String oboRootElement) throws Exception{
        OboReader reader = new OboReader(obo);
        
        HashMap<OboElement,HashSet<NetworkHierachyNode>> elementsToNetwork  =new HashMap<OboElement,HashSet<NetworkHierachyNode>>();
        
        for(OboElement element : reader.elements.values() )
        {
            GeneralFilter filter = new GeneralFilter(Option.TaxId,element.id.replaceAll("NCBITaxon:", ""));
            HashSet<NetworkHierachyNode> networks = new HashSet<NetworkHierachyNode>();
            
            for(NetworkHierachyNode nh : root.getData())
            {
                if ( filter.accept(nh))
                    networks.add(nh);  
            }
            elementsToNetwork.put(element, networks);
        }

        OboElement rootElement = reader.getElement(oboRootElement);
        
        Ontology.addObo(elementsToNetwork,root,rootElement);
        
    }

    private static String FACTOR_FILE="factors.gz";
    
    private void readFactors(List<NetworkHierachyNode> nodes) throws Exception
    {
        HashMap<Integer,NetworkHierachyNode> groupIdToNetwork = new HashMap<Integer,NetworkHierachyNode>();
        for(NetworkHierachyNode nh : nodes ) 
        {
            groupIdToNetwork.put(nh.getGroupId(), nh);
        }
        
        File file = new File(String.format("%s/%s",CroCoProperties.getInstance().getValue("service.Networks"),FACTOR_FILE));
        CroCoLogger.debug("Read: %s", file);
        if ( file.exists())
        {
            Iterator<String> it = FileUtil.getLineIterator(file);
         
            while(it.hasNext())
            {
                String[] tokens = it.next().split("\t");
                Integer groupId = Integer.valueOf(tokens[0]);
                groupIdToNetwork.get(groupId).addOption(Option.FactorList, tokens[1]);
            }
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        NetworkOntology onto = new NetworkOntology();
        
        CroCoNode<NetworkHierachyNode> root = onto.createNetworkOntology();
        onto.persistNetworkOntology(root);
    }
}
