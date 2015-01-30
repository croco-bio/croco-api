package de.lmu.ifi.bio.crco.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CroCoNode implements Comparable<CroCoNode>
{
    public interface Filter
    {
        public boolean accept(NetworkHierachyNode nh);
    }
    
    public String name;
    public Set<String> rootParentNames = null;
    public CroCoNode parent;
    public List<CroCoNode> children = null;
    public CroCoNode fromCloned;
    
    public boolean childShowRootChildren;
    public Set<NetworkHierachyNode> networks;

    /*
    public CroCoNode(String name, CroCoNode parent,Filter filter,boolean generalNode)
    {
        super(name, new ArrayList<DefaultTreeNode<String>>());
        this.name = name;
    }
    */
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

}