package de.lmu.ifi.bio.croco.data;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.ontology.NetworkOntology.LeafNode;

/**
 * Ontology node
 * @author pesch
 *
 * @param <E> -- elements assigned to ontology node
 */
public class CroCoNode<E extends Identifiable> implements Comparable<CroCoNode<E>>
{
    private String name;
    private String id;
    private Set<String> rootParentNames = null;
    private List<CroCoNode<E>> parents;
    private List<CroCoNode<E>> children = null;
    private CroCoNode<E> fromCloned;
    
    public interface Filter<E>
    {
        public boolean accept(E nh);
    }
    
    static public class FactorFilter implements Filter<NetworkMetaInformation>
    {
        Set<String> factors = null;
        public FactorFilter(Set<String> factors)
        {
            this.factors = factors;
        }
        @Override
        public boolean accept(NetworkMetaInformation nh) {
            for(String factor : factors)
            {
                if ( nh.getFactors().contains(factor)) return true;
            }
            return false;
        }
        
    }
    
    static public class NameFilter implements Filter<NetworkMetaInformation>
    {
        String name;
        
        public NameFilter(String name)
        {
            this.name = name;
        }

        @Override
        public boolean accept(NetworkMetaInformation nh) {
            return nh.getName().equals(name);
        }
    }
    
    static public class GeneralFilter implements Filter<NetworkMetaInformation>
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
        public boolean accept(NetworkMetaInformation nh) {
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
        if ( name == null)
            return id;
        
        return name;
    }
    
    public void setRootParentNames(Set<String> rootParentNames) {
        this.rootParentNames = rootParentNames;
    }
    
    public CroCoNode<E> getChild(String id)
    {
        for(CroCoNode<E> c : this.getAllChildren())
        {
            if ( id.equals(c.id)) 
                return c;
        }
        
        return null;
    }
    
    public Set<CroCoNode<E>> getAllChildren()
    {
        HashSet<CroCoNode<E>> ret = new HashSet<CroCoNode<E>> ();
        Stack<CroCoNode<E>> stack = new Stack<CroCoNode<E>>();
        stack.add(this);
        
        while(!stack.isEmpty())
        {
            CroCoNode<E> top = stack.pop();
            if ( ret.contains(top)) continue;
            
            ret.add(top);
            
            if ( top.getChildren() == null) continue;
            
            for(  CroCoNode<E> child: top.getChildren())
            {
                stack.add(child);
            }
            
        }
        
        return ret;
        
    }
    
    public void setParent(CroCoNode<E> parent) {
        if ( parent == null)
            return;
        
        if ( parent.getChildren() == null)
        {
            parent.setChildren(new ArrayList<CroCoNode<E>>());
        }
        parent.getChildren().add(this);
        
        if ( this.parents == null)
            this.parents = new ArrayList<CroCoNode<E>>();
        
        this.parents.add(parent);
    }
    public void setChildren(List<CroCoNode<E>> children) {
        this.children = children;
    }
    public void setFromCloned(CroCoNode<E> fromCloned) {
        this.fromCloned = fromCloned;
    }

    public void setData(Set<E> data) {
        this.data = data;
    }

    public Set<E> getData(String id, Filter<E> ... filters) throws Exception{
        return CroCoNode.getData(id,this, filters);
    } 
    public static<E extends Identifiable> Set<E> getData(String id,CroCoNode<E> rootNode,Filter<E> ... filters) throws Exception
    {
        Set<E> ret = new HashSet<E>(rootNode.getData());
        
        for(Filter<E> filter : filters)
        {
            Set<E> filtered = new HashSet<E>();
            for(E nh :ret)
            {
                if ( filter.accept(nh) ) 
                    filtered.add(nh);
            }
            ret.addAll(filtered);
        }
        
        return ret;

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
        
        //list possible children
        if ( getFromCloned().getChildren() != null)
        {
            for(CroCoNode<E> n : getFromCloned().getChildren())
            {
                possibleChildren.add(n);
            }
        }

        //add possible children
        for(CroCoNode<E> possibleChild :possibleChildren ){
           
            Set<E> data = getRelevantData(this,possibleChild);
            if ( data.size() == 0)
               continue;
            
             nodeNetworks.removeAll(data);
             CroCoNode<E> child = new CroCoNode<E>(possibleChild);
               
             child.setData(data);
             child.setParent(this);

        }

        if ( nodeNetworks.size() > 0)
        {
            Set<String> parents = getParents(root);
            possibleChildren.clear();
           
            //show dimensions with >2 data points
            for(CroCoNode<E> n : root.getChildren())
            {
                if (! parents.contains(n.getId()) && !n.id.equals(this.getId()) )
                {
                    Set<E> data = getRelevantData(this,n);
                    
                    //at least one data point
                    if ( data.size() == 0)
                        continue;
                    
                    //dimension separates the data
                    if (!canBeSeperated(n,this.getData()) )
                        continue;
                    
                     nodeNetworks.removeAll(data);
                     CroCoNode<E> child = new CroCoNode<E>(n);
                       
                     child.setData(data);
                     child.setParent(this);
                }
            }
        }
        //no further categorization is possible
        if ( nodeNetworks.size()> 0)
        {
            CroCoLogger.info("Node %s has %d not assigned networks",getName(),nodeNetworks.size() );
            for(E nh : nodeNetworks)
            {
                CroCoNode<E> leaf = new LeafNode<E>(nh.getId(),nh.toString(),nh);
                leaf.setParent(this);
            }
        }
        
        Collections.sort(getChildren());
    }
    private boolean canBeSeperated(CroCoNode<E> node, Set<E> data)
    {
        for ( CroCoNode<E> c: node.getAllChildren())
        {
            int overlap = 0;
            for(E d : data)
            {
                if ( c.getData().contains(d))
                    overlap++;
            }
            //at least one node exists, which is not empty, and does not include all data points.
            if (overlap > 0 &&  overlap != data.size()) return true;
            
            
        }
        return false;
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
    public CroCoNode(String id, String name)
    {
        this.id = id;
        this.name = name;
    }
    public List<CroCoNode<E>> getParent()
    {
        return this.parents;
    }
    
    //added for xstream
    public CroCoNode(){}
    
    public CroCoNode(CroCoNode<E> node)
    {
        if ( node == null)
            throw new RuntimeException("Node cannot be null (init failed).");
        this.name = node.name;
        this.id = node.id;
        this.fromCloned = node;
    }
    
    public CroCoNode(String id, String name, CroCoNode<E> parent,Set<E> data)
    {
        this(id,name,parent,null,data);
    }
    
    public CroCoNode(String id,String name,CroCoNode<E> parent,Filter<E> filter, Set<E> data)
    {

        this.setParent(parent);
        this.id = id;
        this.name = name;
        if ( filter == null)
        {
            this.data= data;
        }
        else
        {
            this.data = new HashSet<E>();
            for(E nh : data)
            {
                if ( filter.accept(nh))
                    this.data.add(nh);
            }
        }
        
        
    }
    public String toString()
    {
        return String.format("%s (%d data)",this.getName(),data==null?0:data.size()) ;
    }
    public List<CroCoNode<E>> getChildren()
    {
        return children;
    }
    public Set<String> getParents(CroCoNode<E> root)
    {
        HashSet<String> dimensions = new HashSet<String>();
        
        for(CroCoNode<E> c : root.getChildren())
        {
            dimensions.add(c.getId());
        }
        
        if (rootParentNames == null)
        {
            rootParentNames = new HashSet<String>();
            
            List<CroCoNode<E>> parents = this.parents;
            
            while(parents != null)
            {
                for(CroCoNode<E> parent : parents)
                {
                    if ( dimensions.contains(parent.getId()))
                        rootParentNames.add(parent.getId());
                    
                    parents = parent.getParent();
                        
                }
            }
        }
        return rootParentNames;
    }
    
    @Override
    public int compareTo(CroCoNode<E>  o) {
        return this.getName().toUpperCase().compareTo(((CroCoNode<E>) o).getName().toUpperCase());
    }
    public Set<E> getData() {
        return data;
    }
    public CroCoNode<E> getFromCloned() {
        return fromCloned;
    }
    public String getId() {
        return id;
    }
    
    public static<E extends Identifiable> void printAsObo(PrintWriter pw, CroCoNode<E> root) 
    {
        for(CroCoNode<E> c : root.getAllChildren() )
        {
            pw.printf("[Term]\n");
            pw.printf("id: %s\n", c.getId());
            pw.printf("name: %s\n", c.getName());
            if ( c.getParent() != null)
            {
                for(CroCoNode<E> p : c.getParent())
                {
                    pw.printf("is_a: %s ! %s\n", p.getId(),p.getName());
                }    
            }
            pw.print("\n");
        }
        
    }
    public void clearChildren() {
        if ( this.getChildren() != null)
        {
            for(CroCoNode<E> c : this.getChildren())
            {
                if ( c.getParent() != null)
                    c.getParent().remove(this);
                
                this.getChildren().remove(c);
            }
        }
    }

    public static<E extends Identifiable>  void  addOntologyNodes(CroCoNode<E> root, String id,String name,List<CroCoNode<E>> children )
    {
        
        Set<E> data = new HashSet<E>();

        CroCoNode<E> node = root;

        if ( name != null)
            node = new  CroCoNode<E>(id,name,root,null);

        for(CroCoNode<E> c : children)
        {
            if ( c.getData().size() > 0)
            {
                data.addAll(c.getData());
                c.setParent(node);
            }
        }
        node.setData(data);

        if ( node.getChildren().size() == 1)
        {
            CroCoNode<E> firstChild = node.getChildren().get(0);
            for(CroCoNode<E> c : firstChild.getChildren())
            {
                c.getParent().clear();
                c.setParent(node);
            }
            node.getChildren().remove(firstChild);
            firstChild.getParent().clear();
        }
        
        if ( root.getData() == null )
        {
            root.setData(data);
        }else
        {
            root.getData().addAll(data);
        }
        
    }

}