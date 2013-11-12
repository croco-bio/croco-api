package de.lmu.ifi.bio.crco.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;

import de.lmu.ifi.bio.crco.connector.DatabaseConnection;
import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.connector.QueryService;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.exceptions.CroCoException;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.Intersect;
import de.lmu.ifi.bio.crco.operation.ReadNetwork;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Tuple;

public class TargetSet {
	public static void main(String[] args) throws Exception{
		QueryService service = new LocalService(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
		ReadNetwork reader = new ReadNetwork();
		reader.setInput(ReadNetwork.QueryService, service);
		reader.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(99,9606));
		Network dNaseNetwork = reader.operate();
		Integer humanTextMiningNetworkGroupId = 1420;
		
		reader.setInput(ReadNetwork.GlobalRepository,true);
		reader.setInput(ReadNetwork.NetworkHierachyNode, new NetworkHierachyNode(humanTextMiningNetworkGroupId,9606));
		Network humanTextMiningNetwork = reader.operate();
		
		Intersect intersect = new Intersect();
		intersect.setInputNetwork(dNaseNetwork,humanTextMiningNetwork);
		Network ret = intersect.operate();
		
		BufferedWriter bwCommon = new BufferedWriter(new FileWriter("out"));
		for(int  edgeId : ret.getEdgeIds()){
			Tuple<Entity, Entity> edge = ret.getEdge(edgeId);
			Entity factor = edge.getFirst();
			Entity target = edge.getSecond();
		
					
			bwCommon.write(factor.getIdentifier()+ "\t" + target.getIdentifier() + "\n");
		}
		bwCommon.flush();
		bwCommon.close();
	}
}
