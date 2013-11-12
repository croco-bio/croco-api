package de.lmu.ifi.bio.crco.connector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import de.lmu.ifi.bio.crco.data.ContextTreeNode;
import de.lmu.ifi.bio.crco.data.Entity;
import de.lmu.ifi.bio.crco.data.NetworkHierachyNode;
import de.lmu.ifi.bio.crco.data.NetworkType;
import de.lmu.ifi.bio.crco.data.Option;
import de.lmu.ifi.bio.crco.data.Species;
import de.lmu.ifi.bio.crco.intervaltree.peaks.TFBSPeak;
import de.lmu.ifi.bio.crco.network.DirectedNetwork;
import de.lmu.ifi.bio.crco.network.Network;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologDatabaseType;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMapping;
import de.lmu.ifi.bio.crco.operation.ortholog.OrthologMappingInformation;
import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.Pair;


public class LocalService implements QueryService{
	private Connection connection;
	private Logger logger;
	
	public Connection getConnection(){
		return connection;
	}
	public LocalService(){
		this(CroCoLogger.getLogger(),DatabaseConnection.getConnection());
	}
	
	public LocalService(Connection connection){
		this(CroCoLogger.getLogger(),connection);
	}

	public LocalService(Logger logger, Connection connection ){
		this.connection = connection;
		this.logger = logger;
		
	}
	
	@Override
	public NetworkHierachyNode getNetworkHierachy(String path) throws Exception {
		
		PreparedStatement statement = connection.prepareStatement("SELECT group_id , parent_group_id , name, has_network, tax_id,database_identifier_id,network_type FROM NetworkHierachy ORDER BY parent_group_id");
		
		List<NetworkHierachyNode> networks = getNetworks(statement);
		statement.close();
		
		NetworkHierachyNode rootNode = networks.get(0);
		if ( path != null){
			String[] tokens = path.split("/");
			
			for(String token : tokens){
				NetworkHierachyNode newRoot = null;
				for(NetworkHierachyNode child : rootNode.getChildren()){
					if ( child.getName().equals(token)) {
						newRoot = child;
						break;
					}
				}
				if ( newRoot == null) throw new Exception(String.format("Network not found for path %s stopped at %s (id: %d)",path,token,rootNode.getGroupId()));
				rootNode = newRoot;
			}
		}
		
		logger.debug("Number of networks:\t" + networks.size());
		return rootNode;
	}


	@Override
	public NetworkHierachyNode getNetworkHierachyNode(Integer groupId) throws Exception {
		PreparedStatement statement = connection.prepareStatement("SELECT group_id , parent_group_id , name, has_network, tax_id,database_identifier_id,network_type FROM NetworkHierachy where group_id=?");
		statement.setInt(1, groupId);
		
		List<NetworkHierachyNode> networks = getNetworks(statement);
		statement.close();
		if ( networks.size() == 1) 
			return networks.get(0);
		else
			return null;
	}
	@Override
	public List<NetworkHierachyNode> findNetwork(List<Pair<Option, String>> options) throws Exception {
		
		
		StringBuffer condition =new StringBuffer();
		
		for(int i = 0 ; i< options.size(); i++){
			if ( condition.length() > 0) condition.append(" AND ");
			condition.append("nh.group_id IN (SELECT group_id FROM  NetworkOption nop  where nh.group_id and nop.option_id = ? and nop.value like ?) ");
		}
		
		String sql = String.format("SELECT group_id , parent_group_id , name, has_network, tax_id,database_identifier_id,network_type FROM NetworkHierachy nh %s ORDER BY parent_group_id",condition.length() > 0?"where " +condition.toString():"");
		PreparedStatement statement = connection.prepareStatement(sql);
		for(Pair<Option,String> option : options){
			statement.setInt(1, option.getFirst().ordinal());
			statement.setString(2, option.getSecond());
			
		}
		List<NetworkHierachyNode> networks = getNetworks(statement);
		statement.close();
		
		return networks;
	}
	private List<NetworkHierachyNode> getNetworks(PreparedStatement stat) throws SQLException{
		List<NetworkHierachyNode> networks = new ArrayList<NetworkHierachyNode>();

		stat.execute();
		
		ResultSet res = stat.getResultSet();
	
		HashMap<Integer,NetworkHierachyNode> groupIdToNetwork = new HashMap<Integer,NetworkHierachyNode>();
		
		while(res.next()){
			Integer groupId = res.getInt(1);
			Integer parentGroupId = res.getInt(2);
			String name = res.getString(3);
			Boolean hasNetwork = res.getBoolean(4);
			

			Integer taxId = res.getInt(5);
			if ( res.wasNull())  taxId = null;
			NetworkType type = NetworkType.values()[res.getInt(7)];
			NetworkHierachyNode nhn = new NetworkHierachyNode(null,parentGroupId,groupId,name,hasNetwork,taxId,type);
			groupIdToNetwork.put(groupId, nhn);
			networks.add(nhn);
			
		}
		res.close();
		stat.close();
		for(NetworkHierachyNode network : networks){ //creates hierarchy
			Integer parentGroupId = network.getParentGroupdId();
			if ( groupIdToNetwork.containsKey(parentGroupId)){
				groupIdToNetwork.get(parentGroupId).addChild(network);
				network.setParent(groupIdToNetwork.get(parentGroupId));
			}
		}
		
		return networks;
	}
	

	@Override
	public List<OrthologMappingInformation> getOrthologMappingInformation(OrthologDatabaseType database, Species species1, Species species2) throws Exception {
		Statement stat = connection.createStatement();
		String sql =String.format(
				"SELECT ortholog_database_id,tax_id_1,t1.name, database_identifier_id_1,tax_id_2,t2.name,database_identifier_id_2 FROM OrthologMappingInformation " +
				"JOIN Taxonomy t1 on t1.tax_id = tax_id_1  and t1.type like 'scientific name'  JOIN Taxonomy t2 on t2.tax_id = tax_id_2 and t2.type like 'scientific name' " 
				);
		StringBuffer where = new StringBuffer("");
		if ( database != null){
			where.append(String.format("WHERE ortholog_database_id=%d",database.ordinal()));
		}
		
		
		if ( species1 != null || species2 != null){
			if ( species1 != null && species2 != null){
				if ( species1.getTaxId() > species2.getTaxId()){
					Species tmp = species1;
					species1 = species2;
					species2 = tmp;
				}
			}
		
		
			if ( where.length() > 0){
				where.append(" AND ");
			}else{
				where.append("WHERE ");
			}
			if ( species1 != null){
				where.append(String.format(" tax_id_1 = %d",species1.getTaxId()));	
			}
			if ( species1 != null && species2 != null){
				where.append(" AND ");
			}else{
				where.append(String.format(" or tax_id_2 = %d ",species1.getTaxId()));
							
			}
			
			if ( species2 != null){
				where.append(String.format(" tax_id_2 = %d",species2.getTaxId()));	
			}
			
		}
		sql += where;
		logger.debug(sql);
		
		stat.execute(sql);
		
		ResultSet res = stat.getResultSet();
		List<OrthologMappingInformation> ret = new ArrayList<OrthologMappingInformation>();
		while(res.next()){
			OrthologDatabaseType orthologDatabase = OrthologDatabaseType.values()[res.getInt(1)];
			String commonName1 = res.getString(3);
			Species sp1 = new Species(res.getInt(2),commonName1);
			
			String commonName2 = res.getString(6);
			Species sp2 = new Species(res.getInt(5),commonName2);
			
			ret.add(new OrthologMappingInformation(orthologDatabase,sp1,sp2));
			
		}
		res.close();
		stat.close();
		
		return ret;
	}
	

	@Override
	public OrthologMapping getOrthologMapping(OrthologMappingInformation orthologMappingInformation) throws Exception{
		
		Statement stat = connection.createStatement();
		OrthologMapping mapping = new OrthologMapping();
		int k = 0;
		String sql = String.format(
				"SELECT db_id_1,db_id_2  FROM Ortholog where tax_id1 = %d and tax_id2 = %d and ortholog_database_id =%d;",
				orthologMappingInformation.getSpecies1().getTaxId(),
				orthologMappingInformation.getSpecies2().getTaxId(),
				orthologMappingInformation.getDatabase().ordinal()
				);

		logger.debug(sql);
		stat = connection.createStatement();

		stat.setFetchSize(Integer.MIN_VALUE);
		stat.execute(sql);
		ResultSet res = stat.getResultSet();


		while(res.next()){

			Entity e1 = new Entity(res.getString(1));
			Entity e2 = new Entity(res.getString(2));
			k++;

			mapping.addMapping(e1, e2);

		}
		res.close();
		
		logger.debug("Number of ortholog mappings:\t" +k);
		
		stat.close();
		return mapping;
		
	}
	/*

*/
	@Override
	public Network readNetwork(Integer groupId, Integer contextId, boolean gloablRepository) throws Exception{
	
		
		logger.debug("Load:\t" + groupId + " with context " + contextId);

		String condition = null;
		
		if ( contextId != null ){
			condition = String.format("JOIN GeneContext gc on gc.gene = network.gene1 "
						+ "JOIN GeneContext gc2 on gc2.gene = network.gene2 "
						+ "where gc.context_id = %d and gc2.context_id = %d and group_id = %d",
						contextId,contextId,groupId);
		}else{
			condition = String.format("where group_id = %d ", groupId);
		}
		
		Statement stat = connection.createStatement();
		
		NetworkHierachyNode networkNode = this.getNetworkHierachyNode(groupId);
		Network network = new DirectedNetwork(networkNode.getName(),networkNode.getTaxId(),gloablRepository);
		String sql  = String.format("SELECT gene1,gene2 FROM Network network %s", condition);
	
		logger.debug(sql);
		stat.execute(sql);

		ResultSet res = stat.getResultSet();
		while (res.next()) {
			
			Entity e1 = new Entity(res.getString(1));
			Entity e2 = new Entity(res.getString(2));
			
			network.add(e1, e2, groupId);

		}
		res.close();
			
		logger.debug(String.format("Number of edges:%d",network.getSize()));
		stat.close();
		return network;
		
	}
	@Override
	public List<Pair<Option,String>> getNetworkInfo(Integer groupId) throws Exception{

		String sql =String.format("SELECT no.option_id,no.value FROM NetworkOption no JOIN OptionDescription op on op.option_id = no.option_id  where group_id =%d",groupId);
		Statement stat = connection.createStatement();
		stat.execute(sql);
		logger.debug(sql);
		ResultSet res = stat.getResultSet();
		List<Pair<Option,String>> options = new ArrayList<Pair<Option,String>>();
		while(res.next()) {
			Integer optionId = res.getInt(1);
			Option option = Option.values()[optionId];
			
		//	Option option = Option.valueOf(res.getString(1));
			
			String value = res.getString(2);
			
			options.add(new Pair<Option,String>(option,value));
			
		}
		res.close();
		stat.close();
		
		return options;
	}
	
	@Override
	public Integer getNumberOfEdges(Integer groupId) throws Exception {
		Statement stat = connection.createStatement();
		Integer ret =null;
		stat.execute(String.format("SELECT  count(*) FROM Network where group_id = %d",groupId));
		ResultSet res = stat.getResultSet();
		if ( res.next()){
			ret = res.getInt(1);
		}
		res.close();
		stat.close();
		return ret;
		
	}

	@Override
	public List<Species> getSpecies(String prefix) throws Exception {
		Statement stat = connection.createStatement();
		String sql = String.format("SELECT distinct(tax_id),name FROM Taxonomy where name like '%s%%'  ",prefix);
		logger.debug(sql);
		stat.execute(sql);
	
		List<Species> species = new ArrayList<Species>();
		ResultSet res = stat.getResultSet();
		while(res.next()){
			String name = res.getString(2);
			Integer taxId = res.getInt(1);
			species.add(new Species(taxId,name));
		}
		res.close();
		stat.close();
		return species;
	}


	@Override
	public List<OrthologMappingInformation> getTransferTargetSpecies(int taxId) throws Exception {
		List<OrthologMappingInformation> ret = new ArrayList<OrthologMappingInformation>();
		Statement stat = connection.createStatement();
		stat.execute(
				String.format(
						"SELECT ortholog_database_id,tax_id_1,t1.name, database_identifier_id_1,tax_id_2,t2.name,database_identifier_id_2 FROM OrthologMappingInformation " +
						"JOIN Taxonomy t1 on t1.tax_id = tax_id_1  and t1.type like 'scientific name'  JOIN Taxonomy t2 on t2.tax_id = tax_id_2  and t2.type like 'scientific name' " +
						" where tax_id_1 = %d or tax_id_2 = %d", taxId, taxId
						)
				);
		ResultSet res = stat.getResultSet();
		while(res.next()){
			OrthologDatabaseType orthologDatabase = OrthologDatabaseType.values()[res.getInt(1)];
			Integer taxId1 = res.getInt(2);
			String commonName1 = res.getString(3);
			Species sp1 = new Species(taxId1,commonName1);
			
			Integer taxId2 = res.getInt(5);
			String commonName2 = res.getString(6);
			Species sp2 = new Species(taxId2,commonName2);
			
			OrthologMappingInformation mapping = new OrthologMappingInformation(orthologDatabase,sp1,sp2);
			ret.add(mapping);
			
		}
		res.close();
		
		return ret;
	}

	@Override
	public List<Species> getPossibleTransferSpecies() throws SQLException {
		String sql =String.format(
				"SELECT distinct(tx.tax_id),tax.name from (SELECT distinct(tax_id_1) as tax_id FROM OrthologMappingInformation UNION SELECT distinct(tax_id_2) as tax_id FROM OrthologMappingInformation) as tx JOIN Taxonomy tax on tax.tax_id = tx.tax_id  and type like 'scientific name';"
		);
		Statement stat = connection.createStatement();
		logger.debug(sql);
		stat.execute(sql);
		ResultSet res = stat.getResultSet();
		List<Species> ret = new ArrayList<Species>();
		while(res.next()){
			Integer taxId = res.getInt(1);
			String name = res.getString(2);
			ret.add(new Species(taxId,name));
		}
		
		return ret;
	}


	@Override
	public Species getSpecies(Integer taxId) throws Exception {
		Statement stat = connection.createStatement();
		String sql = String.format("SELECT distinct(tax_id),name FROM Taxonomy where tax_id = %d  and type like 'scientific name' ",taxId);
		logger.debug(sql);
		stat.execute(sql);
	
		Species ret = null;
		ResultSet res = stat.getResultSet();
		if(res.next()){
			
			String name = res.getString(2);
			ret = new Species(taxId,name);
		}
		res.close();
		stat.close();
		return ret;
	}

	@Override
	public List<ContextTreeNode> getChildren(ContextTreeNode parent) throws Exception {
		String sql = String.format
				(
						"SELECT childNode.source_id, childNode.source_name, childNode.context_id, childNode.num_children FROM ContextRelation relation " +
								"JOIN ContextHierachyNode parentNode on parentNode.context_id=relation.parent_context_id " +
								"JOIN ContextHierachyNode childNode on childNode.context_id=relation.context_id " +
								"where relation.parent_context_id = '%d'",
								parent.getContextId()
						);

		logger.debug(sql);
		Statement stat = connection.createStatement();
		stat.execute(sql);
		ResultSet res = stat.getResultSet();
		ArrayList<ContextTreeNode> children = new ArrayList<ContextTreeNode>();

		while(res.next()){
			String term = res.getString(1);
			String name = res.getString(2);
			Integer contextId = res.getInt(3);
			Integer leafs = res.getInt(4);

			ContextTreeNode node = new  ContextTreeNode(contextId,term,name,leafs);

			children.add(node);


		}
		return children;
	}

	@Override
	public ContextTreeNode getContextTreeNode(String sourceId) throws Exception {
		String sql = String.format(" SELECT context_id,context_type_id,source_id,source_name,num_children FROM ContextHierachyNode where source_id like '%s' ", sourceId);
		logger.debug(sql);
		Statement stat = connection.createStatement();
		stat.execute(sql);
		ResultSet res = stat.getResultSet();
		ContextTreeNode ret = null;
		if ( res.next()){
			Integer contextId = res.getInt(1);
			//ContextType type = ContextType.values()[res.getInt(2)];
			//String sourceId = res.getString(2);
			String name = res.getString(4);
			Integer numChildren = res.getInt(5);
			ret = new ContextTreeNode(contextId,sourceId,name,numChildren);
		}
		
		stat.close();
		
		return ret;
	}

	@Override
	public List<ContextTreeNode> getContextTreeNodes(String namenToken) throws Exception {
		String sql = String.format(" SELECT context_id,context_type_id,source_id,source_name,num_children FROM ContextHierachyNode where source_name like '%%%s%%' LIMIT 100 ", namenToken);
		logger.debug(sql);
		Statement stat = connection.createStatement();
		stat.execute(sql);
		ResultSet res = stat.getResultSet();
		List<ContextTreeNode> ret = new ArrayList<ContextTreeNode>();
		while ( res.next()){
			Integer contextId = res.getInt(1);
			//ContextType type = ContextType.values()[res.getInt(2)];
			String sourceId = res.getString(3);
			String name = res.getString(4);
			Integer numChildren = res.getInt(5);
			ContextTreeNode node = new ContextTreeNode(contextId,sourceId,name,numChildren);
			ret.add(node);
		}
		
		stat.close();
		
		return ret;
	}


	@Override
	public List<TFBSPeak> getTFBSBindings(int groupId, Integer contextId) throws Exception {
		String where = null;
		
		if ( contextId != null){
			where = String.format("JOIN GeneContext gc on gc.gene = network.gene1 "
							+ "JOIN GeneContext gc2 on gc2.gene = network.gene2 "
							+ "where gc.context_id = %d and gc2.context_id = %d and group_id = %d",
							contextId,contextId,groupId);
		}else{
			where = String.format("where group_id = %d",groupId);
		}
		String sql =String.format(
				"SELECT gene1,gene2,binding_chr,binding_start,binding_end,binding_p_value,binding_motif,open_chrom_chr,open_chrom_start,open_chrom_end FROM Network2Binding network %s",
				where);
		logger.debug(sql);
		Statement stat = connection.createStatement();
		stat.execute(sql);
		ResultSet res = stat.getResultSet();
		List<TFBSPeak> ret = new ArrayList<TFBSPeak>();
		while ( res.next()){
			String gene1 = res.getString(1);
			String gene2 = res.getString(2);
			String bindingChr = res.getString(3);
			Integer bindingStart = res.getInt(4);
			Integer bindingEnd = res.getInt(5);
			Float bindingPValue = res.getFloat(6);
			String bindingMotifName = res.getString(7);
			//String openChromChr = res.getString(8);
			//Integer openChromStart = res.getInt(9);
			//Integer openChromEnd = res.getInt(10);
			
			TFBSPeak tfbsPeak =new TFBSPeak(bindingChr, gene1, gene2, bindingMotifName, bindingPValue, bindingStart, bindingEnd);
			//Peak openChrom = new Peak(openChromStart,openChromEnd);
			
			///DNaseTFBSPeak dPeak = new DNaseTFBSPeak(tfbsPeak,openChrom);
			ret.add(tfbsPeak);
		}
		
		stat.close();
		
		return ret;
		
	}

	@Override
	public List<Entity> getEntities(Species species,String annotation,ContextTreeNode context)  throws Exception{
		
		
		String sql ="SELECT gene.gene,gene.gene_name, type FROM Gene gene ";
		if ( context != null){
			sql += String.format("JOIN GeneContext context on context.gene = gene.gene and context_id=%d ",context.getContextId());
		}
		sql+=String.format("where tax_id=%d",species.getTaxId());
		if ( annotation != null){
			sql+=String.format(" and type like '%s'", annotation);
		}
		
		logger.debug(sql);
		Statement stat = connection.createStatement();
		stat.execute(sql);
		ResultSet res = stat.getResultSet();
		List<Entity> ret = new ArrayList<Entity>();
		while(res.next()){
			String gene = res.getString(1);
			String name = res.getString(2);
			String type = res.getString(3);
			ret.add(new Entity(gene,name,type));
		}
		
		stat.close();
		return ret;
	}
	public Logger getLogger() {
		return logger;
	}


}
