package de.lmu.ifi.bio.croco.util.ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.lmu.ifi.bio.croco.data.CroCoNode;
import de.lmu.ifi.bio.croco.data.Identifiable;
import de.lmu.ifi.bio.croco.util.ontology.Obo.OboElement;

public class Ontology {
    
    
    public static interface LabelExtraction
    {
        public String getName(OboElement element);
    }
    
    static public class Name implements LabelExtraction
    {
        @Override
        public String getName(OboElement element) {
            return element.name;
        }
        
    }
    public static<E  extends Identifiable> CroCoNode<E> toOntology( HashMap<OboElement,HashSet<E>> elementsToNetwork,OboElement rootElement,Obo obo, boolean makeSlim)
    {
        return toOntology(elementsToNetwork,rootElement,obo, new Name(),makeSlim);
    }
    public static<E  extends Identifiable> CroCoNode<E>  toOntology( HashMap<OboElement,HashSet<E>> elementsToNetwork,OboElement rootElement,Obo obo, LabelExtraction nameExtraction, boolean makeSlim)
    {
        CroCoNode<E> root = new CroCoNode<E>(rootElement.getId(),nameExtraction.getName(rootElement),null,null);
        
        LinkedList<CroCoNode<E>> list = new LinkedList<CroCoNode<E>>();
        
        list.add(root);

        HashSet<String> proc = new HashSet<String>();
        while(!list.isEmpty())
        {
            CroCoNode<E> node = list.removeFirst();
            OboElement oboElement = obo.getElement(node.getId());
          //  System.out.println(node);
            
            boolean canBeProc = true;
           
            if ( node.getParent() != null)
            {
                for(CroCoNode<E> parent : node.getParent())
                {
                    if (! proc.contains(parent.getId()))
                        canBeProc= false;
                } 
            }
           
            if (!canBeProc)
            {
                list.add(node);
                continue;
            }
          
            //propagate annotations
            List<OboElement> allChildren = oboElement.getAllChildren();
            Set<E> networks = new HashSet<E>();
            for(OboElement child : allChildren)
            {
                if (! elementsToNetwork.containsKey(child))
                    continue;
                networks.addAll(elementsToNetwork.get(child));
            }
            node.setData(networks);
            
            if ( networks.size() == 0){
                
                if ( node.getParent() != null)
                {
                    for(CroCoNode<E> parent : node.getParent())
                    {
                        parent.getChildren().remove(node);
                        
                    }
                }
                continue;
            }
        
            proc.add(node.getId());
            
            for(OboElement child : oboElement.children)
            {
                list.add(new CroCoNode<E>(child.getId(),nameExtraction.getName(child),node,null));
            }
            
        }
        if ( makeSlim)
            makeSlim(root);
        
        return root;
        
    }
    public static<E extends Identifiable> void makeSlim(CroCoNode<E> node)
    {
      //  CroCoLogger.getLogger().debug("Make slim");
        HashSet<CroCoNode<E>> p = new HashSet<CroCoNode<E>>();
        Stack<CroCoNode<E>> stack = new Stack<CroCoNode<E>>();
        stack.add(node);
        while(!stack.isEmpty())
        {
            CroCoNode<E> top = stack.pop();
            
            if ( p.contains(top))
                continue;
            
            p.add(top);
               
            if ( top.getData().size() == 0)
            {
                List<CroCoNode<E>> parents = top.getParent();
                if ( parents != null)
                {
                    for(CroCoNode<E> parent : parents)
                    {
                        parent.getChildren().remove(top);
                    }
                }
                
                continue;
            }
            
            if ( top.getChildren() == null)
                continue;
            
            if ( top.getChildren().size() == 1 )
            {
                
                CroCoNode<E> child = top.getChildren().get(0);
                if ( child.getChildren() != null)
                {
                    for(CroCoNode<E> cc : child.getChildren())
                    {
                       cc.setParent(top); 
                       cc.getParent().remove(child);
                    }
                }
                
                top.getChildren().remove(child);
                p.remove(top);
                stack.add(top);
                continue;
                /*
                List<CroCoNode<E>> parents = top.getParent();
                
                for(CroCoNode<E> parent : parents)
                {
                    parent.getChildren().remove(top);
                    child.setParent(parent);
                    child.getParent().remove(top);
                }
                */
            }
            
           
            for(CroCoNode<E> child : top.getChildren())
            {
                stack.add(child);
            }
        }
    }

    
}
