package de.lmu.ifi.bio.croco.util;

import java.io.File;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;

import com.google.common.base.Joiner;

import de.lmu.ifi.bio.croco.connector.DatabaseConnection;
import de.lmu.ifi.bio.croco.connector.LocalService;
import de.lmu.ifi.bio.croco.data.NetworkMetaInformation;
import de.lmu.ifi.bio.croco.util.ConsoleParameter.CroCoOption;

public class GenerateFactorList {
    
    private static CroCoOption<File> OUT_FILE = new CroCoOption<File>("out_file",new ConsoleParameter.FileHandler()).isRequired().setArgs(1).setDescription("Out file");
    
    public static void main(String[] args) throws Exception
    {
        ConsoleParameter parameter = new ConsoleParameter();
        parameter.register(
                OUT_FILE
        );
        CommandLine cmdLine = parameter.parseCommandLine(args, GenerateFactorList.class);
        
        File outFile = OUT_FILE.getValue(cmdLine);
        CroCoLogger.info("Output file: %s", outFile);
        PrintWriter pw = new PrintWriter(outFile);
        
        LocalService service = new LocalService();
        List<NetworkMetaInformation> networks = service.getNetworkMetaInformations();
        CroCoLogger.info("Generate factor stat for: %d networks",networks.size());
        PreparedStatement stat = DatabaseConnection.getConnection().prepareStatement("SELECT distinct(gene1) FROM Network where group_id= ?");
        
        int k = 0;
        for(NetworkMetaInformation network : networks)
        {
            CroCoLogger.info("Generate stat for: %d", network.getGroupId());
            stat.setInt(1, network.getGroupId());
            
            stat.execute();
            
            ResultSet res = stat.getResultSet();
            List<String> values  = new ArrayList<String>();
            while(res.next())
            {
                values.add(res.getString(1));
            }
            pw.printf("%d %s\n", network.getGroupId(),Joiner.on("\t").join(values));
            res.close();
            if ( k++%100==0)
                pw.flush();
        }
        pw.close();
    }
}
