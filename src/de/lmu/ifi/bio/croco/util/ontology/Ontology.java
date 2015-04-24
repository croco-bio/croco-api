package de.lmu.ifi.bio.croco.util.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.lmu.ifi.bio.croco.data.CroCoNode;
import de.lmu.ifi.bio.croco.data.Identifiable;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
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
    public static<E  extends Identifiable> void addObo( HashMap<OboElement,HashSet<E>> elementsToNetwork,CroCoNode<E> root , OboElement rootElement,boolean makeSlim)
    {
        addObo(elementsToNetwork,root,rootElement, new Name(),makeSlim);
    }
    public static<E  extends Identifiable> void addObo( HashMap<OboElement,HashSet<E>> elementsToNetwork,CroCoNode<E> root , OboElement rootElement,LabelExtraction nameExtraction, boolean makeSlim)
    {
        //propagate annotations
        LinkedList<CroCoNode<E>> par = new LinkedList<CroCoNode<E>>();
        LinkedList<OboElement> list = new LinkedList<OboElement>();
        
        list.add(rootElement);
        par.add(root);

        HashSet<OboElement> proc = new HashSet<OboElement>();
        while(!list.isEmpty())
        {
            OboElement top = list.removeFirst();
            CroCoNode<E> nodeParent = par.removeFirst();
            boolean canBeProc = true;
            for(OboElement parent : top.parents)
            {
                if (! proc.contains(parent))
                    canBeProc= false;
            }
            if ( !top.id.equals(rootElement.id) && !canBeProc)
            {
                list.add(top);
                par.add(nodeParent);
                continue;
            }
            List<OboElement> allChildren = top.getAllChildren();
            Set<E> networks = new HashSet<E>();
            for(OboElement child : allChildren)
            {
      
                if (! elementsToNetwork.containsKey(child))
                    continue;
                networks.addAll(elementsToNetwork.get(child));
            }
            if ( networks.size() == 0)
                continue;
            
            String name = nameExtraction.getName(top);//top.name;
        
            CroCoNode<E> node = new CroCoNode<E>(top.getId(),name,nodeParent,networks);
            proc.add(top);
            
            for(OboElement child : top.children)
            {
                par.add(node);
                list.add(child);
            }
            
        }
        if ( makeSlim)
            makeSlim(root);
        
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
               
            
            if ( top.getChildren() == null)
                continue;
                
            if ( top.getChildren().size() == 1  && top != node)
            {
                
                List<CroCoNode<E>> parents = top.getParent();
                CroCoNode<E> child = top.getChildren().get(0);
                
                for(CroCoNode<E> parent : parents)
                {
                    parent.getChildren().remove(top);
                    child.setParent(parent);
                    child.getParent().remove(top);
                }

            }
            
           
            for(CroCoNode<E> child : top.getChildren())
            {
                stack.add(child);
            }
        }
    }

    
}
