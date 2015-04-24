package de.lmu.ifi.bio.croco.util.ontology;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

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
         Obo reader = new Obo(new File("/home/users/pesch/workspace/EWMS/taxIds.obo"));
      //  OboReader reader = new OboReader(new File("/mnt/biostor1/Data/PubMed/Synonyms/obo/BrendaTissueOBO_web.obo"));
        
        OboElement element =reader.getElement("NCBITaxon:10090");
        System.out.println(element );
        
        System.out.println(element.getParents());
        
        System.out.println(element.getRoot());
    }
    
  
}
