package de.lmu.ifi.bio.croco.operation.ortholog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.lmu.ifi.bio.croco.data.Entity;

public class OrthologMapping{
	private HashMap<Entity,Set<Entity>> orthologMapping;
	private OrthologMappingInformation oi;
	
	public HashMap<Entity,Set<Entity>>  getMapping(){
		return orthologMapping;
	}
	public Set<Entity> getOrthologs(Entity e){
		return orthologMapping.get(e);
	}
	public int getSize(){
		return orthologMapping.size();
	}
	public OrthologMapping(OrthologMappingInformation oi)
	{
	    this.oi = oi;
	}
	public OrthologMappingInformation getOrthologMappingInformation()
	{
	    return oi;
	}
	public void addMapping(Entity e1, Entity e2){
		if ( orthologMapping == null){
			orthologMapping = new HashMap<Entity,Set<Entity>>();
		}
		if (! orthologMapping.containsKey(e1)){
			orthologMapping.put(e1, new HashSet<Entity>());
		}
		if (! orthologMapping.containsKey(e2)){
			orthologMapping.put(e2, new HashSet<Entity>());
		}
	
		orthologMapping.get(e1).add(e2);
		orthologMapping.get(e2).add(e1);
	}
}
