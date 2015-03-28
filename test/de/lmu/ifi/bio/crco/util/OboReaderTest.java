package de.lmu.ifi.bio.crco.util;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.util.ontology.OboReader;
import de.lmu.ifi.bio.crco.util.ontology.OboReader.OboElement;

public class OboReaderTest {

    @Test
    public void testBrenda() throws Exception{
        File obo = new File("data/BrendaTissue.obo");
        
        OboReader reader = new OboReader(obo);
        
        OboElement e = reader.getElement("BTO:0000165");
        
       // System.out.println(e.getParents().get(0).getParents().get(0).getParents().get(0).getParents().get(0).getParents().get(0).getParents().get(0).getParents());
       // System.exit(1);
        /*
        System.out.println(reader.getElement("BTO:0000000").getParents());
        
        for(OboElement e : reader.getElements())
        {
            if ( e.getChildren().size() > 0)
                System.out.println(e + " " + e.getChildren());
        }
        System.out.println(reader.getElement("BTO:0000007").getSynonyms());
        */
        LocalService service = new LocalService();
        
        File mappingFile = new File("data/BrendaMapping");
        
        Iterator<String> it = FileUtil.getLineIterator(mappingFile);
        
        HashMap<String, String> mapping = FileUtil.mappingFileReader(0, 1, mappingFile).readMappingFile();
        
        List<NetworkHierachyNode> nhs = service.getNetworkHierachy();
        HashSet<String> notFound = new HashSet<String>();
        for(NetworkHierachyNode nh : nhs)
        {
            Integer taxId = Integer.valueOf(nh.getOptions().get(Option.TaxId));
            
            String cellLine = nh.getOptions().get(Option.cellLine) ;
            if ( cellLine != null)
            {
                String map = mapping.get(cellLine);
                if ( map == null){
                    notFound.add(cellLine);
                    continue;
                }
                if ( reader.getElement(map) == null)
                {
                    
                }
                map = map.replace("(non-specific)", "").trim();
                OboElement element = reader.getElement(map);
                if ( element == null)
                    System.out.println("HERE:" + map);
              //  System.out.println(element.getId() + " " + element.getRoot().getId());
                
            }
            if ( cellLine != null){
            //    String map = mapping.get(cellLine);
            //    if( map != null)
            //        System.out.println(cellLine + " " + map);
            }
        }
        System.out.println(notFound);
       //     System.out.println(nh.);
        
        
    }
    
    @Test
    public void testSpecies() throws Exception{
        File obo = new File("data/croco-sp.obo");
        
        OboReader reader = new OboReader(obo);
        
        System.out.println(reader.getElement("NCBITaxon:9606").getParents());
        
        for(OboElement e : reader.getElements())
        {
            System.out.println(e.getChildren());
        }
    }
}
