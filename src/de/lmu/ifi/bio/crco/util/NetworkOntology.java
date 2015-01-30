package de.lmu.ifi.bio.crco.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import com.google.common.base.Joiner;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.data.CroCoNode;
import de.lmu.ifi.bio.crco.data.CroCoNode.Filter;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologRepository;

public class NetworkOntology {
    private static String KHERAPOUR ="Kherapour et al., Reliable prediction of regulator targets using 12 Drosophila genomes, Genome Res., 2007";
    private static String NEPH ="Neph et al., Circuitry and Dynamics of Human Transcription Factor Regulatory Networks, Cell, 2012";
    
    private void addConfidence(CroCoNode root){
        root.children = new ArrayList<CroCoNode>();
        CroCoNode highConf = new CroCoNode("High confidence",root,new GeneralFilter(Option.ConfidenceThreshold,"1.0E-6"),false,root.networks);
        CroCoNode midConf = new CroCoNode("Mid. confidence",root,new GeneralFilter(Option.ConfidenceThreshold,"1.0E-5"),false,root.networks);
        
        CroCoNode neph = new CroCoNode("Neph et. al",root,new GeneralFilter(Option.reference,NEPH),true,root.networks);
        
        root.children.add(highConf);
        
        root.children.add(midConf);
        root.children.add(neph);
       
    }
   
    List<CroCoNode> categorie = new ArrayList<CroCoNode>();
    
    public static class LeafNode extends CroCoNode
    {
        public LeafNode(String name, NetworkHierachyNode network) {
            super(name);
            super.networks = new HashSet<NetworkHierachyNode>();
            super.networks.add(network);
        }
        
    }
    
   
    static public class FactorFilter implements Filter
    {
        Set<String> factors = null;
        public FactorFilter(Set<String> factors)
        {
            this.factors = factors;
        }
        @Override
        public boolean accept(NetworkHierachyNode nh) {
            for(String factor : factors)
            {
                if ( nh.getFactors().contains(factor)) return true;
            }
            return false;
        }
        
    }
    
    static public class NameFilter implements Filter
    {
        String name;
        
        public NameFilter(String name)
        {
            this.name = name;
        }

        @Override
        public boolean accept(NetworkHierachyNode nh) {
            return nh.getName().equals(name);
        }
    }
    
    static public class GeneralFilter implements Filter
    {
        Option option;
        String value;
        boolean debug = false;
        
        GeneralFilter(Option option, String value,boolean debug)
        {
            this.option = option;
            this.value = value;
            this.debug  = debug;
        }
       
        public GeneralFilter(Option option, String value)
        {
            this.option = option;
            this.value = value;
        }
       
        public GeneralFilter(Option option)
        {
            this.option = option;
        }
        
        
        @Override
        public boolean accept(NetworkHierachyNode nh) {
            if ( debug )
            {
                CroCoLogger.debug("Apply option/id/value: %s/%d/%s on  name/group_id: %s (%d) with options: %s ",option.name(),option.ordinal(),value,nh.getName(),nh.getGroupId(),nh.getOptions().toString());
            }
                
            if (! nh.getOptions().containsKey(option)) return false;
            
            if ( value != null)
                return nh.getOptions().get(option).equals(value);
            
            return true;
        }
        public String toString()
        {
            return String.format("%s = %s",option.name(),value);
        }
    }
    public  void addOntologyNodes(Option option,CroCoNode parent)  throws Exception
    {
        parent.children = new ArrayList<CroCoNode>();
        
        if ( option == Option.AntibodyTargetMapped)
        {
            factoraddOntologyNodes(parent);
            return;
        }
        Statement stat = DatabaseConnection.getConnection().createStatement();
        String sql = null;
        if ( option == Option.TaxId )
        {
            sql = String.format("SELECT distinct(tax_id) FROM NetworkHierachy where has_network = 1");
        }else if ( option == Option.NetworkType )
        {
            sql = String.format("SELECT distinct(network_type) FROM NetworkHierachy where has_network = 1");
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
            
            Filter filter = new GeneralFilter(option,value);
            
            
            CroCoNode node = new CroCoNode(name,parent,filter,true,parent.networks);
            
            parent.children.add(node);
            
         
            if ( option == Option.NetworkType && value.equals(NetworkType.TextMining.name()))
            {
                node.childShowRootChildren = false;
                
                CroCoNode directed = new CroCoNode("Directed",node,new GeneralFilter(Option.EdgeType,"Directed"),false,node.networks);
                CroCoNode undirected = new CroCoNode("Undirected",node,new GeneralFilter(Option.EdgeType,"Undirected"),false,node.networks);
                
                node.children = new ArrayList<CroCoNode>();
                node.children.add(directed);
                node.children.add(undirected);
                
                
                for(CroCoNode d : new CroCoNode[]{directed,undirected})
                {
                    d.children = new ArrayList<CroCoNode>();
                    CroCoNode context = new CroCoNode("Species filtered",d,new GeneralFilter(Option.TextMiningSpeciesContext,"Yes"),true,d.networks);
                    CroCoNode nocontext = new CroCoNode("Species unfiltered",d,new GeneralFilter(Option.TextMiningSpeciesContext,"No"),true,d.networks);
                    
                    d.children.add(context);
                    d.children.add(nocontext);
                    
                }
            }
            
            if ( option == Option.NetworkType && value.equals(NetworkType.Database.name()))
            {
                node.childShowRootChildren = false;
                
                CroCoNode transfac = new CroCoNode("Transfac",node,new NameFilter("Transfac9.3"),true,node.networks);
                CroCoNode redfly = new CroCoNode("REDfly",node,new NameFilter("REDfly3.2"),true,node.networks);
                CroCoNode orgenanno = new CroCoNode("ORegAnno",node,new NameFilter("ORegAnno"),true,node.networks);
                CroCoNode ncipathway = new CroCoNode("NCI-Pathway",node,new NameFilter("NCI-Pathway"),true,node.networks);
                CroCoNode biocarta = new CroCoNode("Biocarta",node,new NameFilter("Biocarta"),true,node.networks);
                
                node.children = new ArrayList<CroCoNode>();
                node.children.add(transfac);
                node.children.add(redfly);
                node.children.add(orgenanno);
                node.children.add(ncipathway);
                node.children.add(biocarta);
                
                
                
            }
            if ( option == Option.NetworkType && value.equals(NetworkType.TFBS.name()))
            {
                node.childShowRootChildren = false;
                addConfidence(node);
                
                for(CroCoNode child : node.children)
                {
                    addMotifSet(child);
                    
                }   
                
                CroCoNode kherapour = new CroCoNode("Kherapour et al.",node,new GeneralFilter(Option.reference,KHERAPOUR),false,node.networks);
                CroCoNode kherapour_500 = new CroCoNode("Promoter 500.",kherapour,new GeneralFilter(Option.Upstream,"500"),false,kherapour.networks);
                CroCoNode kherapour_2000 = new CroCoNode("Promoter 2000.",kherapour,new GeneralFilter(Option.Downstream,"2000"),false,kherapour.networks);
                
                
                kherapour.children = new ArrayList<CroCoNode>();
                kherapour.children.add(kherapour_500);
                kherapour.children.add(kherapour_2000);
                
                kherapour_500.children = new ArrayList<CroCoNode>();
                kherapour_2000.children = new ArrayList<CroCoNode>();
                
                for(int i = 0 ; i<= 10 ; i++)
                {
                    float c = (float)i/(float)10;
                    CroCoNode conf_1 = new CroCoNode(String.format("Kherapour conf. %.2f",c),kherapour_500,new GeneralFilter(Option.ConfidenceThreshold,String.format("%.1f",c)),true,kherapour_500.networks);
                    CroCoNode conf_2 = new CroCoNode(String.format("Kherapour conf. %.2f",c),kherapour_2000,new GeneralFilter(Option.ConfidenceThreshold,String.format("%.1f",c)),true,kherapour_2000.networks);
                    
                    kherapour_500.children.add(conf_1);
                    kherapour_2000.children.add(conf_2);
                    
                    //CroCoNode conf_2 = new CroCoNode(String.format("Kherapour conf. %.2f",c),kherapour_2000,new GeneralFilter(Option.reference,KHERAPOUR),false,node.networks);
                    
                         
                }
                
                node.children.add(kherapour);
            }
            
            if ( option == Option.NetworkType && value.equals(NetworkType.OpenChrom.name()))
            {
                node.childShowRootChildren = false;
                
                CroCoNode dgf = new CroCoNode("Digital Genomic Footprinting (DGF)",node,new GeneralFilter(Option.OpenChromType,"DGF"),false,node.networks);
                CroCoNode dnase = new CroCoNode("DNase I hypersensitive sites (DNase)",node,new GeneralFilter(Option.OpenChromType,"DNase"),false,node.networks);
                CroCoNode fair = new CroCoNode("Formaldehyde-Assisted Isolation of Regulatory Elements (FAIRE)",node,new GeneralFilter(Option.OpenChromType,"Faire"),false,node.networks);
                node.children = new ArrayList<CroCoNode>();
                node.children.add(dgf);
                node.children.add(dnase);
                node.children.add(fair);
                
                for(CroCoNode openChromNode : new CroCoNode[]{dgf,dnase,fair})
                {
                    addConfidence(openChromNode);
                    
                    for(CroCoNode child : openChromNode.children)
                    {
                        addMotifSet(child);
                        
                    }
                }
                
                
                //CroCoNode neph = new CroCoNode("Neph et. al",node,new GeneralFilter(Option.reference,"Neph at el., Circuitry and Dynamics of Human Transcription Factor Regulatory Networks, Cell, 2012"),false,node.networks);
                
            }
            
        }
        
        
        stat.close();
    
    }
    private void addMotifSet (CroCoNode root)
    {
        root.children = new ArrayList<CroCoNode>();
        
        for(String motifSetName : getMotifSet(root.networks))
        {
            CroCoNode node = new CroCoNode(motifSetName,root,new GeneralFilter(Option.MotifSet,motifSetName),true,root.networks);
            root.children.add(node);
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
    public void persistNetworkOntology(CroCoNode root) throws Exception
    {
        PreparedStatement stat = DatabaseConnection.getConnection().prepareStatement("INSERT INTO NetworkOntology(node_id,parent_node_id,name,network_ids) values(?,?,?,?)");

        Stack<CroCoNode> stack = new Stack<CroCoNode>();
        stack.add(root);
        int id = 0;
        HashMap<CroCoNode,Integer> nodeToId = new HashMap<CroCoNode,Integer>();
        
        while(!stack.isEmpty())
        {
            CroCoNode top = stack.pop();
            if ( top.networks.size() == 0) continue;
            Integer topId = nodeToId.containsKey(top)?nodeToId.get(top):id++;
            
            Integer parentId = null;
            
            if ( top.parent == null)
            {
                parentId = -1;
            }else
            {
                parentId = nodeToId.get(top.parent);
            }
            nodeToId.put(top,topId);
            List<Integer> ids = new ArrayList<Integer>();
            for(NetworkHierachyNode nh : top.networks)
            {
                ids.add(nh.getGroupId());
            }
            
            stat.setInt(1, topId);
            stat.setInt(2,parentId);
            
            stat.setString(3, top.name);
            stat.setString(4,Joiner.on(" ").join(ids));

            stat.addBatch();
            
            if ( top.children != null)
            {
                for(CroCoNode child : top.children)
                {
                    stack.add(child);
                }
            }
        }
        stat.executeBatch();
    }

   
    private void factoraddOntologyNodes(CroCoNode parent) throws Exception
    {
        Statement stat = DatabaseConnection.getConnection().createStatement();
        
        String sql ="SELECT distinct(gene.gene_name) as name,value,tax_id  FROM NetworkOption op JOIN Gene gene on gene.gene = op.value where option_id = 15 ORDER BY name";
        
        CroCoLogger.getLogger().debug(sql);
        stat.execute(sql);
        ResultSet res = stat.getResultSet();
        HashMap<String,List<Pair<String,Integer>>> names = new HashMap<String,List<Pair<String,Integer>>>();
       
        OrthologRepository orepo = OrthologRepository.getInstance(new LocalService());
        
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
            
            CroCoNode factorNode = new CroCoNode(geneName.getKey(),parent,new FactorFilter(genesOfInterest),true,parent.networks);
            parent.children.add(factorNode);
        }
    }
  
    public CroCoNode createNetworkOntology() throws Exception
    {
        LocalService service = new LocalService();
        
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
        
        NetworkHierachyNode networks = service.getNetworkHierachy(null);
       
        CroCoNode root = new CroCoNode("Root",null,true,new HashSet<NetworkHierachyNode>(networks.getAllChildren()));
        
        CroCoNode cellLine = new CroCoNode("Cell-line",root, new GeneralFilter(Option.cellLine),false,root.networks);
        addOntologyNodes(Option.cellLine,cellLine);
      
        CroCoNode factor = new CroCoNode("Factor",root, false,root.networks);
        addOntologyNodes(Option.AntibodyTargetMapped,factor);
        CroCoNode allFactors = new CroCoNode("All factors",factor,true,factor.networks);
        factor.children.add(allFactors);
        
        CroCoNode specie = new CroCoNode("Species",root, new GeneralFilter(Option.TaxId),false,root.networks);
        addOntologyNodes(Option.TaxId,specie);    

        CroCoNode technique = new CroCoNode("Technique",root, new GeneralFilter(Option.NetworkType),false,root.networks);
        addOntologyNodes(Option.NetworkType,technique);
    
        CroCoNode devstage = new CroCoNode("Development-stage",root, new GeneralFilter(Option.developmentStage),false,root.networks);
        addOntologyNodes(Option.developmentStage,devstage);
    
        CroCoNode treatment = new CroCoNode("Treatment",root, new GeneralFilter(Option.treatment),false,root.networks);
        addOntologyNodes(Option.treatment,treatment);
    
        root.children = new ArrayList<CroCoNode>();
        
        root.children.add(cellLine);
        root.children.add(factor);
        root.children.add(specie);
        root.children.add(technique);
        root.children.add(devstage);
        root.children.add(treatment);
        
        return root;
    }
    
    public static void main(String[] args) throws Exception
    {
        NetworkOntology onto = new NetworkOntology();
        
        CroCoNode root = onto.createNetworkOntology();
        onto.persistNetworkOntology(root);
    }
}