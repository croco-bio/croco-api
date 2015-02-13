package de.lmu.ifi.bio.crco.data;

import java.util.List;

import org.junit.Test;

import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.data.CroCoNode.GeneralFilter;

public class CroCoNodeTest {

    @Test
    public void test()  throws Exception{
        LocalService service = new LocalService();
        
        CroCoNode onto = service.getNetworkOntology();
        
        GeneralFilter g1 = new GeneralFilter(Option.NetworkType,NetworkType.OpenChrom.name());
        GeneralFilter g2 = new GeneralFilter(Option.TaxId,10090+"");
        GeneralFilter g3 = new GeneralFilter(Option.ConfidenceThreshold,"1.0E-6");
        GeneralFilter g4 = new GeneralFilter(Option.MotifSet,"Combined set");
        GeneralFilter g5 = new GeneralFilter(Option.OpenChromType,"DNase");
        
        System.out.println(onto.getNetworks().size());
        
        CroCoNode filtered = onto.getNode("Filtered",g1,g2,g3,g4,g5);
        System.out.println(filtered.getNetworks().size());
        
        String[] esCellLineNames = new String[]{"ES-CJ7","ES-E14","ES-WW6","ES-WW6_F1KO"};
        
        for(String cellLine : esCellLineNames)
        {
            GeneralFilter g6 = new GeneralFilter(Option.cellLine,cellLine);
            CroCoNode t = filtered.getNode(cellLine, g6);
            
            System.out.println(t.getNetworks().size());
        }
        
        
    }

}