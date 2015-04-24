package de.lmu.ifi.bio.croco.data;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.data.CroCoNode.GeneralFilter;

public class CroCoNodeTest {

    @Test
    public void testFilter()  throws Exception{
        LocalService service = new LocalService();
        
        CroCoNode<NetworkMetaInformation> onto = service.getNetworkOntology(false);
        
        GeneralFilter g1 = new GeneralFilter(Option.NetworkType,NetworkType.OpenChrom.name());
        GeneralFilter g2 = new GeneralFilter(Option.TaxId,10090+"");
        GeneralFilter g3 = new GeneralFilter(Option.ConfidenceThreshold,"1.0E-6");
        GeneralFilter g4 = new GeneralFilter(Option.MotifSet,"Combined set");
        GeneralFilter g5 = new GeneralFilter(Option.OpenChromType,"DNase");
        
        assertTrue(onto.getData().size()>0);
        
        Set<NetworkMetaInformation> filtered = onto.getData("Filtered",g1,g2,g3,g4,g5);
        
        String[] esCellLineNames = new String[]{"ES-CJ7","ES-E14","ES-WW6","ES-WW6_F1KO"};
        
        CroCoNode<NetworkMetaInformation> es = new CroCoNode<NetworkMetaInformation>("ES","ES",null,filtered);
        
        for(String cellLine : esCellLineNames)
        {
            GeneralFilter g6 = new GeneralFilter(Option.cellLine,cellLine);
            
            Set<NetworkMetaInformation> t = es.getData(cellLine, g6);
            
            assertTrue(t.size()>0);
        }
        
        
    }

    @Test
    public void cloneTest() throws Exception
    {
        LocalService service = new LocalService();
        
        CroCoNode<NetworkMetaInformation> root = service.getNetworkOntology(false);
        System.out.println(root.getData().size());
        System.out.println("ROOT:" + root.getChildren());
        CroCoNode cloned = new CroCoNode(root);
        cloned.setData(root.getData());
        cloned.initChildren(root);
        
        System.out.println(cloned.getChildren());
    }
}
