package de.lmu.ifi.bio.crco.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.bio.crco.util.CroCoLogger;
import de.lmu.ifi.bio.crco.util.NetworkOntology.LeafNode;

public class CroCoNode implements Comparable<CroCoNode>
{
    public interface Filter
    {
        public boolean accept(NetworkHierachyNode nh);
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
    
    public void setName(String name) {
        this.name = name;
    }
    public String getName()
    {
        return name;
    }
    public CroCoNode()
    {
        
    }
    public void setRootParentNames(Set<String> rootParentNames) {
        this.rootParentNames = rootParentNames;
    }
    public void setParent(CroCoNode parent) {
        this.parent = parent;
    }
    public void setChildren(List<CroCoNode> children) {
        this.children = children;
    }
    public void setFromCloned(CroCoNode fromCloned) {
        this.fromCloned = fromCloned;
    }
    public void setChildShowRootChildren(boolean childShowRootChildren) {
        this.childShowRootChildren = childShowRootChildren;
    }
    public void setNetworks(Set<NetworkHierachyNode> networks) {
        this.networks = networks;
    }

    private String name;
    private Set<String> rootParentNames = null;
    private CroCoNode parent;
    private List<CroCoNode> children = null;
    private CroCoNode fromCloned;
   
    
    public CroCoNode getNode(String name, Filter ... filters) throws Exception{
        return CroCoNode.getNode(name,this, filters);
    }
    public static CroCoNode getNode(String name,CroCoNode rootNode,Filter ... filters) throws Exception
    {
        CroCoNode node = new CroCoNode(name);
        
        Set<NetworkHierachyNode> ret = new HashSet<NetworkHierachyNode>(rootNode.getNetworks());
        
        for(Filter filter : filters)
        {
            Set<NetworkHierachyNode> filtered = new HashSet<NetworkHierachyNode>();
            for(NetworkHierachyNode nh :ret)
            {
                if ( filter.accept(nh) ) 
                    filtered.add(nh);
            }
            ret = filtered;
        }
        node.setNetworks(ret);
        
        return node;
        /*
        String[] tokens = path.split("/");

        for(String token : tokens){
            if(token.length() == 0) continue;
            NetworkHierachyNode newRoot = null;
            for(NetworkHierachyNode child : rootNode.getChildren()){
                if ( child.getName().equals(token)) {
                    newRoot = child;
                    break;
                }
            }
            if ( newRoot == null) throw new Exception(String.format("Network not found for path %s stopped at %s (id: %d).",path,token,rootNode.getGroupId()));
            rootNode = newRoot;
        }
        return rootNode;
        */
    }
    
    
    private boolean childShowRootChildren;
    public boolean isChildShowRootChildren() {
        return childShowRootChildren;
    }

    private Set<NetworkHierachyNode> networks;

    public void initChildren(CroCoNode root) throws Exception
    {
        if ( this.getChildren() != null )
        {
            throw new Exception("Children already set");
        }
        
        HashSet<NetworkHierachyNode> nodeNetworks = new HashSet<NetworkHierachyNode>(getNetworks());
        
        List<CroCoNode> possibleChildren = new ArrayList<CroCoNode>();
       
        setChildren( new ArrayList<CroCoNode>());
        if ( isChildShowRootChildren())
        {
            Set<String> parents = getParents();
            for(CroCoNode n : root.getChildren())
            {
                if (! parents.contains(n.getName()))
                    possibleChildren.add(n);


            }
        }else
        {
            for(CroCoNode n : getFromCloned().getChildren())
            {
                possibleChildren.add(n);
            }
        }
        
        for(CroCoNode possibleChild :possibleChildren ){
           
            Set<NetworkHierachyNode> networks = getRelevantNetworks(this,possibleChild);
            
            if ( networks.size() == 0)
                continue;
             nodeNetworks.removeAll(networks);
             CroCoNode child = new CroCoNode(possibleChild);
               
             child.setNetworks(networks);
             child.setParent(this);
             getChildren().add(child);

        }
        if ( nodeNetworks.size() > 0)
        {
            CroCoLogger.debug("Node %s has %d not assigned networks",getName(),nodeNetworks.size() );
            for(NetworkHierachyNode nh : nodeNetworks)
            {
                CroCoNode leaf = new LeafNode(nh.getName(),nh);
                getChildren().add(leaf);
            }
        }
        Collections.sort(getChildren());
    }
    private Set<NetworkHierachyNode> getRelevantNetworks(CroCoNode parent, CroCoNode child)
    {
        HashSet<NetworkHierachyNode> networks = new HashSet<NetworkHierachyNode>();
        
        for(NetworkHierachyNode network : parent.getNetworks())
        {
            if ( child.getNetworks().contains(network)) networks.add(network);
        }
        
        return networks;
    }
    protected CroCoNode(String name)
    {
        this.name = name;
    }
    public CroCoNode getParent()
    {
        return parent;
    }
    public CroCoNode(CroCoNode node)
    {
        this.name = node.name;
        this.fromCloned = node;
        this.childShowRootChildren = node.childShowRootChildren;
    }
    public CroCoNode(String name, CroCoNode parent,boolean childShowRootChildren,Set<NetworkHierachyNode> networks)
    {
        this.parent = parent;
        this.networks = networks;
        this.name = name;
        this.childShowRootChildren = childShowRootChildren;
    }
    
    public CroCoNode(String name, CroCoNode parent,Filter filter, boolean childShowRootChildren,Set<NetworkHierachyNode> networks)
    {
        this.parent = parent;
        this.name = name;
        this.networks = new HashSet<NetworkHierachyNode>();
        this.childShowRootChildren = childShowRootChildren;
        for(NetworkHierachyNode nh : networks)
        {
            if (filter == null ||  filter.accept(nh))
                this.networks.add(nh);
        }
        
    }
    public String toString()
    {
        return String.format(name + "( %d networks)",networks==null?0:networks.size()) ;
    }
    public List<CroCoNode> getChildren()
    {
        return children;
    }
    public Set<String> getParents()
    {
        if (rootParentNames == null)
        {
            rootParentNames = new HashSet<String>();
            
            CroCoNode parent =(CroCoNode) getParent();;
            
            while(parent != null)
            {
                if ( !parent.childShowRootChildren)
                    rootParentNames.add(parent.name);
                
                parent = (CroCoNode)parent.getParent();
            }
        }
        return rootParentNames;
    }
    
    @Override
    public int compareTo(CroCoNode  o) {
        return this.name.compareTo(((CroCoNode) o).name);
    }
    public Set<NetworkHierachyNode> getNetworks() {
        return networks;
    }
    public CroCoNode getFromCloned() {
        return fromCloned;
    }


}