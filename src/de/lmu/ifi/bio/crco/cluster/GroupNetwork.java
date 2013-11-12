package de.lmu.ifi.bio.crco.cluster;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import de.lmu.ifi.bio.crco.connector.LocalService;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNodeGroup;
import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.util.Pair;

/**
 * Groups NetworkHierachyNode by NetworkType and sub types.
 * @author pesch
 *
 */
public class GroupNetwork {
	
	
	/** 
	 * Groups NetworkHierachyNodes by NetworkType (@see de.lmu.ifi.bio.crco.data.NetworkType) 
	 * @param rootNode  -- a hierarchy node
	 * @return
	 */
	public HashMap<NetworkType,List<NetworkHierachyNode>> groupNetworkType(List<NetworkHierachyNode> nodes){
		HashMap<NetworkType,List<NetworkHierachyNode>> networkGroups = new HashMap<NetworkType,List<NetworkHierachyNode>>();
	
		for(NetworkHierachyNode node : nodes){
			if (! networkGroups.containsKey(node.getType() )) {
				networkGroups.put(node.getType(), new ArrayList<NetworkHierachyNode>());
			}
			networkGroups.get(node.getType()).add(node);
		}
		
		return networkGroups;
	}
	/**
	 * Groups NetworkHierachyNode annotated with the NetworkType DNase by Option.DNaseMotifPVal and motif set. 
	 * @param dnaseNodes
	 * @param service
	 * @return  list of groups
	 * @throws Exception
	 */
	public List<NetworkHierachyNodeGroup> groupDNase(List<NetworkHierachyNode> dnaseNodes,LocalService service) throws Exception{
		HashMap<String,List<NetworkHierachyNode>> groupedByValue = new HashMap<String,List<NetworkHierachyNode>>();
		List<NetworkHierachyNodeGroup> ret = new ArrayList<NetworkHierachyNodeGroup>();
		
		for(NetworkHierachyNode node : dnaseNodes ){
			String motifSet = getOption(node,service,Option.DNaseMotifSet);
			String pValue = getOption(node,service,Option.DNaseMotifPVal);
			String key = String.format("%s (%s)",motifSet, pValue );
			if ( key != null){
				if (! groupedByValue.containsKey(key)){
					groupedByValue.put(key, new ArrayList<NetworkHierachyNode>());
				}
				groupedByValue.get(key).add(node);
			}
		}
		for(Entry<String, List<NetworkHierachyNode>>  e: groupedByValue.entrySet()){
			ret.add(new NetworkHierachyNodeGroup(e.getKey(),e.getValue(),NetworkType.ChIP));
		}
		
		return ret;
	}
	/**
	 * Groups NetworkHierachyNode annotated with the NetworkType ChIP by (ortholog) factors.
	 * @param chipNodes -- List of NetworkHierachyNode
	 * @param service -- query service
	 * @return list of groups
	 * @throws Exception
	 */
	public List<NetworkHierachyNodeGroup> groupChIP(List<NetworkHierachyNode> chipNodes,LocalService service) throws Exception{
		List<NetworkHierachyNodeGroup> ret = new ArrayList<NetworkHierachyNodeGroup>();
		
		HashMap<String,List<NetworkHierachyNode>> groupedByFactor = new HashMap<String,List<NetworkHierachyNode>>();
		
		for(NetworkHierachyNode node : chipNodes ){
			String target = getOption(node,service,Option.AntibodyTargetMapped);
			if ( target != null){
				if (! groupedByFactor.containsKey(target)){
					groupedByFactor.put(target, new ArrayList<NetworkHierachyNode>());
				}
				groupedByFactor.get(target).add(node);
			}
		}
		Statement stat = service.getConnection().createStatement();
		
		for(String factor :groupedByFactor.keySet() ){
			List<NetworkHierachyNode> group = new ArrayList<NetworkHierachyNode>(groupedByFactor.get(factor));
			
			String sql = String.format("SELECT db_id_1,db_id_2 FROM Ortholog where db_id_1 = '%s' or db_id_2 = '%s' and ortholog_database_id = 2;",factor,factor);
			stat.execute(sql);
			HashSet<String> orthologs = new HashSet<String>();
			ResultSet res = stat.getResultSet();
			while(res.next()){
				orthologs.add(res.getString(1));
				orthologs.add(res.getString(2));
				
			}
			res.close();
			
			orthologs.remove(factor);
			for(String ortholog : orthologs){
				if (groupedByFactor.containsKey(ortholog) ){
					group.addAll(groupedByFactor.get(ortholog));
				}
			}
			ret.add(new NetworkHierachyNodeGroup(factor,group,NetworkType.ChIP));
			//groups.add(group);
		}
		stat.close();
		
		
		
		return ret;
	}
	
	private String getOption(NetworkHierachyNode node,  LocalService service, Option op) throws Exception{
		List<Pair<Option, String>> options =  service.getNetworkInfo(node.getGroupId());
		String value = null;
		for(Pair<Option, String> option : options){
			if ( option.getFirst().equals(op)){
				value = option.getSecond();
				return value;
			}
		}
		return null;
	}
	/**
	 * Groups NetworkHierachyNodes by NetworkType (@see de.lmu.ifi.bio.crco.data.NetworkType) and further constraints.
	 * ChIP experiments are grouped by (ortholog) factor, and DNase experiments are grouped by (ortholog) factor. 
	 * The other NetworkTypes (TextMining,Database,TFBS) are not further grouped  
	 * @param rootNode -- a hierarchy node
	 * @param service - a local service
	 * @return A list of groups
	 * @throws Exception
	 */
	public List<NetworkHierachyNodeGroup> group(List<NetworkHierachyNode> nodes,LocalService service) throws Exception{
		List<NetworkHierachyNodeGroup> groups = new ArrayList<NetworkHierachyNodeGroup>();
		
		HashMap<NetworkType, List<NetworkHierachyNode>> networkGroups = groupNetworkType(nodes);
		
		for(NetworkType type : networkGroups.keySet()){
		
			if (type.equals(NetworkType.TFBS) || type.equals(NetworkType.TextMining) ||  type.equals(NetworkType.Database)){
				groups.add(new NetworkHierachyNodeGroup(type.name(),networkGroups.get(type),type));
			}else if (type.equals(NetworkType.ChIP) ){
				groups.addAll(groupChIP(networkGroups.get(NetworkType.ChIP),service));
				
			}else if (type.equals(NetworkType.DNase) ){
				groups.addAll(groupDNase(networkGroups.get(NetworkType.DNase),service));
			}
		}
		
		return groups;
	}

}
