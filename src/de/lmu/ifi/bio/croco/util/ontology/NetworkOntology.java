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

import org.apache.commons.cli.CommandLine;

import com.google.common.base.Joiner;

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
    private static String KHERAPOUR ="Kherapour et al., Reliable prediction of regulator targets using 12 Drosophila genomes, Genome Res., 2007";
    private static String NEPH ="Neph et al., Circuitry and Dynamics of Human Transcription Factor Regulatory Networks, Cell, 2012";
    private static File taxObo= new File("data/obo/croco-sp.obo");
    private static File brendaOBO = new File("data/obo/BrendaTissue.obo");
    private static File brendaMapping = new File("data/obo/BrendaMapping");
    
    private static String all ="croco:all";
    
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
        root.setChildren( new ArrayList<CroCoNode<NetworkMetaInformation>>());
        CroCoNode<NetworkMetaInformation> highConf = new CroCoNode<NetworkMetaInformation>(getId(),"High confidence",root,new GeneralFilter(Option.ConfidenceThreshold,"1.0E-6"),root.getData());
        CroCoNode<NetworkMetaInformation> midConf = new CroCoNode<NetworkMetaInformation>(getId(),"Mid. confidence",root,new GeneralFilter(Option.ConfidenceThreshold,"1.0E-5"),root.getData());
        
        CroCoNode<NetworkMetaInformation> neph = new CroCoNode<NetworkMetaInformation>(getId(),"Neph et. al",root,new GeneralFilter(Option.reference,NEPH),root.getData());
        
        root.getChildren().add(highConf);
        
        root.getChildren().add(midConf);
        root.getChildren().add(neph);
       
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
    

    public  void addOntologyNodes(Option option,CroCoNode<NetworkMetaInformation> parent)  throws Exception
    {
        parent.setChildren(new ArrayList<CroCoNode<NetworkMetaInformation>>());
        
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
            
            Filter<NetworkMetaInformation> filter = new GeneralFilter(option,value);
            
            
            CroCoNode<NetworkMetaInformation> node = new CroCoNode<NetworkMetaInformation>(getId(),name,parent,filter,parent.getData());
            
            parent.getChildren().add(node);
            
         
            if ( option == Option.NetworkType && value.equals(NetworkType.TextMining.name()))
            {
                
                CroCoNode<NetworkMetaInformation> directed = new CroCoNode<NetworkMetaInformation>(getId(),"Directed",node,new GeneralFilter(Option.EdgeType,"Directed"),node.getData());
                CroCoNode<NetworkMetaInformation> undirected = new CroCoNode<NetworkMetaInformation>(getId(),"Undirected",node,new GeneralFilter(Option.EdgeType,"Undirected"),node.getData());
                
                node.setChildren(new ArrayList<CroCoNode<NetworkMetaInformation>>());
                node.getChildren().add(directed);
                node.getChildren().add(undirected);
                
                
                for(CroCoNode<NetworkMetaInformation> d : new CroCoNode[]{directed,undirected})
                {
                    d.setChildren( new ArrayList<CroCoNode<NetworkMetaInformation>>());
                    CroCoNode<NetworkMetaInformation> context = new CroCoNode<NetworkMetaInformation>(getId(),"Species filtered",d,new GeneralFilter(Option.TextMiningSpeciesContext,"Yes"),d.getData());
                    CroCoNode<NetworkMetaInformation> nocontext = new CroCoNode<NetworkMetaInformation>(getId(),"Species unfiltered",d,new GeneralFilter(Option.TextMiningSpeciesContext,"No"),d.getData());
                    
                    d.getChildren().add(context);
                    d.getChildren().add(nocontext);
                    
                }
            }
            
            if ( option == Option.NetworkType && value.equals(NetworkType.Database.name()))
            {
                
                CroCoNode<NetworkMetaInformation> transfac = new CroCoNode<NetworkMetaInformation>(getId(),"Transfac",node,new NameFilter("Transfac9.3"),node.getData());
                CroCoNode<NetworkMetaInformation> redfly = new CroCoNode<NetworkMetaInformation>(getId(),"REDfly",node,new NameFilter("REDfly3.2"),node.getData());
                CroCoNode<NetworkMetaInformation> orgenanno = new CroCoNode<NetworkMetaInformation>(getId(),"ORegAnno",node,new NameFilter("ORegAnno"),node.getData());
                CroCoNode<NetworkMetaInformation> ncipathway = new CroCoNode<NetworkMetaInformation>(getId(),"NCI-Pathway",node,new NameFilter("NCI-Pathway"),node.getData());
                CroCoNode<NetworkMetaInformation> biocarta = new CroCoNode<NetworkMetaInformation>(getId(),"Biocarta",node,new NameFilter("Biocarta"),node.getData());
                
                node.setChildren( new ArrayList<CroCoNode<NetworkMetaInformation>>());
                node.getChildren().add(transfac);
                node.getChildren().add(redfly);
                node.getChildren().add(orgenanno);
                node.getChildren().add(ncipathway);
                node.getChildren().add(biocarta);
            }
            if ( option == Option.NetworkType && value.equals(NetworkType.TFBS.name()))
            {
                addConfidence(node);
                
                for(CroCoNode<NetworkMetaInformation> child : node.getChildren())
                {
                    addMotifSet(child);
                    
                }   
                
                CroCoNode<NetworkMetaInformation> kherapour = new CroCoNode<NetworkMetaInformation>(getId(),"Kherapour et al.",node,new GeneralFilter(Option.reference,KHERAPOUR),node.getData());
                CroCoNode<NetworkMetaInformation> kherapour_500 = new CroCoNode<NetworkMetaInformation>(getId(),"Promoter 500.",kherapour,new GeneralFilter(Option.Upstream,"500"),kherapour.getData());
                CroCoNode<NetworkMetaInformation> kherapour_2000 = new CroCoNode<NetworkMetaInformation>(getId(),"Promoter 2000.",kherapour,new GeneralFilter(Option.Downstream,"2000"),kherapour.getData());
                
                
                kherapour.setChildren( new ArrayList<CroCoNode<NetworkMetaInformation>>());
                kherapour.getChildren().add(kherapour_500);
                kherapour.getChildren().add(kherapour_2000);
                
                kherapour_500.setChildren( new ArrayList<CroCoNode<NetworkMetaInformation>>());
                kherapour_2000.setChildren ( new ArrayList<CroCoNode<NetworkMetaInformation>>());
                
                for(int i = 0 ; i<= 10 ; i++)
                {
                    float c = (float)i/(float)10;
                    CroCoNode<NetworkMetaInformation> conf_1 = new CroCoNode<NetworkMetaInformation>(getId(),String.format("Kherapour conf. %.2f",c),kherapour_500,new GeneralFilter(Option.ConfidenceThreshold,String.format("%.1f",c)),kherapour_500.getData());
                    CroCoNode<NetworkMetaInformation> conf_2 = new CroCoNode<NetworkMetaInformation>(getId(),String.format("Kherapour conf. %.2f",c),kherapour_2000,new GeneralFilter(Option.ConfidenceThreshold,String.format("%.1f",c)),kherapour_2000.getData());
                    
                    kherapour_500.getChildren().add(conf_1);
                    kherapour_2000.getChildren().add(conf_2);
                    
                    //CroCoNode conf_2 = new CroCoNode(String.format("Kherapour conf. %.2f",c),kherapour_2000,new GeneralFilter(Option.reference,KHERAPOUR),false,node.networks);
                    
                         
                }
                
                node.getChildren().add(kherapour);
            }
            
            if ( option == Option.NetworkType && value.equals(NetworkType.OpenChrom.name()))
            {
                
                CroCoNode<NetworkMetaInformation> dgf = new CroCoNode<NetworkMetaInformation>(getId(),"Digital Genomic Footprinting (DGF)",node,new GeneralFilter(Option.OpenChromType,"DGF"),node.getData());
                CroCoNode<NetworkMetaInformation> dnase = new CroCoNode<NetworkMetaInformation>(getId(),"DNase I hypersensitive sites (DNase)",node,new GeneralFilter(Option.OpenChromType,"DNase"),node.getData());
                CroCoNode<NetworkMetaInformation> fair = new CroCoNode<NetworkMetaInformation>(getId(),"Formaldehyde-Assisted Isolation of Regulatory Elements (FAIRE)",node,new GeneralFilter(Option.OpenChromType,"Faire"),node.getData());
                node.setChildren ( new ArrayList<CroCoNode<NetworkMetaInformation>>());
                node.getChildren().add(dgf);
                node.getChildren().add(dnase);
                node.getChildren().add(fair);
                
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
    
    }
    private void addMotifSet (CroCoNode<NetworkMetaInformation> root)
    {
        root.setChildren (new ArrayList<CroCoNode<NetworkMetaInformation>>());
        
        for(String motifSetName : getMotifSet(root.getData()))
        {
            CroCoNode<NetworkMetaInformation> node = new CroCoNode<NetworkMetaInformation>(getId(),motifSetName,root,new GeneralFilter(Option.MotifSet,motifSetName),root.getData());
            root.getChildren().add(node);
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
    /**
     * Writes the ontology to file
     * @param root -- the root node
     * @param oboOut -- obo out file 
     * @param oboMapping -- entity to obo mapping
     * @throws IOException
     */
    public static<E extends Identifiable>  void writeNetworkOntology(CroCoNode<E> root, File oboOut, File oboMapping ) throws IOException
    {
        CroCoLogger.getLogger().debug("Write obo:" + oboOut);
        
        PrintWriter pw = FileUtil.getPrintWriter(oboOut);
        CroCoNode.printAsObo(pw, root);
        pw.close();
        

        CroCoLogger.getLogger().debug("Write mapping:" + oboMapping);
        
        pw = FileUtil.getPrintWriter(oboMapping);
        List<CroCoNode<E>> elements = new ArrayList<CroCoNode<E>>();
        
        elements.addAll(root.getAllChildren());
        
        for(CroCoNode<E> el : elements)
        {
            List<String> ids = new ArrayList<String>();
            for(Identifiable node : el.getData())
            {
                ids.add(node.getId());
            }
            
            if ( el.getId().contains(" "))
                throw new RuntimeException("Ontology ids with ' ' not permitted.");
            
            pw.printf("%s %s\n",el.getId(),el.getData().size()==root.getData().size()?all:Joiner.on(" ").join(ids));
        }
        pw.close();
       
    }
    
    public static<E extends Identifiable> void readOntology(CroCoNode<E> root, File oboFile, File oboMapping) throws Exception
    {
        
        Obo obo = new Obo(oboFile);
        
        HashMap<String,CroCoNode<E>> idToCroCoNode = new HashMap<String,CroCoNode<E>>();
        HashMap<String,List<String>> idToParents = new HashMap<String,List<String>>();
        
        int k = 0;
        for(OboElement el : obo.elements.values())
        {
            
            CroCoNode<E> node = new CroCoNode<E>(el.id,el.name);
            
            if ( idToCroCoNode.containsKey(el.id))
            {
                throw new RuntimeException(el.id + " not unique!");
            }
            
            idToCroCoNode.put(el.id, node);
            
            idToParents.put(el.id, new ArrayList<String>());
            for(OboElement parent : el.getParents() )
            {
                idToParents.get(el.id).add(parent.id);
            }
            k++;
        }
        CroCoLogger.debug("Found %d ontology nodes.",k);

        for(String id : idToParents.keySet())
        {
            CroCoNode<E> node = idToCroCoNode.get(id);
            List<String> parents = idToParents.get(id);
            
            if ( parents.size() == 0)
            {
                node.setParent(root);
                continue;
            }
            
            for(String parentId : parents)
            {
                CroCoNode<E> parent = idToCroCoNode.get(parentId);
                if ( parent.getChildren() == null)
                {
                    node.setParent(parent);
                } else
                {
                    node.setParent(parent);
                }    
            }
        }
        
        HashMap<String,E> idToDataPoint = new HashMap<String,E>();
        
        for(E dataPoint : root.getData() )
        {
            idToDataPoint.put(dataPoint.getId(), dataPoint);
        }
        
        Iterator<String> it = FileUtil.getLineIterator(oboMapping);
        while(it.hasNext())
        {
            String line = it.next();
            String tokens[] = line.split("\\s+");
            
            String id = tokens[0];
            CroCoNode<E> node = idToCroCoNode.get(id);
            
            Set<E> data  = new HashSet<E>();
            for(int i = 1 ; i< tokens.length; i++)
            {
                if ( tokens[i].equals("all"))
                    data.addAll(root.getData());
                else
                    data.add(idToDataPoint.get(tokens[i]));
            }
            node.setData(data);
        }
    }
   
    
    private void factoraddOntologyNodes(CroCoNode<NetworkMetaInformation> parent) throws Exception
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
            
            CroCoNode<NetworkMetaInformation> factorNode = new CroCoNode<NetworkMetaInformation>(getId(),geneName.getKey(),parent,new FactorFilter(genesOfInterest),parent.getData());
            parent.getChildren().add(factorNode);
        }
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
        
        List<NetworkMetaInformation> networks = service.getNetworkMetaInformation();
        readFactors(networks);
        
        CroCoNode<NetworkMetaInformation> root = new CroCoNode<NetworkMetaInformation>(getId(),"Root",null,new HashSet<NetworkMetaInformation>(networks));
      
        CroCoNode<NetworkMetaInformation> cellLine = new CroCoNode<NetworkMetaInformation>(getId(),"Tissue/Cell-line",root, new GeneralFilter(Option.cellLine),root.getData());
        CroCoNode<NetworkMetaInformation> cellLine_flat = new CroCoNode<NetworkMetaInformation>(getId(),"All tissue/cell-lines",cellLine,cellLine.getData());
        cellLine.setChildren(new ArrayList<CroCoNode<NetworkMetaInformation>>());
        cellLine.getChildren().add(cellLine_flat);
        addOntologyNodes(Option.cellLine,cellLine_flat);
        addBrenda(cellLine);
        
        CroCoNode<NetworkMetaInformation> specie = new CroCoNode<NetworkMetaInformation>(getId(),"Species",root, new GeneralFilter(Option.TaxId),root.getData());
        CroCoNode<NetworkMetaInformation> specie_flat = new CroCoNode<NetworkMetaInformation>(getId(),"All species",specie,specie.getData());
        specie.setChildren(new ArrayList<CroCoNode<NetworkMetaInformation>>());
        specie.getChildren().add(specie_flat);
        addOntologyNodes(Option.TaxId,specie_flat);
        addSpeciesObo(specie,taxObo,"NCBITaxon:33213");
        
        
        CroCoNode<NetworkMetaInformation> compendium = new CroCoNode<NetworkMetaInformation>(getId(),"Compendium",root, new GeneralFilter(Option.Compendium),root.getData());
        addOntologyNodes(Option.Compendium,compendium);
       
      
        CroCoNode<NetworkMetaInformation> factor = new CroCoNode<NetworkMetaInformation>(getId(),"ENCODE (ChIP)-Factor",root,root.getData());
        addOntologyNodes(Option.AntibodyTargetMapped,factor);
        CroCoNode<NetworkMetaInformation> allFactors = new CroCoNode<NetworkMetaInformation>(getId(),"All factors",factor,factor.getData());
        factor.getChildren().add(allFactors);
        
        
        CroCoNode<NetworkMetaInformation> technique = new CroCoNode<NetworkMetaInformation>(getId(),"Experimental technique",root, new GeneralFilter(Option.NetworkType),root.getData());
        addOntologyNodes(Option.NetworkType,technique);
    
        CroCoNode<NetworkMetaInformation> devstage = new CroCoNode<NetworkMetaInformation>(getId(),"Development stage",root, new GeneralFilter(Option.developmentStage),root.getData());
        addOntologyNodes(Option.developmentStage,devstage);
    
        CroCoNode<NetworkMetaInformation> treatment = new CroCoNode<NetworkMetaInformation>(getId(),"Treatment",root, new GeneralFilter(Option.treatment),root.getData());
        addOntologyNodes(Option.treatment,treatment);
    
        root.setChildren (new ArrayList<CroCoNode<NetworkMetaInformation>>());
        
        root.getChildren().add(compendium);
        root.getChildren().add(cellLine);
        root.getChildren().add(factor);
        root.getChildren().add(specie);
        root.getChildren().add(technique);
        root.getChildren().add(devstage);
        root.getChildren().add(treatment); 
        
        return root;
    }
    private void addBrenda(CroCoNode <NetworkMetaInformation>root) throws Exception
    {
        String oboRootElement = "BTO:0000000";
        
        Obo reader = new Obo(brendaOBO);
        
        OboElement rootElement = reader.getElement(oboRootElement);
        
        CroCoNode<NetworkMetaInformation> brenda = new CroCoNode<NetworkMetaInformation>(getId(),"Tissues",root, root.getData());
        if ( root.getChildren() == null)
            root.setChildren(new ArrayList<CroCoNode<NetworkMetaInformation>>());
        
        root.getChildren().add(brenda);
        
        HashMap<String, String> mapping = FileUtil.mappingFileReader(0, 1, brendaMapping).readMappingFile();
        
        HashSet<String> notFound = new HashSet<String>();

        HashMap<OboElement,HashSet<NetworkMetaInformation>> elementsToNetwork  =new HashMap<OboElement,HashSet<NetworkMetaInformation>>();
        
        //init mapping
        for(NetworkMetaInformation nh : root.getData())
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
                elementsToNetwork.put(element, new HashSet<NetworkMetaInformation>());
            }
            elementsToNetwork.get(element).add(nh);
        }
          
        CroCoLogger.getLogger().warn("Not mapped:" + notFound);
        
        Ontology.addObo(elementsToNetwork,root,rootElement,true);
        

    }
    private void addSpeciesObo(CroCoNode<NetworkMetaInformation> root, File obo, String oboRootElement) throws Exception{
        Obo reader = new Obo(obo);
        
        HashMap<OboElement,HashSet<NetworkMetaInformation>> elementsToNetwork  =new HashMap<OboElement,HashSet<NetworkMetaInformation>>();
        
        for(OboElement element : reader.elements.values() )
        {
            GeneralFilter filter = new GeneralFilter(Option.TaxId,element.id.replaceAll("NCBITaxon:", ""));
            HashSet<NetworkMetaInformation> networks = new HashSet<NetworkMetaInformation>();
            
            for(NetworkMetaInformation nh : root.getData())
            {
                if ( filter.accept(nh))
                    networks.add(nh);  
            }
            elementsToNetwork.put(element, networks);
        }

        OboElement rootElement = reader.getElement(oboRootElement);
        
        Ontology.addObo(elementsToNetwork,root,rootElement,true);
        
    }

    private static String FACTOR_FILE="factors.gz";
    
    private void readFactors(List<NetworkMetaInformation> nodes) throws Exception
    {
        HashMap<Integer,NetworkMetaInformation> groupIdToNetwork = new HashMap<Integer,NetworkMetaInformation>();
        for(NetworkMetaInformation nh : nodes ) 
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
        
        ConsoleParameter parameter = new ConsoleParameter();
        parameter.register(
                NetworkOntologyWriter.ONTOLOGY_OUT,
                NetworkOntologyWriter.ONTOLOGY_MAPPING_OUT
        );
        CommandLine cmdLine = parameter.parseCommandLine(args, NetworkOntology.class);
        NetworkOntology onto = new NetworkOntology();
        
        CroCoNode<NetworkMetaInformation> root = onto.createNetworkOntology();
        
        CroCoLogger.getLogger().info("Write ontology");
        writeNetworkOntology(root,NetworkOntologyWriter.ONTOLOGY_OUT.getValue(cmdLine),NetworkOntologyWriter.ONTOLOGY_MAPPING_OUT.getValue(cmdLine));
        
        LocalService service = new LocalService();
        
        System.out.println("Read ontoloy");
        service.getNetworkOntology(false);
        
    }
}
