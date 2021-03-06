package de.lmu.ifi.bio.croco.util.ontology;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;

import de.lmu.ifi.bio.croco.data.CroCoNode;
import de.lmu.ifi.bio.croco.data.Identifiable;
import de.lmu.ifi.bio.croco.util.CroCoLogger;
import de.lmu.ifi.bio.croco.util.FileUtil;


public class Obo {
    Pattern TERM = Pattern.compile("\\[(\\w+)\\]");
    Pattern SEP = Pattern.compile(":");
    Pattern SYNONYM = Pattern.compile("\"([^\"]+)\"");
    
    HashMap<String,OboElement> elements = new HashMap<String,OboElement>();
    
    public enum OboField
    {
        //Term fields
        ID("id"),
        NAME("name"),
        SYNONYM("synonym"),
        IS_A("is_a"),
        relationship("relationship"),
        alt_id("alt_id")
        ;
        String name;
        OboField(String name)
        {
            this.name=name;
        }

     
        public String getName()
        {
            return name;
        }
        
    }

    /**
     * Reads an ontology with data mapping
     * @param ontology -- the obo  
     * @param idMapping -- the id mapping file
     * @param allData -- data assigned to the nodes in the ontology
     * @return the root node
     * @throws IOException
     */
    public static<E extends Identifiable>  CroCoNode<E> readOntology(File ontology, File idMapping, Set<E> allData ) throws Exception
    {
        
        //read the obo
        Obo obo = new Obo(ontology);
        
        HashMap<String,CroCoNode<E>> idToCroCoNode = new HashMap<String,CroCoNode<E>>();
        HashMap<String,List<String>> idToParents = new HashMap<String,List<String>>();
        
        int k = 0;
        for(OboElement el : obo.elements.values())
        {
            CroCoNode<E> node = new CroCoNode<E>(el.id,el.name);
            
            if ( idToCroCoNode.containsKey(el.id))
            {
                throw new RuntimeException(el.id + " not unique!");
            }
            
            idToCroCoNode.put(el.id, node);
            
            idToParents.put(el.id, new ArrayList<String>());
            for(OboElement parent : el.getParents() )
            {
                idToParents.get(el.id).add(parent.id);
            }
            k++;
        }
        CroCoLogger.debug("Found %d ontology nodes.",k);

        List<OboElement> roots = obo.getRoots(false);
        if ( roots.size() != 1) 
            throw new IOException("Invalid obo. Expect only one root.");
        
        String rootId  = roots.get(0).id;
        
        CroCoNode<E> root = idToCroCoNode.get(rootId);
        if ( root == null)
            throw new RuntimeException("Cannot find root!");
        
        root.setData(allData);
        
        //assign data points
        HashMap<String,E> idToDataPoint = new HashMap<String,E>();
        
        for(E dataPoint : root.getData() )
        {
            idToDataPoint.put(dataPoint.getId(), dataPoint);
        }
        Iterator<String> it = FileUtil.getLineIterator(idMapping);
        while(it.hasNext())
        {
            String line = it.next();
            String tokens[] = line.split("\\s+");
            
            String id = tokens[0];
            CroCoNode<E> node = idToCroCoNode.get(id);
           
            if ( node == null)
                throw new Exception("Unknown ontology node:" + id + "(invalid obo mapping)");
            
            HashSet<E> data  = new HashSet<E>();
            for(int i = 1 ; i< tokens.length; i++)
            {
                E dataPoint = idToDataPoint.get(tokens[i]);
                if ( dataPoint != null)
                    data.add(dataPoint);
            }
            
            node.setData(data);
        }
        
        //set parent-children relations
        for(String id : idToParents.keySet())
        {
            CroCoNode<E> node = idToCroCoNode.get(id);
            
            if ( node.getData().size() == 0)
                continue;
            
            List<String> parents = idToParents.get(id);
            
            if ( parents.size() == 0 && !id.equals(rootId))
            {
                node.setParent(root);
                continue;
            }
            
            for(String parentId : parents)
            {
                CroCoNode<E> parent = idToCroCoNode.get(parentId);
                if ( parent.getChildren() == null)
                {
                    node.setParent(parent);
                } else
                {
                    node.setParent(parent);
                }    
            }
        }
        
        
        return root;
    }
    
    /**
     * Writes the ontology to file
     * @param root -- the root node
     * @param oboOut -- obo out file 
     * @param oboMapping -- entity to obo mapping
     * @throws IOException
     */
    public static<E extends Identifiable>  void writeOntology(CroCoNode<E> root, File oboOut, File oboMapping ) throws IOException
    {
        CroCoLogger.getLogger().debug("Write obo:" + oboOut);
        
        PrintWriter pw = FileUtil.getPrintWriter(oboOut);
        CroCoNode.printAsObo(pw, root);
        pw.close();
        

        CroCoLogger.getLogger().debug("Write mapping:" + oboMapping);
        
        pw = FileUtil.getPrintWriter(oboMapping);
        List<CroCoNode<E>> elements = new ArrayList<CroCoNode<E>>();
        
        elements.addAll(root.getAllChildren());
        
        for(CroCoNode<E> el : elements)
        {
            List<String> ids = new ArrayList<String>();
            for(Identifiable node : el.getData())
            {
                ids.add(node.getId());
            }
            
            if ( el.getId().contains(" "))
                throw new RuntimeException("Ontology ids with ' ' not permitted." + el.getId());
            
            pw.printf("%s %s\n",el.getId(),Joiner.on(" ").join(ids));
        }
        pw.close();
       
    }
    
    public class OboElement
    {
        public List<OboElement> parents = new ArrayList<OboElement>();
        public List<OboElement> children = new ArrayList<OboElement>();
        
        public String id = null;
        
        public List<String> altIds = new ArrayList<String>();
        public String name = null;
     
        public List<String> synonym = new ArrayList<String>();
        
        @Override
        public String toString()
        {
            return name;
        }
        public OboElement getRoot()
        {
            Stack<OboElement> stack = new Stack<OboElement>();
            stack.add(this);
            while(!stack.isEmpty())
            {
                OboElement top = stack.pop();
                if ( top.parents.size() == 0){
                    return top;
                }
                for(OboElement parent : top.parents)
                {
                    stack.add(parent);
                }
            }
            return null;
        }
        
        public List<OboElement> getAllChildren() {
            Set<OboElement> ret = new HashSet<OboElement>();
            
            Stack<OboElement> stack = new Stack<OboElement>();
            stack.add(this);
            
            while(!stack.isEmpty())
            {
                OboElement top = stack.pop();
                
                ret.add(top);
                
                if ( top.children != null)
                {
                    for(OboElement child : top.children)
                    {
                        stack.add(child);
                    }
                }
            }
            return new ArrayList<OboElement>(ret);
        }

        public List<OboElement> getParents() {
            return parents;
        }

        public List<OboElement> getChildren() {
            return children;
        }

        public List<String> getSynonyms() {
            return synonym;
        }
        public String getId() {
            return id;
        }
        
    }

    public Collection<OboElement> getElements() {
        return elements.values();
    }
    public OboElement getElement(String id)
    {
        return elements.get(id);
    }
    
    public List<OboElement> getRoots(boolean filterSingleton)
    {
        List<OboElement> roots = new ArrayList<OboElement>();
        
        for(OboElement el : elements.values())
        {
            if (el.getParents().size() == 0)
            {
                if ( filterSingleton &&  el.getChildren().size() == 0)
                    continue;
                
                roots.add(el);
                
            }
        }
        
        return roots;
    }
    
    
    public Obo(File file) throws IOException
    {
        Iterator<String> it = FileUtil.getLineIterator(file);
        
        OboElement current = null;
        
        HashMap<String,String> relations = new HashMap<String,String>();
        List<String> additionalReferences = new ArrayList<String>();
        
        List<OboElement> el = new ArrayList<OboElement>();
        String type = null;
        while(it.hasNext())
        {
            String line = it.next();
            
            if ( line.trim().length() ==0)
                continue;
            
            java.util.regex.Matcher matcher = TERM.matcher(line);
            if (matcher.matches())
            {
                
                type = matcher.group(1);
                
                if ( current != null && current.id != null)
                {
                    if ( current.id == null)
                        throw new RuntimeException(line);

                    if (! relations.containsKey(current.id) && additionalReferences.size() > 0)
                    {
                        for(String add: additionalReferences)
                        {
                            relations.put(current.id, add);     
                            
                        }
                    }
                    if ( current.id.contains(","))
                        throw new RuntimeException("Not possible:");
                    el.add(current);
                    elements.put(current.id,current);
                    for(String altId : current.altIds)
                    {
                        elements.put(altId,current);
                    }
                }
                additionalReferences = new ArrayList<String>();
                current = new OboElement();
                continue;
            }
            
            if ( type != null && type.equals("Term"))
            {
                String[] tokens = SEP.split(line,2);
                String key = tokens[0];
               
                String value = tokens[1].trim();
                if ( OboField.alt_id.name.equals(key))
                {
                   current.altIds.add(value);
                }
                if ( OboField.ID.name.equals(key))
                {
                    current.id = value;
                }else if ( OboField.NAME.name.equals(key))
                {
                    current.name = value;
                }else if ( OboField.IS_A.name.equals(key))
                {
                    relations.put(current.id, value.split("!")[0].trim());
                }else if ( OboField.SYNONYM.name.equals(key))
                {
                    matcher = SYNONYM.matcher(line);
                    if ( matcher.find())
                    {
                        current.synonym.add(matcher.group(1));
                        
                    }else
                    {
                        throw new RuntimeException("Incorret line:" + line);
                    }
                }else if ( OboField.relationship.name.equals(key))
                {
                       String[] ref =  value.split("\\s+");
                       if ( ref[0].trim().equals("part_of"))
                       {
                           relations.put(current.id, ref[1].trim());     
                       }else{
                           additionalReferences.add(ref[1].trim());
                        //   additionalReferences.put(current.id, ref[1].trim());     
                       }
                       
                   
                }
            }
        
        }
        if ( current.id != null)
        {
            if ( current.id.contains(","))
                throw new RuntimeException("Not possible:");
            el.add(current);
            elements.put(current.id,current);
        }
        
        for(OboElement element :el)
        {
            if ( relations.containsKey(element.id))
            {
                OboElement parent = elements.get(relations.get(element.id));
                 //String parentId = is_a_relations.get(element.id);
                 parent.children.add(element);
                 element.parents.add(parent);
                 
                 continue;
            }
     
        }
    }
    public static void main(String[] args) throws Exception
    {
         Obo reader = new Obo(new File("/home/proj/pesch/SFB/taxIds.obo"));
      //  OboReader reader = new OboReader(new File("/mnt/biostor1/Data/PubMed/Synonyms/obo/BrendaTissueOBO_web.obo"));
        
         for(OboElement e : reader.getElements())
         {
             System.out.println(e.id);
         }
    }
    
  
}
