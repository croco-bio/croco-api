package de.lmu.ifi.bio.croco.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.ontology.NetworkOntology.LeafNode;

public class CroCoNode<E> implements Comparable<CroCoNode<E>>
{
    public interface Filter<E>
    {
        public boolean accept(E nh);
    }
    
    static public class FactorFilter implements Filter<NetworkHierachyNode>
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
    
    static public class NameFilter implements Filter<NetworkHierachyNode>
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
    
    static public class GeneralFilter implements Filter<NetworkHierachyNode>
    {
        Option option;
        String value;
        boolean debug = false;
        
        public GeneralFilter(Option option, String value,boolean debug)
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
                return nh.getOptions().get(option).toLowerCase().equals(value.toLowerCase());
            
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
    public void setParent(CroCoNode<E> parent) {
        this.parent = parent;
    }
    public void setChildren(List<CroCoNode<E>> children) {
        this.children = children;
    }
    public void setFromCloned(CroCoNode<E> fromCloned) {
        this.fromCloned = fromCloned;
    }
    public void setChildShowRootChildren(boolean childShowRootChildren) {
        this.childShowRootChildren = childShowRootChildren;
    }
    public void setData(Set<E> data) {
        this.data = data;
    }

    private String name;
    private Set<String> rootParentNames = null;
    private CroCoNode<E> parent;
    private List<CroCoNode<E>> children = null;
    private CroCoNode<E> fromCloned;
   
    
    public CroCoNode<E> getNode(String name, Filter<E> ... filters) throws Exception{
        return CroCoNode.getNode(name,this, filters);
    } 
    public static<E> CroCoNode<E> getNode(String name,CroCoNode<E> rootNode,Filter<E> ... filters) throws Exception
    {
        CroCoNode<E> node = new CroCoNode<E>(name);
        
        Set<E> ret = new HashSet<E>(rootNode.getData());
        
        for(Filter<E> filter : filters)
        {
            Set<E> filtered = new HashSet<E>();
            for(E nh :ret)
            {
                if ( filter.accept(nh) ) 
                    filtered.add(nh);
            }
            ret = filtered;
        }
        node.setData(ret);
        
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

    private Set<E> data;

    public void initChildren(CroCoNode<E> root) throws Exception
    {
        if ( this.getChildren() != null )
        {
            throw new Exception("Children already set");
        }
        
        HashSet<E> nodeNetworks = new HashSet<E>(getData());
        
        List<CroCoNode<E>> possibleChildren = new ArrayList<CroCoNode<E>>();
       
        setChildren( new ArrayList<CroCoNode<E>>());
        if ( isChildShowRootChildren())
        {
            Set<String> parents = getParents();
            for(CroCoNode<E> n : root.getChildren())
            {
                if (! parents.contains(n.getName()))
                    possibleChildren.add(n);
            }
        }else
        {
            for(CroCoNode<E> n : getFromCloned().getChildren())
            {
                possibleChildren.add(n);
            }
        }
        
        for(CroCoNode<E> possibleChild :possibleChildren ){
           
            Set<E> data = getRelevantData(this,possibleChild);
            
            if ( data.size() == 0)
                continue;
             nodeNetworks.removeAll(data);
             CroCoNode<E> child = new CroCoNode<E>(possibleChild);
               
             child.setData(data);
             child.setParent(this);
             getChildren().add(child);

        }
        
        if (isChildShowRootChildren() && nodeNetworks.size() > 0)
        {
            CroCoLogger.debug("Node %s has %d not assigned networks",getName(),nodeNetworks.size() );
            for(E nh : nodeNetworks)
            {
                CroCoNode<E> leaf = new LeafNode<E>(nh.toString(),nh);
                getChildren().add(leaf);
            }
        }
        else if ( nodeNetworks.size() > 0)
        {
            Set<String> parents = getParents();
            possibleChildren.clear();
            for(CroCoNode<E> n : root.getChildren())
            {
                if (! parents.contains(n.getName()))
                    possibleChildren.add(n);
            }
           
            for(CroCoNode<E> possibleChild :possibleChildren ){
                
                Set<E> data = getRelevantData(this,possibleChild);
                
                if ( data.size() == 0)
                    continue;
                 nodeNetworks.removeAll(data);
                 CroCoNode<E> child = new CroCoNode<E>(possibleChild);
                   
                 child.setData(data);
                 child.setParent(this);
                 getChildren().add(child);

            }
            
            if ( nodeNetworks.size()> 0)
            {
                CroCoLogger.info("Node %s has %d not assigned networks",getName(),nodeNetworks.size() );
                for(E nh : nodeNetworks)
                {
                    CroCoNode<E> leaf = new LeafNode<E>(nh.toString(),nh);
                    getChildren().add(leaf);
                }
            }
        }
        Collections.sort(getChildren());
    }
    private Set<E> getRelevantData(CroCoNode<E> parent, CroCoNode<E> child)
    {
        HashSet<E> data = new HashSet<E>();
        
        for(E d : parent.getData())
        {
            if ( child.getData().contains(d)) data.add(d);
        }
        
        return data;
    }
    protected CroCoNode(String name)
    {
        this.name = name;
    }
    public CroCoNode<E> getParent()
    {
        return parent;
    }
    public CroCoNode(CroCoNode<E> node)
    {
        if ( node == null)
            throw new RuntimeException("Node cannot be null (init failed).");
        this.name = node.name;
        this.fromCloned = node;
        this.childShowRootChildren = node.childShowRootChildren;
    }
    public CroCoNode(String name, CroCoNode<E> parent,boolean childShowRootChildren,Set<E> data)
    {
        this.parent = parent;
        this.data = data;
        this.name = name;
        this.childShowRootChildren = childShowRootChildren;
    }
    
    public CroCoNode(String name, CroCoNode<E> parent,Filter<E> filter, boolean childShowRootChildren,Set<E> data)
    {
        this.parent = parent;
        this.name = name;
        this.data = new HashSet<E>();
        this.childShowRootChildren = childShowRootChildren;
        for(E nh : data)
        {
            if (filter == null ||  filter.accept(nh))
                this.data.add(nh);
        }
        
    }
    public String toString()
    {
        return String.format(name + "( %d data)",data==null?0:data.size()) ;
    }
    public List<CroCoNode<E>> getChildren()
    {
        return children;
    }
    public Set<String> getParents()
    {
        if (rootParentNames == null)
        {
            rootParentNames = new HashSet<String>();
            
            CroCoNode<E> parent =(CroCoNode<E>) getParent();;
            
            while(parent != null)
            {
                if ( !parent.childShowRootChildren)
                    rootParentNames.add(parent.name);
                
                parent = (CroCoNode<E>)parent.getParent();
            }
        }
        return rootParentNames;
    }
    
    @Override
    public int compareTo(CroCoNode<E>  o) {
        return this.name.compareTo(((CroCoNode<E>) o).name);
    }
    public Set<E> getData() {
        return data;
    }
    public CroCoNode<E> getFromCloned() {
        return fromCloned;
    }


}